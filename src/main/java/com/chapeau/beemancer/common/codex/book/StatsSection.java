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
        // Tier + sep + Activity + Flower + sep + 6 stats + sep + Loot + sep + 3 traits + padding
        // 1 + 2 + 6 + 1 + 3 = 13 lines + 4 separators (3px each)
        return lineH * 13 + 3 + 3 + 3 + 3 + PADDING_BOTTOM;
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

        // Tier (mis en avant avec couleur label)
        graphics.drawString(font, "Tier " + data.tier, x, currentY, LABEL_COLOR, false);
        currentY += lineH;

        // Separator
        graphics.fill(x, currentY, x + pageWidth, currentY + 1, SEPARATOR_COLOR);
        currentY += 3;

        // Activity
        graphics.drawString(font, "Activity: " + formatActivity(data.dayNight), x, currentY, INFO_COLOR, false);
        currentY += lineH;

        // Flower
        graphics.drawString(font, "Flower: " + capitalize(data.flowerType), x, currentY, INFO_COLOR, false);
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

        // Loot
        graphics.drawString(font, "Loot: " + formatItemName(data.lootItem), x, currentY, INFO_COLOR, false);
        currentY += lineH;

        // Separator
        graphics.fill(x, currentY, x + pageWidth, currentY + 1, SEPARATOR_COLOR);
        currentY += 3;

        // Traits (one per line)
        String yesNo;
        yesNo = data.aggressiveToPlayers ? "Yes" : "No";
        renderTraitLine(graphics, font, x, currentY, "Aggr. to Players", yesNo);
        currentY += lineH;

        yesNo = data.aggressiveToHostileMobs ? "Yes" : "No";
        renderTraitLine(graphics, font, x, currentY, "Aggr. to Hostiles", yesNo);
        currentY += lineH;

        yesNo = data.aggressiveToPassiveMobs ? "Yes" : "No";
        renderTraitLine(graphics, font, x, currentY, "Aggr. to Passives", yesNo);
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

    private void renderTraitLine(GuiGraphics graphics, Font font,
                                 int x, int y, String label, String value) {
        graphics.drawString(font, label + ": ", x, y, LABEL_COLOR, false);
        int valueX = x + font.width(label + ": ");
        int valueColor = "Yes".equals(value) ? STAR_FILLED_COLOR : STAR_EMPTY_COLOR;
        graphics.drawString(font, value, valueX, y, valueColor, false);
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
