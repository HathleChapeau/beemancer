/**
 * ============================================================
 * [StatsSection.java]
 * Description: Module stats du Codex Book - affiche les statistiques d'une abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Système de sections modulaires |
 * | BeeSpeciesManager   | Données espèces      | Récupération des stats         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookContent (sections de stats)
 * - CodexBookScreen (rendu des statistiques)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager.BeeSpeciesData;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager.StatType;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class StatsSection extends CodexBookSection {

    private static final int LABEL_COLOR = 0xFF5C3A1E;
    private static final int STAR_FILLED_COLOR = 0xFF8B6914;
    private static final int STAR_EMPTY_COLOR = 0xFFB8956A;
    private static final int SEPARATOR_COLOR = 0xFFB8956A;
    private static final int INFO_COLOR = 0xFF6B5A48;
    private static final int PADDING_BOTTOM = 6;
    private static final int LINE_SPACING = 1;

    private static final String STAR_FILLED = "\u2605";
    private static final String STAR_EMPTY = "\u2606";

    private final String species;

    public StatsSection(String species) {
        this.species = species;
    }

    @Override
    public SectionType getType() {
        return SectionType.STATS;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        int lineH = font.lineHeight + LINE_SPACING;
        // Info line + separator + 6 stat lines + separator + loot line + padding
        return lineH * 9 + 2 + 2 + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesData data = BeeSpeciesManager.getSpecies(species);
        if (data == null) {
            graphics.drawString(font, "Unknown species: " + species, x, y, LABEL_COLOR, false);
            return;
        }

        int lineH = font.lineHeight + LINE_SPACING;
        int currentY = y;

        // Info line: "Tier I  •  Day  •  Flower"
        String activity = formatActivity(data.dayNight);
        String infoLine = "Tier " + data.tier + "  \u2022  " + activity + "  \u2022  " + capitalize(data.flowerType);
        graphics.drawString(font, infoLine, x, currentY, INFO_COLOR, false);
        currentY += lineH;

        // Separator
        graphics.fill(x, currentY, x + pageWidth, currentY + 1, SEPARATOR_COLOR);
        currentY += 3;

        // Stat lines
        renderStatLine(graphics, font, x, currentY, pageWidth, "Attack", data.attackLevel);
        currentY += lineH;
        renderStatLine(graphics, font, x, currentY, pageWidth, "Health", data.healthLevel);
        currentY += lineH;
        renderStatLine(graphics, font, x, currentY, pageWidth, "Production", data.dropLevel);
        currentY += lineH;
        renderStatLine(graphics, font, x, currentY, pageWidth, "Speed", data.flyingSpeedLevel);
        currentY += lineH;
        renderStatLine(graphics, font, x, currentY, pageWidth, "Foraging", data.foragingDurationLevel);
        currentY += lineH;
        renderStatLine(graphics, font, x, currentY, pageWidth, "Tolerance", data.toleranceLevel);
        currentY += lineH;

        // Separator
        graphics.fill(x, currentY, x + pageWidth, currentY + 1, SEPARATOR_COLOR);
        currentY += 3;

        // Loot line
        String lootName = formatItemName(data.lootItem);
        String lootLine = "Loot: " + lootName;
        graphics.drawString(font, lootLine, x, currentY, INFO_COLOR, false);
    }

    private void renderStatLine(GuiGraphics graphics, Font font,
                                int x, int y, int pageWidth,
                                String label, int level) {
        // Label
        graphics.drawString(font, label, x, y, LABEL_COLOR, false);

        // Stars aligned to the right of label area
        int starsX = x + 62;
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            stars.append(i < level ? STAR_FILLED : STAR_EMPTY);
        }

        // Draw filled stars
        int starX = starsX;
        for (int i = 0; i < 4; i++) {
            String star = i < level ? STAR_FILLED : STAR_EMPTY;
            int color = i < level ? STAR_FILLED_COLOR : STAR_EMPTY_COLOR;
            graphics.drawString(font, star, starX, y, color, false);
            starX += font.width(star) + 1;
        }
    }

    private static String formatActivity(String dayNight) {
        return switch (dayNight) {
            case "day" -> "Day";
            case "night" -> "Night";
            case "both" -> "Day & Night";
            default -> dayNight;
        };
    }

    private static String formatItemName(String itemId) {
        // "minecraft:honeycomb" -> "Honeycomb"
        String name = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        return capitalize(name.replace('_', ' '));
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder result = new StringBuilder();
        for (String word : str.split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    public static StatsSection fromJson(JsonObject json) {
        String species = json.has("species") ? json.get("species").getAsString() : "meadow";
        return new StatsSection(species);
    }
}
