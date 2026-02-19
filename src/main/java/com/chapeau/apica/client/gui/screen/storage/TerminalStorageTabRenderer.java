/**
 * ============================================================
 * [TerminalStorageTabRenderer.java]
 * Description: Rendu de l'onglet Storage du Storage Terminal (grille virtuelle + scroll)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation              |
 * |--------------------------|----------------------|--------------------------|
 * | GuiRenderHelper          | Rendu slots          | renderSlot()             |
 * | StorageTerminalMenu      | Pending count        | getPendingItemCount()    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageTerminalScreen.java (composition)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.storage;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Rendu de l'onglet Storage: grille virtuelle 9x5, scrollbar et indicateur pending.
 */
public class TerminalStorageTabRenderer {

    static final int GRID_COLS = 9;
    static final int GRID_ROWS = 5;
    static final int GRID_X = 78;
    static final int GRID_Y = 32;
    static final int SLOT_SIZE = 18;

    static final int SCROLLBAR_X = 243;
    static final int SCROLLBAR_Y = 32;
    static final int SCROLLBAR_HEIGHT = 90;

    public void render(GuiGraphics g, Font font, StorageTerminalMenu menu,
                       List<ItemStack> displayedItems, int scrollOffset,
                       int x, int y, int mouseX, int mouseY) {
        renderVirtualGrid(g, font, displayedItems, scrollOffset, x, y, mouseX, mouseY);
        renderScrollbar(g, displayedItems, scrollOffset, x, y);
        renderPendingIndicator(g, font, menu, x, y);
    }

    private void renderVirtualGrid(GuiGraphics g, Font font, List<ItemStack> displayedItems,
                                   int scrollOffset, int x, int y, int mouseX, int mouseY) {
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                GuiRenderHelper.renderSlot(g, slotX, slotY);

                if (index < displayedItems.size()) {
                    g.renderItem(displayedItems.get(index), slotX + 1, slotY + 1);
                }
            }
        }

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = x + GRID_X + col * SLOT_SIZE;
                int slotY = y + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size()) {
                    ItemStack stack = displayedItems.get(index);
                    String countStr = formatCount(stack.getCount());
                    g.drawString(font, countStr,
                        slotX + 17 - font.width(countStr),
                        slotY + 9, 0xFFFFFF, true);

                    if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                        g.fillGradient(slotX + 1, slotY + 1,
                            slotX + 17, slotY + 17,
                            0x80FFFFFF, 0x80FFFFFF);
                    }
                }
            }
        }
        g.pose().popPose();
    }

    private void renderScrollbar(GuiGraphics g, List<ItemStack> displayedItems,
                                 int scrollOffset, int x, int y) {
        int maxScroll = getMaxScroll(displayedItems);
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

    private void renderPendingIndicator(GuiGraphics g, Font font,
                                        StorageTerminalMenu menu, int x, int y) {
        int pendingCount = menu.getPendingItemCount();
        if (pendingCount <= 0) return;

        int indicatorX = x + GRID_X;
        int indicatorY = y + GRID_Y + GRID_ROWS * SLOT_SIZE + 2;
        String text = "\u23F3 " + formatCount(pendingCount) + " pending";
        g.drawString(font, text, indicatorX, indicatorY, 0xFFFFAA00, false);
    }

    /**
     * Rendu du tooltip au-dessus de la grille virtuelle.
     */
    public void renderTooltip(GuiGraphics g, Font font, List<ItemStack> displayedItems,
                              int scrollOffset, int leftPos, int topPos,
                              int mouseX, int mouseY) {
        int startIndex = scrollOffset * GRID_COLS;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = leftPos + GRID_X + col * SLOT_SIZE;
                int slotY = topPos + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size() && isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                    g.renderTooltip(font, displayedItems.get(index), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    /**
     * Detecte un clic sur un item de la grille. Retourne l'item clique ou null.
     */
    public ItemStack getClickedItem(double mouseX, double mouseY,
                                    List<ItemStack> displayedItems, int scrollOffset,
                                    int leftPos, int topPos) {
        int startIndex = scrollOffset * GRID_COLS;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = startIndex + row * GRID_COLS + col;
                int slotX = leftPos + GRID_X + col * SLOT_SIZE;
                int slotY = topPos + GRID_Y + row * SLOT_SIZE;

                if (index < displayedItems.size() &&
                    isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                    return displayedItems.get(index);
                }
            }
        }
        return null;
    }

    /**
     * Verifie si le clic est sur la scrollbar.
     */
    public boolean isScrollbarClick(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + 12 &&
            mouseY >= topPos + SCROLLBAR_Y && mouseY < topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    // --- Helpers ---

    static int getMaxScroll(List<ItemStack> displayedItems) {
        int totalRows = (displayedItems.size() + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, totalRows - GRID_ROWS);
    }

    static boolean isMouseOverSlot(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
            mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    static String formatCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
