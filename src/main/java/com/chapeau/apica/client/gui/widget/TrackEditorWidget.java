/**
 * ============================================================
 * [TrackEditorWidget.java]
 * Description: Piano-roll editor pour une track — pitches verticaux, steps horizontaux
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | TrackData           | Donnees track        | Lecture/edition des cellules   |
 * | NoteCell            | Cellule de note      | Toggle actif/pitch             |
 * | DubstepInstrument   | Couleur instrument   | Palette par instrument         |
 * | GuiGraphics         | Rendu vectoriel      | fill(), drawString()           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (widget enfant, mode editeur)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.common.data.NoteCell;
import com.chapeau.apica.common.data.TrackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Piano-roll pour une seule track (monophonique).
 * 25 lignes de pitch (F#5 en haut, F#3 en bas) x N colonnes de steps.
 * Toggle uniquement via mouseClicked (pas de drag) pour eviter le flickering.
 */
public class TrackEditorWidget {

    private static final int PITCH_COUNT = 25;
    private static final int LABEL_W = 24;
    public static final int CELL_W = 14;
    public static final int CELL_H = 6;

    private static final int COL_EMPTY = 0xFF181828;
    private static final int COL_EMPTY_ALT = 0xFF1C1C30;
    private static final int COL_BORDER = 0xFF333344;
    private static final int COL_BEAT_LINE = 0xFF444455;
    private static final int COL_PLAYHEAD = 0x5500FF88;
    private static final int COL_PLAYHEAD_LINE = 0xFF00FF88;
    private static final int COL_OCTAVE_LINE = 0xFF2A2A44;
    private static final int COL_LABEL = 0xFF888888;
    private static final int COL_LABEL_C = 0xFFBBBBBB;

    /** Palette arc-en-ciel pour les 16 instruments. */
    private static final int[] INST_COLORS = {
            0xFFFF4444, 0xFFFF8844, 0xFFFFCC44,
            0xFF44DD44, 0xFF44CCFF, 0xFF4488FF, 0xFF8844FF, 0xFFFF44FF,
            0xFFFF6644, 0xFF66FF44, 0xFF44FFCC, 0xFFCC44FF,
            0xFFFF4488, 0xFF44FF88, 0xFFFFAA44, 0xFF88CCFF
    };

    /** Note names for pitches 0-24 (F#3 to F#5). Index = pitch value. */
    private static final String[] NOTE_NAMES = {
            "F#3", "G3", "G#3", "A3", "A#3", "B3",
            "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4",
            "G4", "G#4", "A4", "A#4", "B4",
            "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5"
    };

    private final int x, y, width, height;
    private int playheadStep = -1;

    public interface Listener {
        void onCellEdit(int stepIndex, NoteCell newCell);
        void onCellPreview(int stepIndex, NoteCell cell);
    }

    private final Listener listener;

    public TrackEditorWidget(int x, int y, int width, int height, Listener listener) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.listener = listener;
    }

    public void setPlayheadStep(int step) {
        this.playheadStep = step;
    }

    public void render(GuiGraphics gfx, TrackData track, int stepCount) {
        Font font = Minecraft.getInstance().font;
        int instIdx = track.getInstrument().ordinal();
        int activeColor = INST_COLORS[instIdx % INST_COLORS.length];

        int gridX = x + LABEL_W;

        // Background
        gfx.fill(x, y, x + width, y + height, 0xFF111122);

        // Note labels (top = high pitch, bottom = low pitch)
        for (int p = 0; p < PITCH_COUNT; p++) {
            int pitch = PITCH_COUNT - 1 - p; // Row 0 = F#5 (pitch 24)
            int ry = y + p * CELL_H;
            boolean isC = (pitch == 6 || pitch == 18); // C4, C5

            String label = NOTE_NAMES[pitch];
            // Abbreviate: show full name for C and F# notes, short for others
            if (label.length() > 2) {
                label = label.substring(0, Math.min(3, label.length()));
            }
            int labelColor = isC ? COL_LABEL_C : COL_LABEL;
            gfx.drawString(font, label, x + 1, ry + 1, labelColor, false);

            // Octave highlight for C notes
            if (isC) {
                gfx.fill(gridX, ry, gridX + stepCount * CELL_W, ry + CELL_H, COL_OCTAVE_LINE);
            }
        }

        // Grid cells
        for (int p = 0; p < PITCH_COUNT; p++) {
            int pitch = PITCH_COUNT - 1 - p;
            int ry = y + p * CELL_H;

            for (int s = 0; s < stepCount; s++) {
                int cx = gridX + s * CELL_W;
                NoteCell cell = track.getCell(s);
                boolean inBeatGroup = (s / 4) % 2 == 0;
                int emptyCol = inBeatGroup ? COL_EMPTY : COL_EMPTY_ALT;

                if (cell.active() && cell.pitch() == pitch) {
                    // Active cell at this pitch
                    gfx.fill(cx + 1, ry, cx + CELL_W - 1, ry + CELL_H, activeColor);
                } else {
                    gfx.fill(cx + 1, ry, cx + CELL_W - 1, ry + CELL_H, emptyCol);
                }
            }
        }

        // Beat markers (every 4 steps)
        for (int s = 4; s < stepCount; s += 4) {
            int lx = gridX + s * CELL_W;
            gfx.fill(lx, y, lx + 1, y + PITCH_COUNT * CELL_H, COL_BEAT_LINE);
        }

        // Horizontal lines between pitches (every octave: C4=pitch6, C5=pitch18)
        for (int p = 0; p < PITCH_COUNT; p++) {
            int pitch = PITCH_COUNT - 1 - p;
            if (pitch == 6 || pitch == 12 || pitch == 18) {
                int ly = y + p * CELL_H;
                gfx.fill(gridX, ly, gridX + stepCount * CELL_W, ly + 1, COL_BORDER);
            }
        }

        // Playhead
        if (playheadStep >= 0 && playheadStep < stepCount) {
            int px = gridX + playheadStep * CELL_W;
            gfx.fill(px, y, px + CELL_W, y + PITCH_COUNT * CELL_H, COL_PLAYHEAD);
            gfx.fill(px, y, px + 1, y + PITCH_COUNT * CELL_H, COL_PLAYHEAD_LINE);
        }
    }

    /**
     * Handle click — toggle uniquement (pas de drag).
     * Monophonique : un seul pitch actif par step.
     */
    public boolean mouseClicked(double mx, double my, int button, TrackData track, int stepCount) {
        int gridX = x + LABEL_W;

        if (mx < gridX || mx >= gridX + stepCount * CELL_W) return false;
        if (my < y || my >= y + PITCH_COUNT * CELL_H) return false;

        int col = (int) ((mx - gridX) / CELL_W);
        int row = (int) ((my - y) / CELL_H);

        if (col < 0 || col >= stepCount) return false;
        if (row < 0 || row >= PITCH_COUNT) return false;

        int clickedPitch = PITCH_COUNT - 1 - row;
        NoteCell current = track.getCell(col);

        NoteCell newCell;
        if (current.active() && current.pitch() == clickedPitch) {
            // Deactivate this note
            newCell = new NoteCell(false, clickedPitch, current.velocity());
        } else {
            // Activate at this pitch (replaces any existing note at this step)
            newCell = new NoteCell(true, clickedPitch, current.active() ? current.velocity() : 80);
        }

        listener.onCellEdit(col, newCell);
        if (newCell.active()) {
            listener.onCellPreview(col, newCell);
        }
        return true;
    }
}
