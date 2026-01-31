/**
 * ============================================================
 * [StorageRelayBlockEntity.java]
 * Description: BlockEntity pour le Storage Relay - noeud relais du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | INetworkNode             | Interface reseau       | Mode edition, coffres, noeuds  |
 * | StorageChestManager      | Gestion coffres        | Enregistrement et flood fill   |
 * | StorageEditModeHandler   | Mode edition           | Start/stop editing             |
 * | BeemancerBlockEntities   | Type du BlockEntity    | Constructeur                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageRelayBlock.java (creation et interaction)
 * - StorageControllerRenderer.java (rendu mode edition - via INetworkNode)
 * - StorageEvents.java (interception clics)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.StorageEditModeHandler;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Noeud relais du reseau de stockage.
 * Etend la portee du controller en servant de point de passage
 * pour les abeilles de livraison et l'enregistrement de coffres.
 *
 * Meme rayon d'action que le controller (15 blocs).
 * Peut se connecter a d'autres noeuds (controller ou relay).
 */
public class StorageRelayBlockEntity extends BlockEntity implements INetworkNode {

    public static final int MAX_RANGE = 15;

    private final StorageChestManager chestManager = new StorageChestManager(this);
    private final Set<BlockPos> connectedNodes = new HashSet<>();

    private boolean editMode = false;
    private UUID editingPlayer = null;
    private boolean isSaving = false;

    public StorageRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_RELAY.get(), pos, blockState);
    }

    // === INetworkNode ===

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public Level getNodeLevel() { return level; }

    @Override
    public int getRange() { return MAX_RANGE; }

    @Override
    public StorageChestManager getChestManager() { return chestManager; }

    @Override
    public boolean toggleChest(BlockPos chestPos) { return chestManager.toggleChest(chestPos); }

    @Override
    public Set<BlockPos> getRegisteredChests() { return chestManager.getRegisteredChests(); }

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

    @Override
    public void markDirty() { setChanged(); }

    @Override
    public void syncNodeToClient() { syncToClient(); }

    // === Mode Edition ===

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

    // === Server Tick ===

    public static void serverTick(StorageRelayBlockEntity relay) {
        // Validation mode edition: joueur hors portee ou deconnecte
        if (relay.editMode && relay.editingPlayer != null && relay.level != null) {
            var server = relay.level.getServer();
            if (server != null) {
                var player = server.getPlayerList().getPlayer(relay.editingPlayer);
                if (player == null) {
                    relay.exitEditMode();
                } else {
                    double distSqr = player.distanceToSqr(
                        relay.worldPosition.getX() + 0.5,
                        relay.worldPosition.getY() + 0.5,
                        relay.worldPosition.getZ() + 0.5);
                    if (distSqr > MAX_RANGE * MAX_RANGE) {
                        relay.exitEditMode();
                    }
                }
            }
        }
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);

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
        } finally {
            isSaving = false;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

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

    // === Sync Client ===

    void syncToClient() {
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

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Deconnecter des noeuds lies
        if (level != null && !level.isClientSide()) {
            for (BlockPos nodePos : new ArrayList<>(connectedNodes)) {
                if (!level.isLoaded(nodePos)) continue;
                BlockEntity be = level.getBlockEntity(nodePos);
                if (be instanceof INetworkNode node) {
                    node.disconnectNode(worldPosition);
                }
            }
        }
    }
}
