/**
 * ============================================================
 * [TextSection.java]
 * Description: Module texte du Codex Book - paragraphe avec word wrap
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
 * - CodexBookContent (sections de texte descriptif)
 * - CodexBookScreen (rendu du texte)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
        Component text = resolveText();
        List<FormattedCharSequence> lines = font.split(text, pageWidth);
        return lines.size() * font.lineHeight + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        Component text = resolveText();
        List<FormattedCharSequence> lines = font.split(text, pageWidth);

        int currentY = y;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, currentY, TEXT_COLOR, false);
            currentY += font.lineHeight;
        }
    }

    /**
     * Resout le texte traduit en construisant un Component tree propre.
     * Les codes de formatage (par ex. §6§l...§r) sont convertis en siblings
     * avec des Style explicites, ce qui evite les bugs de Font.split()
     * quand les codes § tombent sur une frontiere de ligne.
     */
    private Component resolveText() {
        String raw = Language.getInstance().getOrDefault(langKey, langKey);
        if (!raw.contains("\u00a7")) {
            return Component.translatable(langKey);
        }
        return parseFormattedText(raw);
    }

    /**
     * Convertit une chaine avec des codes § en un Component tree
     * ou chaque segment de texte est un sibling avec un Style explicite.
     */
    private static Component parseFormattedText(String raw) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00a7' && i + 1 < raw.length()) {
                if (buf.length() > 0) {
                    result.append(Component.literal(buf.toString()).withStyle(style));
                    buf.setLength(0);
                }
                char code = raw.charAt(++i);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        style = Style.EMPTY.applyFormat(fmt);
                    } else {
                        style = style.applyFormat(fmt);
                    }
                }
            } else {
                buf.append(c);
            }
        }

        if (buf.length() > 0) {
            result.append(Component.literal(buf.toString()).withStyle(style));
        }

        return result;
    }

    public static TextSection fromJson(JsonObject json) {
        String key = json.has("key") ? json.get("key").getAsString() : "";
        return new TextSection(key);
    }
}
