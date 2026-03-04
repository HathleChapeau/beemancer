/**
 * ============================================================
 * [TransportBarWidget.java]
 * Description: Barre de transport du DAW — Play/Stop, Mode, BPM, bouton Back optionnel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PlayMode            | Mode de lecture      | Affichage/toggle du mode       |
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

import com.chapeau.apica.client.audio.PlayMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Barre de transport : [Back] (optionnel) | Play/Stop | [Mode] | BPM +/-.
 * Coordonnees absolues (leftPos + offsets passes au constructeur).
 */
public class TransportBarWidget {

    private static final int BAR_H = 18;
    private static final int BTN_SIZE = 14;

    private static final int COL_BG = 0xFF1E1E2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_PLAY = 0xFF00CC66;
    private static final int COL_STOP = 0xFFCC3333;
    private static final int COL_TEXT = 0xFFDDDDDD;
    private static final int COL_ACCENT = 0xFF00FF88;
    private static final int COL_BACK = 0xFF4466AA;
    private static final int COL_MODE = 0xFF335577;
    private static final int COL_ADD = 0xFF00CC66;

    private final int x, y, width;
    private final Listener listener;
    private final Runnable backAction;
    private final Runnable addAction;

    private static final int COL_ADD_DISABLED = 0xFF444444;
    private static final String ADD_LABEL = "Add Instrument";

    private int bpm = 120;
    private boolean playing = false;
    private PlayMode playMode = PlayMode.LOOP;
    private boolean canAdd = false;
    private int addBtnX, addBtnBottomY;

    /** Callback pour les actions transport. */
    public interface Listener {
        void onPlay();
        void onStop();
        void onBpmChange(int delta);
        void onModeChange(PlayMode newMode);
    }

    public TransportBarWidget(int x, int y, int width, Listener listener,
                              Runnable backAction, Runnable addAction) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.listener = listener;
        this.backAction = backAction;
        this.addAction = addAction;
    }

    public void update(int bpm, boolean playing, PlayMode playMode, boolean canAdd) {
        this.bpm = bpm;
        this.playing = playing;
        this.playMode = playMode;
        this.canAdd = canAdd;
    }

    public void render(GuiGraphics gfx) {
        Font font = Minecraft.getInstance().font;

        gfx.fill(x, y, x + width, y + BAR_H, COL_BG);
        gfx.fill(x, y + BAR_H - 1, x + width, y + BAR_H, COL_BORDER);

        int cx = x + 4;
        int btnY = y + 2;

        // Back button (optionnel)
        if (backAction != null) {
            int backW = font.width("< Back") + 6;
            gfx.fill(cx, btnY, cx + backW, btnY + BTN_SIZE, COL_BACK);
            gfx.drawString(font, "< Back", cx + 3, btnY + 3, COL_TEXT, false);
            cx += backW + 4;
        }

        // Play/Stop button
        if (playing) {
            gfx.fill(cx, btnY, cx + BTN_SIZE, btnY + BTN_SIZE, COL_STOP);
            gfx.fill(cx + 3, btnY + 3, cx + BTN_SIZE - 3, btnY + BTN_SIZE - 3, 0xFFFF6666);
        } else {
            gfx.fill(cx, btnY, cx + BTN_SIZE, btnY + BTN_SIZE, COL_PLAY);
            gfx.fill(cx + 4, btnY + 3, cx + 5, btnY + BTN_SIZE - 3, 0xFF00FF88);
            gfx.fill(cx + 5, btnY + 4, cx + 6, btnY + BTN_SIZE - 4, 0xFF00FF88);
            gfx.fill(cx + 6, btnY + 5, cx + 7, btnY + BTN_SIZE - 5, 0xFF00FF88);
            gfx.fill(cx + 7, btnY + 6, cx + 8, btnY + BTN_SIZE - 6, 0xFF00FF88);
        }
        cx += BTN_SIZE + 4;

        // Mode button
        String modeLabel = playMode.getLabel();
        int modeW = font.width(modeLabel) + 6;
        gfx.fill(cx, btnY, cx + modeW, btnY + BTN_SIZE, COL_MODE);
        gfx.drawString(font, modeLabel, cx + 3, btnY + 3, COL_TEXT, false);
        cx += modeW + 4;

        // BPM: < value >
        gfx.fill(cx, btnY, cx + 8, btnY + BTN_SIZE, COL_BORDER);
        gfx.drawString(font, "<", cx + 2, btnY + 3, COL_TEXT, false);
        cx += 10;

        String bpmStr = bpm + " BPM";
        gfx.drawString(font, bpmStr, cx, y + 5, COL_ACCENT, false);
        cx += font.width(bpmStr) + 2;

        gfx.fill(cx, btnY, cx + 8, btnY + BTN_SIZE, COL_BORDER);
        gfx.drawString(font, ">", cx + 2, btnY + 3, COL_TEXT, false);
        cx += 10;

        // Bouton [Add Instrument] (mode principal uniquement)
        if (addAction != null) {
            cx += 4;
            int addW = font.width(ADD_LABEL) + 6;
            int bgCol = canAdd ? COL_ADD : COL_ADD_DISABLED;
            int txtCol = canAdd ? 0xFF111111 : 0xFF888888;
            gfx.fill(cx, btnY, cx + addW, btnY + BTN_SIZE, bgCol);
            gfx.drawString(font, ADD_LABEL, cx + 3, btnY + 3, txtCol, false);
            addBtnX = cx;
            addBtnBottomY = btnY + BTN_SIZE;
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (my < y || my > y + BAR_H) return false;

        Font font = Minecraft.getInstance().font;
        int cx = x + 4;
        int btnY = y + 2;

        // Back button
        if (backAction != null) {
            int backW = font.width("< Back") + 6;
            if (mx >= cx && mx < cx + backW && my >= btnY && my < btnY + BTN_SIZE) {
                backAction.run();
                return true;
            }
            cx += backW + 4;
        }

        // Play/Stop button
        if (mx >= cx && mx < cx + BTN_SIZE && my >= btnY && my < btnY + BTN_SIZE) {
            if (playing) listener.onStop();
            else listener.onPlay();
            return true;
        }
        cx += BTN_SIZE + 4;

        // Mode button
        String modeLabel = playMode.getLabel();
        int modeW = font.width(modeLabel) + 6;
        if (mx >= cx && mx < cx + modeW && my >= btnY && my < btnY + BTN_SIZE) {
            listener.onModeChange(playMode.next());
            return true;
        }
        cx += modeW + 4;

        // BPM decrease
        if (mx >= cx && mx < cx + 8) {
            listener.onBpmChange(-5);
            return true;
        }
        cx += 10;

        String bpmStr = bpm + " BPM";
        cx += font.width(bpmStr) + 2;

        // BPM increase
        if (mx >= cx && mx < cx + 8) {
            listener.onBpmChange(5);
            return true;
        }
        cx += 10;

        // Add instrument button
        if (addAction != null) {
            cx += 4;
            Font f = Minecraft.getInstance().font;
            int addW = f.width(ADD_LABEL) + 6;
            if (canAdd && mx >= cx && mx < cx + addW && my >= btnY && my < btnY + BTN_SIZE) {
                addAction.run();
                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        return false;
    }

    public void mouseReleased() {
    }

    /** Position X du bouton Add Instrument (pour ancrer le dropdown). */
    public int getAddBtnX() { return addBtnX; }

    /** Position Y du bas du bouton Add Instrument (pour ancrer le dropdown). */
    public int getAddBtnBottomY() { return addBtnBottomY; }

    public int getHeight() {
        return BAR_H;
    }
}
