/**
 * ============================================================
 * [TransportBarWidget.java]
 * Description: Barre de transport du DAW — Play/Stop, BPM, Swing, Volume master
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DubstepRadioScreen  | Ecran parent         | Callbacks pour actions         |
 * | GuiGraphics         | Rendu vectoriel      | fill(), drawString()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (widget enfant, zone haute)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Barre de transport : Play/Stop, BPM +/-, Swing slider, Volume master slider.
 * Coordonnees absolues (leftPos + offsets passes au constructeur).
 */
public class TransportBarWidget {

    private static final int BAR_H = 18;
    private static final int BTN_SIZE = 14;
    private static final int SLIDER_W = 50;
    private static final int SLIDER_H = 8;

    // Couleurs
    private static final int COL_BG = 0xFF1E1E2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_PLAY = 0xFF00CC66;
    private static final int COL_STOP = 0xFFCC3333;
    private static final int COL_TEXT = 0xFFDDDDDD;
    private static final int COL_ACCENT = 0xFF00FF88;
    private static final int COL_SLIDER_BG = 0xFF333333;
    private static final int COL_SLIDER_FILL = 0xFF00CC88;

    private final int x, y, width;

    // Etat lu depuis le screen
    private int bpm = 120;
    private boolean playing = false;
    private int swingPct = 0;
    private int volumePct = 80;

    // Drag state pour les sliders
    private int dragTarget = -1; // 0=swing, 1=volume

    /** Callback pour les actions transport. */
    public interface Listener {
        void onPlay();
        void onStop();
        void onBpmChange(int delta);
        void onSwingChange(int pct);
        void onVolumeChange(int pct);
    }

    private final Listener listener;

    public TransportBarWidget(int x, int y, int width, Listener listener) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.listener = listener;
    }

    public void update(int bpm, boolean playing, int swingPct, int volumePct) {
        this.bpm = bpm;
        this.playing = playing;
        this.swingPct = swingPct;
        this.volumePct = volumePct;
    }

    public void render(GuiGraphics gfx) {
        Font font = Minecraft.getInstance().font;

        // Background
        gfx.fill(x, y, x + width, y + BAR_H, COL_BG);
        gfx.fill(x, y + BAR_H - 1, x + width, y + BAR_H, COL_BORDER);

        int cx = x + 4;

        // Play/Stop button
        int btnY = y + 2;
        if (playing) {
            gfx.fill(cx, btnY, cx + BTN_SIZE, btnY + BTN_SIZE, COL_STOP);
            gfx.fill(cx + 3, btnY + 3, cx + BTN_SIZE - 3, btnY + BTN_SIZE - 3, 0xFFFF6666);
        } else {
            gfx.fill(cx, btnY, cx + BTN_SIZE, btnY + BTN_SIZE, COL_PLAY);
            // Triangle play (approximation avec rects)
            gfx.fill(cx + 4, btnY + 3, cx + 5, btnY + BTN_SIZE - 3, 0xFF00FF88);
            gfx.fill(cx + 5, btnY + 4, cx + 6, btnY + BTN_SIZE - 4, 0xFF00FF88);
            gfx.fill(cx + 6, btnY + 5, cx + 7, btnY + BTN_SIZE - 5, 0xFF00FF88);
            gfx.fill(cx + 7, btnY + 6, cx + 8, btnY + BTN_SIZE - 6, 0xFF00FF88);
        }
        cx += BTN_SIZE + 6;

        // BPM: < value >
        gfx.fill(cx, btnY, cx + 8, btnY + BTN_SIZE, COL_BORDER);
        gfx.drawString(font, "<", cx + 2, btnY + 3, COL_TEXT, false);
        cx += 10;

        String bpmStr = bpm + " BPM";
        gfx.drawString(font, bpmStr, cx, y + 5, COL_ACCENT, false);
        cx += font.width(bpmStr) + 2;

        gfx.fill(cx, btnY, cx + 8, btnY + BTN_SIZE, COL_BORDER);
        gfx.drawString(font, ">", cx + 2, btnY + 3, COL_TEXT, false);
        cx += 14;

        // Swing slider
        gfx.drawString(font, "Sw", cx, y + 5, COL_TEXT, false);
        cx += 14;
        renderSlider(gfx, cx, y + 5, SLIDER_W, SLIDER_H, swingPct);
        cx += SLIDER_W + 6;

        // Volume slider
        gfx.drawString(font, "Vol", cx, y + 5, COL_TEXT, false);
        cx += 18;
        renderSlider(gfx, cx, y + 5, SLIDER_W, SLIDER_H, volumePct);
    }

    private void renderSlider(GuiGraphics gfx, int sx, int sy, int sw, int sh, int pct) {
        gfx.fill(sx, sy, sx + sw, sy + sh, COL_SLIDER_BG);
        int fill = (int) (sw * pct / 100.0f);
        gfx.fill(sx, sy, sx + fill, sy + sh, COL_SLIDER_FILL);
        gfx.fill(sx + fill - 1, sy - 1, sx + fill + 1, sy + sh + 1, COL_ACCENT);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (my < y || my > y + BAR_H) return false;

        int cx = x + 4;
        int btnY = y + 2;

        // Play/Stop button
        if (mx >= cx && mx < cx + BTN_SIZE && my >= btnY && my < btnY + BTN_SIZE) {
            if (playing) listener.onStop();
            else listener.onPlay();
            return true;
        }
        cx += BTN_SIZE + 6;

        // BPM decrease
        if (mx >= cx && mx < cx + 8) {
            listener.onBpmChange(-5);
            return true;
        }
        cx += 10;

        Font font = Minecraft.getInstance().font;
        String bpmStr = bpm + " BPM";
        cx += font.width(bpmStr) + 2;

        // BPM increase
        if (mx >= cx && mx < cx + 8) {
            listener.onBpmChange(5);
            return true;
        }
        cx += 14;

        // Swing slider area
        cx += 14; // "Sw" label
        if (mx >= cx && mx < cx + SLIDER_W) {
            int pct = (int) ((mx - cx) / SLIDER_W * 100);
            listener.onSwingChange(Math.max(0, Math.min(100, pct)));
            dragTarget = 0;
            return true;
        }
        cx += SLIDER_W + 6;

        // Volume slider area
        cx += 18; // "Vol" label
        if (mx >= cx && mx < cx + SLIDER_W) {
            int pct = (int) ((mx - cx) / SLIDER_W * 100);
            listener.onVolumeChange(Math.max(0, Math.min(100, pct)));
            dragTarget = 1;
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (dragTarget < 0) return false;

        Font font = Minecraft.getInstance().font;
        int cx = x + 4 + BTN_SIZE + 6 + 10;
        String bpmStr = bpm + " BPM";
        cx += font.width(bpmStr) + 2 + 8 + 14;

        if (dragTarget == 0) {
            cx += 14;
            int pct = (int) ((mx - cx) / SLIDER_W * 100);
            listener.onSwingChange(Math.max(0, Math.min(100, pct)));
            return true;
        } else {
            cx += 14 + SLIDER_W + 6 + 18;
            int pct = (int) ((mx - cx) / SLIDER_W * 100);
            listener.onVolumeChange(Math.max(0, Math.min(100, pct)));
            return true;
        }
    }

    public void mouseReleased() {
        dragTarget = -1;
    }

    public int getHeight() {
        return BAR_H;
    }
}
