/**
 * ============================================================
 * [SaddlePartRenderer.java]
 * Description: Rendu de la selle HoverBee et ses effets (lightning, connector)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SaddlePartModelB    | Modele selle B       | Connector + electrodes         |
 * | LightningArcRenderer| Arcs electriques     | Lightning entre electrodes     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu des selles
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.SaddlePartModelB;
import com.chapeau.apica.client.renderer.LightningArcRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer specifique pour les selles du HoverBee.
 * Gere les effets speciaux par variante:
 * - Variante B: connector avec texture rose + arcs lightning entre electrodes
 */
public final class SaddlePartRenderer {

    private SaddlePartRenderer() {}

    // Lightning constants
    private static final int ARC_REFRESH_TICKS = 4;
    private static final int ARC_NODES = 2;
    private static final float ARC_AMPLITUDE = 0.064f;
    private static final float ARC_HALF_WIDTH = 0.010f;

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

        float r = 0.98f, g = 0.75f, b = 0.16f;
        for (LightningArcRenderer.LightningArc arc : state.arcs) {
            if (arc != null) {
                LightningArcRenderer.renderArc(poseStack, bufferSource, arc,
                        ARC_HALF_WIDTH, r, g, b, 0.9f);
            }
        }
    }
}
