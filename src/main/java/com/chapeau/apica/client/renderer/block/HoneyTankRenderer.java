/**
 * ============================================================
 * [HoneyTankRenderer.java]
 * Description: Renderer dynamique du fluide dans le Honey Tank
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation               |
 * |------------------------------|----------------------|---------------------------|
 * | HoneyTankBlockEntity         | Donnees fluide       | getFluid(), getFluidAmount |
 * | RenderHelper                 | Texture atlas        | getFluidSprite()          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.renderer.util.RenderHelper;
import com.chapeau.apica.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renderer pour le fluide a l'interieur du Honey Tank.
 * Dessine un simple quad plat (surface du fluide) a la hauteur du remplissage.
 */
public class HoneyTankRenderer implements BlockEntityRenderer<HoneyTankBlockEntity> {

    public HoneyTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HoneyTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        FluidStack fluidStack = blockEntity.getFluid();
        if (fluidStack.isEmpty()) return;

        float fillRatio = (float) blockEntity.getFluidAmount() / blockEntity.getCapacity();
        if (fillRatio <= 0f) return;

        TextureAtlasSprite sprite = RenderHelper.getFluidSprite(fluidStack.getFluid());
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        float minX = 2f / 16f;
        float maxX = 14f / 16f;
        float minZ = 2f / 16f;
        float maxZ = 14f / 16f;
        float y = 2f / 16f + (12f * fillRatio / 16f);

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int light = LightTexture.FULL_BRIGHT;

        // Quad surface du fluide (face Y+)
        consumer.addVertex(pose, minX, y, minZ).setColor(1f, 1f, 1f, 0.9f)
            .setUv(u0, v0).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, minX, y, maxZ).setColor(1f, 1f, 1f, 0.9f)
            .setUv(u0, v1).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, y, maxZ).setColor(1f, 1f, 1f, 0.9f)
            .setUv(u1, v1).setLight(light).setNormal(pose, 0, 1, 0);
        consumer.addVertex(pose, maxX, y, minZ).setColor(1f, 1f, 1f, 0.9f)
            .setUv(u1, v0).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
