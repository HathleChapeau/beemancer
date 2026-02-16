/**
 * ============================================================
 * [AbstractNetworkNodeBlockEntity.java]
 * Description: Base abstraite pour les noeuds du reseau de stockage (controller, relay)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | INetworkNode            | Interface reseau     | Contrat commun        |
 * | StorageChestManager     | Gestion coffres      | Flood fill            |
 * | StorageEditModeHandler  | Mode edition         | Cleanup auto          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (extends)
 * - StorageRelayBlockEntity.java (extends)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.StorageEditModeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base abstraite pour controller et relay.
 * Factorise: mode edition, noeuds connectes, gestion coffres, sync client, NBT commun.
 */
public abstract class AbstractNetworkNodeBlockEntity extends BlockEntity implements INetworkNode {

    public static final int MAX_RANGE = 15;

    private final StorageChestManager chestManager = new StorageChestManager(this);
    private final Set<BlockPos> connectedNodes = new HashSet<>();
    private boolean editMode = false;
    private UUID editingPlayer = null;
    protected boolean isSaving = false;

    protected AbstractNetworkNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // === INetworkNode: Position & Range ===

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public Level getNodeLevel() { return level; }

    @Override
    public int getRange() { return MAX_RANGE; }

    @Override
    public void markDirty() { setChanged(); }

    @Override
    public void syncNodeToClient() { syncToClient(); }

    // === INetworkNode: Chest Management ===

    @Override
    public StorageChestManager getChestManager() { return chestManager; }

    @Override
    public boolean toggleChest(BlockPos chestPos) { return chestManager.toggleChest(chestPos); }

    @Override
    public Set<BlockPos> getRegisteredChests() { return chestManager.getRegisteredChests(); }

    // === INetworkNode: Connected Nodes ===

    @Override
    public Set<BlockPos> getConnectedNodes() { return Collections.unmodifiableSet(connectedNodes); }

    @Override
    public void connectNode(BlockPos nodePos) {
        connectedNodes.add(nodePos);
        setChanged();
        syncToClient();
    }

    @Override
    public void disconnectNode(BlockPos nodePos) {
        connectedNodes.remove(nodePos);
        setChanged();
        syncToClient();
    }

    /**
     * Retire un noeud connecte sans declencher de sync client.
     * Utilise pendant setRemoved() pour eviter les modifications monde en cascade
     * durant le world unload (qui causent le hang "saving world").
     */
    /**
     * Deconnecte un noeud sans setChanged() ni syncToClient().
     * Utilise UNIQUEMENT pendant setRemoved() pour eviter de re-dirtier des chunks
     * pendant le world unload (cause boucle infinie saveAllChunks → save hang).
     */
    public void disconnectNodeSilent(BlockPos nodePos) {
        connectedNodes.remove(nodePos);
    }

    // === INetworkNode: Edit Mode ===

    @Override
    public boolean toggleEditMode(UUID playerId) {
        if (editMode && editingPlayer != null && editingPlayer.equals(playerId)) {
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
            return false;
        } else if (!editMode) {
            editMode = true;
            editingPlayer = playerId;
            setChanged();
            syncToClient();
            return true;
        }
        return false;
    }

    @Override
    public void exitEditMode() {
        if (editMode) {
            if (editingPlayer != null) {
                StorageEditModeHandler.stopEditing(editingPlayer);
            }
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
        }
    }

    @Override
    public boolean canEdit(UUID playerId) {
        return editMode && editingPlayer != null && editingPlayer.equals(playerId);
    }

    @Override
    public boolean isEditMode() { return editMode; }

    @Nullable
    @Override
    public UUID getEditingPlayer() { return editingPlayer; }

    // === Edit Mode Tick (player distance validation) ===

    protected void tickEditMode() {
        if (!editMode || editingPlayer == null || level == null) return;
        var server = level.getServer();
        if (server == null) return;
        var player = server.getPlayerList().getPlayer(editingPlayer);
        if (player == null) {
            exitEditMode();
        } else {
            double distSqr = player.distanceToSqr(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            if (distSqr > MAX_RANGE * MAX_RANGE) {
                exitEditMode();
            }
        }
    }

    // === Client Sync ===

    protected void syncToClient() {
        if (isSaving) return;
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

    // === Common NBT ===

    protected void saveCommon(CompoundTag tag) {
        chestManager.save(tag);

        ListTag nodesTag = new ListTag();
        for (BlockPos pos : connectedNodes) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            nodesTag.add(posTag);
        }
        tag.put("ConnectedNodes", nodesTag);

        tag.putBoolean("EditMode", editMode);
        if (editingPlayer != null) {
            tag.putUUID("EditingPlayer", editingPlayer);
        }
    }

    protected void loadCommon(CompoundTag tag) {
        chestManager.load(tag);

        connectedNodes.clear();
        if (tag.contains("ConnectedNodes")) {
            ListTag nodesTag = tag.getList("ConnectedNodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++) {
                NbtUtils.readBlockPos(nodesTag.getCompound(i), "Pos").ifPresent(connectedNodes::add);
            }
        }

        editMode = tag.getBoolean("EditMode");
        if (tag.hasUUID("EditingPlayer")) {
            editingPlayer = tag.getUUID("EditingPlayer");
        } else {
            editingPlayer = null;
        }
    }
}
