/**
 * ============================================================
 * [LeafBlowerProjectileRenderer.java]
 * Description: Renderer pour le projectile orbe du Leaf Blower
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | LeafBlowerProjectileEntity    | Entité rendue        | Accès charge level + pulsing   |
 * | LeafBlowerProjectileModel     | Modèle cube          | Géométrie du rendu             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (registration du renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.entity.LeafBlowerProjectileModel;
import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class LeafBlowerProjectileRenderer extends EntityRenderer<LeafBlowerProjectileEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/leaf_blower_orb.png");

    private final LeafBlowerProjectileModel model;

    public LeafBlowerProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LeafBlowerProjectileModel(
                context.bakeLayer(LeafBlowerProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(LeafBlowerProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Scale based on charge level (bigger = stronger)
        float scale = 0.6F + entity.getChargeLevel() * 0.2F;
        if (entity.isPulsing()) {
            // Pulsating effect while pulsing
            float pulse = (float) Math.sin(entity.tickCount * 0.3) * 0.1F;
            scale += pulse;
        }
        poseStack.scale(scale, scale, scale);

        float ageInTicks = entity.tickCount + partialTick;
        model.setupAnim(entity, 0, 0, ageInTicks, 0, 0);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xAAFFFFFF);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LeafBlowerProjectileEntity entity) {
        return TEXTURE;
    }
}
