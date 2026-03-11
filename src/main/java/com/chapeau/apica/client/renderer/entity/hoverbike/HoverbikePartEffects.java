/**
 * ============================================================
 * [HoverbikePartEffects.java]
 * Description: Effets visuels des parties HoverBee (lightning, ring, connector)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SaddlePartModelB    | Positions electrodes | Lightning arcs saddle B        |
 * | SaddlePartModelC    | Position ring center | Ring effect saddle C           |
 * | ControlPartModelC   | Position ring center | Ring effect control C          |
 * | LightningArcRenderer| Arcs electriques     | Rendu lightning effects        |
 * | AnimationTimer      | Tick count           | Refresh des arcs               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu des effets speciaux
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.hoverbike.ControlPartModelC;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelB;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelC;
import com.chapeau.apica.client.renderer.LightningArcRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Rendu des effets visuels speciaux pour les parties du hoverbike:
 * - Lightning arcs entre electrodes (Saddle B)
 * - Connector avec texture placeholder (Saddle B)
 * - Ring rotatif (Saddle C, Control C)
 */
public final class HoverbikePartEffects {

    private HoverbikePartEffects() {}

    // Lightning arc constants
    private static final int ARC_REFRESH_TICKS = 4;
    private static final int ARC_NODES = 2;
    private static final float ARC_AMPLITUDE = 0.064f;
    private static final float ARC_HALF_WIDTH = 0.010f;

    // Ring constants
    private static final ResourceLocation RING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/particle/ring.png");
    private static final int RING_FACE_COUNT = 12;
    private static final float RING_RADIUS = 0.12f;
    private static final float RING_HALF_DEPTH = 0.015f;

    // ========== Lightning state per entity ==========

    /** Etat des arcs lightning pour une entite. */
    public static class LightningState {
        public LightningArcRenderer.LightningArc[] arcs = new LightningArcRenderer.LightningArc[2];
        public int lastArcTick = -1;
    }

    // ========== Saddle B: Connector ==========

    /**
     * Rend le connecteur entre les electrodes de la selle B avec texture placeholder rose.
     */
    public static void renderSaddleConnector(PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, SaddlePartModelB saddleB) {
        ModelPart connector = saddleB.getConnector();
        VertexConsumer vc = bufferSource.getBuffer(
                RenderType.entityCutout(SaddlePartModelB.CONNECTOR_TEXTURE));

        connector.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
    }

    // ========== Saddle B: Lightning ==========

    /**
     * Rend des arcs electriques entre les deux electrodes de la selle B.
     */
    public static void renderElectrodeLightning(PoseStack poseStack, MultiBufferSource bufferSource,
                                                 LightningState state) {
        Vec3 leftElectrode = SaddlePartModelB.LEFT_ELECTRODE.scale(1.0 / 16.0);
        Vec3 rightElectrode = SaddlePartModelB.RIGHT_ELECTRODE.scale(1.0 / 16.0);

        int currentTick = AnimationTimer.getTicks();
        if (state.lastArcTick < 0 || (currentTick - state.lastArcTick) >= ARC_REFRESH_TICKS) {
            RandomSource random = RandomSource.create(currentTick * 31L);
            for (int i = 0; i < 2; i++) {
                state.arcs[i] = LightningArcRenderer.generateArc(
                        leftElectrode, rightElectrode, ARC_NODES, ARC_AMPLITUDE,
                        ARC_REFRESH_TICKS, false, false, random);
            }
            state.lastArcTick = currentTick;
        }

        // Couleur cyan electrique
        float r = 0.4f, g = 0.9f, b = 1.0f;

        for (LightningArcRenderer.LightningArc arc : state.arcs) {
            if (arc != null) {
                LightningArcRenderer.renderArc(poseStack, bufferSource, arc,
                        ARC_HALF_WIDTH, r, g, b, 0.9f);
            }
        }
    }

    // ========== Saddle C: Ring ==========

    /**
     * Rend un anneau rotatif autour de l'axe X pour la selle C.
     */
    public static void renderSaddleRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, float ageInTicks) {
        Vec3 center = SaddlePartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * 0.15f;

        renderRing(poseStack, bufferSource, packedLight, center, rotation, RING_RADIUS);
    }

    // ========== Control C: Ring ==========

    /**
     * Rend un anneau rotatif autour de l'axe X pour le controle C.
     */
    public static void renderControlRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, float ageInTicks) {
        Vec3 center = ControlPartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * 0.14f;

        renderRing(poseStack, bufferSource, packedLight, center, rotation, RING_RADIUS * 0.8f);
    }

    // ========== Ring rendering (shared) ==========

    private static void renderRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, Vec3 center, float rotation, float radius) {
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

            float y0 = cos0 * radius;
            float z0 = sin0 * radius;
            float y1 = cos1 * radius;
            float z1 = sin1 * radius;

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
