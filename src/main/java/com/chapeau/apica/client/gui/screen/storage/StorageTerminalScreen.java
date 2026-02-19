/**
 * ============================================================
 * [StorageTerminalScreen.java]
 * Description: Screen du Storage Terminal avec rendu texture et 3 onglets
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation           |
 * |-------------------------------|----------------------|-----------------------|
 * | GuiRenderHelper               | Rendu programmatique | Slots, barres         |
 * | StorageTerminalMenu           | Menu associe         | Donnees a afficher    |
 * | TerminalRequestPopup          | Popup demande item   | Composition           |
 * | TerminalTasksTabRenderer      | Onglet Tasks         | Composition           |
 * | TerminalStorageTabRenderer    | Onglet Storage       | Composition           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement du screen)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.storage;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen du Storage Terminal avec layout texture.
 *
 * Haut: storage_bg contenant deposit/pickup (transfer_bg) + inventory (inventory_bg) + tabs
 * Bas: player_inventory_bg contenant craft 3x3 + result + player inventory
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    // === Textures ===
    private static final ResourceLocation STORAGE_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/storage_bg.png");
    private static final ResourceLocation PLAYER_INV_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/player_inventory_bg.png");
    private static final ResourceLocation TRANSFER_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/transfer_bg.png");
    private static final ResourceLocation INVENTORY_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/inventory_bg.png");
    private static final ResourceLocation IMPORT_ICON = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/import_icon.png");
    private static final ResourceLocation EXPORT_ICON = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/storage/export_icon.png");

    // Vanilla advancement tab sprites (left-side tabs, protrude to the left)
    private static final ResourceLocation[] TAB_SPRITES = {
        ResourceLocation.withDefaultNamespace("advancements/tab_left_top"),
        ResourceLocation.withDefaultNamespace("advancements/tab_left_middle"),
        ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom")
    };
    private static final ResourceLocation[] TAB_SPRITES_SELECTED = {
        ResourceLocation.withDefaultNamespace("advancements/tab_left_top_selected"),
        ResourceLocation.withDefaultNamespace("advancements/tab_left_middle_selected"),
        ResourceLocation.withDefaultNamespace("advancements/tab_left_bottom_selected")
    };

    // === Dimensions ===
    private static final int GUI_WIDTH = 270;
    private static final int GUI_HEIGHT = 230;
    private static final int STORAGE_BG_W = 270;
    private static final int STORAGE_BG_H = 135;
    private static final int PLAYER_INV_BG_W = 266;
    private static final int PLAYER_INV_BG_H = 90;
    private static final int PLAYER_INV_BG_X = 2;
    private static final int PLAYER_INV_BG_Y = 140;
    private static final int TRANSFER_BG_W = 63;
    private static final int TRANSFER_BG_H = 57;
    private static final int INVENTORY_BG_W = 189;
    private static final int INVENTORY_BG_H = 103;

    // === Deposit panel (transfer_bg at 7,7 inside storage_bg) ===
    private static final int DEPOSIT_BG_X = 7;
    private static final int DEPOSIT_BG_Y = 7;
    private static final int DEPOSIT_SLOT_X = 11;
    private static final int DEPOSIT_SLOT_Y = 11;
    private static final int DEPOSIT_ICON_X = 10;
    private static final int DEPOSIT_ICON_Y = 48;

    // === Pickup panel (transfer_bg at 7,71 inside storage_bg) ===
    private static final int PICKUP_BG_X = 7;
    private static final int PICKUP_BG_Y = 71;
    private static final int PICKUP_SLOT_X = 11;
    private static final int PICKUP_SLOT_Y = 75;
    private static final int PICKUP_ICON_X = 10;
    private static final int PICKUP_ICON_Y = 112;

    // === Inventory panel (inventory_bg at 74,25 inside storage_bg) ===
    private static final int INV_BG_X = 74;
    private static final int INV_BG_Y = 25;

    // === Search bar (above inventory_bg, aligned with INV_BG_X) ===
    private static final int SEARCH_X = 74;
    private static final int SEARCH_Y = 10;
    private static final int SEARCH_WIDTH = 180;

    // === Tabs (left side of storage_bg, protruding left, 6px overlap to match border) ===
    private static final int TAB_W = 32;
    private static final int TAB_H = 28;
    private static final int TAB_X = -TAB_W + 6;
    private static final int TAB_START_Y = 3;

    // === Page arrows ===
    private static final int PAGE_ARROW_W = 12;
    private static final int PAGE_ARROW_H = 10;
    private static final int ICON_W = 9;
    private static final int ICON_H = 10;

    // Tab icon items
    private static final ItemStack ICON_STORAGE = new ItemStack(Items.CHEST);
    private static final ItemStack ICON_TASKS = new ItemStack(Items.WRITABLE_BOOK);
    private static final ItemStack ICON_CONTROLLER = new ItemStack(Items.COMPARATOR);

    // === State ===
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

        // 1. Tabs (behind everything, left side of storage_bg)
        renderTabs(g, x, y);

        // 2. Storage background (top)
        g.blit(STORAGE_BG, x, y, 0, 0, STORAGE_BG_W, STORAGE_BG_H, STORAGE_BG_W, STORAGE_BG_H);

        // 3. Player inventory background (bottom)
        g.blit(PLAYER_INV_BG, x + PLAYER_INV_BG_X, y + PLAYER_INV_BG_Y,
            0, 0, PLAYER_INV_BG_W, PLAYER_INV_BG_H, PLAYER_INV_BG_W, PLAYER_INV_BG_H);

        // 2b. Vanilla slot overlays for pixel-perfect alignment
        // (avoids UV drift from non-power-of-2 texture width 266)
        int pSlotX = x + StorageTerminalMenu.PLAYER_INV_X - 1;
        int pSlotY = y + StorageTerminalMenu.PLAYER_INV_Y - 1;
        renderVanillaSlotGrid(g, pSlotX, pSlotY, 9, 3);
        renderVanillaSlotGrid(g, pSlotX, y + StorageTerminalMenu.HOTBAR_Y - 1, 9, 1);
        int cSlotX = x + StorageTerminalMenu.CRAFT_X - 1;
        int cSlotY = y + StorageTerminalMenu.CRAFT_Y - 1;
        renderVanillaSlotGrid(g, cSlotX, cSlotY, 3, 3);
        drawSlotBackground(g, x + StorageTerminalMenu.RESULT_X - 1,
            y + StorageTerminalMenu.RESULT_Y - 1);

        // 3. Deposit panel (transfer_bg)
        g.blit(TRANSFER_BG, x + DEPOSIT_BG_X, y + DEPOSIT_BG_Y,
            0, 0, TRANSFER_BG_W, TRANSFER_BG_H, TRANSFER_BG_W, TRANSFER_BG_H);

        // 4. Pickup panel (transfer_bg)
        g.blit(TRANSFER_BG, x + PICKUP_BG_X, y + PICKUP_BG_Y,
            0, 0, TRANSFER_BG_W, TRANSFER_BG_H, TRANSFER_BG_W, TRANSFER_BG_H);

        // 5. Deposit slot backgrounds (2x3) + icon + page arrows
        GuiRenderHelper.renderSlotGrid(g, x + DEPOSIT_SLOT_X, y + DEPOSIT_SLOT_Y, 3, 2);
        g.blit(IMPORT_ICON, x + DEPOSIT_ICON_X, y + DEPOSIT_ICON_Y,
            0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        renderPageArrows(g, x, y + DEPOSIT_ICON_Y,
            menu.getDepositPage(), menu.getDepositPages(), true);

        // 6. Pickup slot backgrounds (2x3) + icon + page arrows
        GuiRenderHelper.renderSlotGrid(g, x + PICKUP_SLOT_X, y + PICKUP_SLOT_Y, 3, 2);
        g.blit(EXPORT_ICON, x + PICKUP_ICON_X, y + PICKUP_ICON_Y,
            0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        renderPageArrows(g, x, y + PICKUP_ICON_Y,
            menu.getPickupPage(), menu.getPickupPages(), false);

        // 8. Tab content (right panel)
        switch (activeTab) {
            case STORAGE -> {
                g.blit(INVENTORY_BG, x + INV_BG_X, y + INV_BG_Y,
                    0, 0, INVENTORY_BG_W, INVENTORY_BG_H, INVENTORY_BG_W, INVENTORY_BG_H);
                storageTabRenderer.render(g, this.font, this.menu,
                    displayedItems, scrollOffset, x, y, mouseX, mouseY);
            }
            case TASKS -> tasksTabRenderer.render(g, this.font, this.menu, x, y, mouseX, mouseY);
            case CONTROLLER -> renderControllerTab(g, x, y, mouseX, mouseY);
        }

        // 9. Request popup (on top)
        if (requestPopup.isVisible()) {
            requestPopup.render(g, this.font, this.width, this.height, mouseX, mouseY);
        }
    }

    private void renderTabs(GuiGraphics g, int x, int y) {
        ItemStack[] icons = { ICON_STORAGE, ICON_TASKS, ICON_CONTROLLER };

        for (int i = 0; i < 3; i++) {
            int tabY = y + TAB_START_Y + i * TAB_H;
            boolean selected = activeTab.ordinal() == i;
            ResourceLocation sprite = selected ? TAB_SPRITES_SELECTED[i] : TAB_SPRITES[i];
            g.blitSprite(sprite, x + TAB_X, tabY, TAB_W, TAB_H);
            g.renderItem(icons[i], x + TAB_X + 6, tabY + 6);
        }
    }

    /**
     * Renders page navigation: [icon] [<] 1/3 [>]
     * Positioned to the right of the import/export icon.
     */
    private void renderPageArrows(GuiGraphics g, int x, int arrowY,
                                   int currentPage, int totalPages, boolean isDeposit) {
        if (totalPages <= 1) return;

        int baseX = isDeposit ? x + DEPOSIT_ICON_X : x + PICKUP_ICON_X;
        int arrowBaseX = baseX + ICON_W + 4;

        // Left arrow
        boolean leftEnabled = currentPage > 0;
        drawSmallArrow(g, arrowBaseX, arrowY, PAGE_ARROW_W, PAGE_ARROW_H, "<", leftEnabled);

        // Page text
        String pageText = (currentPage + 1) + "/" + totalPages;
        int textX = arrowBaseX + PAGE_ARROW_W + 2;
        g.drawString(font, pageText, textX, arrowY + 1, 0x404040, false);

        // Right arrow
        int rightX = textX + font.width(pageText) + 2;
        boolean rightEnabled = currentPage < totalPages - 1;
        drawSmallArrow(g, rightX, arrowY, PAGE_ARROW_W, PAGE_ARROW_H, ">", rightEnabled);
    }

    private void drawSmallArrow(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled) {
        int bg = enabled ? 0xFFC6C6C6 : 0xFF8B8B8B;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.fill(x, y, x + 1, y + h, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.fill(x, y + h - 1, x + w, y + h, enabled ? 0xFF555555 : 0xFF666666);
        g.fill(x + w - 1, y, x + w, y + h, enabled ? 0xFF555555 : 0xFF666666);
        int textColor = enabled ? 0x404040 : 0x606060;
        int textW = font.width(label);
        g.drawString(font, label, x + (w - textW) / 2, y + (h - 8) / 2, textColor, false);
    }

    // === Controller Tab ===

    private void renderControllerTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int statX = x + INV_BG_X;
        int statY = y + INV_BG_Y;
        int lineHeight = 12;
        int rightEdge = x + STORAGE_BG_W - 8;
        int barWidth = rightEdge - statX - 2;

        g.drawString(font, Component.translatable("container.apica.storage_controller"),
            statX, statY, 0x404040, false);
        statY += lineHeight + 2;

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

        int reserveBonus = menu.getHoneyReserveBonus();
        drawStatBar(g, statX, statY, barWidth,
            "gui.apica.storage_controller.honey_reserve",
            reserveBonus, 16000, 0xFFCC4444);
        statY += lineHeight + 2;

        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.honey_consumption",
            menu.getHoneyConsumption() + " mB/s");
        statY += lineHeight;

        int effBonus = menu.getHoneyEfficiency();
        String effText = effBonus > 0 ? "-" + effBonus + "%" : "0%";
        drawStat(g, rightEdge, statX, statY, "gui.apica.storage_controller.honey_efficiency",
            effText);
        statY += lineHeight;

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

        g.drawString(font, Component.translatable("gui.apica.terminal.essences"),
            statX, statY - 2, 0x404040, false);

        int essenceRenderX = x + 155;
        int essenceRenderY = y + 80;

        for (int i = 0; i < 4; i++) {
            GuiRenderHelper.renderSlot(g, essenceRenderX + i * 20, essenceRenderY);
        }

        int bonusRenderY = y + 100;
        for (int i = 0; i < 4; i++) {
            int slotIndex = StorageTerminalMenu.ESSENCE_BONUS_START + i;
            if (menu.isBonusSlotUnlocked(slotIndex)) {
                drawSlotBackground(g, essenceRenderX + i * 20 - 1, bonusRenderY - 1);
            } else {
                drawLockedSlotBackground(g, essenceRenderX + i * 20 - 1, bonusRenderY - 1);
            }
        }
    }

    private void drawStatBar(GuiGraphics g, int x, int y, int totalWidth,
                              String labelKey, int value, int maxValue, int barColor) {
        Component label = Component.translatable(labelKey);
        g.drawString(font, label, x, y, 0x404040, false);
        int labelWidth = font.width(label) + 4;
        int barX = x + labelWidth;
        int barW = totalWidth - labelWidth;
        int barH = 7;
        int barY = y + 1;

        g.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        float ratio = maxValue > 0 ? Math.min(1.0f, (float) value / maxValue) : 0;
        int fillW = Math.round(barW * ratio);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, barColor);
        }
        String valText = String.valueOf(value);
        int valWidth = font.width(valText);
        g.drawString(font, valText, barX + barW - valWidth - 1, y, 0xFFFFFF, false);
    }

    private void renderVanillaSlotGrid(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawSlotBackground(g, x + col * 18, y + row * 18);
            }
        }
    }

    private void drawSlotBackground(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        g.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y, x + 18, y + 17, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

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
        // No default labels — we render manually
    }

    // === Input Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (requestPopup.isVisible()) {
            return requestPopup.handleClick(mouseX, mouseY, this.width, this.height);
        }

        int x = this.leftPos;
        int y = this.topPos;

        // Tab clicks (right side)
        for (int i = 0; i < 3; i++) {
            int tabY = y + TAB_START_Y + i * TAB_H;
            if (mouseX >= x + TAB_X && mouseX < x + TAB_X + TAB_W &&
                mouseY >= tabY && mouseY < tabY + TAB_H) {
                switchTab(StorageTab.values()[i]);
                return true;
            }
        }

        // Page arrow clicks
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

    private boolean handlePageArrowClick(double mouseX, double mouseY, int x, int y) {
        // Deposit arrows
        if (menu.getDepositPages() > 1) {
            int buttonId = getPageArrowButtonId(mouseX, mouseY, x, y + DEPOSIT_ICON_Y,
                menu.getDepositPage(), menu.getDepositPages(),
                StorageTerminalMenu.BUTTON_DEPOSIT_PREV, StorageTerminalMenu.BUTTON_DEPOSIT_NEXT,
                DEPOSIT_ICON_X);
            if (buttonId >= 0) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                return true;
            }
        }

        // Pickup arrows
        if (menu.getPickupPages() > 1) {
            int buttonId = getPageArrowButtonId(mouseX, mouseY, x, y + PICKUP_ICON_Y,
                menu.getPickupPage(), menu.getPickupPages(),
                StorageTerminalMenu.BUTTON_PICKUP_PREV, StorageTerminalMenu.BUTTON_PICKUP_NEXT,
                PICKUP_ICON_X);
            if (buttonId >= 0) {
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                return true;
            }
        }
        return false;
    }

    private int getPageArrowButtonId(double mouseX, double mouseY, int x, int arrowY,
                                      int currentPage, int totalPages,
                                      int prevButtonId, int nextButtonId, int iconX) {
        int baseX = x + iconX + ICON_W + 4;

        // Left arrow
        if (currentPage > 0 &&
            mouseX >= baseX && mouseX < baseX + PAGE_ARROW_W &&
            mouseY >= arrowY && mouseY < arrowY + PAGE_ARROW_H) {
            return prevButtonId;
        }

        // Right arrow
        String pageText = (currentPage + 1) + "/" + totalPages;
        int rightX = baseX + PAGE_ARROW_W + 2 + font.width(pageText) + 2;
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
