/**
 * ============================================================
 * [TrackData.java]
 * Description: Donnees d'une track du sequenceur — polyphonique via bitmask de pitches
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DubstepInstrument   | Instrument assigne   | Son joue par cette track       |
 * | CompoundTag         | Serialisation NBT    | Sauvegarde/chargement          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - SequenceData.java (collection de tracks)
 * - InstrumentColumnWidget.java (affichage track)
 * - TrackEditorWidget.java (affichage/edition piano-roll)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Une track du sequenceur : un instrument + parametres + grille polyphonique.
 * Chaque step stocke un bitmask de pitches actifs (25 bits pour pitches 0-24)
 * et une velocity partagee par toutes les notes du step.
 */
public class TrackData {

    public static final int PITCH_COUNT = 25;

    private DubstepInstrument instrument;
    private float volume;
    private boolean muted;
    private boolean solo;
    private final int[] pitchMasks;
    private final int[] velocities;
    private final int maxSteps;

    public TrackData(DubstepInstrument instrument, int maxSteps) {
        this.instrument = instrument;
        this.volume = 1.0f;
        this.muted = false;
        this.solo = false;
        this.maxSteps = maxSteps;
        this.pitchMasks = new int[maxSteps];
        this.velocities = new int[maxSteps];
        for (int i = 0; i < maxSteps; i++) {
            velocities[i] = 80;
        }
    }

    // === Pitch grid ===

    public boolean isPitchActive(int step, int pitch) {
        if (step < 0 || step >= maxSteps || pitch < 0 || pitch >= PITCH_COUNT) return false;
        return (pitchMasks[step] & (1 << pitch)) != 0;
    }

    public void setPitchActive(int step, int pitch, boolean active) {
        if (step < 0 || step >= maxSteps || pitch < 0 || pitch >= PITCH_COUNT) return;
        if (active) {
            pitchMasks[step] |= (1 << pitch);
        } else {
            pitchMasks[step] &= ~(1 << pitch);
        }
    }

    public boolean hasAnyNote(int step) {
        if (step < 0 || step >= maxSteps) return false;
        return pitchMasks[step] != 0;
    }

    public int getPitchMask(int step) {
        if (step < 0 || step >= maxSteps) return 0;
        return pitchMasks[step];
    }

    public int getVelocity(int step) {
        if (step < 0 || step >= maxSteps) return 80;
        return velocities[step];
    }

    public void setVelocity(int step, int vel) {
        if (step >= 0 && step < maxSteps) {
            velocities[step] = Math.max(0, Math.min(100, vel));
        }
    }

    public void clearAll() {
        for (int i = 0; i < maxSteps; i++) {
            pitchMasks[i] = 0;
            velocities[i] = 80;
        }
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    /**
     * Supprime une page en decalant les donnees vers le bas.
     * Les slots liberes en fin de tableau sont remis a zero.
     */
    public void removePage(int pageIndex, int stepsPerPage) {
        int start = pageIndex * stepsPerPage;
        int end = start + stepsPerPage;
        if (start >= maxSteps) return;
        int remaining = maxSteps - end;
        if (remaining > 0) {
            System.arraycopy(pitchMasks, end, pitchMasks, start, remaining);
            System.arraycopy(velocities, end, velocities, start, remaining);
        }
        for (int i = maxSteps - stepsPerPage; i < maxSteps; i++) {
            pitchMasks[i] = 0;
            velocities[i] = 80;
        }
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
        tag.putIntArray("PitchMasks", pitchMasks.clone());
        tag.putIntArray("Velocities", velocities.clone());
        return tag;
    }

    public static TrackData load(CompoundTag tag, int maxSteps) {
        DubstepInstrument inst = DubstepInstrument.fromId(tag.getString("Instrument"));
        TrackData track = new TrackData(inst, maxSteps);
        track.volume = tag.getFloat("Volume");
        track.muted = tag.getBoolean("Muted");
        track.solo = tag.getBoolean("Solo");

        if (tag.contains("PitchMasks")) {
            int[] masks = tag.getIntArray("PitchMasks");
            int count = Math.min(masks.length, maxSteps);
            System.arraycopy(masks, 0, track.pitchMasks, 0, count);
        }
        if (tag.contains("Velocities")) {
            int[] vels = tag.getIntArray("Velocities");
            int count = Math.min(vels.length, maxSteps);
            System.arraycopy(vels, 0, track.velocities, 0, count);
        }
        return track;
    }
}
