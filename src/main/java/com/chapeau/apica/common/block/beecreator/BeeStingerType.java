/**
 * ============================================================
 * [BeeStingerType.java]
 * Description: Enum des types de dard d'abeille pour le Bee Creator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBeeModel (stinger layer factories, textures)
 * - BeeCreatorBlockEntity (stockage type selectionne)
 * - BeeCreatorScreen (selecteur de dard)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

/**
 * Types de dard d'abeille. Chaque type definit une geometrie et texture differente.
 * Le serveur ne connait que l'index; le client resout textures et layers.
 */
public enum BeeStingerType {
    DEFAULT(0, "default", "Default"),
    SHARP(1, "sharp", "Sharp");

    public static final int COUNT = values().length;

    private final int index;
    private final String id;
    private final String displayName;

    BeeStingerType(int index, String id, String displayName) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static BeeStingerType byIndex(int index) {
        if (index >= 0 && index < COUNT) return values()[index];
        return DEFAULT;
    }

    public BeeStingerType next() {
        return values()[(index + 1) % COUNT];
    }

    public BeeStingerType prev() {
        return values()[(index - 1 + COUNT) % COUNT];
    }
}
