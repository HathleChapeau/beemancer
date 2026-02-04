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

/**
 * Screen du Storage Terminal avec layout panneau gauche + droite.
 *
 * Gauche: onglets verticaux + deposit/craft/result/pickup
 * Droite: search + grille virtuelle 9x5 + inventaire joueur
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    // Dimensions
    private static final int GUI_WIDTH = 258;
    private static final int GUI_HEIGHT = 230;

    // Vertical tabs (far left)
    private static final int VTAB_X = 2;
    private static final int VTAB_Y = 4;
    private static final int VTAB_W = 18;
    private static final int VTAB_H = 20;
    private static final int VTAB_GAP = 2;

    // Left panel — deposit/craft/result/pickup
    private static final int DEPOSIT_LABEL_X = 22;
    private static final int DEPOSIT_LABEL_Y = 4;
    private static final int DEPOSIT_RENDER_X = 23;
    private static final int DEPOSIT_RENDER_Y = 14;

    private static final int CRAFT_LABEL_X = 22;
    private static final int CRAFT_LABEL_Y = 72;
    private static final int CRAFT_RENDER_X = 23;
    private static final int CRAFT_RENDER_Y = 82;

    private static final int RESULT_RENDER_X = 40;
    private static final int RESULT_RENDER_Y = 138;

    private static final int PICKUP_LABEL_X = 22;
    private static final int PICKUP_LABEL_Y = 160;
    private static final int PICKUP_RENDER_X = 23;
    private static final int PICKUP_RENDER_Y = 170;

    // Right panel — search + virtual grid
    private static final int SEARCH_X = 82;
    private static final int SEARCH_Y = 6;
    private static final int SEARCH_WIDTH = 154;

    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 5;
    private static final int GRID_X = 82;
    private static final int GRID_Y = 24;
    private static final int SLOT_SIZE = 18;

    private static final int SCROLLBAR_X = 246;
    private static final int SCROLLBAR_Y = 24;
    private static final int SCROLLBAR_HEIGHT = 90;

    // Right panel — player inventory
    private static final int PLAYER_INV_RENDER_X = 89;
    private static final int PLAYER_INV_Y = 134;
    private static final int HOTBAR_RENDER_Y = 194;

    // Tasks tab
    private static final int TASK_LIST_X = 82;
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
    }

    @Override
    protected void init() {
        super.init();

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

        // Vertical tabs
        renderVerticalTabs(g, x, y, mouseX, mouseY);

        // Left panel: deposit/craft/result/pickup slot backgrounds + labels
        renderLeftPanel(g, x, y);

        // Tab content (right panel area)
        switch (activeTab) {
            case STORAGE -> renderStorageTab(g, x, y, mouseX, mouseY);
            case TASKS -> renderTasksTab(g, x, y, mouseX, mouseY);
            case CONTROLLER -> renderControllerTab(g, x, y, mouseX, mouseY);
        }

        // Player inventory (right panel, all tabs)
        renderPlayerInv(g, x, y);

        // Request popup (on top)
        if (showRequestPopup) {
            renderRequestPopup(g, mouseX, mouseY);
        }
    }

    private void renderVerticalTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        String[] labels = {"S", "T", "C"};
        for (int i = 0; i < 3; i++) {
            int tabY = y + VTAB_Y + i * (VTAB_H + VTAB_GAP);
            GuiRenderHelper.renderVerticalTab(g, this.font,
                x + VTAB_X, tabY, VTAB_W, VTAB_H,
                activeTab.ordinal() == i, labels[i]);
        }
    }

    private void renderLeftPanel(GuiGraphics g, int x, int y) {
        // Deposit
        g.drawString(font, Component.translatable("gui.beemancer.terminal.deposit"),
            x + DEPOSIT_LABEL_X, y + DEPOSIT_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + DEPOSIT_RENDER_X, y + DEPOSIT_RENDER_Y, 3, 3);

        // Craft
        g.drawString(font, Component.translatable("gui.beemancer.terminal.craft"),
            x + CRAFT_LABEL_X, y + CRAFT_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + CRAFT_RENDER_X, y + CRAFT_RENDER_Y, 3, 3);

        // Result slot (below craft, offset right)
        GuiRenderHelper.renderSlot(g, x + RESULT_RENDER_X, y + RESULT_RENDER_Y);

        // Pickup
        g.drawString(font, Component.translatable("gui.beemancer.terminal.pickup"),
            x + PICKUP_LABEL_X, y + PICKUP_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + PICKUP_RENDER_X, y + PICKUP_RENDER_Y, 3, 3);
    }

    private void renderPlayerInv(GuiGraphics g, int x, int y) {
        // Separator line
        g.fill(x + 82, y + 120, x + GUI_WIDTH - 7, y + 121, 0xFF8B8B8B);

        // Inventory label
        g.drawString(font, Component.translatable("container.inventory"),
            x + PLAYER_INV_RENDER_X, y + 123, 0x404040, false);

        // Inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiRenderHelper.renderSlot(g,
                    x + PLAYER_INV_RENDER_X + col * 18,
                    y + PLAYER_INV_Y + row * 18);
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            GuiRenderHelper.renderSlot(g,
                x + PLAYER_INV_RENDER_X + col * 18,
                y + HOTBAR_RENDER_Y);
        }
    }

    // === Storage Tab ===

    private void renderStorageTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // Virtual item grid
        renderVirtualGrid(g, x, y, mouseX, mouseY);

        // Scrollbar
        renderScrollbar(g, x, y);

        // Pending indicator
        renderPendingIndicator(g, x, y);
    }

    private void renderVirtualGrid(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                GuiRenderHelper.renderSlot(g, slotX, slotY);

                if (index < displayedItems.size()) {
                    ItemStack stack = displayedItems.get(index);
                    g.renderItem(stack, slotX + 1, slotY + 1);

                    String countStr = formatCount(stack.getCount());
                    g.drawString(this.font, countStr,
                        slotX + 17 - this.font.width(countStr),
                        slotY + 9, 0xFFFFFF, true);

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

        g.fill(scrollX, scrollY, scrollX + 12, scrollY + SCROLLBAR_HEIGHT, 0xFF8B8B8B);

        if (maxScroll <= 0) {
            g.fill(scrollX + 1, scrollY + 1, scrollX + 11, scrollY + 16, 0xFFC6C6C6);
        } else {
            float ratio = (float) scrollOffset / maxScroll;
            int thumbY = (int) (ratio * (SCROLLBAR_HEIGHT - 15));
            g.fill(scrollX + 1, scrollY + thumbY + 1,
                scrollX + 11, scrollY + thumbY + 16, 0xFFC6C6C6);
        }
    }

    private void renderPendingIndicator(GuiGraphics g, int x, int y) {
        int pendingCount = menu.getPendingItemCount();
        if (pendingCount <= 0) return;

        int indicatorX = x + GRID_X;
        int indicatorY = y + GRID_Y + GRID_ROWS * SLOT_SIZE + 2;
        String text = "\u23F3 " + formatCount(pendingCount) + " pending";
        g.drawString(this.font, text, indicatorX, indicatorY, 0xFFFFAA00, false);
    }

    // === Tasks Tab ===

    private void renderTasksTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        List<TaskDisplayData> tasks = menu.getTaskDisplayData();

        List<TaskDisplayData> requestTasks = new ArrayList<>();
        List<TaskDisplayData> automationTasks = new ArrayList<>();
        for (TaskDisplayData task : tasks) {
            if ("AUTOMATION".equals(task.origin())) {
                automationTasks.add(task);
            } else {
                requestTasks.add(task);
            }
        }

        int currentY = y + TASK_LIST_Y;
        int maxVisible = 8;
        int rendered = 0;
        int skipped = 0;
        int totalEntries = getTotalTaskEntries(requestTasks, automationTasks);

        // Requests header
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            g.drawString(font, Component.translatable("gui.beemancer.tasks.requests")
                .append(" (" + requestTasks.size() + ")"),
                x + TASK_LIST_X, currentY, 0xFFFFAA00, false);
            currentY += 12;
            rendered++;
        }
        skipped++;

        if (requestTasks.isEmpty()) {
            if (skipped >= taskScrollOffset && rendered < maxVisible) {
                g.drawString(font, Component.translatable("gui.beemancer.tasks.empty"),
                    x + TASK_LIST_X + 4, currentY, 0xFF888888, false);
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        } else {
            for (TaskDisplayData task : requestTasks) {
                if (skipped >= taskScrollOffset && rendered < maxVisible) {
                    renderTaskRow(g, x, currentY, task, mouseX, mouseY);
                    currentY += TASK_ROW_HEIGHT;
                    rendered++;
                }
                skipped++;
            }
        }

        // Spacing
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            currentY += 4;
        }

        // Automation header
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            g.drawString(font, Component.translatable("gui.beemancer.tasks.automation")
                .append(" (" + automationTasks.size() + ")"),
                x + TASK_LIST_X, currentY, 0xFF44FF44, false);
            currentY += 12;
            rendered++;
        }
        skipped++;

        if (automationTasks.isEmpty()) {
            if (skipped >= taskScrollOffset && rendered < maxVisible) {
                g.drawString(font, Component.translatable("gui.beemancer.tasks.empty"),
                    x + TASK_LIST_X + 4, currentY, 0xFF888888, false);
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        } else {
            for (TaskDisplayData task : automationTasks) {
                if (skipped >= taskScrollOffset && rendered < maxVisible) {
                    renderTaskRow(g, x, currentY, task, mouseX, mouseY);
                    currentY += TASK_ROW_HEIGHT;
                    rendered++;
                }
                skipped++;
            }
        }
    }

    private void renderTaskRow(GuiGraphics g, int x, int currentY, TaskDisplayData task,
                                int mouseX, int mouseY) {
        // Dependency connector
        if (!task.dependencyIds().isEmpty()) {
            g.fill(x + TASK_LIST_X + 2, currentY - 2,
                x + TASK_LIST_X + 3, currentY + 8, 0xFF888888);
            g.fill(x + TASK_LIST_X + 2, currentY + 7,
                x + TASK_LIST_X + 10, currentY + 8, 0xFF888888);
        }

        int taskX = x + TASK_LIST_X + (task.dependencyIds().isEmpty() ? 0 : 12);

        if (!task.template().isEmpty()) {
            g.renderItem(task.template(), taskX, currentY);
        }

        String itemName = task.template().getHoverName().getString();
        if (itemName.length() > 16) itemName = itemName.substring(0, 14) + "..";
        g.drawString(font, itemName + " x" + task.count(),
            taskX + 18, currentY + 4, 0xFFFFFF, false);

        int stateColor = getStateColor(task.state());
        String stateLabel = getStateLabel(task.state());
        int stateX = x + GUI_WIDTH - 60;
        g.drawString(font, stateLabel, stateX, currentY + 4, stateColor, false);

        String typeChar = task.type().equals("DEPOSIT") ? "\u2193" : "\u2191";
        int typeColor = task.type().equals("DEPOSIT") ? 0xFF44AA44 : 0xFF4488FF;
        g.drawString(font, typeChar, stateX - 10, currentY + 4, typeColor, false);

        int cancelX = x + GUI_WIDTH - 18;
        int cancelY = currentY + 2;
        boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
            mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE;
        g.drawString(font, "X", cancelX, cancelY,
            hoverCancel ? 0xFFFF4444 : 0xFFAA0000, false);
    }

    private int getTotalTaskEntries(List<TaskDisplayData> requestTasks,
                                     List<TaskDisplayData> automationTasks) {
        int total = 2; // 2 headers
        total += requestTasks.isEmpty() ? 1 : requestTasks.size();
        total += automationTasks.isEmpty() ? 1 : automationTasks.size();
        return total;
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
        int statX = x + TASK_LIST_X;
        int statY = y + 10;
        int lineHeight = 14;
        int rightEdge = x + GUI_WIDTH - 8;

        g.drawString(font, Component.translatable("container.beemancer.storage_controller"),
            statX, statY, 0x404040, false);
        statY += lineHeight + 4;

        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.flight_speed",
            menu.getFlightSpeed() + "%");
        statY += lineHeight;
        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.search_speed",
            menu.getSearchSpeed() + "%");
        statY += lineHeight;
        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.craft_speed",
            menu.getCraftSpeed() + "%");
        statY += lineHeight;
        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.quantity",
            String.valueOf(menu.getQuantity()));
        statY += lineHeight;
        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.honey_consumption",
            menu.getHoneyConsumption() + " mB/s");
        statY += lineHeight;

        int effBonus = menu.getHoneyEfficiency();
        String effText = effBonus > 0 ? "100% + " + effBonus + "%" : "100%";
        drawStat(g, rightEdge, statX, statY, "gui.beemancer.storage_controller.honey_efficiency",
            effText);
        statY += lineHeight + 8;

        // Essence slots label + background
        g.drawString(font, Component.translatable("gui.beemancer.terminal.essences"),
            statX, statY - 2, 0x404040, false);

        int essenceRenderX = x + 155;
        int essenceRenderY = y + 80;
        for (int i = 0; i < 4; i++) {
            GuiRenderHelper.renderSlot(g, essenceRenderX + i * 20, essenceRenderY);
        }
    }

    private void drawStat(GuiGraphics g, int rightEdge, int labelX, int y,
                           String labelKey, String value) {
        g.drawString(font, Component.translatable(labelKey), labelX, y, 0x404040, false);
        int valueWidth = font.width(value);
        g.drawString(font, value, rightEdge - valueWidth, y, 0x206040, false);
    }

    // === Request Popup ===

    private void renderRequestPopup(GuiGraphics g, int mouseX, int mouseY) {
        int popupWidth = 120;
        int popupHeight = 80;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        g.fill(popupX - 2, popupY - 2, popupX + popupWidth + 2, popupY + popupHeight + 2, 0xFF000000);
        g.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF3C3C3C);

        Component title = Component.translatable("gui.beemancer.storage_terminal.request");
        g.drawCenteredString(this.font, title, popupX + popupWidth / 2, popupY + 5, 0xFFFFFF);

        g.renderItem(requestItem, popupX + 10, popupY + 20);
        String name = requestItem.getHoverName().getString();
        if (name.length() > 14) name = name.substring(0, 12) + "..";
        g.drawString(this.font, name, popupX + 30, popupY + 24, 0xFFFFFF);

        String countText = String.valueOf(requestCount);
        g.drawCenteredString(this.font, countText, popupX + popupWidth / 2, popupY + 42, 0xFFFF00);

        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        String[] labels = {"---", "--", "-", "+", "++", "+++"};

        for (int i = 0; i < 6; i++) {
            boolean hover = mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12;
            int color = hover ? 0xFF8080FF : 0xFFAAAAAA;
            g.drawString(this.font, labels[i], positions[i], buttonY, color);
        }

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

        if (activeTab == StorageTab.STORAGE && !showRequestPopup) {
            renderVirtualGridTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // No default labels — we render them manually in renderBg
    }

    private void renderVirtualGridTooltip(GuiGraphics g, int mouseX, int mouseY) {
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
        if (showRequestPopup) {
            return handleRequestPopupClick(mouseX, mouseY, button);
        }

        int x = this.leftPos;
        int y = this.topPos;

        // Vertical tab clicks
        for (int i = 0; i < 3; i++) {
            int tabY = y + VTAB_Y + i * (VTAB_H + VTAB_GAP);
            if (mouseX >= x + VTAB_X && mouseX < x + VTAB_X + VTAB_W &&
                mouseY >= tabY && mouseY < tabY + VTAB_H) {
                switchTab(StorageTab.values()[i]);
                return true;
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

                    if (index < displayedItems.size() &&
                        isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
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

        List<TaskDisplayData> requestTasks = new ArrayList<>();
        List<TaskDisplayData> automationTasks = new ArrayList<>();
        for (TaskDisplayData task : tasks) {
            if ("AUTOMATION".equals(task.origin())) {
                automationTasks.add(task);
            } else {
                requestTasks.add(task);
            }
        }

        int x = this.leftPos;
        int y = this.topPos;
        int currentY = y + TASK_LIST_Y;
        int maxVisible = 8;
        int rendered = 0;
        int skipped = 0;

        // Skip requests header
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            currentY += 12;
            rendered++;
        }
        skipped++;

        // Request tasks (or empty placeholder)
        List<TaskDisplayData> reqList = requestTasks.isEmpty()
            ? java.util.Collections.emptyList() : requestTasks;
        if (requestTasks.isEmpty()) {
            if (skipped >= taskScrollOffset && rendered < maxVisible) {
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        }
        for (TaskDisplayData task : reqList) {
            if (skipped >= taskScrollOffset && rendered < maxVisible) {
                if (checkCancelClick(mouseX, mouseY, x, currentY, task)) return true;
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        }

        // Spacing
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            currentY += 4;
        }

        // Skip automation header
        if (skipped >= taskScrollOffset && rendered < maxVisible) {
            currentY += 12;
            rendered++;
        }
        skipped++;

        // Automation tasks (or empty placeholder)
        if (automationTasks.isEmpty()) {
            skipped++;
        }
        for (TaskDisplayData task : automationTasks) {
            if (skipped >= taskScrollOffset && rendered < maxVisible) {
                if (checkCancelClick(mouseX, mouseY, x, currentY, task)) return true;
                currentY += TASK_ROW_HEIGHT;
                rendered++;
            }
            skipped++;
        }

        return false;
    }

    private boolean checkCancelClick(double mouseX, double mouseY, int x, int currentY,
                                      TaskDisplayData task) {
        int cancelX = x + GUI_WIDTH - 18;
        int cancelY = currentY + 2;
        if (mouseX >= cancelX && mouseX < cancelX + TASK_CANCEL_SIZE &&
            mouseY >= cancelY && mouseY < cancelY + TASK_CANCEL_SIZE) {
            PacketDistributor.sendToServer(
                new StorageTaskCancelPacket(menu.getBlockPos(), task.taskId())
            );
            return true;
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
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double dragX, double dragY) {
        if (isScrolling && activeTab == StorageTab.STORAGE) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                float scrollRatio = (float) (mouseY - this.topPos - SCROLLBAR_Y)
                    / (SCROLLBAR_HEIGHT - 15);
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
                List<TaskDisplayData> tasks = menu.getTaskDisplayData();
                List<TaskDisplayData> reqTasks = new ArrayList<>();
                List<TaskDisplayData> autoTasks = new ArrayList<>();
                for (TaskDisplayData t : tasks) {
                    if ("AUTOMATION".equals(t.origin())) autoTasks.add(t);
                    else reqTasks.add(t);
                }
                int totalEntries = getTotalTaskEntries(reqTasks, autoTasks);
                int maxTaskScroll = Math.max(0, totalEntries - 8);
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
