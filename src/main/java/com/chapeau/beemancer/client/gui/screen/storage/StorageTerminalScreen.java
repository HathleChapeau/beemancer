/**
 * ============================================================
 * [StorageTerminalScreen.java]
 * Description: Screen du Storage Terminal avec rendu programmatique et 3 onglets
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation           |
 * |--------------------------|------------------------|-----------------------|
 * | GuiRenderHelper          | Rendu programmatique   | Fond, slots, onglets  |
 * | StorageTerminalMenu      | Menu associé           | Données à afficher    |
 * | StorageTab               | Enum onglets           | Navigation tabs       |
 * | TaskDisplayData          | Données tâches         | Affichage onglet Tasks|
 * | StorageRequestPacket     | Requête items          | Envoi réseau          |
 * | StorageTaskCancelPacket  | Annulation tâche       | Envoi réseau          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du screen)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import com.chapeau.beemancer.core.network.packets.StorageRequestPacket;
import com.chapeau.beemancer.core.network.packets.StorageTaskCancelPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Screen du Storage Terminal avec rendu 100% programmatique (style Crystallizer).
 *
 * 3 onglets:
 * - Storage: grille items, dépôt, pickup, crafting, recherche
 * - Tasks: liste des tâches avec dépendances et annulation
 * - Controller: stats et slots essence
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    // Dimensions
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 280;

    // Tab bar
    private static final int TAB_Y_OFFSET = -16;
    private static final int TAB_HEIGHT = 16;

    // Storage tab: search
    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 6;
    private static final int SEARCH_WIDTH = 194;

    // Storage tab: virtual grid
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 5;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 24;
    private static final int SLOT_SIZE = 18;

    // Scroll
    private static final int SCROLLBAR_X = 172;
    private static final int SCROLLBAR_Y = 24;
    private static final int SCROLLBAR_HEIGHT = 90;

    // Storage tab: deposit / craft / pickup (below grid)
    private static final int SECTION_Y = 118;
    private static final int DEPOSIT_RENDER_X = 8;
    private static final int CRAFT_RENDER_X = 68;
    private static final int RESULT_RENDER_X = 148;
    private static final int PICKUP_RENDER_X = 174;

    // Player inventory
    private static final int PLAYER_INV_RENDER_X = 35;
    private static final int PLAYER_INV_Y = 178;
    private static final int HOTBAR_RENDER_Y = 236;

    // Tasks tab
    private static final int TASK_LIST_X = 8;
    private static final int TASK_LIST_Y = 6;
    private static final int TASK_ROW_HEIGHT = 18;
    private static final int TASK_CANCEL_SIZE = 10;

    // State
    private StorageTab activeTab = StorageTab.STORAGE;
    private EditBox searchBox;
    private String searchText = "";
    private List<ItemStack> displayedItems = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isScrolling = false;
    private int taskScrollOffset = 0;

    // Request popup
    private boolean showRequestPopup = false;
    private ItemStack requestItem = ItemStack.EMPTY;
    private int requestCount = 0;
    private int requestMax = 0;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Search box (only visible on Storage tab)
        int searchX = this.leftPos + SEARCH_X;
        int searchY = this.topPos + SEARCH_Y;
        this.searchBox = new EditBox(this.font, searchX, searchY, SEARCH_WIDTH, 14,
            Component.translatable("gui.beemancer.storage_terminal.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(activeTab == StorageTab.STORAGE);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setResponder(this::onSearchTextChanged);
        this.addRenderableWidget(this.searchBox);

        refreshDisplayedItems();
    }

    private void onSearchTextChanged(String text) {
        this.searchText = text.toLowerCase(Locale.ROOT);
        this.scrollOffset = 0;
        refreshDisplayedItems();
    }

    private void refreshDisplayedItems() {
        List<ItemStack> allItems = menu.getAggregatedItems();
        displayedItems.clear();

        for (ItemStack stack : allItems) {
            if (searchText.isEmpty() ||
                stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(searchText)) {
                displayedItems.add(stack);
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshDisplayedItems();
    }

    // === Rendering ===

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main background
        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, GUI_WIDTH, GUI_HEIGHT);

        // Tab bar
        renderTabs(g, x, y, mouseX, mouseY);

        // Tab content
        switch (activeTab) {
            case STORAGE -> renderStorageTab(g, x, y, mouseX, mouseY);
            case TASKS -> renderTasksTab(g, x, y, mouseX, mouseY);
            case CONTROLLER -> renderControllerTab(g, x, y, mouseX, mouseY);
        }

        // Player inventory (all tabs)
        GuiRenderHelper.renderPlayerInventory(g, x, y, PLAYER_INV_Y, HOTBAR_RENDER_Y);

        // Request popup (on top of everything)
        if (showRequestPopup) {
            renderRequestPopup(g, mouseX, mouseY);
        }
    }

    private void renderTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        String[] labels = {
            Component.translatable(StorageTab.STORAGE.getTranslationKey()).getString(),
            Component.translatable(StorageTab.TASKS.getTranslationKey()).getString(),
            Component.translatable(StorageTab.CONTROLLER.getTranslationKey()).getString()
        };
        GuiRenderHelper.renderTabBar(g, this.font, x, y + TAB_Y_OFFSET,
            GUI_WIDTH, labels, activeTab.ordinal());
    }

    // === Storage Tab ===

    private void renderStorageTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // Virtual item grid
        renderVirtualGrid(g, x, y, mouseX, mouseY);

        // Scrollbar
        renderScrollbar(g, x, y);

        // Section labels
        g.drawString(font, Component.translatable("gui.beemancer.terminal.deposit"),
            x + DEPOSIT_RENDER_X, y + SECTION_Y - 10, 0x404040, false);
        g.drawString(font, Component.translatable("gui.beemancer.terminal.craft"),
            x + CRAFT_RENDER_X, y + SECTION_Y - 10, 0x404040, false);
        g.drawString(font, Component.translatable("gui.beemancer.terminal.pickup"),
            x + PICKUP_RENDER_X, y + SECTION_Y - 10, 0x404040, false);

        // Deposit slots background (3x3)
        GuiRenderHelper.renderSlotGrid(g, x + DEPOSIT_RENDER_X, y + SECTION_Y, 3, 3);

        // Crafting grid background (3x3)
        GuiRenderHelper.renderSlotGrid(g, x + CRAFT_RENDER_X, y + SECTION_Y, 3, 3);

        // Crafting result slot
        GuiRenderHelper.renderSlot(g, x + RESULT_RENDER_X, y + SECTION_Y + 18);

        // Arrow indicator (craft -> result)
        GuiRenderHelper.renderProgressArrow(g, x + CRAFT_RENDER_X + 56, y + SECTION_Y + 18, 1.0f);

        // Pickup slots background (3x3)
        GuiRenderHelper.renderSlotGrid(g, x + PICKUP_RENDER_X, y + SECTION_Y, 3, 3);

        // Pending indicator
        renderPendingIndicator(g, x, y, mouseX, mouseY);
    }

    private void renderVirtualGrid(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                // Slot background
                GuiRenderHelper.renderSlot(g, slotX, slotY);

                if (index < displayedItems.size()) {
                    ItemStack stack = displayedItems.get(index);

                    // Item icon
                    g.renderItem(stack, slotX + 1, slotY + 1);

                    // Count
                    String countStr = formatCount(stack.getCount());
                    g.drawString(this.font, countStr,
                        slotX + 17 - this.font.width(countStr),
                        slotY + 9, 0xFFFFFF, true);

                    // Hover highlight
                    if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                        g.fillGradient(slotX + 1, slotY + 1,
                            slotX + 17, slotY + 17,
                            0x80FFFFFF, 0x80FFFFFF);
                    }
                }
            }
        }
    }

    private void renderScrollbar(GuiGraphics g, int x, int y) {
        int maxScroll = getMaxScroll();
        int scrollX = x + SCROLLBAR_X;
        int scrollY = y + SCROLLBAR_Y;

        // Track background
        g.fill(scrollX, scrollY, scrollX + 12, scrollY + SCROLLBAR_HEIGHT, 0xFF8B8B8B);

        // Thumb
        if (maxScroll <= 0) {
            g.fill(scrollX + 1, scrollY + 1, scrollX + 11, scrollY + 16, 0xFFC6C6C6);
        } else {
            float ratio = (float) scrollOffset / maxScroll;
            int thumbY = (int) (ratio * (SCROLLBAR_HEIGHT - 15));
            g.fill(scrollX + 1, scrollY + thumbY + 1,
                scrollX + 11, scrollY + thumbY + 16, 0xFFC6C6C6);
        }
    }

    private void renderPendingIndicator(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int pendingCount = menu.getPendingItemCount();
        if (pendingCount <= 0) return;

        int indicatorX = x + PICKUP_RENDER_X;
        int indicatorY = y + SECTION_Y - 20;
        int indicatorWidth = 54;
        int indicatorHeight = 10;

        g.fill(indicatorX, indicatorY, indicatorX + indicatorWidth, indicatorY + indicatorHeight, 0xFFFF8800);
        g.fill(indicatorX + 1, indicatorY + 1, indicatorX + indicatorWidth - 1, indicatorY + indicatorHeight - 1, 0xFF442200);

        String text = "\u23F3 " + formatCount(pendingCount);
        int textWidth = this.font.width(text);
        int textX = indicatorX + (indicatorWidth - textWidth) / 2;
        g.drawString(this.font, text, textX, indicatorY + 1, 0xFFFFAA00, false);
    }

    // === Tasks Tab ===

    private void renderTasksTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        List<TaskDisplayData> tasks = menu.getTaskDisplayData();
        int activeCount = menu.getActiveTaskCount();
        int queuedCount = menu.getQueuedTaskCount();

        int currentY = y + TASK_LIST_Y;

        // Active Tasks header
        g.drawString(font, Component.translatable("gui.beemancer.tasks.active")
            .append(" (" + activeCount + "/2)"),
            x + TASK_LIST_X, currentY, 0xFFAA00, false);
        currentY += 12;

        // Render each task
        int maxVisible = 8;
        int rendered = 0;

        for (int i = taskScrollOffset; i < tasks.size() && rendered < maxVisible; i++) {
            TaskDisplayData task = tasks.get(i);
            boolean isActive = task.state().equals("FLYING") || task.state().equals("WAITING")
                || task.state().equals("RETURNING");

            // Separator between active and queued
            if (!isActive && rendered > 0 && i > 0) {
                TaskDisplayData prev = tasks.get(i - 1);
                boolean prevActive = prev.state().equals("FLYING") || prev.state().equals("WAITING")
                    || prev.state().equals("RETURNING");
                if (prevActive) {
                    currentY += 4;
                    g.drawString(font, Component.translatable("gui.beemancer.tasks.queued")
                        .append(" (" + queuedCount + ")"),
                        x + TASK_LIST_X, currentY, 0xAAAA00, false);
                    currentY += 12;
                }
            }

            // Dependency connector (L shape)
            if (!task.dependencyIds().isEmpty()) {
                g.fill(x + TASK_LIST_X + 2, currentY - 2, x + TASK_LIST_X + 3, currentY + 8, 0xFF888888);
                g.fill(x + TASK_LIST_X + 2, currentY + 7, x + TASK_LIST_X + 10, currentY + 8, 0xFF888888);
                g.drawString(font, "L", x + TASK_LIST_X, currentY, 0xFF888888, false);
            }

            int taskX = x + TASK_LIST_X + (task.dependencyIds().isEmpty() ? 0 : 12);

            // Item icon
            if (!task.template().isEmpty()) {
                g.renderItem(task.template(), taskX, currentY);
            }

            // Item name + count
            String itemName = task.template().getHoverName().getString();
            if (itemName.length() > 16) itemName = itemName.substring(0, 14) + "..";
            g.drawString(font, itemName + " x" + task.count(),
                taskX + 18, currentY + 4, 0xFFFFFF, false);

            // State badge
            int stateColor = getStateColor(task.state());
            String stateLabel = getStateLabel(task.state());
            int stateX = x + GUI_WIDTH - 60;
            g.drawString(font, stateLabel, stateX, currentY + 4, stateColor, false);

            // Type indicator
            String typeChar = task.type().equals("DEPOSIT") ? "\u2193" : "\u2191";
            int typeColor = task.type().equals("DEPOSIT") ? 0xFF44AA44 : 0xFF4488FF;
            g.drawString(font, typeChar, stateX - 10, currentY + 4, typeColor, false);

            // Cancel button (X)
            int cancelX = x + GUI_WIDTH - 18;
            int cancelY = currentY + 2;
            boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
                mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE;
            g.drawString(font, "X", cancelX, cancelY, hoverCancel ? 0xFFFF4444 : 0xFFAA0000, false);

            currentY += TASK_ROW_HEIGHT;
            rendered++;
        }

        if (tasks.isEmpty()) {
            g.drawString(font, Component.translatable("gui.beemancer.tasks.empty"),
                x + TASK_LIST_X, currentY, 0xFF888888, false);
        }
    }

    private int getStateColor(String state) {
        return switch (state) {
            case "FLYING" -> 0xFF44AAFF;
            case "WAITING" -> 0xFFFFAA00;
            case "RETURNING" -> 0xFF44FF44;
            case "QUEUED" -> 0xFFAAAAAA;
            case "COMPLETED" -> 0xFF00FF00;
            case "FAILED" -> 0xFFFF0000;
            default -> 0xFFFFFFFF;
        };
    }

    private String getStateLabel(String state) {
        return switch (state) {
            case "FLYING" -> "Flying";
            case "WAITING" -> "Working";
            case "RETURNING" -> "Return";
            case "QUEUED" -> "Queued";
            case "COMPLETED" -> "Done";
            case "FAILED" -> "Failed";
            default -> state;
        };
    }

    // === Controller Tab ===

    private void renderControllerTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int statX = x + 8;
        int statY = y + 10;
        int lineHeight = 14;

        // Title
        g.drawString(font, Component.translatable("container.beemancer.storage_controller"),
            statX, statY, 0x404040, false);
        statY += lineHeight + 4;

        // Stats
        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.flight_speed",
            menu.getFlightSpeed() + "%");
        statY += lineHeight;

        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.search_speed",
            menu.getSearchSpeed() + "%");
        statY += lineHeight;

        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.craft_speed",
            menu.getCraftSpeed() + "%");
        statY += lineHeight;

        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.quantity",
            String.valueOf(menu.getQuantity()));
        statY += lineHeight;

        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.honey_consumption",
            menu.getHoneyConsumption() + " mB/s");
        statY += lineHeight;

        int effBonus = menu.getHoneyEfficiency();
        String effText = effBonus > 0 ? "100% + " + effBonus + "%" : "100%";
        drawStat(g, x, statX, statY, "gui.beemancer.storage_controller.honey_efficiency", effText);
        statY += lineHeight + 8;

        // Essence slots label
        g.drawString(font, Component.translatable("gui.beemancer.terminal.essences"),
            statX, statY - 2, 0x404040, false);

        // Essence slots background (4 slots)
        int essenceRenderX = x + 77;
        int essenceRenderY = y + 80;
        for (int i = 0; i < 4; i++) {
            GuiRenderHelper.renderSlot(g, essenceRenderX + i * 20, essenceRenderY);
        }
    }

    private void drawStat(GuiGraphics g, int containerX, int labelX, int y,
                           String labelKey, String value) {
        g.drawString(font, Component.translatable(labelKey), labelX, y, 0x404040, false);
        int valueWidth = font.width(value);
        g.drawString(font, value, containerX + GUI_WIDTH - 8 - valueWidth, y, 0x206040, false);
    }

    // === Request Popup ===

    private void renderRequestPopup(GuiGraphics g, int mouseX, int mouseY) {
        int popupWidth = 120;
        int popupHeight = 80;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        // Background
        g.fill(popupX - 2, popupY - 2, popupX + popupWidth + 2, popupY + popupHeight + 2, 0xFF000000);
        g.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF3C3C3C);

        // Title
        Component title = Component.translatable("gui.beemancer.storage_terminal.request");
        g.drawCenteredString(this.font, title, popupX + popupWidth / 2, popupY + 5, 0xFFFFFF);

        // Item icon and name
        g.renderItem(requestItem, popupX + 10, popupY + 20);
        String name = requestItem.getHoverName().getString();
        if (name.length() > 14) name = name.substring(0, 12) + "..";
        g.drawString(this.font, name, popupX + 30, popupY + 24, 0xFFFFFF);

        // Count display
        String countText = String.valueOf(requestCount);
        g.drawCenteredString(this.font, countText, popupX + popupWidth / 2, popupY + 42, 0xFFFF00);

        // Buttons: --- -- - [count] + ++ +++
        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        String[] labels = {"---", "--", "-", "+", "++", "+++"};
        int[] deltas = {-64, -10, -1, 1, 10, 64};

        for (int i = 0; i < 6; i++) {
            boolean hover = mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12;
            int color = hover ? 0xFF8080FF : 0xFFAAAAAA;
            g.drawString(this.font, labels[i], positions[i], buttonY, color);
        }

        // Cancel / Request buttons
        int cancelX = popupX + 10;
        int requestXBtn = popupX + 70;
        int actionY = popupY + popupHeight - 15;

        boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + 40 &&
            mouseY >= actionY && mouseY < actionY + 10;
        boolean hoverRequest = mouseX >= requestXBtn && mouseX < requestXBtn + 40 &&
            mouseY >= actionY && mouseY < actionY + 10;

        g.drawString(this.font, Component.translatable("gui.beemancer.cancel"),
            cancelX, actionY, hoverCancel ? 0xFFFF8080 : 0xFFAAAAAA);
        g.drawString(this.font, Component.translatable("gui.beemancer.request"),
            requestXBtn, actionY, hoverRequest ? 0xFF80FF80 : 0xFFAAAAAA);
    }

    // === Main Render ===

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        // Virtual grid tooltip (Storage tab only)
        if (activeTab == StorageTab.STORAGE && !showRequestPopup) {
            renderVirtualGridTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Inventory label
        g.drawString(font, playerInventoryTitle,
            PLAYER_INV_RENDER_X - leftPos + 35, PLAYER_INV_Y - 11, 0x404040, false);
    }

    private void renderVirtualGridTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (activeTab != StorageTab.STORAGE) return;
        int x = this.leftPos;
        int y = this.topPos;
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size() && isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                    g.renderTooltip(this.font, displayedItems.get(index), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    // === Input Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Popup takes priority
        if (showRequestPopup) {
            return handleRequestPopupClick(mouseX, mouseY, button);
        }

        int x = this.leftPos;
        int y = this.topPos;

        // Tab clicks
        int tabWidth = GUI_WIDTH / 3;
        if (mouseY >= y + TAB_Y_OFFSET && mouseY < y + TAB_Y_OFFSET + TAB_HEIGHT) {
            for (int i = 0; i < 3; i++) {
                int tabX = x + i * tabWidth;
                if (mouseX >= tabX && mouseX < tabX + tabWidth) {
                    switchTab(StorageTab.values()[i]);
                    return true;
                }
            }
        }

        // Storage tab interactions
        if (activeTab == StorageTab.STORAGE) {
            // Virtual grid click
            int startIndex = scrollOffset * GRID_COLS;
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int index = startIndex + row * GRID_COLS + col;
                    int slotX = x + GRID_X + col * SLOT_SIZE;
                    int slotY = y + GRID_Y + row * SLOT_SIZE;

                    if (index < displayedItems.size() && isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                        if (!menu.isHoneyDepleted()) {
                            openRequestPopup(displayedItems.get(index));
                        }
                        return true;
                    }
                }
            }

            // Scrollbar click
            if (mouseX >= x + SCROLLBAR_X && mouseX < x + SCROLLBAR_X + 12 &&
                mouseY >= y + SCROLLBAR_Y && mouseY < y + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
                isScrolling = true;
                return true;
            }
        }

        // Tasks tab: cancel button clicks
        if (activeTab == StorageTab.TASKS) {
            return handleTasksCancelClick(mouseX, mouseY);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTasksCancelClick(double mouseX, double mouseY) {
        List<TaskDisplayData> tasks = menu.getTaskDisplayData();
        int x = this.leftPos;
        int y = this.topPos;
        int currentY = y + TASK_LIST_Y + 12;

        int maxVisible = 8;
        int rendered = 0;

        for (int i = taskScrollOffset; i < tasks.size() && rendered < maxVisible; i++) {
            TaskDisplayData task = tasks.get(i);

            // Check separator space
            if (rendered > 0 && i > 0) {
                boolean isActive = task.state().equals("FLYING") || task.state().equals("WAITING")
                    || task.state().equals("RETURNING");
                TaskDisplayData prev = tasks.get(i - 1);
                boolean prevActive = prev.state().equals("FLYING") || prev.state().equals("WAITING")
                    || prev.state().equals("RETURNING");
                if (!isActive && prevActive) {
                    currentY += 16;
                }
            }

            int cancelX = x + GUI_WIDTH - 18;
            int cancelY = currentY + 2;

            if (mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
                mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE) {
                // Send cancel packet
                PacketDistributor.sendToServer(
                    new StorageTaskCancelPacket(menu.getBlockPos(), task.taskId())
                );
                return true;
            }

            currentY += TASK_ROW_HEIGHT;
            rendered++;
        }
        return false;
    }

    private void switchTab(StorageTab tab) {
        this.activeTab = tab;
        this.menu.setActiveTab(tab);

        if (searchBox != null) {
            searchBox.setVisible(tab == StorageTab.STORAGE);
        }
    }

    private boolean handleRequestPopupClick(double mouseX, double mouseY, int button) {
        int popupWidth = 120;
        int popupHeight = 80;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        // Count buttons
        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        int[] deltas = {-64, -10, -1, 1, 10, 64};

        for (int i = 0; i < 6; i++) {
            if (mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12) {
                requestCount = Mth.clamp(requestCount + deltas[i], 0, requestMax);
                return true;
            }
        }

        // Cancel / Request buttons
        int cancelX = popupX + 10;
        int requestXBtn = popupX + 70;
        int actionY = popupY + popupHeight - 15;

        if (mouseY >= actionY && mouseY < actionY + 10) {
            if (mouseX >= cancelX && mouseX < cancelX + 40) {
                closeRequestPopup();
                return true;
            }
            if (mouseX >= requestXBtn && mouseX < requestXBtn + 40) {
                submitRequest();
                return true;
            }
        }

        // Click outside closes popup
        if (mouseX < popupX || mouseX > popupX + popupWidth ||
            mouseY < popupY || mouseY > popupY + popupHeight) {
            closeRequestPopup();
            return true;
        }

        return true;
    }

    private void openRequestPopup(ItemStack item) {
        this.showRequestPopup = true;
        this.requestItem = item.copy();
        this.requestMax = item.getCount();
        this.requestCount = Math.min(64, requestMax);
    }

    private void closeRequestPopup() {
        this.showRequestPopup = false;
        this.requestItem = ItemStack.EMPTY;
        this.requestCount = 0;
        this.requestMax = 0;
    }

    private void submitRequest() {
        if (requestCount > 0 && !requestItem.isEmpty()) {
            PacketDistributor.sendToServer(
                new StorageRequestPacket(menu.getBlockPos(), requestItem, requestCount)
            );
        }
        closeRequestPopup();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling && activeTab == StorageTab.STORAGE) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                float scrollRatio = (float) (mouseY - this.topPos - SCROLLBAR_Y) / (SCROLLBAR_HEIGHT - 15);
                scrollOffset = Mth.clamp((int) (scrollRatio * maxScroll), 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!showRequestPopup) {
            if (activeTab == StorageTab.STORAGE) {
                int maxScroll = getMaxScroll();
                scrollOffset = Mth.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
                return true;
            }
            if (activeTab == StorageTab.TASKS) {
                int maxTaskScroll = Math.max(0, menu.getTaskDisplayData().size() - 8);
                taskScrollOffset = Mth.clamp(taskScrollOffset - (int) scrollY, 0, maxTaskScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showRequestPopup) {
            if (keyCode == 256) {
                closeRequestPopup();
                return true;
            }
            return true;
        }

        if (activeTab == StorageTab.STORAGE && this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) {
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeTab == StorageTab.STORAGE && this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    // === Helpers ===

    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    private boolean isMouseOverSlot(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
            mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private int getMaxScroll() {
        int totalRows = (displayedItems.size() + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, totalRows - GRID_ROWS);
    }
}
