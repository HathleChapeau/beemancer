/**
 * ============================================================
 * [StorageTerminalScreen.java]
 * Description: Screen du Storage Terminal avec rendu programmatique et 3 onglets
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation           |
 * |-------------------------------|----------------------|-----------------------|
 * | GuiRenderHelper               | Rendu programmatique | Fond, slots, onglets  |
 * | StorageTerminalMenu           | Menu associe         | Donnees a afficher    |
 * | TerminalRequestPopup          | Popup demande item   | Composition           |
 * | TerminalTasksTabRenderer      | Onglet Tasks         | Composition           |
 * | TerminalStorageTabRenderer    | Onglet Storage       | Composition           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du screen)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.storage;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

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

    // Page arrows
    private static final int PAGE_ARROW_W = 12;
    private static final int PAGE_ARROW_H = 10;
    private static final int DEPOSIT_PAGE_Y = 69;
    private static final int PICKUP_PAGE_Y = 225;

    // Right panel — search
    private static final int SEARCH_X = 82;
    private static final int SEARCH_Y = 6;
    private static final int SEARCH_WIDTH = 154;

    // Right panel — player inventory
    private static final int PLAYER_INV_RENDER_X = 89;
    private static final int PLAYER_INV_Y = 134;
    private static final int HOTBAR_RENDER_Y = 194;

    // State
    private StorageTab activeTab = StorageTab.STORAGE;
    private EditBox searchBox;
    private String searchText = "";
    private List<ItemStack> displayedItems = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean isScrolling = false;

    // Tab renderers
    private final TerminalTasksTabRenderer tasksTabRenderer = new TerminalTasksTabRenderer(GUI_WIDTH);
    private final TerminalStorageTabRenderer storageTabRenderer = new TerminalStorageTabRenderer();

    // Request popup
    private final TerminalRequestPopup requestPopup = new TerminalRequestPopup(this);

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
            Component.translatable("gui.apica.storage_terminal.search"));
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
            case STORAGE -> storageTabRenderer.render(g, this.font, this.menu,
                    displayedItems, scrollOffset, x, y, mouseX, mouseY);
            case TASKS -> tasksTabRenderer.render(g, this.font, this.menu, x, y, mouseX, mouseY);
            case CONTROLLER -> renderControllerTab(g, x, y, mouseX, mouseY);
        }

        // Player inventory (right panel, all tabs)
        renderPlayerInv(g, x, y);

        // Request popup (on top)
        if (requestPopup.isVisible()) {
            requestPopup.render(g, this.font, this.width, this.height, mouseX, mouseY);
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
        g.drawString(font, Component.translatable("gui.apica.terminal.deposit"),
            x + DEPOSIT_LABEL_X, y + DEPOSIT_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + DEPOSIT_RENDER_X, y + DEPOSIT_RENDER_Y, 3, 3);

        // Deposit page arrows (only if more than 1 page)
        if (StorageTerminalMenu.DEPOSIT_PAGES > 1) {
            renderPageArrows(g, x, y + DEPOSIT_PAGE_Y,
                menu.getDepositPage(), StorageTerminalMenu.DEPOSIT_PAGES);
        }

        // Craft
        g.drawString(font, Component.translatable("gui.apica.terminal.craft"),
            x + CRAFT_LABEL_X, y + CRAFT_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + CRAFT_RENDER_X, y + CRAFT_RENDER_Y, 3, 3);

        // Result slot (below craft, offset right)
        GuiRenderHelper.renderSlot(g, x + RESULT_RENDER_X, y + RESULT_RENDER_Y);

        // Pickup
        g.drawString(font, Component.translatable("gui.apica.terminal.pickup"),
            x + PICKUP_LABEL_X, y + PICKUP_LABEL_Y, 0x404040, false);
        GuiRenderHelper.renderSlotGrid(g, x + PICKUP_RENDER_X, y + PICKUP_RENDER_Y, 3, 3);

        // Pickup page arrows (only if more than 1 page)
        if (StorageTerminalMenu.PICKUP_PAGES > 1) {
            renderPageArrows(g, x, y + PICKUP_PAGE_Y,
                menu.getPickupPage(), StorageTerminalMenu.PICKUP_PAGES);
        }
    }

    /**
     * Renders page navigation arrows: [<] 1/3 [>]
     * Centered under the 3x3 slot grid (grid is at x+23, width 54).
     */
    private void renderPageArrows(GuiGraphics g, int x, int y, int currentPage, int totalPages) {
        int gridCenterX = x + DEPOSIT_RENDER_X + 27; // center of 54px grid
        String pageText = (currentPage + 1) + "/" + totalPages;
        int textW = font.width(pageText);

        // Draw page text centered
        int textX = gridCenterX - textW / 2;
        g.drawString(font, pageText, textX, y + 1, 0x404040, false);

        // Left arrow
        int leftArrowX = textX - PAGE_ARROW_W - 2;
        boolean leftEnabled = currentPage > 0;
        drawSmallArrow(g, leftArrowX, y, PAGE_ARROW_W, PAGE_ARROW_H, "<", leftEnabled);

        // Right arrow
        int rightArrowX = textX + textW + 2;
        boolean rightEnabled = currentPage < totalPages - 1;
        drawSmallArrow(g, rightArrowX, y, PAGE_ARROW_W, PAGE_ARROW_H, ">", rightEnabled);
    }

    /**
     * Draws a small button with a label (< or >). Grayed out if disabled.
     */
    private void drawSmallArrow(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled) {
        int bg = enabled ? 0xFFC6C6C6 : 0xFF8B8B8B;
        g.fill(x, y, x + w, y + h, bg);
        // 3D borders
        g.fill(x, y, x + w, y + 1, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.fill(x, y, x + 1, y + h, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.fill(x, y + h - 1, x + w, y + h, enabled ? 0xFF555555 : 0xFF666666);
        g.fill(x + w - 1, y, x + w, y + h, enabled ? 0xFF555555 : 0xFF666666);
        // Label centered
        int textColor = enabled ? 0x404040 : 0x606060;
        int textW = font.width(label);
        g.drawString(font, label, x + (w - textW) / 2, y + (h - 8) / 2, textColor, false);
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


    // === Controller Tab ===

    private void renderControllerTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int statX = x + 82;
        int statY = y + 10;
        int lineHeight = 12;
        int rightEdge = x + GUI_WIDTH - 8;
        int barWidth = rightEdge - statX - 2;

        g.drawString(font, Component.translatable("container.apica.storage_controller"),
            statX, statY, 0x404040, false);
        statY += lineHeight + 2;

        // Honey gauge [CZ]: amber > 20%, red < 20%
        int honeyStored = menu.getHoneyStored();
        int honeyCapacity = menu.getHoneyCapacity();
        float honeyRatio = honeyCapacity > 0 ? (float) honeyStored / honeyCapacity : 0;
        int gaugeColor = honeyRatio > 0.2f ? 0xFFD4A017 : 0xFFCC3333;
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.honey_gauge",
            honeyStored, honeyCapacity, gaugeColor);
        String honeyText = honeyStored + " / " + honeyCapacity + " mB";
        int honeyTextW = font.width(honeyText);
        g.drawString(font, honeyText, rightEdge - honeyTextW, statY, 0x404040, false);
        statY += lineHeight + 2;

        // Essence stat bars [DJ]
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.flight_speed",
            menu.getFlightSpeed(), 300, 0xFF55BBEE);
        statY += lineHeight;
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.search_speed",
            menu.getSearchSpeed(), 300, 0xFF55CC55);
        statY += lineHeight;
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.quantity",
            menu.getQuantity(), 96, 0xFFDDAA33);
        statY += lineHeight;

        // Honey Reserve (TOLERANCE bonus) — bar
        int reserveBonus = menu.getHoneyReserveBonus();
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.honey_reserve",
            reserveBonus, 16000, 0xFFCC4444);
        statY += lineHeight + 2;

        // Numeric stats
        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.honey_consumption",
            menu.getHoneyConsumption() + " mB/s");
        statY += lineHeight;

        int effBonus = menu.getHoneyEfficiency();
        String effText = effBonus > 0 ? "-" + effBonus + "%" : "0%";
        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.honey_efficiency",
            effText);
        statY += lineHeight;

        // Hive multiplier [DM]
        float multiplier = menu.getHiveMultiplier() / 100.0f;
        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.hive_multiplier",
            "x" + String.format("%.2f", multiplier));
        statY += lineHeight;

        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.hives",
            menu.getLinkedHiveCount() + " / 4");
        statY += lineHeight;
        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.delivery_bees",
            String.valueOf(menu.getMaxBees()));
        statY += lineHeight + 2;

        // Essence slots label + background (4 base + 4 bonus)
        g.drawString(font, Component.translatable("gui.apica.terminal.essences"),
            statX, statY - 2, 0x404040, false);

        int essenceRenderX = x + 155;
        int essenceRenderY = y + 80;

        // 4 base essence slots (always active)
        for (int i = 0; i < 4; i++) {
            GuiRenderHelper.renderSlot(g, essenceRenderX + i * 20, essenceRenderY);
        }

        // 4 bonus essence slots (locked/unlocked per hive count)
        int bonusRenderY = y + 100;
        for (int i = 0; i < 4; i++) {
            int slotIndex = 32 + i; // ESSENCE_BONUS_START + i
            if (menu.isBonusSlotUnlocked(slotIndex)) {
                drawSlotBackground(g, essenceRenderX + i * 20 - 1, bonusRenderY - 1);
            } else {
                drawLockedSlotBackground(g, essenceRenderX + i * 20 - 1, bonusRenderY - 1);
            }
        }
    }

    /**
     * Dessine un label + barre de progression coloree.
     * La barre a un fond sombre et se remplit proportionnellement.
     */
    private void drawStatBar(GuiGraphics g, int x, int y, int totalWidth,
                              String labelKey, int value, int maxValue, int barColor) {
        Component label = Component.translatable(labelKey);
        g.drawString(font, label, x, y, 0x404040, false);
        int labelWidth = font.width(label) + 4;
        int barX = x + labelWidth;
        int barW = totalWidth - labelWidth;
        int barH = 7;
        int barY = y + 1;

        // Background
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        // Fill
        float ratio = maxValue > 0 ? Math.min(1.0f, (float) value / maxValue) : 0;
        int fillW = Math.round(barW * ratio);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, barColor);
        }
        // Value text (right-aligned, over bar)
        String valText = String.valueOf(value);
        int valWidth = font.width(valText);
        g.drawString(font, valText, barX + barW - valWidth - 1, y, 0xFFFFFF, false);
    }

    /**
     * Dessine un fond de slot 18x18 identique au style vanilla.
     */
    private void drawSlotBackground(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        g.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y, x + 18, y + 17, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    /**
     * Dessine un fond de slot 18x18 verrouillé (grisé, pas assez de hives).
     */
    private void drawLockedSlotBackground(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A2A2A);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF2A2A2A);
        g.fill(x, y + 17, x + 18, y + 18, 0xFF9E9E9E);
        g.fill(x + 17, y, x + 18, y + 17, 0xFF9E9E9E);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF555555);
        g.fill(x + 4, y + 8, x + 14, y + 9, 0xFF3A3A3A);
    }

    private void drawStat(GuiGraphics g, int rightEdge, int labelX, int y,
                           String labelKey, String value) {
        g.drawString(font, Component.translatable(labelKey), labelX, y, 0x404040, false);
        int valueWidth = font.width(value);
        g.drawString(font, value, rightEdge - valueWidth, y, 0x206040, false);
    }


    // === Main Render ===

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        if (activeTab == StorageTab.STORAGE && !requestPopup.isVisible()) {
            storageTabRenderer.renderTooltip(g, this.font, displayedItems, scrollOffset,
                this.leftPos, this.topPos, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // No default labels — we render them manually in renderBg
    }


    // === Input Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (requestPopup.isVisible()) {
            return requestPopup.handleClick(mouseX, mouseY, this.width, this.height);
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

        // Page arrow clicks (left panel, visible on all tabs that show deposit/pickup)
        if (handlePageArrowClick(mouseX, mouseY, x, y)) {
            return true;
        }

        // Storage tab interactions
        if (activeTab == StorageTab.STORAGE) {
            ItemStack clickedItem = storageTabRenderer.getClickedItem(
                mouseX, mouseY, displayedItems, scrollOffset, x, y);
            if (clickedItem != null) {
                if (!menu.isHoneyDepleted()) {
                    requestPopup.open(clickedItem, clickedItem.getCount());
                }
                return true;
            }

            if (storageTabRenderer.isScrollbarClick(mouseX, mouseY, x, y)) {
                isScrolling = true;
                return true;
            }
        }

        // Tasks tab: cancel button clicks
        if (activeTab == StorageTab.TASKS) {
            return tasksTabRenderer.handleCancelClick(mouseX, mouseY, this.menu, this.leftPos, this.topPos);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchTab(StorageTab tab) {
        this.activeTab = tab;
        this.menu.setActiveTab(tab);

        if (searchBox != null) {
            searchBox.setVisible(tab == StorageTab.STORAGE);
        }
    }

    /**
     * Checks if a page arrow was clicked and sends the button action to server.
     * Returns true if a click was consumed.
     */
    private boolean handlePageArrowClick(double mouseX, double mouseY, int x, int y) {
        // Deposit arrows
        if (StorageTerminalMenu.DEPOSIT_PAGES > 1) {
            int buttonId = getPageArrowButtonId(mouseX, mouseY, x, y + DEPOSIT_PAGE_Y,
                menu.getDepositPage(), StorageTerminalMenu.DEPOSIT_PAGES,
                StorageTerminalMenu.BUTTON_DEPOSIT_PREV, StorageTerminalMenu.BUTTON_DEPOSIT_NEXT);
            if (buttonId >= 0) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                return true;
            }
        }

        // Pickup arrows
        if (StorageTerminalMenu.PICKUP_PAGES > 1) {
            int buttonId = getPageArrowButtonId(mouseX, mouseY, x, y + PICKUP_PAGE_Y,
                menu.getPickupPage(), StorageTerminalMenu.PICKUP_PAGES,
                StorageTerminalMenu.BUTTON_PICKUP_PREV, StorageTerminalMenu.BUTTON_PICKUP_NEXT);
            if (buttonId >= 0) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                return true;
            }
        }
        return false;
    }

    /**
     * Determines which page arrow button (prev/next) was clicked, if any.
     * Returns the button ID, or -1 if no arrow was clicked.
     */
    private int getPageArrowButtonId(double mouseX, double mouseY, int x, int arrowY,
                                      int currentPage, int totalPages,
                                      int prevButtonId, int nextButtonId) {
        int gridCenterX = x + DEPOSIT_RENDER_X + 27;
        String pageText = (currentPage + 1) + "/" + totalPages;
        int textW = font.width(pageText);
        int textX = gridCenterX - textW / 2;

        // Left arrow bounds
        int leftX = textX - PAGE_ARROW_W - 2;
        if (currentPage > 0 &&
            mouseX >= leftX && mouseX < leftX + PAGE_ARROW_W &&
            mouseY >= arrowY && mouseY < arrowY + PAGE_ARROW_H) {
            return prevButtonId;
        }

        // Right arrow bounds
        int rightX = textX + textW + 2;
        if (currentPage < totalPages - 1 &&
            mouseX >= rightX && mouseX < rightX + PAGE_ARROW_W &&
            mouseY >= arrowY && mouseY < arrowY + PAGE_ARROW_H) {
            return nextButtonId;
        }

        return -1;
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
            int maxScroll = TerminalStorageTabRenderer.getMaxScroll(displayedItems);
            if (maxScroll > 0) {
                float scrollRatio = (float) (mouseY - this.topPos - TerminalStorageTabRenderer.SCROLLBAR_Y)
                    / (TerminalStorageTabRenderer.SCROLLBAR_HEIGHT - 15);
                scrollOffset = Mth.clamp((int) (scrollRatio * maxScroll), 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!requestPopup.isVisible()) {
            if (activeTab == StorageTab.STORAGE) {
                int maxScroll = TerminalStorageTabRenderer.getMaxScroll(displayedItems);
                scrollOffset = Mth.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
                return true;
            }
            if (activeTab == StorageTab.TASKS) {
                int totalEntries = tasksTabRenderer.getTotalEntries(this.menu);
                int maxTaskScroll = Math.max(0, totalEntries - 8);
                int offset = tasksTabRenderer.getTaskScrollOffset();
                tasksTabRenderer.setTaskScrollOffset(
                    Mth.clamp(offset - (int) scrollY, 0, maxTaskScroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (requestPopup.isVisible()) {
            return requestPopup.handleKey(keyCode);
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

}
