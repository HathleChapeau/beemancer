/**
 * ============================================================
 * [PageBarWidget.java]
 * Description: Barre de navigation des pages en bas de l'editeur piano-roll
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SequenceData        | Constantes pages     | MAX_PAGES                      |
 * | GuiGraphics         | Rendu vectoriel      | fill(), drawString()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - WaveMixerScreen (widget enfant, mode editeur, zone basse)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.common.data.SequenceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Barre de navigation des pages : [<] [page/total] [>] [+] [Delete]
 * Affichee en bas de l'editeur. Le numero de page est 1-indexed pour l'utilisateur.
 */
public class PageBarWidget {

    private static final int BAR_H = 16;
    private static final int BTN_H = 12;

    private static final int COL_BG = 0xFF1A1A2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_BTN = 0xFF334466;
    private static final int COL_BTN_DISABLED = 0xFF222233;
    private static final int COL_ADD = 0xFF00CC66;
    private static final int COL_DELETE = 0xFFCC3333;
    private static final int COL_DELETE_DISABLED = 0xFF442222;
    private static final int COL_TEXT = 0xFFDDDDDD;
    private static final int COL_TEXT_DIM = 0xFF666666;
    private static final int COL_ACCENT = 0xFF00FF88;

    private final int x, y, width;
    private final Listener listener;

    public interface Listener {
        void onPageChange(int newPage);
        void onAddPage();
        void onDeletePage(int pageIndex);
    }

    public PageBarWidget(int x, int y, int width, Listener listener) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.listener = listener;
    }

    public void render(GuiGraphics gfx, int currentPage, int pageCount) {
        Font font = Minecraft.getInstance().font;

        // Background
        gfx.fill(x, y, x + width, y + BAR_H, COL_BG);
        gfx.fill(x, y, x + width, y + 1, COL_BORDER);

        int btnY = y + 2;

        // Center the controls
        String pageStr = (currentPage + 1) + "/" + pageCount;
        int pageStrW = font.width(pageStr);
        int totalW = 12 + 4 + pageStrW + 4 + 12 + 10 + 12 + 6 + font.width("Del") + 6;
        int cx = x + (width - totalW) / 2;

        // [<] prev page
        boolean canPrev = currentPage > 0;
        int prevCol = canPrev ? COL_BTN : COL_BTN_DISABLED;
        int prevTextCol = canPrev ? COL_TEXT : COL_TEXT_DIM;
        gfx.fill(cx, btnY, cx + 12, btnY + BTN_H, prevCol);
        gfx.drawString(font, "<", cx + 3, btnY + 2, prevTextCol, false);
        cx += 16;

        // page/total
        gfx.drawString(font, pageStr, cx, btnY + 2, COL_ACCENT, false);
        cx += pageStrW + 4;

        // [>] next page
        boolean canNext = currentPage < pageCount - 1;
        int nextCol = canNext ? COL_BTN : COL_BTN_DISABLED;
        int nextTextCol = canNext ? COL_TEXT : COL_TEXT_DIM;
        gfx.fill(cx, btnY, cx + 12, btnY + BTN_H, nextCol);
        gfx.drawString(font, ">", cx + 3, btnY + 2, nextTextCol, false);
        cx += 22;

        // [+] add page
        boolean canAdd = pageCount < SequenceData.MAX_PAGES;
        int addCol = canAdd ? COL_ADD : COL_BTN_DISABLED;
        int addTextCol = canAdd ? 0xFF111111 : COL_TEXT_DIM;
        gfx.fill(cx, btnY, cx + 12, btnY + BTN_H, addCol);
        gfx.drawString(font, "+", cx + 3, btnY + 2, addTextCol, false);
        cx += 18;

        // [Del] delete page
        boolean canDelete = pageCount > 1;
        int delBtnCol = canDelete ? COL_DELETE : COL_DELETE_DISABLED;
        int delTextCol = canDelete ? COL_TEXT : COL_TEXT_DIM;
        int delW = font.width("Del") + 6;
        gfx.fill(cx, btnY, cx + delW, btnY + BTN_H, delBtnCol);
        gfx.drawString(font, "Del", cx + 3, btnY + 2, delTextCol, false);
    }

    public boolean mouseClicked(double mx, double my, int currentPage, int pageCount) {
        if (my < y || my >= y + BAR_H) return false;

        Font font = Minecraft.getInstance().font;
        int btnY = y + 2;

        String pageStr = (currentPage + 1) + "/" + pageCount;
        int pageStrW = font.width(pageStr);
        int totalW = 12 + 4 + pageStrW + 4 + 12 + 10 + 12 + 6 + font.width("Del") + 6;
        int cx = x + (width - totalW) / 2;

        // [<] prev
        if (mx >= cx && mx < cx + 12 && my >= btnY && my < btnY + BTN_H) {
            if (currentPage > 0) listener.onPageChange(currentPage - 1);
            return true;
        }
        cx += 16;

        // page string (skip)
        cx += pageStrW + 4;

        // [>] next
        if (mx >= cx && mx < cx + 12 && my >= btnY && my < btnY + BTN_H) {
            if (currentPage < pageCount - 1) listener.onPageChange(currentPage + 1);
            return true;
        }
        cx += 22;

        // [+] add
        if (mx >= cx && mx < cx + 12 && my >= btnY && my < btnY + BTN_H) {
            if (pageCount < SequenceData.MAX_PAGES) listener.onAddPage();
            return true;
        }
        cx += 18;

        // [Del]
        int delW = font.width("Del") + 6;
        if (mx >= cx && mx < cx + delW && my >= btnY && my < btnY + BTN_H) {
            if (pageCount > 1) listener.onDeletePage(currentPage);
            return true;
        }

        return false;
    }

    public int getHeight() {
        return BAR_H;
    }
}
