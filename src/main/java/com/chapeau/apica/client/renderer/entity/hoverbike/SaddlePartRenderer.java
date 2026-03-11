/**
 * ============================================================
 * [SaddlePartRenderer.java]
 * Description: Rendu de la selle HoverBee et ses effets (lightning, ring, connector)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SaddlePartModelB    | Modele selle B       | Connector + electrodes         |
 * | SaddlePartModelC    | Modele selle C       | Ring center                    |
 * | LightningArcRenderer| Arcs electriques     | Lightning entre electrodes     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu des selles
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
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
 * Renderer specifique pour les selles du HoverBee.
 * Gere les effets speciaux par variante:
 * - Variante B: connector avec texture rose + arcs lightning entre electrodes
 * - Variante C: anneau rotatif
 */
public final class SaddlePartRenderer {

    private SaddlePartRenderer() {}

    // Lightning constants
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

    /** Etat lightning pour une entite. */
    public static class LightningState {
        public LightningArcRenderer.LightningArc[] arcs = new LightningArcRenderer.LightningArc[2];
        public int lastArcTick = -1;
    }

    /**
     * Prepare le rendu de la selle B en cachant le connector.
     * @return Le modele SaddlePartModelB si applicable, null sinon
     */
    public static SaddlePartModelB prepareRender(HoverbikePartModel part, int variantIndex) {
        if (variantIndex == 1 && part instanceof SaddlePartModelB saddleB) {
            saddleB.getConnector().visible = false;
            return saddleB;
        }
        return null;
    }

    /**
     * Rend les effets de la selle selon la variante.
     */
    public static void renderEffects(PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, float ageInTicks, int variantIndex,
                                      SaddlePartModelB saddleB, LightningState lightningState) {
        // Variante B: connector + lightning
        if (saddleB != null) {
            saddleB.getConnector().visible = true;
            renderConnector(poseStack, bufferSource, packedLight, saddleB);
            renderLightning(poseStack, bufferSource, lightningState);
        }

        // Variante C: ring
        if (variantIndex == 2) {
            renderRing(poseStack, bufferSource, packedLight, ageInTicks);
        }
    }

    // ========== Variante B: Connector ==========

    private static void renderConnector(PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, SaddlePartModelB saddleB) {
        ModelPart connector = saddleB.getConnector();
        VertexConsumer vc = bufferSource.getBuffer(
                RenderType.entityCutout(SaddlePartModelB.CONNECTOR_TEXTURE));
        connector.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
    }

    // ========== Variante B: Lightning ==========

    private static void renderLightning(PoseStack poseStack, MultiBufferSource bufferSource,
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

        float r = 0.4f, g = 0.9f, b = 1.0f;
        for (LightningArcRenderer.LightningArc arc : state.arcs) {
            if (arc != null) {
                LightningArcRenderer.renderArc(poseStack, bufferSource, arc,
                        ARC_HALF_WIDTH, r, g, b, 0.9f);
            }
        }
    }

    // ========== Variante C: Ring ==========

    private static void renderRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, float ageInTicks) {
        Vec3 center = SaddlePartModelC.RING_CENTER.scale(1.0 / 16.0);
        float rotation = ageInTicks * 0.15f;

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
