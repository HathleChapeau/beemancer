/**
 * ============================================================
 * [DubstepInstrument.java]
 * Description: Enum des 16 instruments vanilla Note Block pour le sequenceur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SoundEvents         | Sons Note Block      | Lecture audio par instrument    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - TrackData.java (instrument assigne a une track)
 * - SequencePlaybackEngine.java (joue les sons)
 * - InstrumentColumnWidget.java (dropdown de selection)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/**
 * Les 16 instruments vanilla du Note Block.
 * Separes en deux groupes : percussifs (pitch fixe utile) et melodiques (pitch variable).
 */
public enum DubstepInstrument {

    // --- Percussion ---
    BASS_DRUM("bass_drum", "Bass Drum", () -> SoundEvents.NOTE_BLOCK_BASEDRUM.value(), true),
    SNARE("snare", "Snare", () -> SoundEvents.NOTE_BLOCK_SNARE.value(), true),
    HAT("hat", "Hi-Hat", () -> SoundEvents.NOTE_BLOCK_HAT.value(), true),

    // --- Melodique ---
    HARP("harp", "Harp", () -> SoundEvents.NOTE_BLOCK_HARP.value(), false),
    BASS("bass", "Bass", () -> SoundEvents.NOTE_BLOCK_BASS.value(), false),
    GUITAR("guitar", "Guitar", () -> SoundEvents.NOTE_BLOCK_GUITAR.value(), false),
    FLUTE("flute", "Flute", () -> SoundEvents.NOTE_BLOCK_FLUTE.value(), false),
    BELL("bell", "Bell", () -> SoundEvents.NOTE_BLOCK_BELL.value(), false),
    CHIME("chime", "Chime", () -> SoundEvents.NOTE_BLOCK_CHIME.value(), false),
    XYLOPHONE("xylophone", "Xylophone", () -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), false),
    IRON_XYLOPHONE("iron_xylophone", "Iron Xylo", () -> SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value(), false),
    COW_BELL("cow_bell", "Cow Bell", () -> SoundEvents.NOTE_BLOCK_COW_BELL.value(), false),
    DIDGERIDOO("didgeridoo", "Didgeridoo", () -> SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), false),
    BIT("bit", "Bit", () -> SoundEvents.NOTE_BLOCK_BIT.value(), false),
    BANJO("banjo", "Banjo", () -> SoundEvents.NOTE_BLOCK_BANJO.value(), false),
    PLING("pling", "Pling", () -> SoundEvents.NOTE_BLOCK_PLING.value(), false);

    private final String id;
    private final String displayName;
    private final Supplier<SoundEvent> soundEvent;
    private final boolean percussive;

    DubstepInstrument(String id, String displayName, Supplier<SoundEvent> soundEvent, boolean percussive) {
        this.id = id;
        this.displayName = displayName;
        this.soundEvent = soundEvent;
        this.percussive = percussive;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SoundEvent getSound() {
        return soundEvent.get();
    }

    public boolean isPercussive() {
        return percussive;
    }

    /**
     * Calcule le pitch Minecraft pour une note donnee (0-24).
     * Note 12 = pitch 1.0 (son original). Range: 0.5 (grave) a 2.0 (aigu).
     */
    public float getPitch(int note) {
        return (float) Math.pow(2.0, (note - 12) / 12.0);
    }

    /**
     * Trouve un instrument par son id string. Retourne HARP par defaut.
     */
    public static DubstepInstrument fromId(String id) {
        for (DubstepInstrument inst : values()) {
            if (inst.id.equals(id)) {
                return inst;
            }
        }
        return HARP;
    }

    /**
     * Trouve un instrument par son index ordinal. Retourne HARP si hors limites.
     */
    public static DubstepInstrument fromIndex(int index) {
        DubstepInstrument[] vals = values();
        if (index < 0 || index >= vals.length) return HARP;
        return vals[index];
    }
}
