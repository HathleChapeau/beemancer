/**
 * ============================================================
 * [HandrawLineSection.java]
 * Description: Ligne decorative dessinee a la main entre les sections
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | ResourceLocation    | Chemin texture       | Chargement des lignes          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (separateur decoratif)
 * - CodexBookScreen (rendu)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.Apica;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public class HandrawLineSection extends CodexBookSection {

    private static final int PADDING_TOP = 1;
    private static final int PADDING_BOTTOM = 1;

    private static final int LINE_COUNT = 4;
    private static final int[][] LINE_SIZES = {
        {88, 8},   // hand_line_1
        {90, 3},   // hand_line_2
        {77, 4},   // hand_line_3
        {84, 5},   // hand_line_4
    };

    private static final int MAX_LINE_HEIGHT = 8;

    private int cachedVariant = -1;
    private int cachedSeedSource = Integer.MIN_VALUE;

    @Override
    public SectionType getType() {
        return SectionType.HANDRAW_LINE;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return PADDING_TOP + MAX_LINE_HEIGHT + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        int variant = pickVariant(y);
        int lineW = LINE_SIZES[variant][0];
        int lineH = LINE_SIZES[variant][1];

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID,
                "textures/gui/codex/codex_book/hand_line_" + (variant + 1) + ".png");

        int drawX = x + (pageWidth - lineW) / 2;
        int drawY = y + PADDING_TOP + (MAX_LINE_HEIGHT - lineH) / 2;

        graphics.blit(texture, drawX, drawY, 0, 0, lineW, lineH, lineW, lineH);
    }

    /**
     * Choisit une variante de ligne basee sur la position Y.
     * La seed est deterministe: meme position = meme ligne.
     */
    private int pickVariant(int y) {
        if (cachedSeedSource == y) return cachedVariant;
        cachedSeedSource = y;
        cachedVariant = new Random(y * 31L + 7).nextInt(LINE_COUNT);
        return cachedVariant;
    }
}
