/**
 * ============================================================
 * [FloatingItemHelper.java]
 * Description: Helper pour rendre un item flottant avec bobbing et rotation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation               |
 * |---------------------|----------------------|---------------------------|
 * | ItemRenderer        | Rendu item           | renderStatic()            |
 * | Axis                | Rotation Y           | mulPose()                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InfuserRenderer, IncubatorRenderer, HoneyPedestalRenderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.util;

import com.chapeau.beemancer.client.animation.AnimationTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rend un item flottant avec bobbing sinusoidal et rotation Y.
 * Utilise pour les machines qui affichent un item au centre (infuser, incubator, pedestal).
 */
public final class FloatingItemHelper {

    private FloatingItemHelper() {}

    /**
     * Rend un item flottant avec bobbing et rotation Y.
     *
     * @param itemRenderer  le renderer d'items
     * @param item          l'item a afficher
     * @param level         le level (pour gameTime)
     * @param partialTick   tick partiel pour interpolation
     * @param poseStack     la pose stack
     * @param buffer        le buffer de rendu
     * @param light         packed light
     * @param overlay       packed overlay
     * @param x             position X (centre du bloc = 0.5)
     * @param y             position Y de base (centre = 0.5, dessus pedestal = 1.25)
     * @param z             position Z (centre du bloc = 0.5)
     * @param scale         echelle de l'item (0.4 = standard, 0.5 = pedestal)
     * @param bobAmplitude  amplitude du bobbing (0.03 = subtil, 0.05 = visible)
     * @param rotationSpeed vitesse de rotation en degres/tick (1.5 = lent, 2.0 = normal)
     */
    public static void renderFloatingItem(ItemRenderer itemRenderer, ItemStack item,
                                           Level level, float partialTick,
                                           PoseStack poseStack, MultiBufferSource buffer,
                                           int light, int overlay,
                                           double x, double y, double z,
                                           float scale, float bobAmplitude, float rotationSpeed) {
        float time = AnimationTimer.getRenderTime(partialTick);
        float bob = (float) Math.sin(time * 0.1) * bobAmplitude;

        poseStack.pushPose();
        poseStack.translate(x, y + bob, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * rotationSpeed));
        poseStack.scale(scale, scale, scale);

        itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, light, overlay,
            poseStack, buffer, level, 0);

        poseStack.popPose();
    }
}
