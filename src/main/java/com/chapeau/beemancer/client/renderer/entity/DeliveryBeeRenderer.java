/**
 * ============================================================
 * [DeliveryBeeRenderer.java]
 * Description: Renderer pour DeliveryBeeEntity avec scale réduit et debug phase
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeRenderer         | Renderer vanilla     | Base du rendu                  |
 * | DeliveryBeeEntity   | Entité à rendre      | Type générique + phase debug   |
 * | DebugWandItem       | Flag displayDebug    | Condition d'affichage debug    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.beemancer.common.entity.delivery.DeliveryPhaseGoal;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Bee;
import org.joml.Matrix4f;

/**
 * Renderer pour les abeilles de livraison.
 * Identique au BeeRenderer vanilla mais avec un scale réduit à 0.4f.
 * Affiche la phase courante au-dessus de la tete quand DebugWandItem.displayDebug est actif.
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

    @Override
    public void render(Bee bee, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(bee, entityYaw, partialTicks, poseStack, buffer, packedLight);

        if (!DebugWandItem.displayDebug) return;
        if (!(bee instanceof DeliveryBeeEntity deliveryBee)) return;

        DeliveryPhaseGoal goal = deliveryBee.getDeliveryGoal();
        if (goal == null) return;

        String phaseText = goal.getPhase().name();

        poseStack.pushPose();

        // Position au-dessus de la bee (compensee par le scale reduit)
        poseStack.translate(0.0, bee.getBbHeight() + 0.5, 0.0);

        // Billboard: toujours face au joueur
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        float textWidth = font.width(phaseText);
        int bgColor = (int)(0.5f * 255.0f) << 24;
        Matrix4f matrix = poseStack.last().pose();

        font.drawInBatch(
            phaseText,
            -textWidth / 2,
            0,
            0xFFFFFF00,
            false,
            matrix,
            buffer,
            Font.DisplayMode.SEE_THROUGH,
            bgColor,
            packedLight
        );

        poseStack.popPose();
    }
}
