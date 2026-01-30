/**
 * ============================================================
 * [MultiblockTankRenderer.java]
 * Description: Renderer pour le fluide du multiblock tank
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation               |
 * |-------------------------------|----------------------|---------------------------|
 * | MultiblockTankBlockEntity     | Données fluide       | getFluidTank(), getBB     |
 * | MultiblockTankBlock           | FORMED property      | État formé                |
 * | IClientFluidTypeExtensions    | Texture atlas        | getStillTexture()         |
 * | FluidCubeRenderer             | Rendu cube fluide    | renderFluidCube()         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FluidCubeRenderer;
import com.chapeau.beemancer.common.block.alchemy.MultiblockTankBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Renderer pour le fluide a l'interieur du multiblock tank.
 * Seul le master rend le fluide, couvrant tout le bounding box du cuboid.
 */
public class MultiblockTankRenderer implements BlockEntityRenderer<MultiblockTankBlockEntity> {

    public MultiblockTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MultiblockTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // Seul le master rend le fluide
        if (!blockEntity.isMaster()) return;

        BlockState state = blockEntity.getBlockState();
        boolean formed = state.hasProperty(MultiblockTankBlock.FORMED)
            && state.getValue(MultiblockTankBlock.FORMED);
        if (!formed) return;

        FluidTank tank = blockEntity.getFluidTank();
        if (tank == null || tank.isEmpty()) return;

        FluidStack fluidStack = tank.getFluid();
        float fillRatio = (float) tank.getFluidAmount() / tank.getCapacity();
        if (fillRatio <= 0f) return;

        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        // Calculer le bounding box relatif au master
        int[] bb = blockEntity.getClientBoundingBox();
        BlockPos masterPos = blockEntity.getBlockPos();

        // Convertir en coordonnees relatives au master (le renderer est positionne au master)
        float relMinX = (bb[0] - masterPos.getX());
        float relMinY = (bb[1] - masterPos.getY());
        float relMinZ = (bb[2] - masterPos.getZ());
        float relMaxX = (bb[3] - masterPos.getX() + 1);
        float relMaxY = (bb[4] - masterPos.getY() + 1);
        float relMaxZ = (bb[5] - masterPos.getZ() + 1);

        // Inset de 1 pixel par rapport aux bords exterieurs
        float inset = 1f / 16f;
        float fluidMinX = relMinX + inset;
        float fluidMinY = relMinY + inset;
        float fluidMinZ = relMinZ + inset;
        float fluidMaxX = relMaxX - inset;
        float fluidMaxZ = relMaxZ - inset;

        // Hauteur proportionnelle au remplissage
        float totalHeight = relMaxY - relMinY - (2f * inset);
        float fluidMaxY = fluidMinY + (totalHeight * fillRatio);

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite, fluidMinX, fluidMinY, fluidMinZ, fluidMaxX, fluidMaxY, fluidMaxZ);
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockTankBlockEntity blockEntity) {
        // Le fluide peut depasser les limites du bloc master
        return blockEntity.isMaster() && blockEntity.getBlockCount() > 1;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
