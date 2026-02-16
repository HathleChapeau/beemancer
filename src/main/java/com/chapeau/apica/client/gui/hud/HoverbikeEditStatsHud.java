/**
 * ============================================================
 * [HoverbikeEditStatsHud.java]
 * Description: Panneau HUD affichant les stats du Hoverbike en edit mode
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Source des stats      | Settings, mode, health, gauge  |
 * | HoverbikeSettings   | Constantes physiques | Affichage des 15 parametres    |
 * | EditModeHandler     | Hoverbike en edition | Acces a l'entite editee        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement event handler
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Affiche un panneau de stats en haut a gauche quand le joueur
 * est en edit mode sur un hoverbike. Montre toutes les constantes
 * physiques du HoverbikeSettings plus les stats runtime.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeEditStatsHud {

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int BOX_MARGIN = 10;

    private static final int BG_COLOR = 0xDD000000;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int SECTION_COLOR = 0xFF88CCFF;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int VALUE_COLOR = 0xFFFFFFFF;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.HOTBAR) return;

        HoverbikeEntity hoverbike = HoverbikeEditModeHandler.getEditingHoverbike();
        if (hoverbike == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();

        renderStatsPanel(graphics, font, hoverbike);
    }

    private static void renderStatsPanel(GuiGraphics graphics, Font font, HoverbikeEntity hoverbike) {
        HoverbikeSettings s = hoverbike.getSettings();

        // Lignes a afficher : [label, value]
        String[][] lines = {
                {"--- Vitesses ---", null},
                {"Max Hover", fmt(s.maxHoverSpeed())},
                {"Max Run", fmt(s.maxRunSpeed())},
                {"Run Threshold", fmt(s.runThresholdSpeed())},
                {"--- Acceleration ---", null},
                {"Hover Accel", fmt(s.hoverAcceleration())},
                {"Run Accel", fmt(s.runAcceleration())},
                {"Deceleration", fmt(s.deceleration())},
                {"Brake Decel", fmt(s.brakeDeceleration())},
                {"Hover Friction", fmt(s.hoverFriction())},
{"--- Rotation ---", null},
                {"Rot Speed Max", fmt(s.rotationSpeedMax())},
                {"Rot Speed Min", fmt(s.rotationSpeedMin())},
                {"--- Jauge ---", null},
                {"Gauge Fill", fmt(s.gaugeFillRate())},
                {"Gauge Drain", fmt(s.gaugeDrainRate())},
                {"Lift Speed", fmt(s.liftSpeed())},
                {"--- Runtime ---", null},
                {"Mode", hoverbike.getMode().name()},
                {"Health", String.format("%.0f / %.0f", hoverbike.getHealth(), hoverbike.getMaxHealth())},
                {"Gauge", String.format("%.0f%%", hoverbike.getGaugeLevel() * 100)},
        };

        // Calculer dimensions
        int maxWidth = font.width("Hoverbike Stats");
        for (String[] line : lines) {
            String text;
            if (line[1] == null) {
                text = line[0];
            } else {
                text = line[0] + ": " + line[1];
            }
            maxWidth = Math.max(maxWidth, font.width(text));
        }

        int boxWidth = Math.max(180, maxWidth + PADDING * 2 + 20);
        int boxHeight = PADDING * 2 + LINE_HEIGHT + 4 + LINE_HEIGHT * lines.length;

        int x = BOX_MARGIN;
        int y = BOX_MARGIN;

        // Fond et bordure
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);
        drawBorder(graphics, x, y, boxWidth, boxHeight, BORDER_COLOR);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Titre
        graphics.drawString(font, "Hoverbike Stats", textX, textY, TITLE_COLOR, false);
        textY += LINE_HEIGHT + 2;
        graphics.fill(x + 2, textY - 1, x + boxWidth - 2, textY, BORDER_COLOR);
        textY += 3;

        // Lignes
        int valueX = x + boxWidth - PADDING - 4;
        for (String[] line : lines) {
            if (line[1] == null) {
                // Section header
                graphics.drawString(font, line[0], textX, textY, SECTION_COLOR, false);
            } else {
                graphics.drawString(font, line[0], textX, textY, LABEL_COLOR, false);
                int valW = font.width(line[1]);
                graphics.drawString(font, line[1], valueX - valW, textY, VALUE_COLOR, false);
            }
            textY += LINE_HEIGHT;
        }
    }

    private static String fmt(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.4f", value);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
