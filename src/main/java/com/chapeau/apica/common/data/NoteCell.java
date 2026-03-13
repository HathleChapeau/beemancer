/**
 * ============================================================
 * [NoteCell.java]
 * Description: Cellule unitaire d'une grille de sequenceur (active, pitch, velocity)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | Record autonome      | Encodage compact en short      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - TrackData.java (tableau de cellules par track)
 * - SequenceGridWidget.java (affichage et edition)
 * - WaveMixerEditPacket.java (transmission reseau)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

/**
 * Cellule immutable d'une grille de sequenceur.
 * Encodage compact sur 16 bits : 1 bit active + 5 bits pitch (0-24) + 7 bits velocity (0-100).
 */
public record NoteCell(boolean active, int pitch, int velocity) {

    public static final NoteCell EMPTY = new NoteCell(false, 12, 80);

    private static final int ACTIVE_BIT = 0x8000;
    private static final int PITCH_SHIFT = 7;
    private static final int PITCH_MASK = 0x1F;
    private static final int VELOCITY_MASK = 0x7F;

    /**
     * Encode cette cellule en un short compact (16 bits).
     * Layout: [1 active][5 pitch][7 velocity][3 unused]
     */
    public short toCompact() {
        int bits = active ? ACTIVE_BIT : 0;
        bits |= (pitch & PITCH_MASK) << PITCH_SHIFT;
        bits |= (velocity & VELOCITY_MASK);
        return (short) bits;
    }

    /**
     * Decode un short compact en NoteCell.
     */
    public static NoteCell fromCompact(short val) {
        boolean a = (val & ACTIVE_BIT) != 0;
        int p = (val >> PITCH_SHIFT) & PITCH_MASK;
        int v = val & VELOCITY_MASK;
        return new NoteCell(a, p, v);
    }

    /**
     * Retourne une copie avec l'etat active inverse.
     */
    public NoteCell toggled() {
        return new NoteCell(!active, pitch, velocity);
    }

    /**
     * Retourne une copie avec un nouveau pitch.
     */
    public NoteCell withPitch(int newPitch) {
        return new NoteCell(active, Math.max(0, Math.min(24, newPitch)), velocity);
    }

    /**
     * Retourne une copie avec une nouvelle velocity.
     */
    public NoteCell withVelocity(int newVelocity) {
        return new NoteCell(active, pitch, Math.max(0, Math.min(100, newVelocity)));
    }
}
