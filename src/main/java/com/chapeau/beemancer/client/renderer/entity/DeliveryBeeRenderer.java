/**
 * ============================================================
 * [DeliveryBeeRenderer.java]
 * Description: Renderer pour DeliveryBeeEntity avec scale réduit
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeRenderer         | Renderer vanilla     | Base du rendu                  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Bee;

/**
 * Renderer pour les abeilles de livraison.
 * Identique au BeeRenderer vanilla mais avec un scale réduit à 0.4f.
 * Le debug de la phase est géré par DebugWandItem.addDisplay() dans DeliveryBeeEntity.
 */
public class DeliveryBeeRenderer extends BeeRenderer {

    private static final float DELIVERY_BEE_SCALE = 0.4f;

    public DeliveryBeeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Bee bee, PoseStack poseStack, float partialTick) {
        poseStack.scale(DELIVERY_BEE_SCALE, DELIVERY_BEE_SCALE, DELIVERY_BEE_SCALE);
    }
}
