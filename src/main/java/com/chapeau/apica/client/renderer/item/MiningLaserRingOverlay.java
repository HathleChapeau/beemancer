/**
 * ============================================================
 * [MiningLaserRingOverlay.java]
 * Description: Rendu de la géométrie ring dans l'espace item du Mining Laser
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PoseStack           | Transforms 3D        | Rotation de l'anneau           |
 * | VertexConsumer      | Emission vertices    | Quads formant l'anneau         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MiningLaserItemRenderer.java (rendu des rings sur l'item)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Responsabilité unique : dessine un polygone circulaire (N quads en anneau)
 * autour d'un point donné dans l'espace item du BEWLR.
 * Chaque face de l'anneau est un quad texturé avec la texture ring.png.
 */
@OnlyIn(Dist.CLIENT)
public final class MiningLaserRingOverlay {

    private static final int FACE_COUNT = 12;
    private static final float RING_INNER_RADIUS = 0.1f;
    private static final float RING_OUTER_RADIUS = 0.125f;
    private static final float FACE_HALF_WIDTH = 0.025f;

    /**
     * Rend un anneau géométrique autour d'un point dans l'espace item.
     *
     * @param poseStack  Pile de transformations
     * @param buffer     Source de buffers
     * @param light      Packed light
     * @param centerX    Centre X de l'anneau (espace item, unités bloc)
     * @param centerY    Centre Y de l'anneau (espace item, unités bloc)
     * @param centerZ    Centre Z de l'anneau (espace item, unités bloc)
     * @param rotation   Angle de rotation en radians
     * @param texture    Texture à utiliser pour les faces
     */
    public static void renderRing(PoseStack poseStack, MultiBufferSource buffer, int light,
                                   float centerX, float centerY, float centerZ,
                                   float rotation, ResourceLocation texture) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(45));
        poseStack.mulPose(Axis.ZP.rotation(rotation));

        float angleStep = (float) (2.0 * Math.PI / FACE_COUNT);

        for (int i = 0; i < FACE_COUNT; i++) {
            float angle0 = i * angleStep;
            float angle1 = (i + 1) * angleStep;

            float cos0 = (float) Math.cos(angle0);
            float sin0 = (float) Math.sin(angle0);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);

            float ix0 = cos0 * RING_INNER_RADIUS;
            float iy0 = sin0 * RING_INNER_RADIUS;
            float ox0 = cos0 * RING_OUTER_RADIUS;
            float oy0 = sin0 * RING_OUTER_RADIUS;
            float ix1 = cos1 * RING_INNER_RADIUS;
            float iy1 = sin1 * RING_INNER_RADIUS;
            float ox1 = cos1 * RING_OUTER_RADIUS;
            float oy1 = sin1 * RING_OUTER_RADIUS;

            // Face avant (z = +FACE_HALF_WIDTH)
            emitQuad(vc, poseStack.last(), light, overlay,
                    ix0, iy0, FACE_HALF_WIDTH,
                    ox0, oy0, FACE_HALF_WIDTH,
                    ox1, oy1, FACE_HALF_WIDTH,
                    ix1, iy1, FACE_HALF_WIDTH,
                    0, 0, 1);

            // Face arrière (z = -FACE_HALF_WIDTH)
            emitQuad(vc, poseStack.last(), light, overlay,
                    ix1, iy1, -FACE_HALF_WIDTH,
                    ox1, oy1, -FACE_HALF_WIDTH,
                    ox0, oy0, -FACE_HALF_WIDTH,
                    ix0, iy0, -FACE_HALF_WIDTH,
                    0, 0, -1);
        }

        poseStack.popPose();
    }

    private static void emitQuad(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float nx, float ny, float nz) {
        vc.addVertex(pose, x0, y0, z0).setColor(1f, 1f, 1f, 0.8f)
                .setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x1, y1, z1).setColor(1f, 1f, 1f, 0.8f)
                .setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x2, y2, z2).setColor(1f, 1f, 1f, 0.8f)
                .setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, x3, y3, z3).setColor(1f, 1f, 1f, 0.8f)
                .setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private MiningLaserRingOverlay() {
    }
}
