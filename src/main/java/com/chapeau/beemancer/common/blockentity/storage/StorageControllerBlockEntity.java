/**
 * ============================================================
 * [StorageControllerBlockEntity.java]
 * Description: BlockEntity pour le Storage Controller - gère le réseau de coffres et le multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | AbstractNetworkNodeBlockEntity| Base reseau          | Edit mode, nodes, chests, sync |
 * | StorageNetworkRegistry        | Registre central     | Propriete exclusive des blocs  |
 * | StorageItemAggregator         | Agregation items     | Depot, extraction, sync        |
 * | StorageDeliveryManager        | Systeme livraison    | Queue, bees, honey consumption |
 * | StorageMultiblockManager      | Multibloc            | Formation, destruction         |
 * | MultiblockController          | Interface multibloc  | Formation/destruction          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlock.java (creation et interaction)
 * - StorageTerminalBlockEntity.java (recuperation items agreges)
 * - StorageControllerRenderer.java (rendu mode edition)
 * - DeliveryPhaseGoal.java (extraction/depot items)
 * - DeliveryBeeEntity.java (verification formation)
 * - StorageEvents.java (enregistrement reseau)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
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
 * Unite centrale du reseau de stockage.
 * Etend AbstractNetworkNodeBlockEntity pour heriter: mode edition, noeuds connectes,
 * gestion coffres, synchronisation client.
 *
 * Delegue aux managers specialises:
 * - StorageMultiblockManager: formation/destruction du multibloc 3x3x3
 * - StorageItemAggregator: agregation, depot, extraction d'items
 * - StorageDeliveryManager: queue de livraison, spawn de bees, honey consumption
 *
 * Possede le StorageNetworkRegistry central: registre de tous les blocs du reseau
 * avec propriete exclusive (un bloc = un proprietaire).
 */
public class StorageControllerBlockEntity extends AbstractNetworkNodeBlockEntity
        implements MultiblockController, MenuProvider {

    // === Registre central du reseau ===
    private final StorageNetworkRegistry networkRegistry = new StorageNetworkRegistry();

    // === Managers ===
    private final StorageMultiblockManager multiblockManager = new StorageMultiblockManager(this);
    private final StorageItemAggregator itemAggregator = new StorageItemAggregator(this);
    private final StorageDeliveryManager deliveryManager = new StorageDeliveryManager(this);
    private final RequestManager requestManager = new RequestManager(this);

    // === Essence slots ===
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
                case 4 -> ControllerStats.getHoneyConsumption(essenceSlots, networkRegistry.getChestCount());
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

    // === Registry Accessor ===

    public StorageNetworkRegistry getNetworkRegistry() { return networkRegistry; }

    // === Manager Accessors ===

    public StorageMultiblockManager getMultiblockManager() { return multiblockManager; }
    public StorageItemAggregator getItemAggregator() { return itemAggregator; }
    public StorageDeliveryManager getDeliveryManager() { return deliveryManager; }
    public RequestManager getRequestManager() { return requestManager; }
    public ItemStackHandler getEssenceSlots() { return essenceSlots; }

    // === MultiblockController Interface (delegue) ===

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

    // === Validation du reseau ===

    /**
     * Valide tous les blocs enregistres dans le registre central.
     * Retire ceux qui ont ete casses ou dont l'interface s'est deliee.
     * Appele a l'entree en mode edition.
     */
    public void validateNetworkBlocks() {
        if (level == null || level.isClientSide()) return;

        List<BlockPos> toRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, StorageNetworkRegistry.NetworkEntry> entry : networkRegistry.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            StorageNetworkRegistry.NetworkBlockType type = entry.getValue().type();

            if (!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);

            switch (type) {
                case CHEST -> {
                    if (!com.chapeau.beemancer.core.util.StorageHelper.isStorageContainer(level.getBlockState(pos))) {
                        toRemove.add(pos);
                    }
                }
                case TERMINAL -> {
                    if (!(be instanceof StorageTerminalBlockEntity terminal) || terminal.getControllerPos() == null) {
                        toRemove.add(pos);
                    }
                }
                case INTERFACE -> {
                    if (!(be instanceof NetworkInterfaceBlockEntity iface) || iface.getControllerPos() == null) {
                        toRemove.add(pos);
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            for (BlockPos pos : toRemove) {
                networkRegistry.unregisterBlock(pos);
            }
            setChanged();
            syncToClient();
        }
    }

    // === Reseau: coffres, terminaux, interfaces (via registre) ===

    /**
     * Retourne TOUS les coffres du reseau via le registre central.
     */
    public Set<BlockPos> getAllNetworkChests() {
        return networkRegistry.getAllChests();
    }

    /**
     * Retourne tous les terminaux du reseau via le registre central.
     */
    public Set<BlockPos> getLinkedTerminals() {
        return networkRegistry.getAllTerminals();
    }

    /**
     * Retourne toutes les interfaces du reseau via le registre central.
     */
    public Set<BlockPos> getLinkedInterfaces() {
        return networkRegistry.getAllInterfaces();
    }

    /**
     * Enregistre un terminal dans le registre du reseau.
     * Delegation vers le registre central avec propriete exclusive.
     */
    public void linkTerminal(BlockPos terminalPos) {
        networkRegistry.registerBlock(terminalPos, worldPosition, StorageNetworkRegistry.NetworkBlockType.TERMINAL);
        setChanged();
        syncToClient();
    }

    /**
     * Retire un terminal du registre du reseau.
     * Annule les demandes en cours depuis ce terminal.
     */
    public void unlinkTerminal(BlockPos terminalPos) {
        networkRegistry.unregisterBlock(terminalPos);
        itemAggregator.removeViewersForTerminal(terminalPos);
        requestManager.cancelRequestsFromSource(terminalPos);
        setChanged();
        syncToClient();
    }

    /**
     * Enregistre une interface dans le registre du reseau.
     * Delegation vers le registre central avec propriete exclusive.
     */
    public void linkInterface(BlockPos interfacePos) {
        networkRegistry.registerBlock(interfacePos, worldPosition, StorageNetworkRegistry.NetworkBlockType.INTERFACE);
        setChanged();
        syncToClient();
    }

    /**
     * Retire une interface du registre du reseau.
     * Annule les demandes en cours depuis cette interface.
     */
    public void unlinkInterface(BlockPos interfacePos) {
        networkRegistry.unregisterBlock(interfacePos);
        requestManager.cancelRequestsFromSource(interfacePos);
        setChanged();
        syncToClient();
    }

    // === Agregation Items (delegue) ===

    public List<ItemStack> getAggregatedItems() { return itemAggregator.getAggregatedItems(); }

    public void refreshAggregatedItems() { itemAggregator.refreshAggregatedItems(); }

    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) { return itemAggregator.findSlotForItem(stack); }

    public ItemStack depositItem(ItemStack stack) { return itemAggregator.depositItem(stack); }

    public ItemStack extractItem(ItemStack template, int count) { return itemAggregator.extractItem(template, count); }

    public void addViewer(UUID playerId, BlockPos terminalPos) { itemAggregator.addViewer(playerId, terminalPos); }

    public void removeViewer(UUID playerId) { itemAggregator.removeViewer(playerId); }

    // === Delivery System (delegue) ===

    public void addDeliveryTask(DeliveryTask task) { deliveryManager.addDeliveryTask(task); }

    @Nullable
    public BlockPos findChestWithItem(ItemStack template, int minCount) {
        return deliveryManager.findChestWithItem(template, minCount);
    }

    @Nullable
    public BlockPos findChestWithSpace(ItemStack template, int count) {
        return deliveryManager.findChestWithSpace(template, count);
    }

    public int countItemInChest(ItemStack template, BlockPos chestPos) {
        return deliveryManager.countItemInChest(template, chestPos);
    }

    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        return deliveryManager.extractItemForDelivery(template, count, chestPos);
    }

    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        return deliveryManager.depositItemForDelivery(stack, chestPos);
    }

    public boolean isHoneyDepleted() { return deliveryManager.isHoneyDepleted(); }

    public boolean cancelTask(UUID taskId) { return deliveryManager.cancelTask(taskId); }

    public List<com.chapeau.beemancer.common.block.storage.TaskDisplayData> getTaskDisplayData() {
        return deliveryManager.getTaskDisplayData();
    }

    // === Tick ===

    public static void serverTick(StorageControllerBlockEntity be) {
        be.itemAggregator.tickSync();

        if (be.multiblockManager.isFormed()) {
            be.deliveryManager.tickHoneyConsumption();
        }

        be.requestManager.tick();
        be.deliveryManager.tickDelivery();
        be.tickEditMode();
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

            // Common: edit mode, connected nodes, chests (from abstract base)
            saveCommon(tag);

            // Registre central du reseau
            networkRegistry.save(tag);

            // Managers specifiques au controller
            multiblockManager.save(tag);
            deliveryManager.save(tag, registries);
            requestManager.save(tag, registries);

            // Essence slots
            tag.put("EssenceSlots", essenceSlots.serializeNBT(registries));
        } finally {
            isSaving = false;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Common: edit mode, connected nodes, chests (from abstract base)
        loadCommon(tag);

        // Managers specifiques au controller
        multiblockManager.load(tag);
        deliveryManager.load(tag, registries);
        requestManager.load(tag, registries);

        // Essence slots
        if (tag.contains("EssenceSlots")) {
            essenceSlots.deserializeNBT(registries, tag.getCompound("EssenceSlots"));
        }

        // Registre central: charger ou migrer depuis l'ancien format
        if (tag.contains("NetworkRegistry")) {
            networkRegistry.load(tag);
        } else {
            migrateOldNetworkData(tag);
        }
    }

    /**
     * Migration depuis l'ancien format NBT (avant le registre central).
     * Lit les anciennes listes LinkedTerminals, LinkedInterfaces et les coffres
     * du chestManager, puis les enregistre dans le registre avec le controller
     * comme proprietaire.
     * @deprecated Kept for backward compatibility with pre-registry saves. Will be removed in a future version.
     */
    @Deprecated
    private void migrateOldNetworkData(CompoundTag tag) {
        // Migrer les coffres du chestManager vers le registre
        for (BlockPos chestPos : getChestManager().getRegisteredChests()) {
            networkRegistry.registerBlock(chestPos, worldPosition,
                    StorageNetworkRegistry.NetworkBlockType.CHEST);
        }

        // Migrer les terminaux depuis l'ancien format
        if (tag.contains("LinkedTerminals")) {
            ListTag terminalsTag = tag.getList("LinkedTerminals", Tag.TAG_COMPOUND);
            for (int i = 0; i < terminalsTag.size(); i++) {
                NbtUtils.readBlockPos(terminalsTag.getCompound(i), "Pos").ifPresent(pos ->
                        networkRegistry.registerBlock(pos, worldPosition,
                                StorageNetworkRegistry.NetworkBlockType.TERMINAL));
            }
        }

        // Migrer les interfaces depuis l'ancien format
        if (tag.contains("LinkedInterfaces")) {
            ListTag interfacesTag = tag.getList("LinkedInterfaces", Tag.TAG_COMPOUND);
            for (int i = 0; i < interfacesTag.size(); i++) {
                NbtUtils.readBlockPos(interfacesTag.getCompound(i), "Pos").ifPresent(pos ->
                        networkRegistry.registerBlock(pos, worldPosition,
                                StorageNetworkRegistry.NetworkBlockType.INTERFACE));
            }
        }
    }
}
