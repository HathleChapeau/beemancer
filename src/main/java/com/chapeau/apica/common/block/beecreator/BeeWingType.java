/**
 * ============================================================
 * [BeeWingType.java]
 * Description: Enum des types d'ailes d'abeille pour le Bee Creator
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
 * - ApicaBeeModel (wing layer factories, textures)
 * - BeeCreatorBlockEntity (stockage type selectionne)
 * - BeeCreatorScreen (selecteur d'ailes)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

/**
 * Types d'ailes d'abeille. Chaque type definit une geometrie et texture differente.
 * Le serveur ne connait que l'index; le client resout textures et layers.
 */
public enum BeeWingType {
    DEFAULT(0, "default", "Default"),
    ROUND(1, "round", "Round");

    public static final int COUNT = values().length;

    private final int index;
    private final String id;
    private final String displayName;

    BeeWingType(int index, String id, String displayName) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static BeeWingType byIndex(int index) {
        if (index >= 0 && index < COUNT) return values()[index];
        return DEFAULT;
    }

    public BeeWingType next() {
        return values()[(index + 1) % COUNT];
    }

    public BeeWingType prev() {
        return values()[(index - 1 + COUNT) % COUNT];
    }
}
