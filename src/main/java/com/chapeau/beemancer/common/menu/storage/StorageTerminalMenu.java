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
 * | BeemancerMenus                  | Type du menu           | Constructeur          |
 * | StorageTerminalBlockEntity      | Source des données     | Accès inventaire      |
 * | BeemancerSlot                   | Slots filtrés          | Essence slots, output |
 * | BeemancerTags                   | Tag ESSENCES           | Filtre essence        |
 * | StorageTab                      | Enum onglets           | Visibilité slots      |
 * | TaskDisplayData                 | Données tâches         | Affichage onglet Tasks|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalBlockEntity.java (création du menu)
 * - StorageTerminalScreen.java (affichage)
 * - BeemancerMenus.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.client.gui.screen.storage.StorageTab;
import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.CraftingContainer;
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
 * - 0-8:   Deposit slots (3x3, onglet Storage)
 * - 9-17:  Pickup slots (3x3, output-only, onglet Storage)
 * - 18-26: Crafting grid (3x3, onglet Storage)
 * - 27:    Crafting result (output-only, onglet Storage)
 * - 28-31: Essence slots (4, filtrés ESSENCES, onglet Controller)
 * - 32-67: Player inventory + hotbar (36, tous onglets)
 */
public class StorageTerminalMenu extends AbstractContainerMenu {

    // Slot ranges
    public static final int DEPOSIT_START = 0;
    public static final int DEPOSIT_END = 9;
    public static final int PICKUP_START = 9;
    public static final int PICKUP_END = 18;
    public static final int CRAFT_START = 18;
    public static final int CRAFT_END = 27;
    public static final int RESULT_INDEX = 27;
    public static final int ESSENCE_START = 28;
    public static final int ESSENCE_END = 32;
    public static final int PLAYER_START = 32;
    public static final int PLAYER_END = 68;
    public static final int TOTAL_SLOTS = 68;

    // ContainerData indices
    public static final int DATA_PENDING_COUNT = 0;
    public static final int DATA_PENDING_TYPES = 1;
    public static final int DATA_HONEY_DEPLETED = 2;
    public static final int DATA_FLIGHT_SPEED = 3;
    public static final int DATA_SEARCH_SPEED = 4;
    public static final int DATA_CRAFT_SPEED = 5;
    public static final int DATA_QUANTITY = 6;
    public static final int DATA_HONEY_CONSUMPTION = 7;
    public static final int DATA_HONEY_EFFICIENCY = 8;
    public static final int DATA_ACTIVE_TASKS = 9;
    public static final int DATA_QUEUED_TASKS = 10;
    public static final int DATA_MAX_BEES = 11;
    public static final int DATA_SIZE = 12;

    // Slot positions — Left panel (deposit/craft/pickup stacked vertically)
    private static final int DEPOSIT_X = 23;
    private static final int DEPOSIT_Y = 14;
    private static final int CRAFT_X = 23;
    private static final int CRAFT_Y = 82;
    private static final int RESULT_X = 41;
    private static final int RESULT_Y = 138;
    private static final int PICKUP_X = 23;
    private static final int PICKUP_Y = 170;
    // Controller tab essences (right panel)
    private static final int ESSENCE_X = 155;
    private static final int ESSENCE_Y = 80;
    // Player inventory (right panel)
    private static final int PLAYER_INV_X = 89;
    private static final int PLAYER_INV_Y = 134;
    private static final int HOTBAR_Y = 194;

    private final BlockPos blockPos;
    @Nullable
    private final StorageTerminalBlockEntity terminal;
    private final ContainerData data;
    private final Inventory playerInventory;

    // Crafting
    private final TransientCraftingContainer craftingContainer;
    private final ResultContainer resultContainer = new ResultContainer();

    // Cache des items agrégés (pour affichage dans la GUI)
    private List<ItemStack> aggregatedItems = new ArrayList<>();

    // Cache des tâches (pour onglet Tasks)
    private List<TaskDisplayData> taskDisplayData = new ArrayList<>();

    // Onglet actif
    private StorageTab activeTab = StorageTab.STORAGE;

    // Client constructor (from network)
    public StorageTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, null,
             extraData.readBlockPos(),
             new SimpleContainerData(DATA_SIZE),
             new ItemStackHandler(4));
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
        super(BeemancerMenus.STORAGE_TERMINAL.get(), containerId);
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

        // Container pour deposit/pickup (si serveur utilise le terminal, sinon dummy)
        Container container;
        if (terminal != null) {
            container = terminal;
        } else {
            container = new SimpleContainer(StorageTerminalBlockEntity.TOTAL_SLOTS);
        }

        // === Deposit slots (0-8) ===
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new TabSlot(container, index,
                    DEPOSIT_X + col * 18, DEPOSIT_Y + row * 18, StorageTab.STORAGE));
            }
        }

        // === Pickup slots (9-17, output-only) ===
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = StorageTerminalBlockEntity.DEPOSIT_SLOTS + col + row * 3;
                this.addSlot(new OutputOnlyTabSlot(container, index,
                    PICKUP_X + col * 18, PICKUP_Y + row * 18, StorageTab.STORAGE));
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

        // === Essence slots (28-31) ===
        for (int i = 0; i < 4; i++) {
            this.addSlot(new TabBeemancerSlot(essenceSlots, i,
                ESSENCE_X + i * 20, ESSENCE_Y, StorageTab.CONTROLLER)
                .withFilter(stack -> stack.is(BeemancerTags.Items.ESSENCES)));
        }

        // === Player inventory (32-58) ===
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
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getFlightSpeed(controller.getEssenceSlots());
                    }
                    case DATA_SEARCH_SPEED -> {
                        if (controller == null) yield 100;
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getSearchSpeed(controller.getEssenceSlots());
                    }
                    case DATA_CRAFT_SPEED -> {
                        if (controller == null) yield 100;
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getCraftSpeed(controller.getEssenceSlots());
                    }
                    case DATA_QUANTITY -> {
                        if (controller == null) yield 32;
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getQuantity(controller.getEssenceSlots());
                    }
                    case DATA_HONEY_CONSUMPTION -> {
                        if (controller == null) yield 0;
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getHoneyConsumption(controller.getEssenceSlots(),
                                controller.getChestManager().getRegisteredChestCount());
                    }
                    case DATA_HONEY_EFFICIENCY -> {
                        if (controller == null) yield 0;
                        yield com.chapeau.beemancer.common.block.storage.ControllerStats
                            .getHoneyEfficiency(controller.getEssenceSlots());
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
                if (slotStack.is(BeemancerTags.Items.ESSENCES)) {
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
    public int getCraftSpeed() { return this.data.get(DATA_CRAFT_SPEED); }
    public int getQuantity() { return this.data.get(DATA_QUANTITY); }
    public int getHoneyConsumption() { return this.data.get(DATA_HONEY_CONSUMPTION); }
    public int getHoneyEfficiency() { return this.data.get(DATA_HONEY_EFFICIENCY); }
    public int getActiveTaskCount() { return this.data.get(DATA_ACTIVE_TASKS); }
    public int getQueuedTaskCount() { return this.data.get(DATA_QUEUED_TASKS); }
    public int getMaxBees() { return this.data.get(DATA_MAX_BEES); }

    public BlockPos getBlockPos() { return blockPos; }

    @Nullable
    public StorageTerminalBlockEntity getTerminal() { return terminal; }

    public boolean isLinked() {
        return terminal != null && terminal.isLinked();
    }

    // === Inner Classes ===

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
     * BeemancerSlot lié à un onglet.
     */
    private class TabBeemancerSlot extends BeemancerSlot {
        private final StorageTab requiredTab;

        public TabBeemancerSlot(ItemStackHandler handler, int index, int x, int y, StorageTab tab) {
            super(handler, index, x, y);
            this.requiredTab = tab;
        }

        @Override
        public boolean isActive() {
            return activeTab == requiredTab;
        }
    }
}
