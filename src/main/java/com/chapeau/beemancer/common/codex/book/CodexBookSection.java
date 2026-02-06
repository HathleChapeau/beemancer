/**
 * ============================================================
 * [CodexBookSection.java]
 * Description: Section modulaire abstraite pour les pages du Codex Book
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson                | Parsing JSON         | Chargement depuis data-driven  |
 * | Font                | Calcul hauteur       | Layout des pages               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (liste de sections)
 * - CodexBookScreen (rendu des sections)
 * - BookPageLayout (calcul de pagination)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public abstract class CodexBookSection {

    public enum SectionType {
        HEADER("header"),
        TEXT("text"),
        IMAGE("image");

        private final String id;

        SectionType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SectionType fromId(String id) {
            for (SectionType type : values()) {
                if (type.id.equalsIgnoreCase(id)) {
                    return type;
                }
            }
            return TEXT;
        }
    }

    public abstract SectionType getType();

    /**
     * Calcule la hauteur en pixels que cette section occupe sur la page.
     * @param font Le font utilisé pour le rendu
     * @param pageWidth La largeur disponible sur la page
     * @return Hauteur en pixels
     */
    public abstract int getHeight(Font font, int pageWidth);

    /**
     * Rend cette section sur la page.
     * @param graphics Le contexte graphique
     * @param font Le font
     * @param x Position X de la page
     * @param y Position Y courante (haut de la section)
     * @param pageWidth Largeur disponible
     * @param nodeTitle Titre du node (pour HeaderSection)
     * @param relativeDay Jour relatif (pour HeaderSection, -1 si pas disponible)
     */
    public abstract void render(GuiGraphics graphics, Font font, int x, int y,
                                int pageWidth, String nodeTitle, long relativeDay);

    /**
     * Parse une section depuis un objet JSON.
     * @param json L'objet JSON décrivant la section
     * @return La section correspondante
     */
    public static CodexBookSection fromJson(JsonObject json) {
        String typeStr = json.has("type") ? json.get("type").getAsString() : "text";
        SectionType type = SectionType.fromId(typeStr);

        return switch (type) {
            case HEADER -> new HeaderSection();
            case TEXT -> TextSection.fromJson(json);
            case IMAGE -> ImageSection.fromJson(json);
        };
    }
}
