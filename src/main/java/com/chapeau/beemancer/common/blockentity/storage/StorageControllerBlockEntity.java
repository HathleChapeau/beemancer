/**
 * ============================================================
 * [StorageControllerBlockEntity.java]
 * Description: BlockEntity pour le Storage Controller - gère le réseau de coffres et le multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | StorageChestManager      | Gestion coffres        | Enregistrement et flood fill   |
 * | StorageItemAggregator    | Agrégation items       | Dépôt, extraction, sync        |
 * | StorageDeliveryManager   | Système livraison      | Queue, bees, honey consumption |
 * | StorageMultiblockManager | Multibloc              | Formation, destruction         |
 * | BeemancerBlockEntities   | Type du BlockEntity    | Constructeur                   |
 * | MultiblockController     | Interface multibloc    | Formation/destruction          |
 * | StorageEditModeHandler   | Mode édition           | Start/stop editing             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java (création et interaction)
 * - StorageTerminalBlockEntity.java (récupération items agrégés)
 * - StorageControllerRenderer.java (rendu mode édition)
 * - DeliveryPhaseGoal.java (extraction/dépôt items)
 * - DeliveryBeeEntity.java (vérification formation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.StorageEditModeHandler;
import com.chapeau.beemancer.common.menu.storage.StorageControllerMenu;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Unité centrale du réseau de stockage.
 * Délègue aux managers spécialisés:
 * - StorageMultiblockManager: formation/destruction du multibloc 3x3x3
 * - StorageChestManager: enregistrement coffres (flood fill)
 * - StorageItemAggregator: agrégation, dépôt, extraction d'items
 * - StorageDeliveryManager: queue de livraison, spawn de bees, honey consumption
 */
public class StorageControllerBlockEntity extends BlockEntity implements MultiblockController, MenuProvider, INetworkNode {

    public static final int MAX_RANGE = 15;

    // === Managers ===
    private final StorageMultiblockManager multiblockManager = new StorageMultiblockManager(this);
    private final StorageChestManager chestManager = new StorageChestManager(this);
    private final StorageItemAggregator itemAggregator = new StorageItemAggregator(this);
    private final StorageDeliveryManager deliveryManager = new StorageDeliveryManager(this);

    // === Mode édition (inline — trop petit pour extraire) ===
    private boolean editMode = false;
    private UUID editingPlayer = null;

    // === Noeuds connectes (relays) ===
    private final Set<BlockPos> connectedNodes = new HashSet<>();

    // === Terminaux liés (inline — trop petit pour extraire) ===
    private final Set<BlockPos> linkedTerminals = new HashSet<>();

    // === Essence slots ===
    private boolean isSaving = false;

    private final ItemStackHandler essenceSlots = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(BeemancerTags.Items.ESSENCES);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!isSaving) {
                syncToClient();
            }
        }
    };

    // === GUI sync ===
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ControllerStats.getFlightSpeed(essenceSlots);
                case 1 -> ControllerStats.getSearchSpeed(essenceSlots);
                case 2 -> ControllerStats.getCraftSpeed(essenceSlots);
                case 3 -> ControllerStats.getQuantity(essenceSlots);
                case 4 -> ControllerStats.getHoneyConsumption(essenceSlots, chestManager.getRegisteredChestCount());
                case 5 -> ControllerStats.getHoneyEfficiency(essenceSlots);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() { return 6; }
    };

    public StorageControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_CONTROLLER.get(), pos, blockState);
    }

    // === Manager Accessors ===

    public StorageMultiblockManager getMultiblockManager() { return multiblockManager; }
    public StorageChestManager getChestManager() { return chestManager; }
    public StorageItemAggregator getItemAggregator() { return itemAggregator; }
    public StorageDeliveryManager getDeliveryManager() { return deliveryManager; }
    public ItemStackHandler getEssenceSlots() { return essenceSlots; }

    // === MultiblockController Interface (délègue) ===

    @Override
    public MultiblockPattern getPattern() { return multiblockManager.getPattern(); }

    @Override
    public boolean isFormed() { return multiblockManager.isFormed(); }

    @Override
    public BlockPos getControllerPos() { return worldPosition; }

    @Override
    public int getRotation() { return multiblockManager.getRotation(); }

    @Override
    public void onMultiblockFormed() { multiblockManager.onMultiblockFormed(); }

    @Override
    public void onMultiblockBroken() { multiblockManager.onMultiblockBroken(); }

    public boolean tryFormStorage() { return multiblockManager.tryFormStorage(); }

    // === INetworkNode Interface ===

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public net.minecraft.world.level.Level getNodeLevel() { return level; }

    @Override
    public int getRange() { return MAX_RANGE; }

    @Override
    public void markDirty() { setChanged(); }

    @Override
    public void syncNodeToClient() { syncToClient(); }

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

    // === Mode Édition ===

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

    // === Gestion des Coffres (délègue) ===

    public boolean toggleChest(BlockPos chestPos) { return chestManager.toggleChest(chestPos); }

    public Set<BlockPos> getRegisteredChests() { return chestManager.getRegisteredChests(); }

    /**
     * Retourne TOUS les coffres du reseau: ceux du controller + ceux de tous les relays connectes.
     * Parcours BFS du graphe de noeuds.
     */
    public Set<BlockPos> getAllNetworkChests() {
        Set<BlockPos> allChests = new LinkedHashSet<>(chestManager.getRegisteredChests());
        if (level == null) return allChests;

        Set<BlockPos> visitedNodes = new HashSet<>();
        visitedNodes.add(worldPosition);
        Queue<BlockPos> queue = new LinkedList<>(connectedNodes);

        while (!queue.isEmpty()) {
            BlockPos nodePos = queue.poll();
            if (!visitedNodes.add(nodePos)) continue;
            if (!level.isLoaded(nodePos)) continue;

            BlockEntity be = level.getBlockEntity(nodePos);
            if (be instanceof INetworkNode node) {
                allChests.addAll(node.getRegisteredChests());
                for (BlockPos neighbor : node.getConnectedNodes()) {
                    if (!visitedNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return allChests;
    }

    // === Gestion des Terminaux ===

    public void linkTerminal(BlockPos terminalPos) {
        linkedTerminals.add(terminalPos);
        setChanged();
    }

    public void unlinkTerminal(BlockPos terminalPos) {
        linkedTerminals.remove(terminalPos);
        itemAggregator.removeViewersForTerminal(terminalPos);
        setChanged();
    }

    public Set<BlockPos> getLinkedTerminals() {
        return Collections.unmodifiableSet(linkedTerminals);
    }

    // === Agrégation Items (délègue) ===

    public List<ItemStack> getAggregatedItems() { return itemAggregator.getAggregatedItems(); }

    public void refreshAggregatedItems() { itemAggregator.refreshAggregatedItems(); }

    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) { return itemAggregator.findSlotForItem(stack); }

    public ItemStack depositItem(ItemStack stack) { return itemAggregator.depositItem(stack); }

    public ItemStack extractItem(ItemStack template, int count) { return itemAggregator.extractItem(template, count); }

    public void addViewer(UUID playerId, BlockPos terminalPos) { itemAggregator.addViewer(playerId, terminalPos); }

    public void removeViewer(UUID playerId) { itemAggregator.removeViewer(playerId); }

    // === Delivery System (délègue) ===

    public void addDeliveryTask(DeliveryTask task) { deliveryManager.addDeliveryTask(task); }

    @Nullable
    public BlockPos findChestWithItem(ItemStack template, int minCount) {
        return deliveryManager.findChestWithItem(template, minCount);
    }

    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        return deliveryManager.extractItemForDelivery(template, count, chestPos);
    }

    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        return deliveryManager.depositItemForDelivery(stack, chestPos);
    }

    public boolean isHoneyDepleted() { return deliveryManager.isHoneyDepleted(); }

    public boolean cancelTask(java.util.UUID taskId) { return deliveryManager.cancelTask(taskId); }

    public java.util.List<com.chapeau.beemancer.common.block.storage.TaskDisplayData> getTaskDisplayData() {
        return deliveryManager.getTaskDisplayData();
    }

    // === Tick ===

    public static void serverTick(StorageControllerBlockEntity be) {
        be.itemAggregator.tickSync();

        if (be.multiblockManager.isFormed()) {
            be.deliveryManager.tickHoneyConsumption();
        }

        be.deliveryManager.tickDelivery();

        // Validation mode édition
        if (be.editMode && be.editingPlayer != null && be.level != null) {
            var server = be.level.getServer();
            if (server != null) {
                var player = server.getPlayerList().getPlayer(be.editingPlayer);
                if (player == null) {
                    be.exitEditMode();
                } else {
                    double distSqr = player.distanceToSqr(
                        be.worldPosition.getX() + 0.5,
                        be.worldPosition.getY() + 0.5,
                        be.worldPosition.getZ() + 0.5);
                    if (distSqr > MAX_RANGE * MAX_RANGE) {
                        be.exitEditMode();
                    }
                }
            }
        }

        be.itemAggregator.cleanupViewers();
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.storage_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new StorageControllerMenu(containerId, playerInv, essenceSlots, dataAccess);
    }

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        super.setRemoved();
        deliveryManager.killAllDeliveryBees();
        MultiblockEvents.unregisterController(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (multiblockManager.isFormed() && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);

            // Délégation aux managers
            multiblockManager.save(tag);
            chestManager.save(tag);
            deliveryManager.save(tag, registries);

            // Terminaux liés (inline)
            ListTag terminalsTag = new ListTag();
            for (BlockPos pos : linkedTerminals) {
                CompoundTag posTag = new CompoundTag();
                posTag.put("Pos", NbtUtils.writeBlockPos(pos));
                terminalsTag.add(posTag);
            }
            tag.put("LinkedTerminals", terminalsTag);

            // Noeuds connectes
            ListTag nodesTag = new ListTag();
            for (BlockPos pos : connectedNodes) {
                CompoundTag posTag = new CompoundTag();
                posTag.put("Pos", NbtUtils.writeBlockPos(pos));
                nodesTag.add(posTag);
            }
            tag.put("ConnectedNodes", nodesTag);

            // Mode édition (inline)
            tag.putBoolean("EditMode", editMode);
            if (editingPlayer != null) {
                tag.putUUID("EditingPlayer", editingPlayer);
            }

            // Essence slots
            tag.put("EssenceSlots", essenceSlots.serializeNBT(registries));
        } finally {
            isSaving = false;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Délégation aux managers
        multiblockManager.load(tag);
        chestManager.load(tag);
        deliveryManager.load(tag, registries);

        // Terminaux liés (inline)
        linkedTerminals.clear();
        ListTag terminalsTag = tag.getList("LinkedTerminals", Tag.TAG_COMPOUND);
        for (int i = 0; i < terminalsTag.size(); i++) {
            NbtUtils.readBlockPos(terminalsTag.getCompound(i), "Pos").ifPresent(linkedTerminals::add);
        }

        // Noeuds connectes
        connectedNodes.clear();
        if (tag.contains("ConnectedNodes")) {
            ListTag nodesTag = tag.getList("ConnectedNodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++) {
                NbtUtils.readBlockPos(nodesTag.getCompound(i), "Pos").ifPresent(connectedNodes::add);
            }
        }

        // Mode édition (inline)
        editMode = tag.getBoolean("EditMode");
        if (tag.hasUUID("EditingPlayer")) {
            editingPlayer = tag.getUUID("EditingPlayer");
        } else {
            editingPlayer = null;
        }

        // Essence slots
        if (tag.contains("EssenceSlots")) {
            essenceSlots.deserializeNBT(registries, tag.getCompound("EssenceSlots"));
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
}
