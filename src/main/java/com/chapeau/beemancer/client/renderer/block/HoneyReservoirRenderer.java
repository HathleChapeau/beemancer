/**
 * ============================================================
 * [HoneyReservoirRenderer.java]
 * Description: Renderer pour le fluide du Honey Reservoir avec texture atlas animée
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation           |
 * |----------------------------|----------------------|-----------------------|
 * | HoneyReservoirBlockEntity  | Données fluide       | getFluidAmount()      |
 * | HoneyReservoirBlock        | État formé           | FORMED                |
 * | BlockEntityRenderer        | Interface renderer   | Rendu fluide          |
 * | IClientFluidTypeExtensions | Texture atlas        | getStillTexture()     |
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renderer pour le fluide à l'intérieur du Honey Reservoir.
 * Utilise les textures atlas animées des fluides pour un rendu dynamique.
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

        // Obtenir les infos du fluide
        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);

        // Obtenir la texture still du fluide depuis l'atlas
        ResourceLocation stillTexture = fluidExtensions.getStillTexture(fluidStack);
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        // Obtenir la couleur de tint du fluide
        int color = fluidExtensions.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.9f;

        poseStack.pushPose();

        // Rendre le fluide avec texture animée
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        if (formed) {
            renderFormedFluid(poseStack, consumer, sprite, packedLight, fillRatio, r, g, b, a);
        } else {
            renderUnformedFluid(poseStack, consumer, sprite, packedLight, fillRatio, r, g, b, a);
        }

        poseStack.popPose();
    }

    /**
     * Rend le fluide pour le réservoir non-formé.
     */
    private void renderUnformedFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                      int packedLight, float fillRatio, float r, float g, float b, float a) {
        var pose = poseStack.last();

        // Hauteur max du fluide: 6 pixels (de Y=5 à Y=11)
        float fluidHeight = 6.0f * fillRatio;

        // Coordonnées du fluide à l'intérieur du corps verre
        float minX = 4f / 16f;
        float maxX = 12f / 16f;
        float minZ = 5f / 16f;
        float maxZ = 11f / 16f;
        float minY = 5f / 16f;
        float maxY = minY + (fluidHeight / 16f);

        renderFluidCubeWithSprite(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, packedLight);
    }

    /**
     * Rend le fluide pour le réservoir formé.
     */
    private void renderFormedFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    int packedLight, float fillRatio, float r, float g, float b, float a) {
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

        renderFluidCubeWithSprite(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, packedLight);
    }

    /**
     * Rend un cube de fluide avec texture atlas sprite animée.
     */
    private void renderFluidCubeWithSprite(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                            float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ,
                                            float r, float g, float b, float a, int packedLight) {

        // UV du sprite (animé via l'atlas)
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Calculer les UV proportionnels à la taille du cube
        float uWidth = u1 - u0;
        float vHeight = v1 - v0;

        // Face dessus (Y+) - plus brillante
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-) - plus sombre
        float darkFactor = 0.6f;
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * darkFactor, g * darkFactor, b * darkFactor, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, -1, 0);

        // Faces latérales (UV adaptés à la hauteur du fluide)
        float sideFactor = 0.8f;
        float heightRatio = (maxY - minY) / (maxX - minX); // Ratio pour ajuster les UV verticaux
        float vSide1 = v1 - (vHeight * heightRatio);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u0, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u1, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u0, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u1, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * sideFactor, g * sideFactor, b * sideFactor, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        float sideFactorX = 0.7f;
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u0, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u1, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u0, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u1, vSide1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r * sideFactorX, g * sideFactorX, b * sideFactorX, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 1, 0, 0);
    }
}
