/**
 * ============================================================
 * [HoverbikeGaugeHud.java]
 * Description: Jauge d'envol du Hoverbike affichee au-dessus de la hotbar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Source des donnees   | gaugeLevel                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java: Enregistrement event handler
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Affiche la jauge d'envol du Hoverbike au centre de l'ecran, au-dessus de la hotbar.
 * Barre horizontale : fond sombre, remplissage dore (miel).
 * Visible uniquement quand le joueur monte un Hoverbike.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeGaugeHud {

    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_Y_OFFSET = 32;

    private static final int BG_COLOR = 0xAA222222;
    private static final int BORDER_COLOR = 0xAA444444;
    private static final int FILL_COLOR_FULL = 0xFFFFCC00;
    private static final int FILL_COLOR_EMPTY = 0xFFFF4400;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof HoverbikeEntity hoverbike)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float gauge = hoverbike.getGaugeLevel();

        // Position : centre horizontalement, au-dessus de la hotbar
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - BAR_Y_OFFSET;

        // Fond + bordure
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);

        // Remplissage avec couleur interpolee (rouge quand vide, dore quand plein)
        int fillWidth = Math.round(gauge * BAR_WIDTH);
        if (fillWidth > 0) {
            int color = lerpColor(gauge, FILL_COLOR_EMPTY, FILL_COLOR_FULL);
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, color);
        }
    }

    /**
     * Interpole lineairement entre deux couleurs ARGB.
     */
    private static int lerpColor(float t, int colorA, int colorB) {
        int aA = (colorA >> 24) & 0xFF, rA = (colorA >> 16) & 0xFF, gA = (colorA >> 8) & 0xFF, bA = colorA & 0xFF;
        int aB = (colorB >> 24) & 0xFF, rB = (colorB >> 16) & 0xFF, gB = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
