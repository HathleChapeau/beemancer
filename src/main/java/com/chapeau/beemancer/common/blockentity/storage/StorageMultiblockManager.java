/**
 * ============================================================
 * [StorageMultiblockManager.java]
 * Description: Gestion de la formation/destruction du multibloc Storage Controller
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Parent BlockEntity   | Back-reference                 |
 * | MultiblockPattern             | Pattern structure    | Offsets et rotation            |
 * | MultiblockValidator           | Validation formation | validateWithRotations          |
 * | MultiblockEvents              | Registre contrôleurs | register/unregister            |
 * | StorageControllerBlock        | BlockState FORMED    | Mise à jour blockstate         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (délégation, MultiblockController interface)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.multiblock.BlockMatcher;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Gère la formation, destruction et état du multibloc Storage Controller.
 * Met à jour les blockstates FORMED et FORMED_ROTATION sur les blocs structurels.
 */
public class StorageMultiblockManager {
    private final StorageControllerBlockEntity parent;

    private boolean storageFormed = false;
    private int multiblockRotation = 0;

    public StorageMultiblockManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    // === Accessors ===

    public MultiblockPattern getPattern() {
        return MultiblockPatterns.STORAGE_CONTROLLER;
    }

    public boolean isFormed() {
        return storageFormed;
    }

    public int getRotation() {
        return multiblockRotation;
    }

    // === Formation ===

    public void onMultiblockFormed() {
        storageFormed = true;
        if (parent.getLevel() != null && !parent.getLevel().isClientSide()) {
            parent.getLevel().setBlock(parent.getBlockPos(),
                parent.getBlockState().setValue(StorageControllerBlock.MULTIBLOCK, MultiblockProperty.STORAGE), 3);
            setFormedOnStructureBlocks(true);
            MultiblockEvents.registerActiveController(parent.getLevel(), parent.getBlockPos());
            parent.notifyLinkedHives();
            parent.setChanged();
        }
    }

    public void onMultiblockBroken() {
        storageFormed = false;
        if (parent.getLevel() != null && !parent.getLevel().isClientSide()) {
            parent.getDeliveryManager().killAllDeliveryBees();
            parent.getDeliveryManager().clearAllTasks();

            setFormedOnStructureBlocks(false);
            multiblockRotation = 0;
            if (parent.getLevel().getBlockState(parent.getBlockPos())
                .hasProperty(StorageControllerBlock.MULTIBLOCK)) {
                parent.getLevel().setBlock(parent.getBlockPos(),
                    parent.getBlockState().setValue(StorageControllerBlock.MULTIBLOCK, MultiblockProperty.NONE), 3);
            }
            MultiblockEvents.unregisterController(parent.getBlockPos());
            parent.notifyLinkedHives();
            parent.setChanged();
        }
    }

    /**
     * Tente de former le multibloc Storage Controller.
     * Essaie les 4 rotations horizontales.
     * @return true si la formation a réussi
     */
    public boolean tryFormStorage() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(
            getPattern(), parent.getLevel(), parent.getBlockPos());

        if (rotation >= 0) {
            multiblockRotation = rotation;
            onMultiblockFormed();
            return true;
        }

        Beemancer.LOGGER.debug("Storage controller validation failed at {} - no valid rotation found",
            parent.getBlockPos());
        return false;
    }

    /**
     * Met à jour MULTIBLOCK, FORMED_ROTATION et FACING sur tous les blocs structurels.
     */
    private void setFormedOnStructureBlocks(boolean formed) {
        if (parent.getLevel() == null) return;

        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;

            Vec3i originalOffset = element.offset();
            Vec3i rotatedOffset = MultiblockPattern.rotateY(originalOffset, multiblockRotation);
            BlockPos blockPos = parent.getBlockPos().offset(rotatedOffset);
            if (!parent.getLevel().hasChunkAt(blockPos)) continue;
            BlockState state = parent.getLevel().getBlockState(blockPos);

            boolean changed = false;

            // --- Reservoir: spread + facing ---
            if (state.getBlock() instanceof HoneyReservoirBlock) {
                BlockEntity be = parent.getLevel().getBlockEntity(blockPos);
                if (be instanceof HoneyReservoirBlockEntity reservoirBe) {
                    float spreadX = 0.0f;
                    float spreadZ = 0.0f;
                    if (formed) {
                        spreadX = rotatedOffset.getX() * 1.0f / 16.0f;
                        spreadZ = rotatedOffset.getZ() * 1.0f / 16.0f;
                    }
                    reservoirBe.setFormedSpread(spreadX, spreadZ);
                }
                if (state.hasProperty(HoneyReservoirBlock.FACING)) {
                    Direction facing = formed ? computeReservoirFacing(originalOffset) : Direction.NORTH;
                    if (state.getValue(HoneyReservoirBlock.FACING) != facing) {
                        state = state.setValue(HoneyReservoirBlock.FACING, facing);
                        changed = true;
                    }
                }
            }

            // --- Multiblock property ---
            EnumProperty<MultiblockProperty> multiblockProp = findMultiblockProperty(state);
            MultiblockProperty multiblockValue = computeMultiblockValue(originalOffset, state, formed);
            if (multiblockProp != null && state.getValue(multiblockProp) != multiblockValue) {
                state = state.setValue(multiblockProp, multiblockValue);
                changed = true;
            }

            // --- Formed rotation (only set when forming, keep value when breaking) ---
            IntegerProperty rotProp = findFormedRotationProperty(state);
            if (rotProp != null && formed) {
                int rotation = computeBlockRotation(originalOffset);
                if (state.getValue(rotProp) != rotation) {
                    state = state.setValue(rotProp, rotation);
                    changed = true;
                }
            }

            if (changed) {
                parent.getLevel().setBlock(blockPos, state, 3);
            }
        }
    }

    /**
     * Calcule la rotation à appliquer sur un bloc de la structure selon sa position.
     * Les blocs cardinaux sont orientés pour pointer vers l'extérieur (terminaux)
     * ou vers le centre (foundations). Les coins gardent la rotation de base.
     */
    private int computeBlockRotation(Vec3i originalOffset) {
        int base = 0;
        int ox = originalOffset.getX();
        int oz = originalOffset.getZ();

        if (oz < 0 && ox == 0) base = 0;       // Nord
        else if (ox > 0 && oz == 0) base = 1;   // Est
        else if (oz > 0 && ox == 0) base = 2;   // Sud
        else if (ox < 0 && oz == 0) base = 3;   // Ouest

        return (base + multiblockRotation) % 4;
    }

    /**
     * Calcule la direction FACING d'un reservoir pour pointer vers le centre du multibloc.
     * La direction de base (vers le centre) est ensuite tournée selon la rotation du multibloc.
     */
    private Direction computeReservoirFacing(Vec3i originalOffset) {
        Direction baseFacing;
        if (originalOffset.getZ() < 0) baseFacing = Direction.SOUTH;
        else if (originalOffset.getZ() > 0) baseFacing = Direction.NORTH;
        else if (originalOffset.getX() > 0) baseFacing = Direction.WEST;
        else baseFacing = Direction.EAST;

        Direction facing = baseFacing;
        for (int i = 0; i < multiblockRotation; i++) {
            facing = facing.getClockWise();
        }
        return facing;
    }

    /**
     * Calcule la valeur de la property MULTIBLOCK pour un bloc selon sa position.
     * Y+1 (couche supérieure) = STORAGE_TOP, sinon STORAGE.
     */
    private MultiblockProperty computeMultiblockValue(Vec3i offset, BlockState state, boolean formed) {
        if (!formed) return MultiblockProperty.NONE;
        if (offset.getY() > 0) return MultiblockProperty.STORAGE_TOP;
        return MultiblockProperty.STORAGE;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static EnumProperty<MultiblockProperty> findMultiblockProperty(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("multiblock") && prop instanceof EnumProperty<?> enumProp) {
                return (EnumProperty<MultiblockProperty>) enumProp;
            }
        }
        return null;
    }

    @Nullable
    private static IntegerProperty findFormedRotationProperty(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("formed_rotation") && prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }

    // === NBT ===

    public void save(CompoundTag tag) {
        tag.putBoolean("StorageFormed", storageFormed);
        tag.putInt("MultiblockRotation", multiblockRotation);
    }

    public void load(CompoundTag tag) {
        storageFormed = tag.getBoolean("StorageFormed");
        multiblockRotation = tag.getInt("MultiblockRotation");
    }
}
