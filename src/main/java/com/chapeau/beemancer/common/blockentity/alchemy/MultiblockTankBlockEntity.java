/**
 * ============================================================
 * [MultiblockTankBlockEntity.java]
 * Description: BlockEntity pour tank multi-bloc dynamique
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.menu.alchemy.MultiblockTankMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class MultiblockTankBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY_PER_BLOCK = 8000;

    // Master position (null = this is master)
    @Nullable
    private BlockPos masterPos = null;

    // Only for master: connected blocks and fluid storage
    private final Set<BlockPos> connectedBlocks = new HashSet<>();
    private FluidTank fluidTank;
    private boolean validCuboid = true;

    // Bucket slot (only used by master for GUI)
    protected final ItemStackHandler bucketSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isBucketWithFluid(stack);
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            MultiblockTankBlockEntity master = getMaster();
            if (master == null) return 0;
            return switch (index) {
                case 0 -> master.fluidTank != null ? master.fluidTank.getFluidAmount() : 0;
                case 1 -> master.getTotalCapacity();
                case 2 -> master.connectedBlocks.size();
                case 3 -> master.validCuboid ? 1 : 0;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 4; }
    };

    public MultiblockTankBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MULTIBLOCK_TANK.get(), pos, state);
        initializeAsMaster();
    }

    private void initializeAsMaster() {
        this.masterPos = null;
        this.connectedBlocks.clear();
        this.connectedBlocks.add(worldPosition);
        this.fluidTank = createFluidTank(CAPACITY_PER_BLOCK);
        this.validCuboid = true;
    }

    private FluidTank createFluidTank(int capacity) {
        return new FluidTank(capacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    public boolean isMaster() {
        return masterPos == null;
    }

    public BlockPos getMasterPos() {
        return isMaster() ? worldPosition : masterPos;
    }

    @Nullable
    public MultiblockTankBlockEntity getMaster() {
        if (isMaster()) return this;
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(masterPos);
        return be instanceof MultiblockTankBlockEntity tank ? tank : null;
    }

    public FluidTank getFluidTank() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.fluidTank : null;
    }

    public int getTotalCapacity() {
        return CAPACITY_PER_BLOCK * connectedBlocks.size();
    }

    public int getBlockCount() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.connectedBlocks.size() : 1;
    }

    public boolean isValidCuboid() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null && master.validCuboid;
    }

    public ItemStackHandler getBucketSlot() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.bucketSlot : bucketSlot;
    }

    // Called when block is placed
    public void onPlaced() {
        if (level == null || level.isClientSide()) return;

        // Find adjacent multiblock tanks
        Set<MultiblockTankBlockEntity> adjacentMasters = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof MultiblockTankBlockEntity neighbor) {
                MultiblockTankBlockEntity neighborMaster = neighbor.getMaster();
                if (neighborMaster != null) {
                    adjacentMasters.add(neighborMaster);
                }
            }
        }

        if (adjacentMasters.isEmpty()) {
            // No neighbors - become a new master
            initializeAsMaster();
        } else {
            // Join existing multiblock(s)
            // If multiple masters, merge them all
            MultiblockTankBlockEntity primaryMaster = adjacentMasters.iterator().next();

            // Add this block to primary master
            this.masterPos = primaryMaster.worldPosition;
            primaryMaster.connectedBlocks.add(worldPosition);

            // First: collect all blocks and calculate total capacity BEFORE merging fluids
            int totalBlocks = primaryMaster.connectedBlocks.size();
            FluidStack totalFluid = primaryMaster.fluidTank != null && !primaryMaster.fluidTank.isEmpty()
                ? primaryMaster.fluidTank.getFluid().copy()
                : FluidStack.EMPTY;

            for (MultiblockTankBlockEntity otherMaster : adjacentMasters) {
                if (otherMaster != primaryMaster) {
                    // Count blocks
                    totalBlocks += otherMaster.connectedBlocks.size();
                    // Accumulate fluid
                    if (otherMaster.fluidTank != null && !otherMaster.fluidTank.isEmpty()) {
                        FluidStack otherFluid = otherMaster.fluidTank.getFluid();
                        if (totalFluid.isEmpty()) {
                            totalFluid = otherFluid.copy();
                        } else if (totalFluid.getFluid() == otherFluid.getFluid()) {
                            totalFluid.grow(otherFluid.getAmount());
                        }
                        // Incompatible fluids: keep primary's fluid
                    }
                }
            }

            // Update primary master's tank with new capacity BEFORE transferring blocks
            int newCapacity = totalBlocks * CAPACITY_PER_BLOCK;
            primaryMaster.fluidTank = primaryMaster.createFluidTank(newCapacity);
            if (!totalFluid.isEmpty()) {
                // Truncate if over capacity (shouldn't happen normally)
                int toFill = Math.min(totalFluid.getAmount(), newCapacity);
                primaryMaster.fluidTank.fill(new FluidStack(totalFluid.getFluid(), toFill), IFluidHandler.FluidAction.EXECUTE);
            }

            // Now merge blocks (fluid already handled)
            for (MultiblockTankBlockEntity otherMaster : adjacentMasters) {
                if (otherMaster != primaryMaster) {
                    mergeBlocksInto(otherMaster, primaryMaster);
                }
            }

            // Recalculate structure (validates cuboid shape)
            primaryMaster.recalculateStructure();
        }

        setChanged();
    }

    private void mergeBlocksInto(MultiblockTankBlockEntity source, MultiblockTankBlockEntity target) {
        if (level == null) return;

        // Transfer all blocks from source to target (fluid already handled in onPlaced)
        for (BlockPos pos : source.connectedBlocks) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank && tank != target) {
                tank.masterPos = target.worldPosition;
                target.connectedBlocks.add(pos);
            }
        }

        source.connectedBlocks.clear();
        source.fluidTank = null;
    }

    // Called when block is broken
    public void onBroken() {
        if (level == null || level.isClientSide()) return;

        MultiblockTankBlockEntity master = getMaster();
        if (master == null) return;

        // Destroy all fluid (as per spec)
        if (master.fluidTank != null) {
            master.fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        }

        // Remove this block from the structure
        master.connectedBlocks.remove(worldPosition);

        // If this was the master, elect new master
        if (isMaster()) {
            electNewMaster();
        } else {
            // Just recalculate the master's structure
            master.recalculateStructure();
        }
    }

    private void electNewMaster() {
        if (level == null || connectedBlocks.isEmpty()) return;

        // Find the block with minimum position (lexicographic: Y, X, Z)
        BlockPos newMasterPos = null;
        for (BlockPos pos : connectedBlocks) {
            if (!pos.equals(worldPosition)) {
                if (newMasterPos == null || comparePositions(pos, newMasterPos) < 0) {
                    newMasterPos = pos;
                }
            }
        }

        if (newMasterPos == null) return;

        BlockEntity be = level.getBlockEntity(newMasterPos);
        if (be instanceof MultiblockTankBlockEntity newMaster) {
            // Transfer master status
            newMaster.masterPos = null;
            newMaster.connectedBlocks.clear();
            newMaster.connectedBlocks.addAll(connectedBlocks);
            newMaster.connectedBlocks.remove(worldPosition);
            newMaster.fluidTank = createFluidTank(newMaster.getTotalCapacity());

            // Update all slaves to point to new master
            for (BlockPos slavePos : newMaster.connectedBlocks) {
                if (!slavePos.equals(newMasterPos)) {
                    BlockEntity slaveBe = level.getBlockEntity(slavePos);
                    if (slaveBe instanceof MultiblockTankBlockEntity slave) {
                        slave.masterPos = newMasterPos;
                        slave.setChanged();
                    }
                }
            }

            newMaster.recalculateStructure();
        }
    }

    private int comparePositions(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) return Integer.compare(a.getY(), b.getY());
        if (a.getX() != b.getX()) return Integer.compare(a.getX(), b.getX());
        return Integer.compare(a.getZ(), b.getZ());
    }

    private void recalculateStructure() {
        if (!isMaster()) return;

        // Update tank capacity only if needed
        int newCapacity = getTotalCapacity();
        if (fluidTank == null) {
            fluidTank = createFluidTank(newCapacity);
        } else if (fluidTank.getCapacity() != newCapacity) {
            FluidStack currentFluid = fluidTank.getFluid().copy();
            fluidTank = createFluidTank(newCapacity);
            if (!currentFluid.isEmpty()) {
                // Truncate fluid if over capacity
                int toFill = Math.min(currentFluid.getAmount(), newCapacity);
                fluidTank.fill(new FluidStack(currentFluid.getFluid(), toFill), IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // Validate cuboid shape
        validCuboid = validateCuboid();

        // Update all connected blocks
        if (level != null) {
            for (BlockPos pos : connectedBlocks) {
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }

        setChanged();
    }

    private boolean validateCuboid() {
        if (connectedBlocks.isEmpty()) return true;
        if (connectedBlocks.size() == 1) return true;

        // Calculate bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : connectedBlocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // Calculate expected volume
        int expectedSize = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        // Check if we have exactly the right number of blocks
        if (connectedBlocks.size() != expectedSize) return false;

        // Verify every position in bounding box has a tank
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!connectedBlocks.contains(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MultiblockTankBlockEntity be) {
        // Only master processes logic
        if (!be.isMaster()) return;

        // Process bucket slot
        be.processBucketSlot();
    }

    protected void processBucketSlot() {
        if (!validCuboid) return; // No bucket processing if invalid shape

        ItemStack bucket = bucketSlot.getStackInSlot(0);
        if (bucket.isEmpty() || fluidTank == null) return;

        FluidStack contained = getFluidFromBucket(bucket);
        if (contained.isEmpty() || !fluidTank.isFluidValid(contained)) return;

        int canFill = fluidTank.fill(contained, IFluidHandler.FluidAction.SIMULATE);
        if (canFill >= FluidType.BUCKET_VOLUME) {
            fluidTank.fill(new FluidStack(contained.getFluid(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            bucketSlot.setStackInSlot(0, new ItemStack(Items.BUCKET));
        }
    }

    protected boolean isBucketWithFluid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidStack fluid = getFluidFromBucket(stack);
        return !fluid.isEmpty() && (fluidTank == null || fluidTank.isFluidValid(fluid));
    }

    protected FluidStack getFluidFromBucket(ItemStack bucket) {
        if (bucket.isEmpty()) return FluidStack.EMPTY;
        var cap = bucket.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (cap != null) {
            FluidStack drained = cap.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.multiblock_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        // Always use master for menu
        MultiblockTankBlockEntity master = getMaster();
        if (master == null) return null;
        return new MultiblockTankMenu(containerId, playerInv, master, master.dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putBoolean("IsMaster", isMaster());

        if (isMaster()) {
            // Save master data
            if (fluidTank != null) {
                tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
            }
            tag.put("Bucket", bucketSlot.serializeNBT(registries));
            tag.putBoolean("ValidCuboid", validCuboid);

            // Save connected blocks
            ListTag blocksList = new ListTag();
            for (BlockPos pos : connectedBlocks) {
                blocksList.add(NbtUtils.writeBlockPos(pos));
            }
            tag.put("ConnectedBlocks", blocksList);
        } else {
            // Save slave data
            tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        boolean isMaster = tag.getBoolean("IsMaster");

        if (isMaster) {
            this.masterPos = null;

            // Load connected blocks
            connectedBlocks.clear();
            if (tag.contains("ConnectedBlocks")) {
                ListTag blocksList = tag.getList("ConnectedBlocks", Tag.TAG_COMPOUND);
                for (int i = 0; i < blocksList.size(); i++) {
                    NbtUtils.readBlockPos(blocksList.getCompound(i), "").ifPresent(connectedBlocks::add);
                }
            }
            if (connectedBlocks.isEmpty()) {
                connectedBlocks.add(worldPosition);
            }

            // Load fluid tank
            int capacity = getTotalCapacity();
            this.fluidTank = createFluidTank(capacity);
            if (tag.contains("Fluid")) {
                fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
            }

            if (tag.contains("Bucket")) {
                bucketSlot.deserializeNBT(registries, tag.getCompound("Bucket"));
            }

            this.validCuboid = tag.getBoolean("ValidCuboid");
        } else {
            // Load slave data
            if (tag.contains("MasterPos")) {
                NbtUtils.readBlockPos(tag.getCompound("MasterPos"), "").ifPresent(pos -> this.masterPos = pos);
            }
            this.fluidTank = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsMaster", isMaster());

        if (isMaster()) {
            if (fluidTank != null) {
                tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
            }
            tag.putInt("BlockCount", connectedBlocks.size());
            tag.putBoolean("ValidCuboid", validCuboid);
        } else if (masterPos != null) {
            tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        }

        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
