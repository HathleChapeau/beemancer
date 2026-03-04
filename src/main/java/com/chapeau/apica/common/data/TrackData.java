/**
 * ============================================================
 * [TrackData.java]
 * Description: Donnees d'une track du sequenceur (instrument, volume, mute, cellules)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | NoteCell            | Cellule de note      | Tableau de cellules par step    |
 * | DubstepInstrument   | Instrument assigne   | Son joue par cette track       |
 * | CompoundTag         | Serialisation NBT    | Sauvegarde/chargement          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - SequenceData.java (collection de tracks)
 * - InstrumentColumnWidget.java (affichage track)
 * - SequenceGridWidget.java (affichage cellules)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Une track du sequenceur : un instrument + ses parametres + un tableau de NoteCell.
 */
public class TrackData {

    private DubstepInstrument instrument;
    private float volume;
    private boolean muted;
    private boolean solo;
    private final NoteCell[] cells;

    public TrackData(DubstepInstrument instrument, int maxSteps) {
        this.instrument = instrument;
        this.volume = 1.0f;
        this.muted = false;
        this.solo = false;
        this.cells = new NoteCell[maxSteps];
        for (int i = 0; i < maxSteps; i++) {
            cells[i] = NoteCell.EMPTY;
        }
    }

    // === Cellules ===

    public NoteCell getCell(int step) {
        if (step < 0 || step >= cells.length) return NoteCell.EMPTY;
        return cells[step];
    }

    public void setCell(int step, NoteCell cell) {
        if (step >= 0 && step < cells.length) {
            cells[step] = cell;
        }
    }

    public void toggleCell(int step) {
        if (step >= 0 && step < cells.length) {
            cells[step] = cells[step].toggled();
        }
    }

    public void clearAll() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = NoteCell.EMPTY;
        }
    }

    public int getCellCount() {
        return cells.length;
    }

    // === Instrument ===

    public DubstepInstrument getInstrument() {
        return instrument;
    }

    public void setInstrument(DubstepInstrument instrument) {
        this.instrument = instrument;
    }

    // === Volume ===

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    // === Mute / Solo ===

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isSolo() {
        return solo;
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
    }

    // === NBT ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Instrument", instrument.getId());
        tag.putFloat("Volume", volume);
        tag.putBoolean("Muted", muted);
        tag.putBoolean("Solo", solo);

        int[] compactCells = new int[cells.length];
        for (int i = 0; i < cells.length; i++) {
            compactCells[i] = cells[i].toCompact() & 0xFFFF;
        }
        tag.putIntArray("Cells", compactCells);
        return tag;
    }

    public static TrackData load(CompoundTag tag, int maxSteps) {
        DubstepInstrument inst = DubstepInstrument.fromId(tag.getString("Instrument"));
        TrackData track = new TrackData(inst, maxSteps);
        track.volume = tag.getFloat("Volume");
        track.muted = tag.getBoolean("Muted");
        track.solo = tag.getBoolean("Solo");

        if (tag.contains("Cells")) {
            int[] compactCells = tag.getIntArray("Cells");
            int count = Math.min(compactCells.length, maxSteps);
            for (int i = 0; i < count; i++) {
                track.cells[i] = NoteCell.fromCompact((short) compactCells[i]);
            }
        }
        return track;
    }
}
