/**
 * ============================================================
 * [HeaderSection.java]
 * Description: Module en-tête du Codex Book - affiche "Day X - Titre" et breeding
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | CodexNode           | Donnees du node      | Affichage breeding parents     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (section par defaut pour chaque node)
 * - CodexBookScreen (rendu de l'en-tete, passe le node via setNode)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.common.codex.CodexNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;

public class HeaderSection extends CodexBookSection {

    private static final int HEADER_COLOR = 0xFF5C3A1E;
    private static final int DAY_COLOR = 0xFF8B6914;
    private static final int BREEDING_COLOR = 0xFF6B5A48;
    private static final int SEPARATOR_COLOR = 0xFFB8956A;
    private static final int PADDING_BOTTOM = 8;
    private static final int SEPARATOR_HEIGHT = 1;

    @Nullable
    private CodexNode node;

    public void setNode(CodexNode node) {
        this.node = node;
    }

    @Override
    public SectionType getType() {
        return SectionType.HEADER;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        int height = font.lineHeight;
        if (node != null && node.hasBreedingParents()) {
            height += 2 + font.lineHeight;
        }
        height += 4 + SEPARATOR_HEIGHT + PADDING_BOTTOM;
        return height;
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

        // Titre en gras (dessiner 2 fois decale de 1px pour effet bold)
        graphics.drawString(font, displayText, x, y, HEADER_COLOR, false);
        graphics.drawString(font, displayText, x + 1, y, HEADER_COLOR, false);

        int currentY = y + font.lineHeight;

        // Breeding parents (entre le titre et le separateur)
        if (node != null && node.hasBreedingParents()) {
            currentY += 2;
            String p1 = formatSpeciesName(node.getBreedingParent1());
            String p2 = formatSpeciesName(node.getBreedingParent2());
            graphics.drawString(font, p1 + " + " + p2, x, currentY, BREEDING_COLOR, false);
            currentY += font.lineHeight;
        }

        // Ligne de separation
        int sepY = currentY + 4;
        graphics.fill(x, sepY, x + pageWidth, sepY + SEPARATOR_HEIGHT, SEPARATOR_COLOR);
    }

    private static String formatSpeciesName(String nodeId) {
        if (nodeId == null) return "?";
        String name = nodeId.replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1));
            }
        }
        return result.toString();
    }
}
