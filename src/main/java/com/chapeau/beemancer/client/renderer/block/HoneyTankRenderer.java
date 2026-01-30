/**
 * ============================================================
 * [HoneyTankRenderer.java]
 * Description: Renderer dynamique du fluide dans le Honey Tank
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation               |
 * |------------------------------|----------------------|---------------------------|
 * | HoneyTankBlockEntity         | Données fluide       | getFluid(), getFluidAmount |
 * | IClientFluidTypeExtensions   | Texture atlas        | getStillTexture()         |
 * | HoneyReservoirRenderer       | Pattern rendu fluide | Même style sprite cube    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renderer pour le fluide à l'intérieur du Honey Tank.
 * Affiche un cube de fluide translucent dont la hauteur est proportionnelle au remplissage.
 * Utilise la texture atlas animée du fluide pour un rendu dynamique.
 */
public class HoneyTankRenderer implements BlockEntityRenderer<HoneyTankBlockEntity> {

    public HoneyTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HoneyTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        FluidStack fluidStack = blockEntity.getFluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        float fillRatio = (float) blockEntity.getFluidAmount() / blockEntity.getCapacity();
        if (fillRatio <= 0f) return;

        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        // Cube de fluide de [2,2,2] a [14, 2 + fillRatio*12, 14] (en pixels / 16)
        float minX = 2f / 16f;
        float maxX = 14f / 16f;
        float minZ = 2f / 16f;
        float maxZ = 14f / 16f;
        float minY = 2f / 16f;
        float maxY = minY + (12f * fillRatio / 16f);

        renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Rend un cube de fluide avec texture atlas sprite animée.
     * Fullbright, couleur blanche (pas de tint).
     */
    private void renderFluidCube(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                  float minX, float minY, float minZ,
                                  float maxX, float maxY, float maxZ) {

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.9f;

        int light = LightTexture.FULL_BRIGHT;

        // Face dessus (Y+)
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);

        // Face dessous (Y-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);

        // Face nord (Z-)
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);

        // Face sud (Z+)
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);

        // Face ouest (X-)
        consumer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);
        consumer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -1, 0, 0);

        // Face est (X+)
        consumer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a)
            .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a)
            .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
        consumer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a)
            .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 1, 0, 0);
    }
}
