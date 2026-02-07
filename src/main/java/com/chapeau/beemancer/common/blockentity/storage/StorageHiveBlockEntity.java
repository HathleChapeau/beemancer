/**
 * ============================================================
 * [StorageHiveBlockEntity.java]
 * Description: BlockEntity pour la Storage Hive - lien au controller et état visuel
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity| Controller réseau    | Vérification lien              |
 * | StorageHiveBlock            | Bloc parent          | Lecture tier, HiveState        |
 * | BeemancerBlockEntities      | Registre BE          | Type du BlockEntity            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageHiveBlock.java (création et ticker)
 * - StorageEvents.java (linking/unlinking)
 * - StorageControllerBlockEntity.java (notification état)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.StorageHiveBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stocke la position du controller lié et met à jour l'état visuel du bloc.
 * Le tier est lu depuis le bloc parent (StorageHiveBlock).
 *
 * Vérifie périodiquement que le controller existe toujours (toutes les 100 ticks).
 * Met à jour le blockstate HIVE_STATE quand notifié par le controller.
 */
public class StorageHiveBlockEntity extends BlockEntity {

    private static final int VALIDATE_INTERVAL = 100;

    @Nullable
    private BlockPos controllerPos = null;
    private int validateTimer = 0;

    public StorageHiveBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_HIVE.get(), pos, blockState);
    }

    // === Linking ===

    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        updateVisualState();
    }

    public void unlinkController() {
        this.controllerPos = null;
        setChanged();
        updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * Retourne le controller lié, ou null si invalide.
     * Invalide automatiquement le lien si le controller n'existe plus.
     */
    @Nullable
    public StorageControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (!level.isLoaded(controllerPos)) return null;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            return controller;
        }

        controllerPos = null;
        setChanged();
        updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
        return null;
    }

    /**
     * Retourne le tier depuis le bloc parent.
     */
    public int getTier() {
        if (getBlockState().getBlock() instanceof StorageHiveBlock hiveBlock) {
            return hiveBlock.getTier();
        }
        return 1;
    }

    // === Visual State ===

    /**
     * Met à jour l'état visuel selon l'état du controller.
     * Appelé par le controller quand le multibloc forme/détruit, ou quand lié.
     */
    public void updateVisualState() {
        if (controllerPos == null) {
            updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
            return;
        }

        StorageControllerBlockEntity controller = getController();
        if (controller == null) {
            updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
            return;
        }

        if (controller.isFormed()) {
            updateBlockState(StorageHiveBlock.HiveState.ACTIVE);
        } else {
            updateBlockState(StorageHiveBlock.HiveState.LINKED);
        }
    }

    private void updateBlockState(StorageHiveBlock.HiveState newState) {
        if (level == null) return;
        BlockState current = getBlockState();
        if (current.getValue(StorageHiveBlock.HIVE_STATE) != newState) {
            level.setBlock(worldPosition, current.setValue(StorageHiveBlock.HIVE_STATE, newState), 3);
        }
    }

    // === Tick ===

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        validateTimer++;
        if (validateTimer >= VALIDATE_INTERVAL) {
            validateTimer = 0;
            if (controllerPos != null) {
                StorageControllerBlockEntity controller = getController();
                if (controller == null) {
                    updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
                }
            }
        }
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ControllerPos")) {
            NbtUtils.readBlockPos(tag, "ControllerPos").ifPresent(pos -> controllerPos = pos);
        }
    }
}
