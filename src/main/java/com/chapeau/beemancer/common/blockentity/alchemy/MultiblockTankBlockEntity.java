/**
 * ============================================================
 * [MultiblockTankBlockEntity.java]
 * Description: BlockEntity pour tank multi-bloc dynamique
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.MultiblockTankBlock;
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
import java.util.ArrayDeque;
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

    // Deferred validation: onLoad() ne peut pas valider car les voisins ne sont pas encore charges
    private boolean needsLoadValidation = false;
    private int loadValidationDelay = 0;
    private static final int LOAD_VALIDATION_WAIT_TICKS = 10;

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

    /**
     * Retourne la taille du cube (côté).
     * Si le cube n'est pas valide, retourne 0.
     * Utilise le cache client si disponible.
     */
    public int getCubeSize() {
        // Utiliser le cache client si on est sur le client et qu'il est défini
        if (level != null && level.isClientSide() && isMaster() && clientCubeSize > 0) {
            return clientCubeSize;
        }

        MultiblockTankBlockEntity master = getMaster();
        if (master == null || !master.validCuboid) return 0;

        // Cache client du master
        if (level != null && level.isClientSide() && master.clientCubeSize > 0) {
            return master.clientCubeSize;
        }

        int[] bb = master.getBoundingBox();
        return bb[3] - bb[0] + 1; // maxX - minX + 1
    }

    public ItemStackHandler getBucketSlot() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.bucketSlot : bucketSlot;
    }

    /**
     * Appele quand un bloc est place. Utilise BFS pour decouvrir
     * tous les tanks adjacents et reformer le multibloc entier.
     */
    public void onPlaced() {
        if (level == null || level.isClientSide()) return;
        reformMultiblock();
        setChanged();
    }

    /**
     * BFS flood-fill pour decouvrir et reformer tout le multibloc.
     * Utilise par onPlaced() et performLoadValidation().
     * Inspire de Create's ConnectivityHandler.formMulti().
     */
    private void reformMultiblock() {
        if (level == null) return;

        // BFS depuis cette position pour trouver tous les tanks connectes
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        found.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (found.contains(neighbor)) continue;
                if (!level.isLoaded(neighbor)) continue;
                if (level.getBlockEntity(neighbor) instanceof MultiblockTankBlockEntity) {
                    found.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        if (found.size() == 1) {
            initializeAsMaster();
            recalculateStructure();
            return;
        }

        // Elire le master: plus petite position (Y, X, Z)
        BlockPos newMasterPos = worldPosition;
        for (BlockPos pos : found) {
            if (comparePositions(pos, newMasterPos) < 0) {
                newMasterPos = pos;
            }
        }

        BlockEntity masterBe = level.getBlockEntity(newMasterPos);
        if (!(masterBe instanceof MultiblockTankBlockEntity newMaster)) return;

        // Collecter le fluide depuis tous les anciens masters
        FluidStack totalFluid = FluidStack.EMPTY;
        for (BlockPos pos : found) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank && tank.fluidTank != null && !tank.fluidTank.isEmpty()) {
                FluidStack f = tank.fluidTank.getFluid().copy();
                if (totalFluid.isEmpty()) {
                    totalFluid = f;
                } else if (totalFluid.getFluid() == f.getFluid()) {
                    totalFluid.grow(f.getAmount());
                }
                // Fluides incompatibles: garder le premier trouve
            }
        }

        // Configurer le master
        newMaster.masterPos = null;
        newMaster.connectedBlocks.clear();
        newMaster.connectedBlocks.addAll(found);
        int newCapacity = found.size() * CAPACITY_PER_BLOCK;
        newMaster.fluidTank = newMaster.createFluidTank(newCapacity);
        if (!totalFluid.isEmpty()) {
            int toFill = Math.min(totalFluid.getAmount(), newCapacity);
            newMaster.fluidTank.fill(new FluidStack(totalFluid.getFluid(), toFill), IFluidHandler.FluidAction.EXECUTE);
        }

        // Configurer les slaves
        for (BlockPos pos : found) {
            if (pos.equals(newMasterPos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity slave) {
                slave.masterPos = newMasterPos;
                slave.fluidTank = null;
                slave.connectedBlocks.clear();
                slave.needsLoadValidation = false;
                slave.setChanged();
            }
        }

        newMaster.needsLoadValidation = false;
        newMaster.recalculateStructure();
    }

    /**
     * Appele quand un bloc est casse. Detruit le fluide et
     * reforme la structure restante via BFS depuis un voisin.
     */
    public void onBroken() {
        if (level == null || level.isClientSide()) return;

        MultiblockTankBlockEntity master = getMaster();
        if (master == null) return;

        // Destroy all fluid (as per spec)
        if (master.fluidTank != null) {
            master.fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        }

        // Find a remaining neighbor to trigger reformation
        BlockPos reformFrom = null;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof MultiblockTankBlockEntity) {
                reformFrom = neighbor;
                break;
            }
        }

        // Clear this block's state
        this.masterPos = null;
        this.connectedBlocks.clear();
        this.fluidTank = null;

        // Trigger reformation from a remaining neighbor
        if (reformFrom != null) {
            BlockEntity be = level.getBlockEntity(reformFrom);
            if (be instanceof MultiblockTankBlockEntity neighbor) {
                neighbor.reformMultiblock();
            }
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

        // Update blockstate connection properties on all connected blocks
        if (level != null) {
            boolean formed = validCuboid && connectedBlocks.size() > 1;
            for (BlockPos pos : connectedBlocks) {
                BlockState currentState = level.getBlockState(pos);
                if (!(currentState.getBlock() instanceof MultiblockTankBlock)) continue;

                BlockState newState = currentState;
                for (Direction dir : Direction.values()) {
                    boolean connected = formed && connectedBlocks.contains(pos.relative(dir));
                    newState = newState.setValue(MultiblockTankBlock.getPropertyForDirection(dir), connected);
                }
                if (newState != currentState) {
                    level.setBlock(pos, newState, 3);
                }
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }

        setChanged();
    }

    /**
     * Valide que la structure forme un CUBE parfait (h = l = p).
     * Minimum 2x2x2 pour être valide.
     * Un bloc seul n'est pas valide (trop petit).
     */
    private boolean validateCuboid() {
        if (connectedBlocks.isEmpty()) return false;

        // Un bloc seul n'est pas valide (min 2x2x2)
        if (connectedBlocks.size() == 1) return false;

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

        // Calculate dimensions
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        // CUBE: toutes les dimensions doivent être égales
        if (sizeX != sizeY || sizeY != sizeZ) return false;

        // Minimum 2x2x2
        if (sizeX < 2) return false;

        // Calculate expected volume
        int expectedSize = sizeX * sizeY * sizeZ;

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

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            needsLoadValidation = true;
            loadValidationDelay = 0;
        }
    }

    /**
     * Validation differee apres chargement.
     * Attend quelques ticks puis reforme le multibloc via BFS.
     */
    private void performLoadValidation() {
        if (level == null) return;

        loadValidationDelay++;
        if (loadValidationDelay < LOAD_VALIDATION_WAIT_TICKS) return;

        needsLoadValidation = false;
        reformMultiblock();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MultiblockTankBlockEntity be) {
        if (be.needsLoadValidation) {
            be.performLoadValidation();
        }

        if (!be.isMaster()) return;

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

            // Charger bounding box pour le client
            if (tag.contains("BoundingBox")) {
                this.clientBoundingBox = tag.getIntArray("BoundingBox");
                if (this.clientBoundingBox.length != 6) {
                    this.clientBoundingBox = null;
                }
            }

            // Charger taille du cube pour le client
            if (tag.contains("CubeSize")) {
                this.clientCubeSize = tag.getInt("CubeSize");
            }
        } else {
            // Load slave data
            if (tag.contains("MasterPos")) {
                NbtUtils.readBlockPos(tag.getCompound("MasterPos"), "").ifPresent(pos -> this.masterPos = pos);
            }
            this.fluidTank = null;
        }
    }

    /**
     * Calcule le bounding box du cuboid (pour le rendu client).
     * Retourne [minX, minY, minZ, maxX, maxY, maxZ] en coordonnees monde.
     */
    public int[] getBoundingBox() {
        if (connectedBlocks.isEmpty()) {
            return new int[]{worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                             worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()};
        }
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
        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    // Bounding box cache pour le client (charge via updateTag)
    private int[] clientBoundingBox = null;

    // Taille du cube cache pour le client
    private int clientCubeSize = 0;

    public int[] getClientBoundingBox() {
        if (clientBoundingBox != null) return clientBoundingBox;
        return getBoundingBox();
    }

    /**
     * Calcule le ratio de remplissage [0.0-1.0] pour un bloc specifique
     * selon sa position Y dans le multibloc.
     */
    public float getFluidFillRatioForBlock(BlockPos blockPos) {
        MultiblockTankBlockEntity master = getMaster();
        if (master == null || master.fluidTank == null || master.fluidTank.isEmpty()) return 0f;

        int[] bb = master.getClientBoundingBox();
        int minY = bb[1];
        int maxY = bb[4];
        int totalHeight = maxY - minY + 1;
        if (totalHeight <= 0) return 0f;

        float globalFill = (float) master.fluidTank.getFluidAmount() / master.fluidTank.getCapacity();
        int blockIndex = blockPos.getY() - minY;

        float fillPerBlock = 1.0f / totalHeight;
        float blockStartFill = blockIndex * fillPerBlock;
        float blockEndFill = (blockIndex + 1) * fillPerBlock;

        if (globalFill <= blockStartFill) return 0f;
        if (globalFill >= blockEndFill) return 1f;
        return (globalFill - blockStartFill) / fillPerBlock;
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

            // Bounding box pour le rendu client du fluide
            int[] bb = getBoundingBox();
            tag.putIntArray("BoundingBox", bb);

            // Taille du cube pour le renderer
            int cubeSize = validCuboid ? (bb[3] - bb[0] + 1) : 0;
            tag.putInt("CubeSize", cubeSize);
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
