/**
 * ============================================================
 * [BeeBodyType.java]
 * Description: Enum des types de corps d'abeille pour le Bee Creator
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
 * - ApicaBeeModel (layer factories, textures, attachments)
 * - BeeCreatorBlockEntity (stockage type selectionne)
 * - BeeCreatorScreen (selecteur de body)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

/**
 * Types de corps d'abeille. Chaque type definit une geometrie differente
 * avec ses propres textures et points d'attache pour les ailes/dard.
 * Le serveur ne connait que l'index; le client resout textures et layers.
 */
public enum BeeBodyType {
    DEFAULT(0, "default", "Default"),
    ROYAL(1, "royal", "Royal"),
    SEGMENTED(2, "segmented", "Segmented");

    public static final int COUNT = values().length;

    private final int index;
    private final String id;
    private final String displayName;

    BeeBodyType(int index, String id, String displayName) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public static BeeBodyType byIndex(int index) {
        if (index >= 0 && index < COUNT) return values()[index];
        return DEFAULT;
    }

    public BeeBodyType next() {
        return values()[(index + 1) % COUNT];
    }

    public BeeBodyType prev() {
        return values()[(index - 1 + COUNT) % COUNT];
    }
}
