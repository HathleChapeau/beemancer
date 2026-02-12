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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stocke la position du controller lié et met à jour l'état visuel du bloc.
 * Le tier est lu depuis le bloc parent (StorageHiveBlock).
 *
 * Met à jour le blockstate HIVE_STATE periodiquement via tick stagger.
 * La validation du lien controller→hive est faite par le controller
 * dans validateNetworkBlocksAmortized() (pas de double validation).
 *
 * Sécurité: updateBlockState() vérifie l'état RÉEL du monde avant setBlock()
 * pour éviter de recréer un bloc fantôme pendant onRemove().
 */
public class StorageHiveBlockEntity extends BlockEntity {

    private static final int VALIDATE_INTERVAL = 100;

    @Nullable
    private BlockPos controllerPos = null;

    public StorageHiveBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_HIVE.get(), pos, blockState);
    }

    // === Linking ===

    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        updateVisualState();
        syncToClient();
    }

    public void unlinkController() {
        this.controllerPos = null;
        setChanged();
        updateBlockState(StorageHiveBlock.HiveState.UNLINKED);
        syncToClient();
    }

    /**
     * Efface la reference au controller sans modifier le blockstate ni sync le client.
     * Utilise par le controller dans setRemoved() pour eviter les modifications monde
     * pendant le world unload (qui causent le hang "saving world").
     * La hive mettra a jour son etat visuel d'elle-meme au prochain serverTick.
     */
    public void clearControllerRef() {
        this.controllerPos = null;
        setChanged();
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * Retourne le controller lié, ou null si invalide.
     * Getter pur: aucun side-effect, ne modifie pas l'état.
     * La validation du lien est faite dans serverTick().
     */
    @Nullable
    public StorageControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (!level.hasChunkAt(controllerPos)) return null;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            if (!controller.isFormed()) return null;
            return controller;
        }
        return null;
    }

    /**
     * Retourne le controller lie sans verifier isFormed().
     * Utiliser UNIQUEMENT dans les cleanup paths (onRemove, unlinkController)
     * ou le controller doit etre accessible meme si le multibloc n'est pas forme.
     */
    @Nullable
    public StorageControllerBlockEntity getControllerRaw() {
        if (controllerPos == null || level == null) return null;
        if (!level.hasChunkAt(controllerPos)) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof StorageControllerBlockEntity ctrl ? ctrl : null;
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

    /**
     * Met à jour le blockstate HIVE_STATE.
     * Vérifie l'état RÉEL du monde (pas le cache) pour éviter de recréer
     * un bloc fantôme si le bloc a été détruit (pendant onRemove/setRemoved).
     */
    private void updateBlockState(StorageHiveBlock.HiveState newState) {
        if (level == null) return;
        BlockState worldState = level.getBlockState(worldPosition);
        if (!(worldState.getBlock() instanceof StorageHiveBlock)) return;
        if (worldState.getValue(StorageHiveBlock.HIVE_STATE) != newState) {
            level.setBlock(worldPosition, worldState.setValue(StorageHiveBlock.HIVE_STATE, newState), 3);
        }
    }

    // === Tick ===

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (controllerPos == null) return;
        long gameTick = level.getGameTime();
        if ((gameTick + worldPosition.hashCode()) % VALIDATE_INTERVAL == 0) {
            updateVisualState();
        }
    }

    // === Client Sync ===

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
        } else {
            controllerPos = null;
        }
    }
}
