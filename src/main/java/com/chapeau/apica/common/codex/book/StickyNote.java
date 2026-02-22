/**
 * ============================================================
 * [StickyNote.java]
 * Description: Donnees d'une sticky note attachee a un node du Codex Book
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson                | Parsing JSON         | Chargement data-driven         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (liste de sticky notes par node)
 * - CodexBookScreen (affichage des boutons et overlay)
 * - ResonationNoteRenderer (rendu contenu resonation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.google.gson.JsonObject;

/**
 * Represente une sticky note attachee a un node du Codex.
 * @param title Titre court (si vide, deduit du craftItem)
 * @param color Couleur de fond de la note (ARGB hex)
 * @param craftItem ResourceLocation de l'item dont on affiche la recette (ex: "apica:honey_pedestal")
 * @param type Type de note ("resonation", "process", ou vide pour craft par defaut)
 * @param species Species ID pour les notes de type resonation (ex: "meadow")
 * @param input Item d'entree pour les notes de type process (ex: "minecraft:stone")
 * @param output Item de sortie pour les notes de type process (ex: "apica:infuser")
 */
public record StickyNote(String title, int color, String craftItem, String type, String species,
                         String input, String output) {

    private static final int DEFAULT_COLOR = 0xFFE8D44D;

    public static StickyNote fromJson(JsonObject json) {
        String title = json.has("title") ? json.get("title").getAsString() : "";
        int color = DEFAULT_COLOR;
        if (json.has("color")) {
            color = (int) Long.parseLong(json.get("color").getAsString().replace("0x", "").replace("#", ""), 16);
        }
        String craft = json.has("craft") ? json.get("craft").getAsString() : "";
        String type = json.has("type") ? json.get("type").getAsString() : "";
        String species = json.has("species") ? json.get("species").getAsString() : "";
        String input = json.has("input") ? json.get("input").getAsString() : "";
        String output = json.has("output") ? json.get("output").getAsString() : "";
        return new StickyNote(title, color, craft, type, species, input, output);
    }

    public boolean isResonation() {
        return "resonation".equals(type);
    }

    public boolean isProcess() {
        return "process".equals(type);
    }
}
