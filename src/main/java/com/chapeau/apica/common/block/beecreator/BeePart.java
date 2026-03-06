/**
 * ============================================================
 * [BeePart.java]
 * Description: Enum des parties d'abeille configurables dans le Bee Creator
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
 * - BeeCreatorBlockEntity (stockage couleurs par partie)
 * - BeeCreatorMenu (sync ContainerData)
 * - BeeCreatorScreen (rendu GUI)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.beecreator;

/**
 * Parties d'abeille configurables.
 * Chaque partie a un index (pour ContainerData), un ID interne et un nom affiche.
 */
public enum BeePart {
    BODY(0, "body", "Corps", 0xCC8800),
    STRIPE(1, "stripe", "Rayure", 0x1A1A1A),
    WING(2, "wing", "Aile", 0xAADDFF),
    ANTENNA(3, "antenna", "Antenne", 0x1A1A1A),
    STINGER(4, "stinger", "Dard", 0xDDAA00),
    EYE(5, "eye", "Oeil", 0x1A1A1A),
    PUPIL(6, "pupil", "Pupille", 0xFFFFFF);

    public static final int COUNT = values().length;

    private final int index;
    private final String id;
    private final String displayName;
    private final int defaultColor;

    BeePart(int index, String id, String displayName, int defaultColor) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
        this.defaultColor = defaultColor;
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getDefaultColor() { return defaultColor; }

    public static BeePart byIndex(int index) {
        if (index < 0 || index >= COUNT) return BODY;
        return values()[index];
    }
}
