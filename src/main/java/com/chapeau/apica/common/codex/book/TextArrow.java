/**
 * ============================================================
 * [TextArrow.java]
 * Description: Fleche annotee pour les pages du Codex Book - positionnement absolu
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson                | Parsing JSON         | Chargement depuis data-driven  |
 * | GuiGraphics         | Rendu                | Dessin de la fleche et texte   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (liste de fleches)
 * - CodexBookScreen (rendu par-dessus les sections)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public record TextArrow(int x, int y, Direction direction, String text, String page) {

    /**
     * Direction de la fleche.
     * UP/DOWN: texte au-dessus
     * LEFT/RIGHT: texte a gauche
     */
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    // Constantes de rendu
    private static final int ARROW_LENGTH = 12;
    private static final int ARROW_THICKNESS = 1;
    private static final int ARROW_HEAD_SIZE = 3;
    private static final int TEXT_PADDING = 3;
    private static final int ARROW_COLOR = 0xFF3B2A1A;
    private static final int TEXT_COLOR = 0xFF3B2A1A;

    /**
     * Rend la fleche a une position absolue sur la page.
     * @param graphics Contexte graphique
     * @param font Font pour le texte
     * @param pageX Position X de la page
     * @param pageY Position Y de la page
     * @param scale Echelle de contenu (CONTENT_SCALE)
     */
    public void render(GuiGraphics graphics, Font font, int pageX, int pageY, float scale) {
        int drawX = Math.round((pageX + x) / scale);
        int drawY = Math.round((pageY + y) / scale);

        switch (direction) {
            case UP -> renderUpArrow(graphics, font, drawX, drawY);
            case DOWN -> renderDownArrow(graphics, font, drawX, drawY);
            case LEFT -> renderLeftArrow(graphics, font, drawX, drawY);
            case RIGHT -> renderRightArrow(graphics, font, drawX, drawY);
        }
    }

    private void renderUpArrow(GuiGraphics graphics, Font font, int x, int y) {
        // Ligne verticale
        graphics.fill(x, y, x + ARROW_THICKNESS, y + ARROW_LENGTH, ARROW_COLOR);
        // Pointe (triangle vers le haut)
        for (int i = 0; i < ARROW_HEAD_SIZE; i++) {
            graphics.fill(x - i, y + i, x + ARROW_THICKNESS + i, y + i + 1, ARROW_COLOR);
        }
        // Texte au-dessus
        int textY = y - font.lineHeight - TEXT_PADDING;
        int textX = x - font.width(text) / 2;
        graphics.drawString(font, text, textX, textY, TEXT_COLOR, false);
    }

    private void renderDownArrow(GuiGraphics graphics, Font font, int x, int y) {
        // Ligne verticale
        graphics.fill(x, y, x + ARROW_THICKNESS, y + ARROW_LENGTH, ARROW_COLOR);
        // Pointe (triangle vers le bas)
        int tipY = y + ARROW_LENGTH;
        for (int i = 0; i < ARROW_HEAD_SIZE; i++) {
            graphics.fill(x - i, tipY - i - 1, x + ARROW_THICKNESS + i, tipY - i, ARROW_COLOR);
        }
        // Texte au-dessus
        int textY = y - font.lineHeight - TEXT_PADDING;
        int textX = x - font.width(text) / 2;
        graphics.drawString(font, text, textX, textY, TEXT_COLOR, false);
    }

    private void renderLeftArrow(GuiGraphics graphics, Font font, int x, int y) {
        // Ligne horizontale
        graphics.fill(x, y, x + ARROW_LENGTH, y + ARROW_THICKNESS, ARROW_COLOR);
        // Pointe (triangle vers la gauche)
        for (int i = 0; i < ARROW_HEAD_SIZE; i++) {
            graphics.fill(x + i, y - i, x + i + 1, y + ARROW_THICKNESS + i, ARROW_COLOR);
        }
        // Texte a gauche
        int textX = x - font.width(text) - TEXT_PADDING;
        int textY = y - font.lineHeight / 2;
        graphics.drawString(font, text, textX, textY, TEXT_COLOR, false);
    }

    private void renderRightArrow(GuiGraphics graphics, Font font, int x, int y) {
        // Ligne horizontale
        graphics.fill(x, y, x + ARROW_LENGTH, y + ARROW_THICKNESS, ARROW_COLOR);
        // Pointe (triangle vers la droite)
        int tipX = x + ARROW_LENGTH;
        for (int i = 0; i < ARROW_HEAD_SIZE; i++) {
            graphics.fill(tipX - i - 1, y - i, tipX - i, y + ARROW_THICKNESS + i, ARROW_COLOR);
        }
        // Texte a gauche
        int textX = x - font.width(text) - TEXT_PADDING;
        int textY = y - font.lineHeight / 2;
        graphics.drawString(font, text, textX, textY, TEXT_COLOR, false);
    }

    /**
     * Parse une TextArrow depuis un objet JSON.
     * Format: {"x": 50, "y": 100, "direction": "right", "text": "IN", "page": "left|right"}
     */
    public static TextArrow fromJson(JsonObject json) {
        int x = json.has("x") ? json.get("x").getAsInt() : 0;
        int y = json.has("y") ? json.get("y").getAsInt() : 0;
        String dirStr = json.has("direction") ? json.get("direction").getAsString() : "right";
        Direction dir = switch (dirStr.toLowerCase()) {
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            case "left" -> Direction.LEFT;
            default -> Direction.RIGHT;
        };
        String text = json.has("text") ? json.get("text").getAsString() : "";
        String page = json.has("page") ? json.get("page").getAsString() : "left";
        return new TextArrow(x, y, dir, text, page);
    }
}
