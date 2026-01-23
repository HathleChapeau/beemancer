/**
 * ============================================================
 * [CodexNodeCategory.java]
 * Description: Catégories de nodes avec styles visuels différents
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (Aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexNode (définition de la catégorie)
 * - CodexNodeWidget (sélection du background)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

public enum CodexNodeCategory {
    ROOT("root", 0, 0),
    NORMAL("normal", 26, 0),
    GOAL("goal", 52, 0),
    CHALLENGE("challenge", 78, 0);

    private final String id;
    private final int textureU;
    private final int textureV;

    CodexNodeCategory(String id, int textureU, int textureV) {
        this.id = id;
        this.textureU = textureU;
        this.textureV = textureV;
    }

    public String getId() {
        return id;
    }

    public int getTextureU() {
        return textureU;
    }

    public int getTextureV() {
        return textureV;
    }

    public static CodexNodeCategory fromId(String id) {
        for (CodexNodeCategory category : values()) {
            if (category.id.equalsIgnoreCase(id)) {
                return category;
            }
        }
        return NORMAL;
    }
}
