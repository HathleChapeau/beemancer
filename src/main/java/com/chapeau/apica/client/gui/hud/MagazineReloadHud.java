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
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.client.renderer.item.MagazineReloadAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Affiche une barre de progression horizontale sous le curseur
 * pendant le reload d'un IMagazineHolder.
 * Utilise la meme barre de progression que le Crystallizer.
 */
@OnlyIn(Dist.CLIENT)
public class MagazineReloadHud {

    // Dimensions de la barre (style Crystallizer)
    private static final int BAR_WIDTH = 58;
    private static final int BAR_HEIGHT = 3;
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

        // Progress bar style Crystallizer (GuiRenderHelper)
        GuiRenderHelper.renderProgressBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, progress);
    }
}
