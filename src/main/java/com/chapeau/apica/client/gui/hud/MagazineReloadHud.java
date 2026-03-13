/**
 * ============================================================
 * [MagazineReloadHud.java]
 * Description: Barre de progression du reload sous le curseur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MagazineReloadAnimator  | Progression reload   | getActiveAnimator, getProgress |
 * | AnimationTimer          | Temps client         | getRenderTime                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event handler)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.hud;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.renderer.item.MagazineReloadAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Affiche une barre de progression horizontale sous le curseur
 * pendant le reload d'un IMagazineHolder.
 * Utilise les textures vanilla de la barre d'XP.
 */
@OnlyIn(Dist.CLIENT)
public class MagazineReloadHud {

    // Textures vanilla de la barre d'XP
    private static final ResourceLocation XP_BAR_BACKGROUND =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation XP_BAR_PROGRESS =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    // Dimensions de la barre (vanilla XP bar = 182x5)
    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 5;
    private static final int OFFSET_Y = 16; // Pixels sous le centre de l'écran

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        MagazineReloadAnimator animator = MagazineReloadAnimator.getActiveAnimator();
        if (animator == null || !animator.isAnimating()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float currentTime = AnimationTimer.getRenderTime(
                mc.getTimer().getGameTimeDeltaPartialTick(true));
        float progress = animator.getProgress(currentTime);
        if (progress < 0) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = (screenHeight / 2) + OFFSET_Y;

        // Background (barre vide)
        graphics.blitSprite(XP_BAR_BACKGROUND, 182, 5, 0, 0, x, y, BAR_WIDTH, BAR_HEIGHT);

        // Progress (barre remplie)
        int filledWidth = (int) (BAR_WIDTH * progress);
        if (filledWidth > 0) {
            graphics.blitSprite(XP_BAR_PROGRESS, 182, 5, 0, 0, x, y, filledWidth, BAR_HEIGHT);
        }
    }
}
