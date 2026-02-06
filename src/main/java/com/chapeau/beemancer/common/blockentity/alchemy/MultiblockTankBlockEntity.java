/**
 * ============================================================
 * [MultiblockTankBlockEntity.java]
 * Description: BlockEntity pour tank multibloc cube dynamique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                  | Utilisation                    |
 * |-------------------------------|------------------------|--------------------------------|
 * | MultiblockTankBlock           | Bloc associé           | Propriété MULTIBLOCK           |
 * | MultiblockProperty            | État multibloc         | NONE/TANK                      |
 * | BeemancerBlockEntities        | Registre               | Type BlockEntity               |
 * | BeemancerFluids               | Fluides acceptés       | Validation FluidTank           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MultiblockTankBlock.java
 * - MultiblockTankRenderer.java
 * - MultiblockTankMenu.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.MultiblockTankBlock;
import com.chapeau.beemancer.common.menu.alchemy.MultiblockTankMenu;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class MultiblockTankBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY_PER_BLOCK = 8000;

    // Master position (null = this IS the master)
    @Nullable
    private BlockPos masterPos = null;

    // Only for master: connected blocks and fluid storage
    private final Set<BlockPos> connectedBlocks = new HashSet<>();
    private FluidTank fluidTank;
    private int cubeSize = 0;

    // Validation différée après chargement
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
                case 3 -> master.isFormed() ? 1 : 0;
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
        initializeAsSingle();
    }

    private void initializeAsSingle() {
        this.masterPos = null;
        this.connectedBlocks.clear();
        this.connectedBlocks.add(worldPosition);
        this.fluidTank = createFluidTank(CAPACITY_PER_BLOCK);
        this.cubeSize = 0;
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

    // ==================== Multiblock State ====================

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

    /**
     * Vérifie si le multibloc est formé via le blockstate.
     */
    public boolean isFormed() {
        BlockState state = getBlockState();
        if (state.hasProperty(MultiblockTankBlock.MULTIBLOCK)) {
            return state.getValue(MultiblockTankBlock.MULTIBLOCK) == MultiblockProperty.TANK;
        }
        return false;
    }

    public int getCubeSize() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.cubeSize : 0;
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

    public ItemStackHandler getBucketSlot() {
        MultiblockTankBlockEntity master = getMaster();
        return master != null ? master.bucketSlot : bucketSlot;
    }

    // ==================== Multiblock Formation ====================

    /**
     * Appelé quand un bloc est placé. Découvre et forme le multibloc.
     */
    public void onPlaced() {
        if (level == null || level.isClientSide()) return;
        tryFormMultiblock();
    }

    /**
     * BFS flood-fill pour découvrir tous les tanks adjacents,
     * puis valide si c'est un cube et forme le multibloc.
     */
    private void tryFormMultiblock() {
        if (level == null) return;

        // BFS depuis cette position
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

        // Un seul bloc = pas de multibloc
        if (found.size() == 1) {
            initializeAsSingle();
            setChanged();
            return;
        }

        // Valider que c'est un cube parfait
        int[] bb = calculateBoundingBox(found);
        int sizeX = bb[3] - bb[0] + 1;
        int sizeY = bb[4] - bb[1] + 1;
        int sizeZ = bb[5] - bb[2] + 1;

        // Doit être un cube (toutes dimensions égales)
        if (sizeX != sizeY || sizeY != sizeZ) {
            resetAllBlocks(found);
            return;
        }

        // Minimum 2x2x2
        if (sizeX < 2) {
            resetAllBlocks(found);
            return;
        }

        // Vérifier que toutes les positions sont remplies
        int expectedSize = sizeX * sizeY * sizeZ;
        if (found.size() != expectedSize) {
            resetAllBlocks(found);
            return;
        }

        for (int x = bb[0]; x <= bb[3]; x++) {
            for (int y = bb[1]; y <= bb[4]; y++) {
                for (int z = bb[2]; z <= bb[5]; z++) {
                    if (!found.contains(new BlockPos(x, y, z))) {
                        resetAllBlocks(found);
                        return;
                    }
                }
            }
        }

        // Cube valide! Former le multibloc
        formMultiblock(found, sizeX);
    }

    /**
     * Forme le multibloc avec les blocs trouvés.
     */
    private void formMultiblock(Set<BlockPos> blocks, int size) {
        if (level == null) return;

        // Élire le master (plus petite position Y, puis X, puis Z)
        BlockPos masterPos = worldPosition;
        for (BlockPos pos : blocks) {
            if (comparePositions(pos, masterPos) < 0) {
                masterPos = pos;
            }
        }

        // Collecter le fluide depuis tous les anciens tanks
        FluidStack totalFluid = FluidStack.EMPTY;
        for (BlockPos pos : blocks) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank && tank.fluidTank != null && !tank.fluidTank.isEmpty()) {
                FluidStack f = tank.fluidTank.getFluid().copy();
                if (totalFluid.isEmpty()) {
                    totalFluid = f;
                } else if (totalFluid.getFluid() == f.getFluid()) {
                    totalFluid.grow(f.getAmount());
                }
            }
        }

        // Configurer le master
        BlockEntity masterBe = level.getBlockEntity(masterPos);
        if (!(masterBe instanceof MultiblockTankBlockEntity master)) return;

        master.masterPos = null;
        master.connectedBlocks.clear();
        master.connectedBlocks.addAll(blocks);
        master.cubeSize = size;

        int newCapacity = blocks.size() * CAPACITY_PER_BLOCK;
        master.fluidTank = master.createFluidTank(newCapacity);
        if (!totalFluid.isEmpty()) {
            int toFill = Math.min(totalFluid.getAmount(), newCapacity);
            master.fluidTank.fill(new FluidStack(totalFluid.getFluid(), toFill), IFluidHandler.FluidAction.EXECUTE);
        }

        master.setChanged();

        // Mettre à jour TOUS les blocs: blockstate + BlockEntity + sync client
        for (BlockPos pos : blocks) {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.hasProperty(MultiblockTankBlock.MULTIBLOCK)) {
                level.setBlock(pos, blockState.setValue(MultiblockTankBlock.MULTIBLOCK, MultiblockProperty.TANK), 3);
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank) {
                if (!pos.equals(masterPos)) {
                    tank.masterPos = masterPos;
                    tank.connectedBlocks.clear();
                    tank.fluidTank = null;
                    tank.cubeSize = 0;
                    tank.needsLoadValidation = false;
                }
                tank.setChanged();
                // Synchroniser au client
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
    }

    /**
     * Reset tous les blocs à l'état non formé.
     */
    private void resetAllBlocks(Set<BlockPos> blocks) {
        if (level == null) return;

        for (BlockPos pos : blocks) {
            // Reset blockstate
            BlockState blockState = level.getBlockState(pos);
            if (blockState.hasProperty(MultiblockTankBlock.MULTIBLOCK)) {
                level.setBlock(pos, blockState.setValue(MultiblockTankBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }

            // Reset BlockEntity + sync client
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank) {
                tank.masterPos = null;
                tank.connectedBlocks.clear();
                tank.connectedBlocks.add(pos);
                tank.fluidTank = tank.createFluidTank(CAPACITY_PER_BLOCK);
                tank.cubeSize = 0;
                tank.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
    }

    /**
     * Appelé quand un bloc est cassé. Détruit le fluide et reset la structure.
     */
    public void onBroken() {
        if (level == null || level.isClientSide()) return;

        // Si on est le master et formé
        if (isMaster() && isFormed()) {
            // Détruire le fluide
            if (fluidTank != null) {
                fluidTank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            }

            // Copier la liste des blocs
            Set<BlockPos> blocksToReset = new HashSet<>(connectedBlocks);
            blocksToReset.remove(worldPosition); // Exclure le bloc cassé

            // Reset TOUS les autres blocs + sync client
            for (BlockPos pos : blocksToReset) {
                // Reset blockstate
                BlockState blockState = level.getBlockState(pos);
                if (blockState.hasProperty(MultiblockTankBlock.MULTIBLOCK)) {
                    level.setBlock(pos, blockState.setValue(MultiblockTankBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
                }

                // Reset BlockEntity + sync
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MultiblockTankBlockEntity tank) {
                    tank.masterPos = null;
                    tank.connectedBlocks.clear();
                    tank.connectedBlocks.add(pos);
                    tank.fluidTank = tank.createFluidTank(CAPACITY_PER_BLOCK);
                    tank.cubeSize = 0;
                    tank.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            }

            // Trouver un voisin restant pour reformer
            BlockPos reformFrom = null;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = worldPosition.relative(dir);
                if (blocksToReset.contains(neighbor)) {
                    reformFrom = neighbor;
                    break;
                }
            }

            // Reformer depuis un voisin
            if (reformFrom != null) {
                BlockEntity be = level.getBlockEntity(reformFrom);
                if (be instanceof MultiblockTankBlockEntity neighbor) {
                    neighbor.tryFormMultiblock();
                }
            }
        } else if (masterPos != null) {
            // On est un slave: notifier le master
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof MultiblockTankBlockEntity master) {
                master.onBroken();
            }
        }

        // Reset ce bloc
        this.masterPos = null;
        this.connectedBlocks.clear();
        this.fluidTank = null;
        this.cubeSize = 0;
    }

    // ==================== Helpers ====================

    private int[] calculateBoundingBox(Set<BlockPos> blocks) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private int comparePositions(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) return Integer.compare(a.getY(), b.getY());
        if (a.getX() != b.getX()) return Integer.compare(a.getX(), b.getX());
        return Integer.compare(a.getZ(), b.getZ());
    }

    // ==================== Tick ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            needsLoadValidation = true;
            loadValidationDelay = 0;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MultiblockTankBlockEntity be) {
        if (be.needsLoadValidation) {
            be.loadValidationDelay++;
            if (be.loadValidationDelay >= LOAD_VALIDATION_WAIT_TICKS) {
                be.needsLoadValidation = false;
                be.tryFormMultiblock();
            }
        }

        if (!be.isMaster() || !be.isFormed()) return;

        be.processBucketSlot();
    }

    protected void processBucketSlot() {
        if (!isFormed()) return;

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

    // ==================== Menu ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.multiblock_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        MultiblockTankBlockEntity master = getMaster();
        if (master == null) return null;
        return new MultiblockTankMenu(containerId, playerInv, master, master.dataAccess);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putBoolean("IsMaster", isMaster());

        if (isMaster()) {
            if (fluidTank != null) {
                tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
            }
            tag.put("Bucket", bucketSlot.serializeNBT(registries));
            tag.putInt("CubeSize", cubeSize);

            ListTag blocksList = new ListTag();
            for (BlockPos pos : connectedBlocks) {
                blocksList.add(NbtUtils.writeBlockPos(pos));
            }
            tag.put("ConnectedBlocks", blocksList);
        } else if (masterPos != null) {
            tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        boolean isMaster = tag.getBoolean("IsMaster");

        if (isMaster) {
            this.masterPos = null;

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

            int capacity = getTotalCapacity();
            this.fluidTank = createFluidTank(capacity);
            if (tag.contains("Fluid")) {
                fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
            }

            if (tag.contains("Bucket")) {
                bucketSlot.deserializeNBT(registries, tag.getCompound("Bucket"));
            }

            this.cubeSize = tag.getInt("CubeSize");
        } else {
            if (tag.contains("MasterPos")) {
                NbtUtils.readBlockPos(tag.getCompound("MasterPos"), "").ifPresent(pos -> this.masterPos = pos);
            }
            this.fluidTank = null;
            this.cubeSize = 0;
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
