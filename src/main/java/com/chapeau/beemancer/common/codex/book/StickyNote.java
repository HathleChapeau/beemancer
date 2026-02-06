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
package com.chapeau.beemancer.common.codex.book;

import com.google.gson.JsonObject;

/**
 * Représente une sticky note attachée à un node du Codex.
 * @param title Titre court affiché sur le bouton
 * @param color Couleur de fond de la note (ARGB hex)
 */
public record StickyNote(String title, int color) {

    private static final int DEFAULT_COLOR = 0xFFE8D44D;

    public static StickyNote fromJson(JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : "Note";
        int color = DEFAULT_COLOR;
        if (json.has("color")) {
            color = (int) Long.parseLong(json.get("color").getAsString().replace("0x", "").replace("#", ""), 16);
        }
        return new StickyNote(title, color);
    }
}
