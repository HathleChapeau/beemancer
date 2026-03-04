/**
 * ============================================================
 * [InstrumentColumnWidget.java]
 * Description: Colonne gauche du DAW — liste des tracks avec mute/solo et bouton +
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
 * - DubstepRadioScreen (widget enfant, colonne gauche)
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
 * Colonne gauche : liste des tracks avec nom instrument, boutons M/S, bouton [+].
 * Gere aussi le dropdown de selection d'instrument.
 */
public class InstrumentColumnWidget {

    public static final int COL_W = 56;
    public static final int ROW_H = 14;
    private static final int BTN_W = 10;

    // Couleurs
    private static final int COL_BG = 0xFF1A1A2E;
    private static final int COL_ROW_ALT = 0xFF1F1F35;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_TEXT = 0xFFCCCCCC;
    private static final int COL_MUTE = 0xFFFF4444;
    private static final int COL_MUTE_OFF = 0xFF553333;
    private static final int COL_SOLO = 0xFFFFCC00;
    private static final int COL_SOLO_OFF = 0xFF555522;
    private static final int COL_ADD = 0xFF00CC66;
    private static final int COL_DROPDOWN_BG = 0xEE222244;
    private static final int COL_DROPDOWN_HOVER = 0xFF333366;

    private final int x, y, height;
    private boolean dropdownOpen = false;
    private int dropdownScroll = 0;
    private int selectedTrack = -1;

    public interface Listener {
        void onAddTrack(DubstepInstrument instrument);
        void onRemoveTrack(int trackIndex);
        void onToggleMute(int trackIndex);
        void onToggleSolo(int trackIndex);
        void onSelectTrack(int trackIndex);
    }

    private final Listener listener;

    public InstrumentColumnWidget(int x, int y, int height, Listener listener) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.listener = listener;
    }

    public int getSelectedTrack() {
        return selectedTrack;
    }

    public boolean isDropdownOpen() {
        return dropdownOpen;
    }

    public void render(GuiGraphics gfx, SequenceData data) {
        Font font = Minecraft.getInstance().font;

        // Background
        gfx.fill(x, y, x + COL_W, y + height, COL_BG);
        gfx.fill(x + COL_W - 1, y, x + COL_W, y + height, COL_BORDER);

        int trackCount = data.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            TrackData track = data.getTrack(i);
            if (track == null) continue;

            int ry = y + i * ROW_H;
            int bg = (i % 2 == 0) ? COL_BG : COL_ROW_ALT;
            if (i == selectedTrack) bg = 0xFF2A2A4A;
            gfx.fill(x, ry, x + COL_W - 1, ry + ROW_H, bg);

            // Nom instrument (tronque)
            String name = track.getInstrument().getDisplayName();
            if (name.length() > 5) name = name.substring(0, 5);
            gfx.drawString(font, name, x + 2, ry + 3, COL_TEXT, false);

            // Bouton M (mute)
            int mx = x + COL_W - BTN_W * 2 - 4;
            int mCol = track.isMuted() ? COL_MUTE : COL_MUTE_OFF;
            gfx.fill(mx, ry + 2, mx + BTN_W, ry + ROW_H - 2, mCol);
            gfx.drawString(font, "M", mx + 2, ry + 3, 0xFFFFFFFF, false);

            // Bouton S (solo)
            int sx = x + COL_W - BTN_W - 2;
            int sCol = track.isSolo() ? COL_SOLO : COL_SOLO_OFF;
            gfx.fill(sx, ry + 2, sx + BTN_W, ry + ROW_H - 2, sCol);
            gfx.drawString(font, "S", sx + 2, ry + 3, 0xFF111111, false);

            // Ligne separatrice
            gfx.fill(x, ry + ROW_H - 1, x + COL_W - 1, ry + ROW_H, 0xFF333344);
        }

        // Bouton [+]
        if (trackCount < SequenceData.MAX_TRACKS) {
            int addY = y + trackCount * ROW_H;
            gfx.fill(x + 2, addY + 2, x + COL_W - 3, addY + ROW_H - 2, COL_ADD);
            gfx.drawString(font, "+ Add", x + 6, addY + 3, 0xFF111111, false);
        }

        // Dropdown
        if (dropdownOpen) {
            renderDropdown(gfx, font, data.getTrackCount());
        }
    }

    private void renderDropdown(GuiGraphics gfx, Font font, int trackCount) {
        int ddX = x;
        int ddY = y + trackCount * ROW_H + ROW_H;
        int ddW = 80;
        DubstepInstrument[] instruments = DubstepInstrument.values();
        int visible = Math.min(instruments.length - dropdownScroll, 8);
        int ddH = visible * 12 + 4;

        // Ombre + fond
        gfx.fill(ddX - 1, ddY - 1, ddX + ddW + 1, ddY + ddH + 1, COL_BORDER);
        gfx.fill(ddX, ddY, ddX + ddW, ddY + ddH, COL_DROPDOWN_BG);

        for (int i = 0; i < visible; i++) {
            int idx = i + dropdownScroll;
            if (idx >= instruments.length) break;
            DubstepInstrument inst = instruments[idx];
            int iy = ddY + 2 + i * 12;

            // Separateur percussion / melodique
            if (idx == 3 && i > 0) {
                gfx.fill(ddX + 2, iy - 1, ddX + ddW - 2, iy, COL_BORDER);
            }

            String label = (inst.isPercussive() ? "*" : " ") + inst.getDisplayName();
            gfx.drawString(font, label, ddX + 4, iy + 1, COL_TEXT, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int button, SequenceData data) {
        // Dropdown click
        if (dropdownOpen) {
            int trackCount = data.getTrackCount();
            int ddX = x;
            int ddY = y + trackCount * ROW_H + ROW_H;
            int ddW = 80;
            DubstepInstrument[] instruments = DubstepInstrument.values();
            int visible = Math.min(instruments.length - dropdownScroll, 8);

            if (mx >= ddX && mx < ddX + ddW && my >= ddY && my < ddY + visible * 12 + 4) {
                int idx = (int) ((my - ddY - 2) / 12) + dropdownScroll;
                if (idx >= 0 && idx < instruments.length) {
                    listener.onAddTrack(instruments[idx]);
                    dropdownOpen = false;
                    return true;
                }
            }
            dropdownOpen = false;
            return true;
        }

        if (mx < x || mx >= x + COL_W || my < y || my >= y + height) return false;

        int trackCount = data.getTrackCount();
        int row = (int) ((my - y) / ROW_H);

        // Click on [+] button
        if (row == trackCount && trackCount < SequenceData.MAX_TRACKS) {
            dropdownOpen = true;
            dropdownScroll = 0;
            return true;
        }

        if (row < 0 || row >= trackCount) return false;

        // Right click = remove
        if (button == 1) {
            listener.onRemoveTrack(row);
            if (selectedTrack >= trackCount - 1) selectedTrack = trackCount - 2;
            return true;
        }

        // Check M/S buttons
        int mxRel = (int) mx;
        int mBtnX = x + COL_W - BTN_W * 2 - 4;
        int sBtnX = x + COL_W - BTN_W - 2;

        if (mxRel >= mBtnX && mxRel < mBtnX + BTN_W) {
            listener.onToggleMute(row);
            return true;
        }
        if (mxRel >= sBtnX && mxRel < sBtnX + BTN_W) {
            listener.onToggleSolo(row);
            return true;
        }

        // Select track
        selectedTrack = row;
        listener.onSelectTrack(row);
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!dropdownOpen) return false;
        DubstepInstrument[] instruments = DubstepInstrument.values();
        dropdownScroll = Math.max(0, Math.min(instruments.length - 8, dropdownScroll - (int) delta));
        return true;
    }
}
