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
 * | FluidCubeRenderer            | Rendu cube fluide    | renderFluidCube()         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FluidCubeRenderer;
import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
