/**
 * ============================================================
 * [PageBreakSection.java]
 * Description: Section invisible qui force un saut de page dans le Codex Book
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BookPageLayout (detection du saut de page lors de la pagination)
 * - CodexBookContent (parsing JSON)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class PageBreakSection extends CodexBookSection {

    @Override
    public SectionType getType() {
        return SectionType.PAGE_BREAK;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return 0;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        // Invisible, ne rend rien
    }
}
