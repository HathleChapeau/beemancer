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
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
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


    // === Controller Tab ===

    private void renderControllerTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int statX = x + 82; // Right panel start X
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

        // Storage tab interactions
        if (activeTab == StorageTab.STORAGE) {
            ItemStack clickedItem = storageTabRenderer.getClickedItem(
                mouseX, mouseY, displayedItems, scrollOffset, x, y);
            if (clickedItem != null) {
                if (!menu.isHoneyDepleted()) {
                    requestPopup.open(clickedItem, clickedItem.getCount(),
                        menu.getCraftableRecipeFor(clickedItem));
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
        if (requestPopup.isVisible()) {
            return requestPopup.handleScroll(scrollY);
        }
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
