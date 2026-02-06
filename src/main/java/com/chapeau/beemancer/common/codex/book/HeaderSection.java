/**
 * ============================================================
 * [HeaderSection.java]
 * Description: Module en-tête du Codex Book - affiche "Day X - Titre"
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Système de sections modulaires |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (section par défaut pour chaque node)
 * - CodexBookScreen (rendu de l'en-tête)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class HeaderSection extends CodexBookSection {

    private static final int HEADER_COLOR = 0xFF5C3A1E;
    private static final int DAY_COLOR = 0xFF8B6914;
    private static final int SEPARATOR_COLOR = 0xFFB8956A;
    private static final int PADDING_BOTTOM = 8;
    private static final int SEPARATOR_HEIGHT = 1;

    @Override
    public SectionType getType() {
        return SectionType.HEADER;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return font.lineHeight + PADDING_BOTTOM + SEPARATOR_HEIGHT + 4;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        String displayText;
        if (relativeDay > 0) {
            displayText = "Day " + relativeDay + " - " + nodeTitle;
        } else {
            displayText = nodeTitle;
        }

        // Titre en gras (dessiner 2 fois décalé de 1px pour effet bold)
        graphics.drawString(font, displayText, x, y, HEADER_COLOR, false);
        graphics.drawString(font, displayText, x + 1, y, HEADER_COLOR, false);

        // Ligne de séparation
        int sepY = y + font.lineHeight + 4;
        graphics.fill(x, sepY, x + pageWidth, sepY + SEPARATOR_HEIGHT, SEPARATOR_COLOR);
    }
}
