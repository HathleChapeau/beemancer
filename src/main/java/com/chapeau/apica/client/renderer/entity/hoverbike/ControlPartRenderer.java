/**
 * ============================================================
 * [ControlPartRenderer.java]
 * Description: Rendu des controles HoverBee et ses effets (ring, flip)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ControlPartModelC   | Modele control C     | Ring center                    |
 * | HoverbikePart       | Type de partie       | Detection LEFT/RIGHT           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu des controles
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.hoverbike.ControlPartModelC;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer specifique pour les controles du HoverBee.
 * - Gere le flip horizontal pour le controle droit
 * - Rend l'anneau rotatif pour la variante C
 */
public final class ControlPartRenderer {

    private ControlPartRenderer() {}

    // Ring constants
    private static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/ring.png");
    private static final int RING_FACE_COUNT = 12;
    private static final float RING_RADIUS = 0.12f * 0.8f;  // Plus petit que saddle
    private static final float RING_HALF_DEPTH = 0.015f;
    private static final float RING_ROTATION_SPEED = 0.14f;

    /**
     * Verifie si c'est un controle droit (necessite flip horizontal).
     */
    public static boolean isRightControl(HoverbikePart partType) {
        return partType == HoverbikePart.CONTROL_RIGHT;
    }

    /**
     * Applique le flip horizontal pour le controle droit.
     */
    public static void applyFlipIfNeeded(PoseStack poseStack, HoverbikePart partType) {
        if (isRightControl(partType)) {
            poseStack.scale(-1.0f, 1.0f, 1.0f);
        }
    }

    /**
     * Rend les effets du controle selon la variante.
     */
    public static void renderEffects(PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, float ageInTicks, int variantIndex) {
        // Variante C: ring
        if (variantIndex == 2) {
            renderRing(poseStack, bufferSource, packedLight, ageInTicks);
        }
    }

    // ========== Variante C: Ring ==========

    private static void renderRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, float ageInTicks) {
        Vec3 center = ControlPartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * RING_ROTATION_SPEED;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(RING_TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.XP.rotation(rotation));

        float angleStep = (float) (2.0 * Math.PI / RING_FACE_COUNT);

        for (int i = 0; i < RING_FACE_COUNT; i++) {
            float angle0 = i * angleStep;
            float angle1 = (i + 1) * angleStep;
            float angleMid = (angle0 + angle1) * 0.5f;

            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            float y0 = cos0 * RING_RADIUS;
            float z0 = sin0 * RING_RADIUS;
            float y1 = cos1 * RING_RADIUS;
            float z1 = sin1 * RING_RADIUS;

            float ny = (float) Math.cos(angleMid);
            float nz = (float) Math.sin(angleMid);

            PoseStack.Pose pose = poseStack.last();
            vc.addVertex(pose, -RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(0f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 0f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
            vc.addVertex(pose, -RING_HALF_DEPTH, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                    .setUv(1f, 1f).setOverlay(overlay).setLight(packedLight).setNormal(pose, 0, ny, nz);
        }

        poseStack.popPose();
    }
}
