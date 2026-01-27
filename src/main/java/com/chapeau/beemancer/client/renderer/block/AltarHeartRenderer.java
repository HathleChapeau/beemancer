/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer du Honey Altar multibloc complet
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed()            |
 * | AltarConduitAnimator    | Animation conduits   | Calcul positions      |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.animation.AltarConduitAnimator;
import com.chapeau.beemancer.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand le multibloc est forme, rend un coeur de 2 blocs de large.
 * Les conduits tournent autour du coeur en orbite.
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Seulement rendre si le multibloc est forme
        if (!blockEntity.isFormed()) {
            return;
        }

        // === COEUR ACTIVE: 2 blocs de large ===
        poseStack.pushPose();
        renderBigHeart(poseStack, buffer, packedLight);
        poseStack.popPose();

        // === CONDUITS EN ORBITE === (COMMENTÉ POUR DEBUG)
        // renderOrbitingConduits(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les 4 conduits qui tournent sur eux-mêmes à leurs positions fixes.
     */
    private void renderOrbitingConduits(AltarHeartBlockEntity blockEntity, float partialTick,
                                         PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight, int packedOverlay) {

        float rotationAngle = AltarConduitAnimator.getRotationAngle(blockEntity, partialTick);

        // Utiliser le BlockState NON-formé (RenderShape.MODEL) pour le rendu
        BlockState conduitState = BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get().defaultBlockState()
            .setValue(HoneyCrystalConduitBlock.FORMED, false)
            .setValue(HoneyCrystalConduitBlock.FACING, Direction.NORTH);

        // Positions des 4 conduits cardinaux relatifs au contrôleur
        int[][] conduitOffsets = {
            {0, 1, -1},  // Nord
            {0, 1, 1},   // Sud
            {1, 1, 0},   // Est
            {-1, 1, 0}   // Ouest
        };

        for (int[] offset : conduitOffsets) {
            poseStack.pushPose();

            // Translater vers la position du conduit
            poseStack.translate(offset[0], offset[1], offset[2]);

            // Centrer pour la rotation
            poseStack.translate(0.5, 0.5, 0.5);

            // Rotation sur l'axe Y (tourner sur lui-même)
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));

            // Revenir au coin du bloc
            poseStack.translate(-0.5, -0.5, -0.5);

            // Rendre le modèle
            blockRenderer.renderSingleBlock(
                conduitState,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
            );

            poseStack.popPose();
        }
    }

    /**
     * Rend le coeur active - un gros cube de 2 blocs de large.
     */
    private void renderBigHeart(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        var pose = poseStack.last();

        // Dimensions: 2 blocs de large, centre sur le controleur
        float minX = -0.5f;
        float minY = 0.0f;
        float minZ = -0.5f;
        float maxX = 1.5f;
        float maxY = 2.0f;
        float maxZ = 1.5f;

        // Couleur doree/ambre pour le coeur actif
        float r = 1.0f;
        float g = 0.75f;
        float b = 0.2f;
        float a = 1.0f;

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * 0.6f, g * 0.6f, b * 0.6f, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * 0.6f, g * 0.6f, b * 0.6f, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * 0.6f, g * 0.6f, b * 0.6f, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * 0.6f, g * 0.6f, b * 0.6f, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * 0.8f, g * 0.8f, b * 0.8f, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * 0.7f, g * 0.7f, b * 0.7f, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        // Le coeur de 2 blocs depasse du bloc controleur
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
