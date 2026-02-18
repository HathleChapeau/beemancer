/**
 * ============================================================
 * [HoverbikeDebugHud.java]
 * Description: HUD debug affichant les infos du Hoverbike en haut a droite
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Source des donnees   | Mode, velocity, speed          |
 * | HoverbikeSettings   | Limites              | Affichage max speed            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement event handler
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeMode;
import com.chapeau.apica.common.entity.mount.HoverbikeSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Affiche les informations de debug du Hoverbike en haut a droite de l'ecran.
 * Visible uniquement quand le joueur monte un Hoverbike.
 */
@OnlyIn(Dist.CLIENT)
public class HoverbikeDebugHud {

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int HOVER_COLOR = 0x55FF55;
    private static final int RUN_COLOR = 0xFF5555;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof HoverbikeEntity hoverbike)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        HoverbikeMode mode = hoverbike.getMode();
        Vec3 vel = hoverbike.getRideVelocity();
        HoverbikeSettings settings = hoverbike.getSettings();
        double forwardSpeed = hoverbike.getForwardSpeed();
        double maxSpeed = (mode == HoverbikeMode.RUN) ? settings.maxRunSpeed() : settings.maxHoverSpeed();
        double speedPercent = maxSpeed > 0 ? (forwardSpeed / maxSpeed) * 100.0 : 0;

        int x = screenWidth - PADDING;
        int y = PADDING;

        // Mode
        int modeColor = (mode == HoverbikeMode.RUN) ? RUN_COLOR : HOVER_COLOR;
        drawRight(graphics, font, "Mode: " + mode.name(), x, y, modeColor);
        y += LINE_HEIGHT;

        // Forward speed
        drawRight(graphics, font, String.format("Speed: %.3f / %.3f (%.0f%%)", forwardSpeed, maxSpeed, speedPercent), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // Velocity components
        drawRight(graphics, font, String.format("Vel X:%.3f Y:%.3f Z:%.3f", vel.x, vel.y, vel.z), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // Horizontal speed
        double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        drawRight(graphics, font, String.format("Horiz: %.3f", horizSpeed), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // Entity yaw
        drawRight(graphics, font, String.format("Yaw: %.1f", hoverbike.getYRot()), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // On ground
        drawRight(graphics, font, "Ground: " + hoverbike.onGround(), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // Gauge
        drawRight(graphics, font, String.format("Gauge: %.0f%%", hoverbike.getGaugeLevel() * 100), x, y, TEXT_COLOR);
        y += LINE_HEIGHT;

        // Raycasts predictifs — distances
        if (hoverbike.isDebugRaysActive()) {
            double[] dists = hoverbike.getRayDistances();
            String[] labels = {"L", "C", "R", "BL", "BC", "BR"};
            StringBuilder sb = new StringBuilder("Rays:");
            for (int i = 0; i < 6; i++) {
                sb.append(" ").append(labels[i]).append(":");
                sb.append(dists[i] < 0 ? "--" : String.format("%.1f", dists[i]));
            }
            drawRight(graphics, font, sb.toString(), x, y, TEXT_COLOR);
        } else {
            drawRight(graphics, font, "Rays: inactive", x, y, 0x888888);
        }
    }

    private static void drawRight(GuiGraphics graphics, Font font, String text, int rightX, int y, int color) {
        int width = font.width(text);
        graphics.drawString(font, text, rightX - width, y, color, true);
    }
}
