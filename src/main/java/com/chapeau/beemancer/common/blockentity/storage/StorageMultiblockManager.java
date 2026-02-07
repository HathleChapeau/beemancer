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
import com.chapeau.beemancer.common.block.storage.ControllerPipeBlock;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.multiblock.BlockMatcher;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import net.minecraft.core.BlockPos;
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
     * Met à jour FORMED et FORMED_ROTATION sur tous les blocs structurels du multibloc.
     */
    private void setFormedOnStructureBlocks(boolean formed) {
        if (parent.getLevel() == null) return;

        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;

            Vec3i originalOffset = element.offset();
            Vec3i rotatedOffset = MultiblockPattern.rotateY(originalOffset, multiblockRotation);
            BlockPos blockPos = parent.getBlockPos().offset(rotatedOffset);
            BlockState state = parent.getLevel().getBlockState(blockPos);

            float spreadX = 0.0f;
            float spreadZ = 0.0f;
            if (formed && originalOffset.getX() != 0) {
                spreadX = rotatedOffset.getX() * 1.0f / 16.0f;
                spreadZ = rotatedOffset.getZ() * 1.0f / 16.0f;
            }

            if (state.getBlock() instanceof ControllerPipeBlock) {
                int rotation = formed ? computeBlockRotation(originalOffset, state) : 0;
                BlockEntity be = parent.getLevel().getBlockEntity(blockPos);
                if (be instanceof ControllerPipeBlockEntity pipeBe) {
                    if (formed) {
                        pipeBe.setFormed(spreadX, spreadZ);
                    } else {
                        pipeBe.clearFormed();
                    }
                }
                BlockState newState = state
                    .setValue(ControllerPipeBlock.MULTIBLOCK, formed ? MultiblockProperty.STORAGE : MultiblockProperty.NONE)
                    .setValue(ControllerPipeBlock.FORMED_ROTATION, rotation);
                if (!newState.equals(state)) {
                    parent.getLevel().setBlock(blockPos, newState, 3);
                }
                continue;
            }

            if (state.getBlock() instanceof HoneyReservoirBlock) {
                BlockEntity be = parent.getLevel().getBlockEntity(blockPos);
                if (be instanceof HoneyReservoirBlockEntity reservoirBe) {
                    reservoirBe.setFormedSpread(spreadX, spreadZ);
                }
            }

            boolean changed = false;

            EnumProperty<MultiblockProperty> multiblockProp = findMultiblockProperty(state);
            MultiblockProperty multiblockValue = formed ? MultiblockProperty.STORAGE : MultiblockProperty.NONE;
            if (multiblockProp != null && state.getValue(multiblockProp) != multiblockValue) {
                state = state.setValue(multiblockProp, multiblockValue);
                changed = true;
            }

            IntegerProperty rotProp = findFormedRotationProperty(state);
            if (rotProp != null) {
                int rotation = formed ? computeBlockRotation(originalOffset, state) : 0;
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
     * Calcule la rotation à appliquer sur un bloc de la structure.
     */
    private int computeBlockRotation(Vec3i originalOffset, BlockState state) {
        if (state.getBlock() instanceof ControllerPipeBlock) {
            int baseRotation = (originalOffset.getX() < 0 ? 0 : 2);
            int yRotation = (baseRotation + multiblockRotation) & 3;
            boolean bottom = originalOffset.getY() < 0;
            return bottom ? yRotation + 4 : yRotation;
        }
        return multiblockRotation;
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
