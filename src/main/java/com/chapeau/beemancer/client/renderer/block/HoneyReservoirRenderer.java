/**
 * ============================================================
 * [HoneyReservoirRenderer.java]
 * Description: Renderer pour le fluide du Honey Reservoir avec scale dynamique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation           |
 * |----------------------------|----------------------|-----------------------|
 * | HoneyReservoirBlockEntity  | Données fluide       | getFluidAmount()      |
 * | HoneyReservoirBlock        | État formé           | FORMED                |
 * | BlockEntityRenderer        | Interface renderer   | Rendu fluide          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renderer pour le fluide à l'intérieur du Honey Reservoir.
 * Le fluide est rendu avec un scale dynamique basé sur le niveau de remplissage.
 * Le bloc lui-même est rendu par le model JSON, seul le fluide est géré ici.
 */
public class HoneyReservoirRenderer implements BlockEntityRenderer<HoneyReservoirBlockEntity> {

    public HoneyReservoirRenderer(BlockEntityRendererProvider.Context context) {
        // Context fourni par NeoForge
    }

    @Override
    public void render(HoneyReservoirBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Ne rien rendre si le tank est vide
        FluidStack fluidStack = blockEntity.getFluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        boolean formed = state.getValue(HoneyReservoirBlock.FORMED);

        // Calculer le ratio de remplissage (0.0 à 1.0)
        float fillRatio = (float) blockEntity.getFluidAmount() / HoneyReservoirBlockEntity.CAPACITY;

        // Obtenir la couleur du fluide
        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        int color = fluidExtensions.getTintColor();

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.85f;

        poseStack.pushPose();

        // Rendre le fluide (le bloc est déjà tourné par le blockstate, donc on rend toujours en X)
        if (formed) {
            renderFormedFluid(poseStack, buffer, packedLight, fillRatio, r, g, b, a);
        } else {
            renderUnformedFluid(poseStack, buffer, packedLight, fillRatio, r, g, b, a);
        }

        poseStack.popPose();
    }

    /**
     * Rend le fluide pour le réservoir non-formé.
     * Corps verre: [3,4,4] to [13,12,12] - intérieur avec petit padding
     */
    private void renderUnformedFluid(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      float fillRatio, float r, float g, float b, float a) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        // Hauteur max du fluide: 6 pixels (de Y=5 à Y=11)
        float fluidHeight = 6.0f * fillRatio;

        // Coordonnées du fluide à l'intérieur du corps verre (axe X = longueur)
        float minX = 4f / 16f;
        float maxX = 12f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        renderFluidCube(consumer, pose, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, packedLight);
    }

    /**
     * Rend le fluide pour le réservoir formé.
     * Corps: [1,4,4] to [15,12,12] - légèrement plus grand
     */
    private void renderFormedFluid(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                    float fillRatio, float r, float g, float b, float a) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        // Hauteur max du fluide: 6 pixels
        float fluidHeight = 6.0f * fillRatio;

        // Coordonnées du fluide (plus large car formé)
        float minX = 3f / 16f;
        float maxX = 13f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        renderFluidCube(consumer, pose, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, packedLight);
    }

    /**
     * Rend un cube de fluide translucide.
     */
    private void renderFluidCube(VertexConsumer consumer, PoseStack.Pose pose,
                                  float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ,
                                  float r, float g, float b, float a, int packedLight) {

        // Face dessus (Y+) - plus brillante
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-) - plus sombre
        float darkFactor = 0.6f;
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);

        // Faces latérales
        float sideFactor = 0.8f;

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        float sideFactorX = 0.7f;
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
    }
}
