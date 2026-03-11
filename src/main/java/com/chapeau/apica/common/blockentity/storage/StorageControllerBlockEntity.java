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
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.ControllerStats;
import com.chapeau.apica.common.block.storage.DeliveryTask;
import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.common.block.storage.StorageEvents;
import com.chapeau.apica.common.blockentity.storage.task.AbstractInterfaceTaskHandler;
import com.chapeau.apica.common.blockentity.storage.task.InterfaceTaskHandlerFactory;
import com.chapeau.apica.core.multiblock.MultiblockCapabilityProvider;
import com.chapeau.apica.core.multiblock.MultiblockController;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.registry.ApicaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

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
        implements MultiblockController, MultiblockCapabilityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageControllerBlockEntity.class);
    private static final int VALIDATION_BATCH_INTERVAL = 20;
    private static final int VALIDATION_SPREAD_TICKS = 10;
    private static final int BASE_HONEY_CAPACITY = 8000;
    private int beeCapacity = ControllerStats.BASE_DROP;
    private int validationIndex = 0;
    private boolean isLoading = false;

    // Honey buffer interne au controller
    private int honeyStored = 0;
    private int honeyCapacity = BASE_HONEY_CAPACITY;

    // [BM] Coffres deja pris par d'autres reseaux (transient, recalcule en edit mode)
    private final Set<BlockPos> takenChestPositions = new HashSet<>();

    // === Registre central du reseau ===
    private final StorageNetworkRegistry networkRegistry = new StorageNetworkRegistry();

    // === Managers ===
    private final HiveManager hiveManager = new HiveManager(this);
    private final StorageMultiblockManager multiblockManager = new StorageMultiblockManager(this);
    private final StorageItemAggregator itemAggregator = new StorageItemAggregator(this);
    private final StorageDeliveryManager deliveryManager = new StorageDeliveryManager(this);
    private final RequestManager requestManager = new RequestManager(this);

    // === Essence slots ===
    private final ItemStackHandler essenceSlots = new ItemStackHandler(HiveManager.MAX_ESSENCE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ApicaTags.Items.ESSENCES);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (isLoading) return;
            setChanged();
            recalculateBeeCapacity();
            recalculateHoneyCapacity();
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
                case 2 -> ControllerStats.getHoneyCapacityBonus(essenceSlots);
                case 3 -> ControllerStats.getQuantity(essenceSlots);
                case 4 -> ControllerStats.getHoneyConsumption(essenceSlots, networkRegistry.getChestCount(), getHiveMultiplier(), getRelayCount(), getInterfaceRelayCost(), level != null && level.isDay());
                case 5 -> ControllerStats.getHoneyEfficiency(essenceSlots, level != null && level.isDay());
                case 6 -> getLinkedHiveCount();
                case 7 -> getMaxDeliveryBees();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() { return 8; }
    };

    public StorageControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ApicaBlockEntities.STORAGE_CONTROLLER.get(), pos, blockState);
    }

    // === Registry Accessor ===

    public StorageNetworkRegistry getNetworkRegistry() { return networkRegistry; }

    // === Manager Accessors ===

    public HiveManager getHiveManager() { return hiveManager; }
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
        List<BlockPos> hivesToRemove = new ArrayList<>();

        for (Map.Entry<BlockPos, StorageNetworkRegistry.NetworkEntry> entry : networkRegistry.getAll().entrySet()) {
            BlockPos pos = entry.getKey();
            StorageNetworkRegistry.NetworkBlockType type = entry.getValue().type();

            if (!level.hasChunkAt(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);

            switch (type) {
                case CHEST -> {
                    if (!com.chapeau.apica.core.util.StorageHelper.hasItemHandlerCapability(level, pos, null)
                            && !com.chapeau.apica.core.util.StorageHelper.isStorageContainer(level.getBlockState(pos))) {
                        toRemove.add(pos);
                    }
                }
                case TERMINAL -> {
                    if (!(be instanceof StorageTerminalBlockEntity terminal)
                            || terminal.getControllerPos() == null
                            || !terminal.getControllerPos().equals(worldPosition)) {
                        toRemove.add(pos);
                    }
                }
                case INTERFACE -> {
                    if (!(be instanceof NetworkInterfaceBlockEntity iface)
                            || iface.getControllerPos() == null
                            || !iface.getControllerPos().equals(worldPosition)) {
                        toRemove.add(pos);
                    }
                }
                case HIVE -> {
                    if (!(be instanceof StorageHiveBlockEntity hive)
                            || hive.getControllerPos() == null
                            || !hive.getControllerPos().equals(worldPosition)) {
                        hivesToRemove.add(pos);
                    }
                }
            }
        }

        // Remove non-hive blocks
        for (BlockPos pos : toRemove) {
            networkRegistry.unregisterBlock(pos);
        }

        // Remove hives with proper cleanup (notify hive + drop overflow essences)
        for (BlockPos hivePos : hivesToRemove) {
            if (!level.hasChunkAt(hivePos)) {
                networkRegistry.unregisterBlock(hivePos);
                continue;
            }
            BlockEntity be = level.getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.unlinkController();
            }
            networkRegistry.unregisterBlock(hivePos);
        }
        if (!hivesToRemove.isEmpty()) {
            hiveManager.dropOverflowEssences();
        }

        if (!toRemove.isEmpty() || !hivesToRemove.isEmpty()) {
            setChanged();
            syncToClient();
        }

        // [BM] Recalculer les coffres pris par d'autres reseaux (pour l'affichage edit mode)
        computeTakenChestPositions();
    }

    /**
     * Validation amortie: valide un batch de blocs par appel au lieu de tout d'un coup.
     * Repartit la charge sur VALIDATION_SPREAD_TICKS ticks pour eviter les lag spikes
     * sur les grands reseaux (200+ coffres).
     */
    private void validateNetworkBlocksAmortized() {
        if (level == null || level.isClientSide()) return;

        Map<BlockPos, StorageNetworkRegistry.NetworkEntry> allEntries = networkRegistry.getAll();
        if (allEntries.isEmpty()) {
            validationIndex = 0;
            return;
        }

        List<Map.Entry<BlockPos, StorageNetworkRegistry.NetworkEntry>> entries =
                new ArrayList<>(allEntries.entrySet());

        int batchSize = Math.max(1, (int) Math.ceil(entries.size() / (double) VALIDATION_SPREAD_TICKS));
        int start = Math.min(validationIndex, entries.size());
        int end = Math.min(start + batchSize, entries.size());

        List<BlockPos> toRemove = new ArrayList<>();
        List<BlockPos> hivesToRemove = new ArrayList<>();

        for (int i = start; i < end; i++) {
            Map.Entry<BlockPos, StorageNetworkRegistry.NetworkEntry> entry = entries.get(i);
            BlockPos pos = entry.getKey();
            StorageNetworkRegistry.NetworkBlockType type = entry.getValue().type();

            if (!level.hasChunkAt(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);

            switch (type) {
                case CHEST -> {
                    if (!com.chapeau.apica.core.util.StorageHelper.hasItemHandlerCapability(level, pos, null)
                            && !com.chapeau.apica.core.util.StorageHelper.isStorageContainer(level.getBlockState(pos))) {
                        toRemove.add(pos);
                    }
                }
                case TERMINAL -> {
                    if (!(be instanceof StorageTerminalBlockEntity terminal)
                            || terminal.getControllerPos() == null
                            || !terminal.getControllerPos().equals(worldPosition)) {
                        toRemove.add(pos);
                    }
                }
                case INTERFACE -> {
                    if (!(be instanceof NetworkInterfaceBlockEntity iface)
                            || iface.getControllerPos() == null
                            || !iface.getControllerPos().equals(worldPosition)) {
                        toRemove.add(pos);
                    }
                }
                case HIVE -> {
                    if (!(be instanceof StorageHiveBlockEntity hive)
                            || hive.getControllerPos() == null
                            || !hive.getControllerPos().equals(worldPosition)) {
                        hivesToRemove.add(pos);
                    }
                }
            }
        }

        validationIndex = end >= entries.size() ? 0 : end;

        for (BlockPos pos : toRemove) {
            networkRegistry.unregisterBlock(pos);
        }

        for (BlockPos hivePos : hivesToRemove) {
            if (!level.hasChunkAt(hivePos)) {
                networkRegistry.unregisterBlock(hivePos);
                continue;
            }
            BlockEntity be = level.getBlockEntity(hivePos);
            if (be instanceof StorageHiveBlockEntity hive) {
                hive.unlinkController();
            }
            networkRegistry.unregisterBlock(hivePos);
        }
        if (!hivesToRemove.isEmpty()) {
            hiveManager.dropOverflowEssences();
        }

        if (!toRemove.isEmpty() || !hivesToRemove.isEmpty()) {
            setChanged();
            syncToClient();
        }
    }

    // === [BM] Deduplication inter-reseau ===

    /**
     * Calcule les positions des coffres deja enregistres dans d'AUTRES reseaux.
     * Appele a l'entree en mode edition (depuis validateNetworkBlocks).
     * Le resultat est synchronise au client pour l'affichage (outlines verts).
     */
    public void computeTakenChestPositions() {
        takenChestPositions.clear();
        if (level == null || level.isClientSide()) return;

        for (BlockPos ctrlPos : MultiblockEvents.getActiveControllers()) {
            if (ctrlPos.equals(worldPosition)) continue;
            if (!level.isLoaded(ctrlPos)) continue;
            BlockEntity be = level.getBlockEntity(ctrlPos);
            if (be instanceof StorageControllerBlockEntity otherCtrl && otherCtrl.isFormed()) {
                takenChestPositions.addAll(otherCtrl.getNetworkRegistry().getAllChests());
            }
        }
    }

    /**
     * Retourne les positions des coffres pris par d'autres reseaux.
     * Utilise par les renderers en mode edition pour afficher les outlines verts.
     */
    public Set<BlockPos> getTakenChestPositions() {
        return Collections.unmodifiableSet(takenChestPositions);
    }

    // === Reseau: relays, coffres, terminaux, interfaces (via registre) ===

    /**
     * Compte le nombre de relays dans le reseau via BFS depuis les noeuds connectes.
     * Les relays ne sont pas dans le NetworkRegistry (ce sont des noeuds, pas des blocs enregistres).
     */
    public int getRelayCount() {
        if (level == null) return 0;
        int count = 0;
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>(getConnectedNodes());
        visited.add(worldPosition);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) continue;
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageRelayBlockEntity relay) {
                count++;
                for (BlockPos neighbor : relay.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return count;
    }

    /**
     * Calcule le cout total en miel des interfaces en fonction de leur profondeur relay.
     * Chaque interface coute 5 mB/s × nombre de relays qui la separent du controller.
     * Interfaces directement sur le controller (owner == controllerPos) coutent 0.
     */
    public int getInterfaceRelayCost() {
        if (level == null) return 0;

        // BFS depuis le controller pour calculer la profondeur de chaque relay
        java.util.Map<BlockPos, Integer> relayDepth = new java.util.HashMap<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.Queue<java.util.Map.Entry<BlockPos, Integer>> queue = new java.util.LinkedList<>();

        visited.add(worldPosition);
        for (BlockPos node : getConnectedNodes()) {
            queue.add(java.util.Map.entry(node, 1));
        }

        while (!queue.isEmpty()) {
            var entry = queue.poll();
            BlockPos pos = entry.getKey();
            int depth = entry.getValue();
            if (!visited.add(pos)) continue;
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageRelayBlockEntity relay) {
                relayDepth.put(pos, depth);
                for (BlockPos neighbor : relay.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        queue.add(java.util.Map.entry(neighbor, depth + 1));
                    }
                }
            }
        }

        // Somme: 5 × profondeur relay pour chaque interface
        int totalCost = 0;
        for (BlockPos ifacePos : networkRegistry.getAllInterfaces()) {
            BlockPos owner = networkRegistry.getOwner(ifacePos);
            if (owner == null || owner.equals(worldPosition)) continue;
            Integer depth = relayDepth.get(owner);
            if (depth != null) {
                totalCost += 5 * depth;
            }
        }
        return totalCost;
    }

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
     */
    public void unlinkInterface(BlockPos interfacePos) {
        networkRegistry.unregisterBlock(interfacePos);
        setChanged();
        syncToClient();
    }

    // === Hives (delegue au HiveManager) ===

    public void linkHive(BlockPos hivePos) { hiveManager.linkHive(hivePos); }
    public void unlinkHive(BlockPos hivePos) { hiveManager.unlinkHive(hivePos); }
    public int getLinkedHiveCount() { return hiveManager.getLinkedHiveCount(); }
    public int getMaxDeliveryBees() { return hiveManager.getMaxDeliveryBees(); }
    public float getHiveMultiplier() { return hiveManager.getHiveMultiplier(); }
    public void notifyLinkedHives() { hiveManager.notifyLinkedHives(); }

    // === Player Feedback ===

    /**
     * Envoie un message action bar a tous les joueurs qui ont un terminal ouvert sur ce reseau.
     */
    public void notifyViewers(Component message) {
        if (level == null || level.isClientSide()) return;
        for (UUID playerId : itemAggregator.getViewerIds()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.displayClientMessage(message, true);
            }
        }
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
        return deliveryManager.getContainerOps().findChestWithItem(template, minCount);
    }

    @Nullable
    public BlockPos findChestWithSpace(ItemStack template, int count) {
        return deliveryManager.getContainerOps().findChestWithSpace(template, count);
    }

    public int countItemInChest(ItemStack template, BlockPos chestPos) {
        return deliveryManager.getContainerOps().countItemInChest(template, chestPos);
    }

    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        return deliveryManager.getContainerOps().extractItemForDelivery(template, count, chestPos);
    }

    /**
     * [FIX] Extrait des items de slots specifiques d'un coffre pour une livraison.
     */
    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos, int[] sourceSlots) {
        return deliveryManager.getContainerOps().extractItemForDelivery(template, count, chestPos, sourceSlots);
    }

    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        return deliveryManager.getContainerOps().depositItemForDelivery(stack, chestPos);
    }

    public boolean isHoneyDepleted() { return deliveryManager.isHoneyDepleted(); }

    // === Honey Buffer ===

    public int getHoneyStored() { return honeyStored; }
    public int getHoneyCapacity() { return honeyCapacity; }

    public void setHoneyStored(int amount) {
        this.honeyStored = Math.max(0, Math.min(amount, honeyCapacity));
    }

    /**
     * Recalcule la capacite du buffer de miel en fonction des essences TOLERANCE.
     * Base 8000 mB + bonus par essence TOLERANCE.
     */
    public void recalculateHoneyCapacity() {
        this.honeyCapacity = BASE_HONEY_CAPACITY + ControllerStats.getHoneyCapacityBonus(essenceSlots);
        if (honeyStored > honeyCapacity) {
            honeyStored = honeyCapacity;
        }
    }

    // === Capability delegation (reservoirs -> controller honey buffer) ===

    /**
     * IFluidHandler exposant le buffer miel interne du controller.
     * Les reservoirs du multibloc deleguent leurs capabilities vers ce handler,
     * permettant aux pipes de remplir le buffer directement sans stocker dans le reservoir.
     */
    private final IFluidHandler honeyBufferHandler = new IFluidHandler() {
        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (honeyStored <= 0) return FluidStack.EMPTY;
            return new FluidStack(ApicaFluids.HONEY_SOURCE.get(), honeyStored);
        }

        @Override
        public int getTankCapacity(int tank) { return honeyCapacity; }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid() == ApicaFluids.HONEY_SOURCE.get();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != ApicaFluids.HONEY_SOURCE.get()) return 0;
            int space = honeyCapacity - honeyStored;
            int toFill = Math.min(resource.getAmount(), space);
            if (toFill <= 0) return 0;
            if (action.execute()) {
                honeyStored += toFill;
                setChanged();
            }
            return toFill;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    @Override
    public IFluidHandler getFluidHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        return honeyBufferHandler;
    }

    // === Bee Capacity ===

    /**
     * Retourne la capacite max d'une bee (items par trajet).
     * Cache recalcule quand les essences changent.
     */
    public int getBeeCapacity() { return beeCapacity; }

    private void recalculateBeeCapacity() {
        this.beeCapacity = ControllerStats.getQuantity(essenceSlots);
    }

    /**
     * Assigne une bee a une InterfaceTask d'interface.
     * Utilise le handler approprié (Import/Export) via la factory.
     * @return true si la task a ete assignee avec succes
     */
    public boolean assignBeeToInterfaceTask(NetworkInterfaceBlockEntity iface, InterfaceTask task) {
        if (level == null) return false;

        AbstractInterfaceTaskHandler handler = InterfaceTaskHandlerFactory.getHandler(
            task.getType(),
            this,
            deliveryManager.getContainerOps()
        );

        return handler.assignTask(iface, task);
    }

    public boolean cancelTask(UUID id) {
        if (deliveryManager.cancelTask(id)) return true;
        requestManager.cancelRequest(id);
        return true;
    }

    public List<com.chapeau.apica.common.block.storage.TaskDisplayData> getTaskDisplayData() {
        return deliveryManager.getTaskDisplayData();
    }

    // === Tick ===

    private static final int TERMINAL_BACKUP_INTERVAL = 20;

    public static void serverTick(StorageControllerBlockEntity be) {
        if (be.level == null) return;
        // [FIX] Pendant le shutdown, ne plus modifier le monde (setChanged, syncToClient, level.setBlock).
        // saveAllChunks() boucle infiniment si des chunks sont re-dirtied apres sauvegarde.
        if (StorageEvents.isShuttingDown()) return;
        long gameTick = be.level.getGameTime();
        long offset = be.worldPosition.hashCode();

        be.itemAggregator.tickSync(gameTick);

        if (be.multiblockManager.isFormed()) {
            be.deliveryManager.tickHoneyConsumption(gameTick);
        }

        be.requestManager.tick(gameTick);
        be.deliveryManager.tickDelivery(gameTick);
        be.processTerminals(gameTick);
        be.tickEditMode();
        be.itemAggregator.cleanupViewers();

        if ((gameTick + offset) % VALIDATION_BATCH_INTERVAL == 0) {
            be.validateNetworkBlocksAmortized();
        }
    }

    /**
     * Parcourt tous les terminaux du reseau et traite leurs flags:
     * - needsProcessPending: requetes en attente a traiter (slot pickup libere)
     * - needsDepositScan: deposit slots changes, creer des EXPORT requests
     * - Backup periodique: force un scan toutes les TERMINAL_BACKUP_INTERVAL ticks
     */
    private void processTerminals(long gameTick) {
        for (BlockPos terminalPos : networkRegistry.getAllTerminals()) {
            if (!level.hasChunkAt(terminalPos)) continue;
            BlockEntity tbe = level.getBlockEntity(terminalPos);
            if (!(tbe instanceof StorageTerminalBlockEntity terminal)) continue;

            // Backup periodique: force un rescan (rattrape les flags manques)
            if ((gameTick + terminalPos.hashCode()) % TERMINAL_BACKUP_INTERVAL == 0) {
                terminal.processPendingRequests();
                terminal.needsDepositScan = true;
            }

            if (terminal.needsProcessPending) {
                terminal.processPendingRequests();
                terminal.needsProcessPending = false;
            }

            if (terminal.needsDepositScan) {
                terminal.scanDepositSlots();
                terminal.needsDepositScan = false;
            }
        }
    }

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        if (StorageEvents.isShuttingDown()) {
            LOGGER.info("[Apica] Removing Controller at {} (shutdown)", worldPosition);
        }
        LOGGER.debug("[Controller] setRemoved START at {}", worldPosition);
        super.setRemoved();
        deliveryManager.killAllDeliveryBees();
        // [FIX] Silent hive cleanup: clear references without world modification (no setBlock/syncToClient)
        // unlinkAllHives() appelait hive.unlinkController() qui fait level.setBlock() + syncToClient()
        // Cela causait des modifications monde en cascade pendant le world unload → hang "saving world"
        // Les hives corrigeront leur etat visuel au prochain serverTick (VALIDATE_INTERVAL)
        // [FIX] Pendant le shutdown, NE PAS appeler level.getBlockEntity() car si la hive a deja
        // ete remove du chunk, getBlockEntity() la RE-CREE → re-dirtie le chunk → boucle infinie.
        if (level != null && !level.isClientSide() && !StorageEvents.isShuttingDown()) {
            for (BlockPos hivePos : networkRegistry.getAllHives()) {
                if (!level.hasChunkAt(hivePos)) continue;
                BlockEntity be = level.getBlockEntity(hivePos);
                if (be instanceof StorageHiveBlockEntity hive) {
                    hive.clearControllerRef();
                }
            }
        }
        if (level != null) {
            MultiblockEvents.unregisterController(level, worldPosition);
        } else {
            MultiblockEvents.unregisterController(worldPosition);
        }
        LOGGER.debug("[Controller] setRemoved END at {}", worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (multiblockManager.isFormed() && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // === Client Sync (override pour envoyer seulement les données client-relevant) ===

    /**
     * Override getUpdateTag pour envoyer un sous-ensemble leger au client.
     * Le client n'a PAS besoin de: delivery tasks, requests, network registry details.
     * Il a besoin de: edit mode, connected nodes, chests, formed state, honey, essences, taken positions.
     * Ceci reduit drastiquement la taille des packets sendBlockUpdated et evite les save hangs
     * causes par des serialisations excessives (chaque syncToClient() appelait saveAdditional complet).
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        // Donnees communes: edit mode, connected nodes, chests enregistres
        saveCommon(tag);

        // Multibloc formed state (necessaire pour les renderers)
        multiblockManager.save(tag);

        // [FIX] Registre central du reseau (necessaire pour edit mode sur relays)
        networkRegistry.save(tag);

        // Honey buffer (necessaire pour la GUI jauge miel)
        tag.putInt("HoneyStored", honeyStored);
        tag.putInt("HoneyCapacity", honeyCapacity);

        // Essence slots (necessaire pour la GUI stats)
        tag.put("EssenceSlots", essenceSlots.serializeNBT(registries));

        // [BM] Coffres pris par d'autres reseaux (necessaire pour outlines edit mode)
        ListTag takenTag = new ListTag();
        for (BlockPos pos : takenChestPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            takenTag.add(posTag);
        }
        tag.put("TakenChests", takenTag);

        return tag;
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (StorageEvents.isShuttingDown()) {
            LOGGER.info("[Apica] Saving Controller at {} — chests:{}, terminals:{}, hives:{}, honey:{}/{}",
                worldPosition,
                networkRegistry.getAllChests().size(),
                networkRegistry.getAllTerminals().size(),
                networkRegistry.getAllHives().size(),
                honeyStored, honeyCapacity);
        }
        LOGGER.debug("[Controller] saveAdditional START at {}", worldPosition);
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

            // Honey buffer
            tag.putInt("HoneyStored", honeyStored);
            tag.putInt("HoneyCapacity", honeyCapacity);

            // [BM] Coffres pris par d'autres reseaux (transient, pour sync client edit mode)
            ListTag takenTag = new ListTag();
            for (BlockPos pos : takenChestPositions) {
                CompoundTag posTag = new CompoundTag();
                posTag.put("Pos", NbtUtils.writeBlockPos(pos));
                takenTag.add(posTag);
            }
            tag.put("TakenChests", takenTag);

        } finally {
            isSaving = false;
            LOGGER.debug("[Controller] saveAdditional END at {}", worldPosition);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isLoading = true;
        try {
            super.loadAdditional(tag, registries);

            // Common: edit mode, connected nodes, chests (from abstract base)
            loadCommon(tag);

            // Managers specifiques au controller
            multiblockManager.load(tag);
            deliveryManager.load(tag, registries);
            requestManager.load(tag, registries);

            // Honey buffer (charger AVANT essence slots pour eviter clamping a 0)
            if (tag.contains("HoneyStored")) {
                honeyStored = tag.getInt("HoneyStored");
            }

            // Essence slots
            if (tag.contains("EssenceSlots")) {
                essenceSlots.deserializeNBT(registries, tag.getCompound("EssenceSlots"));
            }
            recalculateBeeCapacity();
            recalculateHoneyCapacity();

            // [BM] Coffres pris par d'autres reseaux (transient, depuis sync client)
            takenChestPositions.clear();
            if (tag.contains("TakenChests")) {
                ListTag takenTag = tag.getList("TakenChests", Tag.TAG_COMPOUND);
                for (int i = 0; i < takenTag.size(); i++) {
                    NbtUtils.readBlockPos(takenTag.getCompound(i), "Pos").ifPresent(takenChestPositions::add);
                }
            }

            // Registre central: charger ou migrer depuis l'ancien format
            if (tag.contains("NetworkRegistry")) {
                networkRegistry.load(tag);
            } else {
                migrateOldNetworkData(tag);
            }
        } finally {
            isLoading = false;
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
