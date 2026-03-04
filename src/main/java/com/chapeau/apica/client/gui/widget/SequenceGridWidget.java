/**
 * ============================================================
 * [SequenceGridWidget.java]
 * Description: Grille de notes du sequenceur — toggle cellules, playhead, drag-paint
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SequenceData        | Donnees sequence     | Lecture tracks et cellules     |
 * | NoteCell            | Cellule de note      | Etat actif/pitch/velocity      |
 * | DubstepInstrument   | Couleur instrument   | Palette par instrument         |
 * | GuiGraphics         | Rendu vectoriel      | fill() pour chaque cellule     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (widget enfant, zone principale)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.common.data.NoteCell;
import com.chapeau.apica.common.data.SequenceData;
import com.chapeau.apica.common.data.TrackData;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Grille du sequenceur : colonnes = steps, lignes = tracks.
 * Click gauche toggle les cellules, drag peint, playhead anime.
 */
public class SequenceGridWidget {

    public static final int CELL_W = 14;
    public static final int CELL_H = 14;
    private static final int CELL_PAD = 1;

    // Couleurs
    private static final int COL_EMPTY = 0xFF181828;
    private static final int COL_EMPTY_ALT = 0xFF1C1C30;
    private static final int COL_BORDER = 0xFF333344;
    private static final int COL_BEAT_LINE = 0xFF444455;
    private static final int COL_PLAYHEAD = 0x5500FF88;
    private static final int COL_PLAYHEAD_LINE = 0xFF00FF88;

    /** Palette arc-en-ciel pour les 16 instruments. */
    private static final int[] INST_COLORS = {
            0xFFFF4444, 0xFFFF8844, 0xFFFFCC44, // Perc: red, orange, yellow
            0xFF44DD44, 0xFF44CCFF, 0xFF4488FF, 0xFF8844FF, 0xFFFF44FF, // Melodic 1-5
            0xFFFF6644, 0xFF66FF44, 0xFF44FFCC, 0xFFCC44FF,             // Melodic 6-9
            0xFFFF4488, 0xFF44FF88, 0xFFFFAA44, 0xFF88CCFF              // Melodic 10-13
    };

    private final int x, y, width, height;
    private int playheadStep = -1;
    private boolean dragging = false;
    private boolean dragActivate = true;

    public interface Listener {
        void onCellToggle(int trackIndex, int stepIndex);
        void onCellPreview(int trackIndex, int stepIndex);
    }

    private final Listener listener;

    public SequenceGridWidget(int x, int y, int width, int height, Listener listener) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.listener = listener;
    }

    public void setPlayheadStep(int step) {
        this.playheadStep = step;
    }

    public void render(GuiGraphics gfx, SequenceData data) {
        int trackCount = data.getTrackCount();
        int stepCount = data.getStepCount();

        // Fond
        gfx.fill(x, y, x + width, y + height, 0xFF111122);

        for (int t = 0; t < trackCount; t++) {
            TrackData track = data.getTrack(t);
            if (track == null) continue;

            int instIdx = track.getInstrument().ordinal();
            int activeColor = INST_COLORS[instIdx % INST_COLORS.length];

            for (int s = 0; s < stepCount; s++) {
                int cx = x + s * CELL_W;
                int cy = y + t * CELL_H;

                NoteCell cell = track.getCell(s);
                boolean inBeatGroup = (s / 4) % 2 == 0;
                int emptyCol = inBeatGroup ? COL_EMPTY : COL_EMPTY_ALT;

                if (cell.active()) {
                    // Cellule active avec couleur instrument
                    gfx.fill(cx + CELL_PAD, cy + CELL_PAD,
                            cx + CELL_W - CELL_PAD, cy + CELL_H - CELL_PAD, activeColor);
                    // Indicateur velocity (barre interne proportionnelle)
                    int velH = (int) ((CELL_H - 4) * cell.velocity() / 100.0f);
                    int velY = cy + CELL_H - CELL_PAD - 1 - velH;
                    gfx.fill(cx + CELL_PAD + 1, velY,
                            cx + CELL_W - CELL_PAD - 1, cy + CELL_H - CELL_PAD - 1,
                            brighten(activeColor, 40));
                } else {
                    gfx.fill(cx + CELL_PAD, cy + CELL_PAD,
                            cx + CELL_W - CELL_PAD, cy + CELL_H - CELL_PAD, emptyCol);
                }
            }
        }

        // Lignes de beat (tous les 4 steps)
        for (int s = 4; s < stepCount; s += 4) {
            int lx = x + s * CELL_W;
            gfx.fill(lx, y, lx + 1, y + trackCount * CELL_H, COL_BEAT_LINE);
        }

        // Bordure basse des tracks
        for (int t = 1; t < trackCount; t++) {
            int ly = y + t * CELL_H;
            gfx.fill(x, ly, x + stepCount * CELL_W, ly, COL_BORDER);
        }

        // Playhead
        if (playheadStep >= 0 && playheadStep < stepCount) {
            int px = x + playheadStep * CELL_W;
            gfx.fill(px, y, px + CELL_W, y + trackCount * CELL_H, COL_PLAYHEAD);
            gfx.fill(px, y, px + 1, y + trackCount * CELL_H, COL_PLAYHEAD_LINE);
        }
    }

    private static int brighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mx, double my, int button, SequenceData data) {
        int[] cell = getCellAt(mx, my, data);
        if (cell == null) return false;

        int trackIdx = cell[0];
        int stepIdx = cell[1];
        TrackData track = data.getTrack(trackIdx);
        if (track == null) return false;

        // Toggle la cellule
        listener.onCellToggle(trackIdx, stepIdx);
        dragActivate = !track.getCell(stepIdx).active();
        dragging = true;

        // Preview audio
        listener.onCellPreview(trackIdx, stepIdx);
        return true;
    }

    public boolean mouseDragged(double mx, double my, SequenceData data) {
        if (!dragging) return false;
        int[] cell = getCellAt(mx, my, data);
        if (cell == null) return false;

        TrackData track = data.getTrack(cell[0]);
        if (track == null) return false;

        boolean isActive = track.getCell(cell[1]).active();
        if (isActive != dragActivate) {
            listener.onCellToggle(cell[0], cell[1]);
        }
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    private int[] getCellAt(double mx, double my, SequenceData data) {
        if (mx < x || my < y) return null;

        int col = (int) ((mx - x) / CELL_W);
        int row = (int) ((my - y) / CELL_H);

        if (col < 0 || col >= data.getStepCount()) return null;
        if (row < 0 || row >= data.getTrackCount()) return null;

        return new int[]{row, col};
    }
}
