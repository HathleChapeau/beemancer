/**
 * ============================================================
 * [StorageTerminalMenu.java]
 * Description: Menu du Storage Terminal avec dépôt, pickup, crafting, essences et onglets
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | ApicaMenus                  | Type du menu           | Constructeur          |
 * | StorageTerminalBlockEntity      | Source des données     | Accès inventaire      |
 * | ApicaSlot                   | Slots filtrés          | Essence slots, output |
 * | ApicaTags                   | Tag ESSENCES           | Filtre essence        |
 * | StorageTab                      | Enum onglets           | Visibilité slots      |
 * | TaskDisplayData                 | Données tâches         | Affichage onglet Tasks|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalBlockEntity.java (création du menu)
 * - StorageTerminalScreen.java (affichage)
 * - ApicaMenus.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu.storage;

import com.chapeau.apica.client.gui.screen.storage.StorageTab;
import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.block.storage.TaskDisplayData;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.apica.core.registry.ApicaMenus;
import com.chapeau.apica.core.registry.ApicaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Menu du Storage Terminal avec système d'onglets.
 *
 * Slots Layout:
 * - 0-5:   Deposit slots (2x3, onglet Storage, paginated)
 * - 6-11:  Pickup slots (2x3, output-only, onglet Storage, paginated)
 * - 12-20: Crafting grid (3x3, onglet Storage)
 * - 21:    Crafting result (output-only, onglet Storage)
 * - 22-25: Base essence slots (4, filtrés ESSENCES, onglet Controller)
 * - 26-29: Bonus essence slots (4, dynamic lock, onglet Controller)
 * - 30-65: Player inventory + hotbar (36, tous onglets)
 */
public class StorageTerminalMenu extends AbstractContainerMenu {

    // Slot ranges (menu slot indices, not container indices)
    public static final int VISIBLE_PER_PAGE = 6;
    public static final int DEPOSIT_START = 0;
    public static final int DEPOSIT_END = 6;
    public static final int PICKUP_START = 6;
    public static final int PICKUP_END = 12;
    public static final int CRAFT_START = 12;
    public static final int CRAFT_END = 21;
    public static final int RESULT_INDEX = 21;
    public static final int ESSENCE_START = 22;
    public static final int ESSENCE_BASE_END = 26;
    public static final int ESSENCE_BONUS_START = 26;
    public static final int ESSENCE_END = 30;
    public static final int PLAYER_START = 30;
    public static final int PLAYER_END = 66;
    public static final int TOTAL_SLOTS = 66;

    // Pagination (dynamic based on content)
    public static final int BUTTON_DEPOSIT_NEXT = 100;
    public static final int BUTTON_DEPOSIT_PREV = 101;
    public static final int BUTTON_PICKUP_NEXT = 102;
    public static final int BUTTON_PICKUP_PREV = 103;

    // ContainerData indices
    public static final int DATA_PENDING_COUNT = 0;
    public static final int DATA_PENDING_TYPES = 1;
    public static final int DATA_HONEY_DEPLETED = 2;
    public static final int DATA_FLIGHT_SPEED = 3;
    public static final int DATA_SEARCH_SPEED = 4;
    public static final int DATA_HONEY_RESERVE = 5;
    public static final int DATA_QUANTITY = 6;
    public static final int DATA_HONEY_CONSUMPTION = 7;
    public static final int DATA_HONEY_EFFICIENCY = 8;
    public static final int DATA_ACTIVE_TASKS = 9;
    public static final int DATA_QUEUED_TASKS = 10;
    public static final int DATA_MAX_BEES = 11;
    public static final int DATA_LINKED_HIVES = 12;
    public static final int DATA_DEPOSIT_PAGE = 13;
    public static final int DATA_PICKUP_PAGE = 14;
    public static final int DATA_HONEY_STORED = 15;
    public static final int DATA_HONEY_CAPACITY = 16;
    public static final int DATA_HIVE_MULTIPLIER = 17;
    public static final int DATA_SIZE = 18;

    // Slot positions — Deposit (inside transfer_bg at 7,7: slot bg at 11,11, item at +1)
    // Menu pos = renderSlot pos + 1 (18x18 slot bg, 16x16 item centered)
    private static final int DEPOSIT_X = 12;
    private static final int DEPOSIT_Y = 12;
    // Slot positions — Pickup (inside transfer_bg at 7,71: slot bg at 11,75, item at +1)
    private static final int PICKUP_X = 12;
    private static final int PICKUP_Y = 76;
    // Slot positions — Craft grid (inside player_inventory_bg at 2,140: slots at 14,7)
    public static final int CRAFT_X = 17;
    public static final int CRAFT_Y = 148;
    // Slot positions — Craft result (inside player_inventory_bg: slot at 32,65)
    public static final int RESULT_X = 35;
    public static final int RESULT_Y = 206;
    // Controller tab essences (right panel, unchanged)
    private static final int ESSENCE_X = 155;
    private static final int ESSENCE_Y = 80;
    private static final int ESSENCE_BONUS_Y = 100;
    // Player inventory (inside player_inventory_bg: slots at 96,7)
    public static final int PLAYER_INV_X = 99;
    public static final int PLAYER_INV_Y = 148;
    public static final int HOTBAR_Y = 206;

    private final BlockPos blockPos;
    @Nullable
    private final StorageTerminalBlockEntity terminal;
    private final ContainerData data;
    private final Inventory playerInventory;

    // Pagination state
    private int depositPage = 0;
    private int pickupPage = 0;
    private final java.util.List<PaginatedTabSlot> depositPaginatedSlots = new java.util.ArrayList<>();
    private final java.util.List<PaginatedTabSlot> pickupPaginatedSlots = new java.util.ArrayList<>();

    // Crafting
    private final TransientCraftingContainer craftingContainer;
    private final ResultContainer resultContainer = new ResultContainer();

    // Cache des items agrégés (pour affichage dans la GUI)
    private List<ItemStack> aggregatedItems = new ArrayList<>();
    // Buffer pour accumulation des fragments lors d'un full sync fragmenté
    @Nullable
    private List<ItemStack> fullSyncBuffer = null;

    // Cache des tâches (pour onglet Tasks)
    private List<TaskDisplayData> taskDisplayData = new ArrayList<>();

    // Onglet actif
    private StorageTab activeTab = StorageTab.STORAGE;

    // Client constructor (from network)
    public StorageTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null,
             extraData.readBlockPos(),
             new SimpleContainerData(DATA_SIZE),
             new ItemStackHandler(com.chapeau.apica.common.blockentity.storage.HiveManager.MAX_ESSENCE_SLOTS));
    }

    // Server constructor
    public StorageTerminalMenu(int containerId, Inventory playerInventory,
                                StorageTerminalBlockEntity terminal, BlockPos pos,
                                ItemStackHandler essenceSlots) {
        this(containerId, playerInventory, terminal, pos, null, essenceSlots);
    }

    // Full constructor
    private StorageTerminalMenu(int containerId, Inventory playerInventory,
                                 @Nullable StorageTerminalBlockEntity terminal,
                                 BlockPos pos, @Nullable ContainerData externalData,
                                 ItemStackHandler essenceSlots) {
        super(ApicaMenus.STORAGE_TERMINAL.get(), containerId);
        this.blockPos = pos;
        this.terminal = terminal;
        this.playerInventory = playerInventory;

        // ContainerData
        if (terminal != null) {
            this.data = createServerData(terminal);
        } else {
            this.data = externalData != null ? externalData : new SimpleContainerData(DATA_SIZE);
        }

        // Crafting container
        this.craftingContainer = new TransientCraftingContainer(this, 3, 3);

        // Handlers pour deposit/pickup (serveur: vrais handlers, client: dummy)
        ItemStackHandler depositHandler;
        ItemStackHandler pickupHandler;
        if (terminal != null) {
            depositHandler = terminal.getDepositSlots();
            pickupHandler = terminal.getPickupSlots();
        } else {
            depositHandler = new ItemStackHandler(StorageTerminalBlockEntity.DEPOSIT_SLOTS);
            pickupHandler = new ItemStackHandler(StorageTerminalBlockEntity.PICKUP_SLOTS);
        }

        // === Deposit slots (0-5, paginated, 2 rows x 3 cols) ===
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int slotInPage = col + row * 3;
                PaginatedTabSlot slot = new PaginatedTabSlot(depositHandler, slotInPage,
                    DEPOSIT_X + col * 18, DEPOSIT_Y + row * 18, StorageTab.STORAGE, true);
                depositPaginatedSlots.add(slot);
                this.addSlot(slot);
            }
        }

        // === Pickup slots (6-11, output-only, paginated, 2 rows x 3 cols) ===
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int slotInPage = col + row * 3;
                PaginatedTabSlot slot = new PaginatedTabSlot(pickupHandler, slotInPage,
                    PICKUP_X + col * 18, PICKUP_Y + row * 18, StorageTab.STORAGE, false);
                pickupPaginatedSlots.add(slot);
                this.addSlot(slot);
            }
        }

        // === Crafting grid slots (18-26) ===
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new TabSlot(craftingContainer, col + row * 3,
                    CRAFT_X + col * 18, CRAFT_Y + row * 18, StorageTab.STORAGE));
            }
        }

        // === Crafting result slot (27) ===
        this.addSlot(new TabResultSlot(playerInventory.player, craftingContainer,
            resultContainer, 0, RESULT_X, RESULT_Y, StorageTab.STORAGE));

        // === Base essence slots (28-31) ===
        for (int i = 0; i < 4; i++) {
            this.addSlot(new TabApicaSlot(essenceSlots, i,
                ESSENCE_X + i * 20, ESSENCE_Y, StorageTab.CONTROLLER)
                .withFilter(stack -> stack.is(ApicaTags.Items.ESSENCES)));
        }

        // === Bonus essence slots (32-35, locked by hive count) ===
        for (int i = 0; i < 4; i++) {
            int slotIndex = 4 + i;
            int requiredHives = i + 1;
            this.addSlot(new DynamicTabEssenceSlot(essenceSlots, slotIndex,
                ESSENCE_X + i * 20, ESSENCE_BONUS_Y, StorageTab.CONTROLLER, requiredHives));
        }

        // === Player inventory (36-62) ===
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // === Player hotbar (59-67) ===
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                PLAYER_INV_X + col * 18, HOTBAR_Y));
        }

        this.addDataSlots(this.data);

        // Positionner les slots selon l'onglet initial
        updateSlotPositions();
    }

    /**
     * Crée le ContainerData côté serveur qui lit les données du terminal et controller.
     */
    private ContainerData createServerData(StorageTerminalBlockEntity terminal) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                StorageControllerBlockEntity controller = terminal.getController();
                return switch (index) {
                    case DATA_PENDING_COUNT -> terminal.getTotalPendingCount();
                    case DATA_PENDING_TYPES -> terminal.getPendingRequests().size();
                    case DATA_HONEY_DEPLETED -> {
                        yield (controller != null && controller.isHoneyDepleted()) ? 1 : 0;
                    }
                    case DATA_FLIGHT_SPEED -> {
                        if (controller == null) yield 100;
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getFlightSpeed(controller.getEssenceSlots());
                    }
                    case DATA_SEARCH_SPEED -> {
                        if (controller == null) yield 100;
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getSearchSpeed(controller.getEssenceSlots());
                    }
                    case DATA_HONEY_RESERVE -> {
                        if (controller == null) yield 0;
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getHoneyCapacityBonus(controller.getEssenceSlots());
                    }
                    case DATA_QUANTITY -> {
                        if (controller == null) yield 32;
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getQuantity(controller.getEssenceSlots());
                    }
                    case DATA_HONEY_CONSUMPTION -> {
                        if (controller == null) yield 0;
                        boolean day = controller.getLevel() != null && controller.getLevel().isDay();
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getHoneyConsumption(controller.getEssenceSlots(),
                                controller.getNetworkRegistry().getChestCount(),
                                controller.getHiveMultiplier(),
                                controller.getRelayCount(),
                                controller.getInterfaceRelayCost(), day);
                    }
                    case DATA_HONEY_EFFICIENCY -> {
                        if (controller == null) yield 0;
                        boolean day = controller.getLevel() != null && controller.getLevel().isDay();
                        yield com.chapeau.apica.common.block.storage.ControllerStats
                            .getHoneyEfficiency(controller.getEssenceSlots(), day);
                    }
                    case DATA_ACTIVE_TASKS -> {
                        if (controller == null) yield 0;
                        yield controller.getDeliveryManager().getActiveTaskCount();
                    }
                    case DATA_QUEUED_TASKS -> {
                        if (controller == null) yield 0;
                        yield controller.getDeliveryManager().getQueuedTaskCount();
                    }
                    case DATA_MAX_BEES -> {
                        if (controller == null) yield 0;
                        yield controller.getMaxDeliveryBees();
                    }
                    case DATA_LINKED_HIVES -> {
                        if (controller == null) yield 0;
                        yield controller.getLinkedHiveCount();
                    }
                    case DATA_DEPOSIT_PAGE -> depositPage;
                    case DATA_PICKUP_PAGE -> pickupPage;
                    case DATA_HONEY_STORED -> {
                        if (controller == null) yield 0;
                        yield controller.getHoneyStored();
                    }
                    case DATA_HONEY_CAPACITY -> {
                        if (controller == null) yield 0;
                        yield controller.getHoneyCapacity();
                    }
                    case DATA_HIVE_MULTIPLIER -> {
                        if (controller == null) yield 100;
                        yield Math.round(controller.getHiveMultiplier() * 100.0f);
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() { return DATA_SIZE; }
        };
    }

    // === Tab Management ===

    /**
     * Change l'onglet actif et repositionne les slots.
     */
    public void setActiveTab(StorageTab tab) {
        this.activeTab = tab;
        updateSlotPositions();
    }

    public StorageTab getActiveTab() {
        return activeTab;
    }

    /**
     * Notifie les slots du changement d'onglet.
     * Les TabSlot utilisent isActive() pour se masquer automatiquement.
     */
    private void updateSlotPositions() {
        // Les TabSlot vérifient activeTab dans isActive()
        // Rien d'autre à faire ici, la visibilité est gérée automatiquement.
    }

    // === Crafting Logic ===

    @Override
    public void slotsChanged(Container container) {
        if (container == craftingContainer) {
            updateCraftingResult();
        }
        super.slotsChanged(container);
    }

    private void updateCraftingResult() {
        Level level = playerInventory.player.level();
        if (level.isClientSide()) return;

        CraftingInput input = craftingContainer.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, level);

        if (recipe.isPresent()) {
            ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
            resultContainer.setItem(0, result);
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    // === Quick Move ===

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot == null || !slot.hasItem()) return result;

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        // From terminal/craft/essence slots to player
        if (slotIndex < PLAYER_START) {
            if (!this.moveItemStackTo(slotStack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            // Si c'est le result slot, gérer la décrémentation des ingrédients
            if (slotIndex == RESULT_INDEX) {
                slot.onTake(player, slotStack);
            }
        }
        // From player inventory
        else {
            if (activeTab == StorageTab.STORAGE) {
                // Essayer les deposit slots
                if (!this.moveItemStackTo(slotStack, DEPOSIT_START, DEPOSIT_END, false)) {
                    // Sinon déplacer entre inv et hotbar
                    if (slotIndex < PLAYER_START + 27) {
                        if (!this.moveItemStackTo(slotStack, PLAYER_START + 27, PLAYER_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!this.moveItemStackTo(slotStack, PLAYER_START, PLAYER_START + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else if (activeTab == StorageTab.CONTROLLER) {
                // Essayer les essence slots si c'est une essence
                if (slotStack.is(ApicaTags.Items.ESSENCES)) {
                    if (!this.moveItemStackTo(slotStack, ESSENCE_START, ESSENCE_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Inv <-> Hotbar
                    if (slotIndex < PLAYER_START + 27) {
                        if (!this.moveItemStackTo(slotStack, PLAYER_START + 27, PLAYER_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!this.moveItemStackTo(slotStack, PLAYER_START, PLAYER_START + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else {
                // Tasks tab: inv <-> hotbar only
                if (slotIndex < PLAYER_START + 27) {
                    if (!this.moveItemStackTo(slotStack, PLAYER_START + 27, PLAYER_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, PLAYER_START, PLAYER_START + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal != null) {
            return terminal.stillValid(player);
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // Drop crafting grid contents
        this.clearContainer(player, craftingContainer);

        // Notifier le terminal de la fermeture
        if (terminal != null) {
            terminal.onMenuClosed(player);
        }
    }

    // === Accès aux Items Agrégés ===

    public List<ItemStack> getAggregatedItems() {
        if (terminal != null) {
            return terminal.getAggregatedItems();
        }
        return aggregatedItems;
    }

    public void setAggregatedItems(List<ItemStack> items) {
        this.aggregatedItems = items;
        this.aggregatedItems.sort(java.util.Comparator.comparing(
            stack -> stack.getHoverName().getString()
        ));
    }

    /**
     * Reçoit un fragment de full sync. Accumule les fragments jusqu'au dernier,
     * puis trie et remplace la liste.
     */
    public void receiveFullSyncFragment(List<ItemStack> items, boolean lastFragment) {
        if (!lastFragment) {
            if (fullSyncBuffer == null) {
                fullSyncBuffer = new ArrayList<>(items);
            } else {
                fullSyncBuffer.addAll(items);
            }
        } else {
            if (fullSyncBuffer != null) {
                fullSyncBuffer.addAll(items);
                setAggregatedItems(fullSyncBuffer);
                fullSyncBuffer = null;
            } else {
                setAggregatedItems(new ArrayList<>(items));
            }
        }
    }

    /**
     * Applique des deltas incrémentiels sur le cache local des items agrégés.
     * Chaque delta est un ItemStack: count > 0 = nouveau count pour cet item, count = 0 = item supprimé.
     * Utilise binary search pour insérer les nouveaux items à la bonne position (tri alphabétique maintenu).
     */
    public void applyDeltaItems(List<ItemStack> deltas) {
        for (ItemStack delta : deltas) {
            int deltaCount = delta.getCount();
            ItemStack deltaTemplate = delta.copyWithCount(1);
            boolean found = false;
            for (int i = 0; i < aggregatedItems.size(); i++) {
                ItemStack existing = aggregatedItems.get(i).copyWithCount(1);
                if (ItemStack.isSameItemSameComponents(existing, deltaTemplate)) {
                    if (deltaCount == 0) {
                        aggregatedItems.remove(i);
                    } else {
                        aggregatedItems.get(i).setCount(deltaCount);
                    }
                    found = true;
                    break;
                }
            }
            if (!found && deltaCount > 0) {
                // Binary search insert pour maintenir le tri alphabétique
                String name = delta.getHoverName().getString();
                int insertionPoint = java.util.Collections.binarySearch(
                    aggregatedItems, delta,
                    java.util.Comparator.comparing(s -> s.getHoverName().getString())
                );
                if (insertionPoint < 0) {
                    insertionPoint = -(insertionPoint + 1);
                }
                aggregatedItems.add(insertionPoint, delta.copy());
            }
        }
    }

    // === Accès aux Tâches ===

    public List<TaskDisplayData> getTaskDisplayData() {
        return taskDisplayData;
    }

    public void setTaskDisplayData(List<TaskDisplayData> tasks) {
        this.taskDisplayData = tasks;
    }

    // === Data Accessors ===

    public int getPendingItemCount() { return this.data.get(DATA_PENDING_COUNT); }
    public int getPendingRequestTypes() { return this.data.get(DATA_PENDING_TYPES); }
    public boolean isHoneyDepleted() { return this.data.get(DATA_HONEY_DEPLETED) != 0; }
    public int getFlightSpeed() { return this.data.get(DATA_FLIGHT_SPEED); }
    public int getSearchSpeed() { return this.data.get(DATA_SEARCH_SPEED); }
    public int getHoneyReserveBonus() { return this.data.get(DATA_HONEY_RESERVE); }
    public int getQuantity() { return this.data.get(DATA_QUANTITY); }
    public int getHoneyConsumption() { return this.data.get(DATA_HONEY_CONSUMPTION); }
    public int getHoneyEfficiency() { return this.data.get(DATA_HONEY_EFFICIENCY); }
    public int getActiveTaskCount() { return this.data.get(DATA_ACTIVE_TASKS); }
    public int getQueuedTaskCount() { return this.data.get(DATA_QUEUED_TASKS); }
    public int getMaxBees() { return this.data.get(DATA_MAX_BEES); }
    public int getLinkedHiveCount() { return this.data.get(DATA_LINKED_HIVES); }
    public int getHoneyStored() { return this.data.get(DATA_HONEY_STORED); }
    public int getHoneyCapacity() { return this.data.get(DATA_HONEY_CAPACITY); }
    public int getHiveMultiplier() { return this.data.get(DATA_HIVE_MULTIPLIER); }

    /**
     * Vérifie si un bonus slot essence (index 32-35) est déverrouillé.
     */
    public boolean isBonusSlotUnlocked(int slotIndex) {
        if (slotIndex < ESSENCE_BONUS_START || slotIndex >= ESSENCE_END) return true;
        net.minecraft.world.inventory.Slot slot = slots.get(slotIndex);
        if (slot instanceof DynamicTabEssenceSlot dynamicSlot) {
            return dynamicSlot.isUnlocked();
        }
        return true;
    }

    public int getDepositPage() { return this.data.get(DATA_DEPOSIT_PAGE); }
    public int getPickupPage() { return this.data.get(DATA_PICKUP_PAGE); }

    /**
     * Nombre de pages dynamique pour le depot: basé sur le contenu + 1 page vide.
     */
    public int getDepositPages() {
        int lastNonEmpty = findLastNonEmpty(depositPaginatedSlots.isEmpty() ? null
            : depositPaginatedSlots.get(0).handler);
        int needed = (lastNonEmpty + VISIBLE_PER_PAGE) / VISIBLE_PER_PAGE + 1;
        int maxPages = (depositPaginatedSlots.isEmpty() ? 1
            : (depositPaginatedSlots.get(0).handler.getSlots() + VISIBLE_PER_PAGE - 1) / VISIBLE_PER_PAGE);
        return Math.min(needed, maxPages);
    }

    /**
     * Nombre de pages dynamique pour le pickup: basé sur le contenu + 1 page vide.
     */
    public int getPickupPages() {
        int lastNonEmpty = findLastNonEmpty(pickupPaginatedSlots.isEmpty() ? null
            : pickupPaginatedSlots.get(0).handler);
        int needed = (lastNonEmpty + VISIBLE_PER_PAGE) / VISIBLE_PER_PAGE + 1;
        int maxPages = (pickupPaginatedSlots.isEmpty() ? 1
            : (pickupPaginatedSlots.get(0).handler.getSlots() + VISIBLE_PER_PAGE - 1) / VISIBLE_PER_PAGE);
        return Math.min(needed, maxPages);
    }

    private int findLastNonEmpty(ItemStackHandler handler) {
        if (handler == null) return -1;
        for (int i = handler.getSlots() - 1; i >= 0; i--) {
            if (!handler.getStackInSlot(i).isEmpty()) return i;
        }
        return -1;
    }

    public void setDepositPage(int page) {
        page = Math.max(0, Math.min(page, getDepositPages() - 1));
        this.depositPage = page;
        int offset = page * VISIBLE_PER_PAGE;
        for (PaginatedTabSlot slot : depositPaginatedSlots) {
            slot.setPageOffset(offset);
        }
    }

    public void setPickupPage(int page) {
        page = Math.max(0, Math.min(page, getPickupPages() - 1));
        this.pickupPage = page;
        int offset = page * VISIBLE_PER_PAGE;
        for (PaginatedTabSlot slot : pickupPaginatedSlots) {
            slot.setPageOffset(offset);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BUTTON_DEPOSIT_NEXT -> { setDepositPage(depositPage + 1); yield true; }
            case BUTTON_DEPOSIT_PREV -> { setDepositPage(depositPage - 1); yield true; }
            case BUTTON_PICKUP_NEXT -> { setPickupPage(pickupPage + 1); yield true; }
            case BUTTON_PICKUP_PREV -> { setPickupPage(pickupPage - 1); yield true; }
            default -> false;
        };
    }

    public BlockPos getBlockPos() { return blockPos; }

    @Nullable
    public StorageTerminalBlockEntity getTerminal() { return terminal; }

    public boolean isLinked() {
        return terminal != null && terminal.isLinked();
    }

    // === Inner Classes ===

    /**
     * Slot paginé lié à un onglet. Accède à un ItemStackHandler avec un offset de page.
     * Le paramètre allowPlace contrôle si les items peuvent être déposés (false pour pickup).
     */
    private class PaginatedTabSlot extends Slot {
        private final ItemStackHandler handler;
        private final int slotInPage;
        private final StorageTab requiredTab;
        private final boolean allowPlace;
        private int pageOffset = 0;

        PaginatedTabSlot(ItemStackHandler handler, int slotInPage, int x, int y,
                          StorageTab tab, boolean allowPlace) {
            super(new SimpleContainer(1), 0, x, y);
            this.handler = handler;
            this.slotInPage = slotInPage;
            this.requiredTab = tab;
            this.allowPlace = allowPlace;
        }

        void setPageOffset(int offset) { this.pageOffset = offset; }
        private int actualSlot() { return slotInPage + pageOffset; }

        @Override public ItemStack getItem() {
            int s = actualSlot();
            return s < handler.getSlots() ? handler.getStackInSlot(s) : ItemStack.EMPTY;
        }

        @Override public void set(ItemStack stack) {
            int s = actualSlot();
            if (s < handler.getSlots()) handler.setStackInSlot(s, stack);
        }

        @Override public ItemStack remove(int amount) {
            int s = actualSlot();
            return s < handler.getSlots() ? handler.extractItem(s, amount, false) : ItemStack.EMPTY;
        }

        @Override public int getMaxStackSize() { return handler.getSlotLimit(actualSlot()); }
        @Override public boolean hasItem() { return !getItem().isEmpty(); }
        @Override public boolean mayPlace(ItemStack stack) { return allowPlace; }
        @Override public boolean isActive() { return activeTab == requiredTab; }
        @Override public void setChanged() { }
    }

    /**
     * Slot lié à un onglet. isActive() retourne false quand l'onglet n'est pas actif.
     */
    private class TabSlot extends Slot {
        private final StorageTab requiredTab;

        public TabSlot(Container container, int index, int x, int y, StorageTab tab) {
            super(container, index, x, y);
            this.requiredTab = tab;
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }
    }

    /**
     * Slot output-only lié à un onglet.
     */
    private class OutputOnlyTabSlot extends Slot {
        private final StorageTab requiredTab;

        public OutputOnlyTabSlot(Container container, int index, int x, int y, StorageTab tab) {
            super(container, index, x, y);
            this.requiredTab = tab;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }
    }

    /**
     * ResultSlot lié à un onglet.
     */
    private class TabResultSlot extends ResultSlot {
        private final StorageTab requiredTab;

        public TabResultSlot(Player player, CraftingContainer craftSlots,
                             Container container, int index, int x, int y, StorageTab tab) {
            super(player, craftSlots, container, index, x, y);
            this.requiredTab = tab;
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }
    }

    /**
     * ApicaSlot lié à un onglet.
     */
    private class TabApicaSlot extends ApicaSlot {
        private final StorageTab requiredTab;

        public TabApicaSlot(ItemStackHandler handler, int index, int x, int y, StorageTab tab) {
            super(handler, index, x, y);
            this.requiredTab = tab;
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }
    }

    /**
     * Slot essence dynamique lié à un onglet: toujours rendu (isActive suit l'onglet)
     * mais verrouillé tant que le nombre de hives liées est insuffisant.
     * requiredHives=1 signifie qu'il faut au moins 1 hive pour déverrouiller ce slot.
     */
    private class DynamicTabEssenceSlot extends ApicaSlot {
        private final StorageTab requiredTab;
        private final int requiredHives;

        public DynamicTabEssenceSlot(ItemStackHandler handler, int index, int x, int y,
                                      StorageTab tab, int requiredHives) {
            super(handler, index, x, y);
            this.requiredTab = tab;
            this.requiredHives = requiredHives;
            withFilter(stack -> stack.is(ApicaTags.Items.ESSENCES));
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }

        public boolean isUnlocked() {
            return data.get(DATA_LINKED_HIVES) >= requiredHives;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isUnlocked()) return false;
            return super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!isUnlocked()) return false;
            return super.mayPickup(player);
        }
    }
}
