/**
 * ============================================================
 * [InstrumentColumnWidget.java]
 * Description: Liste pleine largeur des tracks avec boutons M/S/X/Edit et dropdown
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DubstepInstrument   | Liste instruments    | Dropdown de selection          |
 * | TrackData           | Donnees track        | Nom instrument, mute, solo     |
 * | GuiGraphics         | Rendu vectoriel      | fill(), drawString()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (widget enfant, mode principal)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.common.data.DubstepInstrument;
import com.chapeau.apica.common.data.SequenceData;
import com.chapeau.apica.common.data.TrackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Liste pleine largeur des tracks avec nom instrument, boutons M/S/X/Edit, et bouton [+].
 * Gere aussi le dropdown de selection d'instrument.
 */
public class InstrumentColumnWidget {

    public static final int ROW_H = 18;
    private static final int BTN_W = 14;
    private static final int BTN_GAP = 2;

    private static final int COL_BG = 0xFF1A1A2E;
    private static final int COL_ROW_ALT = 0xFF1F1F35;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_TEXT = 0xFFCCCCCC;
    private static final int COL_MUTE = 0xFFFF4444;
    private static final int COL_MUTE_OFF = 0xFF553333;
    private static final int COL_SOLO = 0xFFFFCC00;
    private static final int COL_SOLO_OFF = 0xFF555522;
    private static final int COL_DELETE = 0xFFCC2222;
    private static final int COL_DELETE_OFF = 0xFF442222;
    private static final int COL_EDIT = 0xFF3388CC;
    private static final int COL_ADD = 0xFF00CC66;
    private static final int COL_DROPDOWN_BG = 0xEE222244;
    private static final int COL_SCROLLBAR_BG = 0xFF333355;
    private static final int COL_SCROLLBAR_THUMB = 0xFF8888CC;
    private static final int DROPDOWN_VISIBLE = 8;
    private static final int DROPDOWN_ITEM_H = 12;
    private static final int SCROLLBAR_W = 4;

    private final int x, y, width, height;
    private boolean dropdownOpen = false;
    private int dropdownScroll = 0;
    private boolean draggingScrollbar = false;

    public interface Listener {
        void onAddTrack(DubstepInstrument instrument);
        void onDeleteTrack(int trackIndex);
        void onToggleMute(int trackIndex);
        void onToggleSolo(int trackIndex);
        void onEditTrack(int trackIndex);
    }

    private final Listener listener;

    public InstrumentColumnWidget(int x, int y, int width, int height, Listener listener) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.listener = listener;
    }

    public boolean isDropdownOpen() {
        return dropdownOpen;
    }

    public void render(GuiGraphics gfx, SequenceData data) {
        Font font = Minecraft.getInstance().font;

        gfx.fill(x, y, x + width, y + height, COL_BG);

        int trackCount = data.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            TrackData track = data.getTrack(i);
            if (track == null) continue;

            int ry = y + i * ROW_H;
            int bg = (i % 2 == 0) ? COL_BG : COL_ROW_ALT;
            gfx.fill(x, ry, x + width, ry + ROW_H, bg);

            // Nom instrument complet
            String name = track.getInstrument().getDisplayName();
            gfx.drawString(font, name, x + 4, ry + 5, COL_TEXT, false);

            // Boutons apres le nom : [M] [S] [X] [Edit]
            int bx = x + 105;
            int by = ry + 2;
            int bh = ROW_H - 4;

            // M (mute)
            int mCol = track.isMuted() ? COL_MUTE : COL_MUTE_OFF;
            gfx.fill(bx, by, bx + BTN_W, by + bh, mCol);
            gfx.drawString(font, "M", bx + 3, by + 2, 0xFFFFFFFF, false);
            bx += BTN_W + BTN_GAP;

            // S (solo)
            int sCol = track.isSolo() ? COL_SOLO : COL_SOLO_OFF;
            gfx.fill(bx, by, bx + BTN_W, by + bh, sCol);
            gfx.drawString(font, "S", bx + 3, by + 2, 0xFF111111, false);
            bx += BTN_W + BTN_GAP;

            // X (delete)
            gfx.fill(bx, by, bx + BTN_W, by + bh, COL_DELETE_OFF);
            gfx.drawString(font, "X", bx + 3, by + 2, 0xFFFF6666, false);
            bx += BTN_W + BTN_GAP;

            // Edit
            int editW = font.width("Edit") + 6;
            gfx.fill(bx, by, bx + editW, by + bh, COL_EDIT);
            gfx.drawString(font, "Edit", bx + 3, by + 2, 0xFFFFFFFF, false);

            // Ligne separatrice
            gfx.fill(x, ry + ROW_H - 1, x + width, ry + ROW_H, 0xFF333344);
        }

        // Bouton [+ Add Instrument]
        if (trackCount < SequenceData.MAX_TRACKS) {
            int addY = y + trackCount * ROW_H;
            int addW = font.width("+ Add Instrument") + 10;
            int addX = x + (width - addW) / 2;
            gfx.fill(addX, addY + 2, addX + addW, addY + ROW_H - 2, COL_ADD);
            gfx.drawString(font, "+ Add Instrument", addX + 5, addY + 5, 0xFF111111, false);
        }

        if (dropdownOpen) {
            renderDropdown(gfx, font, trackCount);
        }
    }

    private void renderDropdown(GuiGraphics gfx, Font font, int trackCount) {
        int ddX = x + width / 2 - 50;
        int ddY = y + trackCount * ROW_H + ROW_H;
        int ddW = 100;
        DubstepInstrument[] instruments = DubstepInstrument.values();
        int totalCount = instruments.length;
        int visible = Math.min(totalCount - dropdownScroll, DROPDOWN_VISIBLE);
        int ddH = visible * DROPDOWN_ITEM_H + 4;
        boolean needsScroll = totalCount > DROPDOWN_VISIBLE;

        gfx.fill(ddX - 1, ddY - 1, ddX + ddW + 1, ddY + ddH + 1, COL_BORDER);
        gfx.fill(ddX, ddY, ddX + ddW, ddY + ddH, COL_DROPDOWN_BG);

        for (int i = 0; i < visible; i++) {
            int idx = i + dropdownScroll;
            if (idx >= totalCount) break;
            DubstepInstrument inst = instruments[idx];
            int iy = ddY + 2 + i * DROPDOWN_ITEM_H;

            if (idx == 3 && i > 0) {
                gfx.fill(ddX + 2, iy - 1, ddX + ddW - 2, iy, COL_BORDER);
            }

            int textRight = needsScroll ? ddX + ddW - SCROLLBAR_W - 2 : ddX + ddW;
            String label = (inst.isPercussive() ? "*" : " ") + inst.getDisplayName();
            gfx.drawString(font, label, ddX + 4, iy + 1, COL_TEXT, false);
        }

        // Scrollbar
        if (needsScroll) {
            int sbX = ddX + ddW - SCROLLBAR_W;
            int sbY = ddY + 2;
            int sbH = ddH - 4;
            gfx.fill(sbX, sbY, sbX + SCROLLBAR_W, sbY + sbH, COL_SCROLLBAR_BG);

            int maxScroll = totalCount - DROPDOWN_VISIBLE;
            int thumbH = Math.max(8, sbH * DROPDOWN_VISIBLE / totalCount);
            int thumbY = sbY + (sbH - thumbH) * dropdownScroll / maxScroll;
            gfx.fill(sbX, thumbY, sbX + SCROLLBAR_W, thumbY + thumbH, COL_SCROLLBAR_THUMB);
        }
    }

    public boolean mouseClicked(double mx, double my, int button, SequenceData data) {
        // Dropdown click
        if (dropdownOpen) {
            int trackCount = data.getTrackCount();
            int ddX = x + width / 2 - 50;
            int ddY = y + trackCount * ROW_H + ROW_H;
            int ddW = 100;
            DubstepInstrument[] instruments = DubstepInstrument.values();
            int totalCount = instruments.length;
            int visible = Math.min(totalCount - dropdownScroll, DROPDOWN_VISIBLE);
            int ddH = visible * DROPDOWN_ITEM_H + 4;
            boolean needsScroll = totalCount > DROPDOWN_VISIBLE;

            // Scrollbar drag start
            if (needsScroll) {
                int sbX = ddX + ddW - SCROLLBAR_W;
                if (mx >= sbX && mx < sbX + SCROLLBAR_W && my >= ddY && my < ddY + ddH) {
                    draggingScrollbar = true;
                    updateScrollFromMouse(my, ddY, ddH, totalCount);
                    return true;
                }
            }

            if (mx >= ddX && mx < ddX + ddW && my >= ddY && my < ddY + ddH) {
                int idx = (int) ((my - ddY - 2) / DROPDOWN_ITEM_H) + dropdownScroll;
                if (idx >= 0 && idx < totalCount) {
                    listener.onAddTrack(instruments[idx]);
                    dropdownOpen = false;
                    return true;
                }
            }
            dropdownOpen = false;
            return true;
        }

        if (mx < x || mx >= x + width || my < y || my >= y + height) return false;

        Font font = Minecraft.getInstance().font;
        int trackCount = data.getTrackCount();
        int row = (int) ((my - y) / ROW_H);

        // Click on [+ Add Instrument]
        if (row == trackCount && trackCount < SequenceData.MAX_TRACKS) {
            dropdownOpen = true;
            dropdownScroll = 0;
            return true;
        }

        if (row < 0 || row >= trackCount) return false;

        // Check button hits from right to left
        int ry = y + row * ROW_H;
        int by = ry + 2;
        int bh = ROW_H - 4;
        if (my < by || my >= by + bh) return false;

        int editW = font.width("Edit") + 6;
        int bx = x + 105;

        // M
        if (mx >= bx && mx < bx + BTN_W) {
            listener.onToggleMute(row);
            return true;
        }
        bx += BTN_W + BTN_GAP;

        // S
        if (mx >= bx && mx < bx + BTN_W) {
            listener.onToggleSolo(row);
            return true;
        }
        bx += BTN_W + BTN_GAP;

        // X
        if (mx >= bx && mx < bx + BTN_W) {
            listener.onDeleteTrack(row);
            return true;
        }
        bx += BTN_W + BTN_GAP;

        // Edit
        if (mx >= bx && mx < bx + editW) {
            listener.onEditTrack(row);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mx, double my, SequenceData data) {
        if (!draggingScrollbar || !dropdownOpen) return false;
        int trackCount = data.getTrackCount();
        int ddY = y + trackCount * ROW_H + ROW_H;
        DubstepInstrument[] instruments = DubstepInstrument.values();
        int totalCount = instruments.length;
        int visible = Math.min(totalCount - dropdownScroll, DROPDOWN_VISIBLE);
        int ddH = visible * DROPDOWN_ITEM_H + 4;
        updateScrollFromMouse(my, ddY, ddH, totalCount);
        return true;
    }

    public boolean mouseReleased() {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!dropdownOpen) return false;
        DubstepInstrument[] instruments = DubstepInstrument.values();
        dropdownScroll = Math.max(0, Math.min(instruments.length - DROPDOWN_VISIBLE,
                dropdownScroll - (int) delta));
        return true;
    }

    private void updateScrollFromMouse(double mouseY, int ddY, int ddH, int totalCount) {
        int sbY = ddY + 2;
        int sbH = ddH - 4;
        int maxScroll = totalCount - DROPDOWN_VISIBLE;
        double ratio = (mouseY - sbY) / sbH;
        dropdownScroll = (int) Math.round(ratio * maxScroll);
        dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll));
    }
}
