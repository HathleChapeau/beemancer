/**
 * ============================================================
 * [StickyNote.java]
 * Description: Données d'une sticky note attachée à un node du Codex Book
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson                | Parsing JSON         | Chargement data-driven         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (liste de sticky notes par node)
 * - CodexBookScreen (affichage des boutons et overlay)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.google.gson.JsonObject;

/**
 * Représente une sticky note attachée à un node du Codex.
 * @param title Titre court (si vide, déduit du craftItem)
 * @param color Couleur de fond de la note (ARGB hex)
 * @param craftItem ResourceLocation de l'item dont on affiche la recette (ex: "apica:honey_pedestal")
 */
public record StickyNote(String title, int color, String craftItem) {

    private static final int DEFAULT_COLOR = 0xFFE8D44D;

    public static StickyNote fromJson(JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : "";
        int color = DEFAULT_COLOR;
        if (json.has("color")) {
            color = (int) Long.parseLong(json.get("color").getAsString().replace("0x", "").replace("#", ""), 16);
        }
        String craft = json.has("craft") ? json.get("craft").getAsString() : "";
        return new StickyNote(title, color, craft);
    }
}
