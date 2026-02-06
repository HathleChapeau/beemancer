/**
 * ============================================================
 * [TextSection.java]
 * Description: Module texte du Codex Book - paragraphe avec word wrap
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
 * - CodexBookContent (sections de texte descriptif)
 * - CodexBookScreen (rendu du texte)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextSection extends CodexBookSection {

    private static final int TEXT_COLOR = 0xFF3B2A1A;
    private static final int PADDING_BOTTOM = 6;

    private final String langKey;

    public TextSection(String langKey) {
        this.langKey = langKey;
    }

    public String getLangKey() {
        return langKey;
    }

    @Override
    public SectionType getType() {
        return SectionType.TEXT;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        Component text = Component.translatable(langKey);
        List<FormattedCharSequence> lines = font.split(text, pageWidth);
        return lines.size() * font.lineHeight + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        Component text = Component.translatable(langKey);
        List<FormattedCharSequence> lines = font.split(text, pageWidth);

        int currentY = y;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, currentY, TEXT_COLOR, false);
            currentY += font.lineHeight;
        }
    }

    public static TextSection fromJson(JsonObject json) {
        String key = json.has("key") ? json.get("key").getAsString() : "";
        return new TextSection(key);
    }
}
