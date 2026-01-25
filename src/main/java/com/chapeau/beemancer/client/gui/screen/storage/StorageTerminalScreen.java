/**
 * ============================================================
 * [StorageTerminalScreen.java]
 * Description: Screen du Storage Terminal avec grille items virtuelle
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation           |
 * |--------------------------|------------------------|-----------------------|
 * | Beemancer                | MOD_ID                 | Chemin texture        |
 * | StorageTerminalMenu      | Menu associé           | Données à afficher    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du screen)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import com.chapeau.beemancer.core.network.packets.StorageRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen du Storage Terminal.
 *
 * Affiche:
 * - Barre de recherche en haut
 * - Grille d'items virtuels (9x6) avec scroll
 * - Slots de dépôt (3x3 gauche)
 * - Slots de pickup (3x3 droite)
 * - Inventaire joueur
 *
 * Popup request quand on clique sur un item virtuel.
 */
public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/storage_terminal.png");

    // Layout constants
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 6;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 28;
    private static final int SLOT_SIZE = 18;

    // Scroll
    private int scrollOffset = 0;
    private boolean isScrolling = false;
    private static final int SCROLLBAR_X = 156;
    private static final int SCROLLBAR_Y = 28;
    private static final int SCROLLBAR_HEIGHT = 108;

    // Search
    private EditBox searchBox;
    private String searchText = "";

    // Filtered items
    private List<ItemStack> displayedItems = new ArrayList<>();

    // Request popup
    private boolean showRequestPopup = false;
    private ItemStack requestItem = ItemStack.EMPTY;
    private int requestCount = 0;
    private int requestMax = 0;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 266;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Search box
        int searchX = this.leftPos + 8;
        int searchY = this.topPos + 8;
        this.searchBox = new EditBox(this.font, searchX, searchY, 140, 14,
            Component.translatable("gui.beemancer.storage_terminal.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
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
        if (this.searchBox != null) {
            this.searchBox.tick();
        }
        // Refresh items periodically
        refreshDisplayedItems();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Scrollbar
        renderScrollbar(guiGraphics, x, y);

        // Virtual grid items
        renderVirtualGrid(guiGraphics, x, y, mouseX, mouseY);

        // Request popup
        if (showRequestPopup) {
            renderRequestPopup(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            // Scrollbar disabled
            guiGraphics.blit(TEXTURE, x + SCROLLBAR_X, y + SCROLLBAR_Y,
                176, 0, 12, 15);
            return;
        }

        // Scrollbar enabled
        float scrollRatio = (float) scrollOffset / maxScroll;
        int scrollbarY = (int) (scrollRatio * (SCROLLBAR_HEIGHT - 15));
        guiGraphics.blit(TEXTURE, x + SCROLLBAR_X, y + SCROLLBAR_Y + scrollbarY,
            176, 15, 12, 15);
    }

    private void renderVirtualGrid(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                // Render slot background
                guiGraphics.blit(TEXTURE, slotX, slotY, 176, 30, 18, 18);

                if (index < displayedItems.size()) {
                    ItemStack stack = displayedItems.get(index);

                    // Render item
                    guiGraphics.renderItem(stack, slotX + 1, slotY + 1);

                    // Render count
                    String countStr = formatCount(stack.getCount());
                    guiGraphics.drawString(this.font, countStr,
                        slotX + 17 - this.font.width(countStr),
                        slotY + 9, 0xFFFFFF, true);

                    // Highlight on hover
                    if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                        guiGraphics.fillGradient(slotX + 1, slotY + 1,
                            slotX + 17, slotY + 17,
                            0x80FFFFFF, 0x80FFFFFF);
                    }
                }
            }
        }
    }

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

    private void renderRequestPopup(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int popupWidth = 120;
        int popupHeight = 80;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        // Background
        guiGraphics.fill(popupX - 2, popupY - 2, popupX + popupWidth + 2, popupY + popupHeight + 2, 0xFF000000);
        guiGraphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF3C3C3C);

        // Title
        Component title = Component.translatable("gui.beemancer.storage_terminal.request");
        guiGraphics.drawCenteredString(this.font, title, popupX + popupWidth / 2, popupY + 5, 0xFFFFFF);

        // Item icon and name
        guiGraphics.renderItem(requestItem, popupX + 10, popupY + 20);
        guiGraphics.drawString(this.font, requestItem.getHoverName().getString(),
            popupX + 30, popupY + 24, 0xFFFFFF);

        // Count display
        String countText = String.valueOf(requestCount);
        guiGraphics.drawCenteredString(this.font, countText, popupX + popupWidth / 2, popupY + 42, 0xFFFF00);

        // Buttons: --- -- - [count] + ++ +++
        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        String[] labels = {"---", "--", "-", "+", "++", "+++"};
        int[] deltas = {-64, -10, -1, 1, 10, 64};

        for (int i = 0; i < 6; i++) {
            boolean hover = mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                           mouseY >= buttonY && mouseY < buttonY + 12;
            int color = hover ? 0xFF8080FF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, labels[i], positions[i], buttonY, color);
        }

        // Cancel / Request buttons
        int cancelX = popupX + 10;
        int requestX = popupX + 70;
        int actionY = popupY + popupHeight - 15;

        boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + 40 &&
                             mouseY >= actionY && mouseY < actionY + 10;
        boolean hoverRequest = mouseX >= requestX && mouseX < requestX + 40 &&
                              mouseY >= actionY && mouseY < actionY + 10;

        guiGraphics.drawString(this.font, Component.translatable("gui.beemancer.cancel"),
            cancelX, actionY, hoverCancel ? 0xFFFF8080 : 0xFFAAAAAA);
        guiGraphics.drawString(this.font, Component.translatable("gui.beemancer.request"),
            requestX, actionY, hoverRequest ? 0xFF80FF80 : 0xFFAAAAAA);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for virtual grid items
        if (!showRequestPopup) {
            renderVirtualGridTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderVirtualGridTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size() && isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                    guiGraphics.renderTooltip(this.font, displayedItems.get(index), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showRequestPopup) {
            return handleRequestPopupClick(mouseX, mouseY, button);
        }

        // Check if clicking on virtual grid
        int x = this.leftPos;
        int y = this.topPos;
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size() && isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                    openRequestPopup(displayedItems.get(index));
                    return true;
                }
            }
        }

        // Check scrollbar
        if (mouseX >= this.leftPos + SCROLLBAR_X && mouseX < this.leftPos + SCROLLBAR_X + 12 &&
            mouseY >= this.topPos + SCROLLBAR_Y && mouseY < this.topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            isScrolling = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleRequestPopupClick(double mouseX, double mouseY, int button) {
        int popupWidth = 120;
        int popupHeight = 80;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = (this.height - popupHeight) / 2;

        // Check count buttons
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

        // Check Cancel / Request buttons
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
            // Send request packet to server
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
        if (isScrolling) {
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
            int maxScroll = getMaxScroll();
            scrollOffset = Mth.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getMaxScroll() {
        int totalRows = (displayedItems.size() + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, totalRows - GRID_ROWS);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showRequestPopup) {
            if (keyCode == 256) { // Escape
                closeRequestPopup();
                return true;
            }
            return true;
        }

        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
