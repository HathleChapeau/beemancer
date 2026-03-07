/**
 * ============================================================
 * [BeeAntennaType.java]
 * Description: Enum des types d'antennes d'abeille pour le Bee Creator
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
 * - ApicaBeeModel (antenna layer factories, textures)
 * - BeeCreatorBlockEntity (stockage type selectionne)
 * - BeeCreatorScreen (selecteur d'antennes)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

/**
 * Types d'antennes d'abeille. Chaque type definit une geometrie et texture differente.
 * Le serveur ne connait que l'index; le client resout textures et layers.
 */
public enum BeeAntennaType {
    DEFAULT(0, "default", "Default"),
    LONG(1, "long", "Long");

    public static final int COUNT = values().length;

    private final int index;
    private final String id;
    private final String displayName;

    BeeAntennaType(int index, String id, String displayName) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static BeeAntennaType byIndex(int index) {
        if (index >= 0 && index < COUNT) return values()[index];
        return DEFAULT;
    }

    public BeeAntennaType next() {
        return values()[(index + 1) % COUNT];
    }

    public BeeAntennaType prev() {
        return values()[(index - 1 + COUNT) % COUNT];
    }
}
