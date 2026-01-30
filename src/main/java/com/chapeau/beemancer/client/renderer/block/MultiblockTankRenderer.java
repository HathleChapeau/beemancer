/**
 * ============================================================
 * [MultiblockTankRenderer.java]
 * Description: Renderer pour le fluide du multiblock tank (rendu par bloc)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation               |
 * |-------------------------------|----------------------|---------------------------|
 * | MultiblockTankBlockEntity     | Données fluide       | getFluidTank(), fillRatio |
 * | MultiblockTankBlock           | Connection props     | Face culling              |
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Renderer pour le fluide a l'interieur du multiblock tank.
 * Chaque bloc rend sa propre portion de fluide selon sa position Y
 * et le niveau de remplissage global du multibloc.
 */
public class MultiblockTankRenderer implements BlockEntityRenderer<MultiblockTankBlockEntity> {

    private static final float INSET = 2f / 16f;

    public MultiblockTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MultiblockTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        MultiblockTankBlockEntity master = blockEntity.getMaster();
        if (master == null) return;

        FluidTank tank = master.getFluidTank();
        if (tank == null || tank.isEmpty()) return;

        float fillRatio = blockEntity.getFluidFillRatioForBlock(blockEntity.getBlockPos());
        if (fillRatio <= 0f) return;

        FluidStack fluidStack = tank.getFluid();
        Fluid fluid = fluidStack.getFluid();
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(stillTexture);

        // Read connection properties from blockstate
        BlockState state = blockEntity.getBlockState();
        boolean connNorth = state.getValue(MultiblockTankBlock.NORTH);
        boolean connSouth = state.getValue(MultiblockTankBlock.SOUTH);
        boolean connEast = state.getValue(MultiblockTankBlock.EAST);
        boolean connWest = state.getValue(MultiblockTankBlock.WEST);
        boolean connUp = state.getValue(MultiblockTankBlock.UP);
        boolean connDown = state.getValue(MultiblockTankBlock.DOWN);

        // Fluid bounds: extend to block edge if connected, inset 2px if exposed
        float minX = connWest ? 0f : INSET;
        float maxX = connEast ? 1f : (1f - INSET);
        float minZ = connNorth ? 0f : INSET;
        float maxZ = connSouth ? 1f : (1f - INSET);
        float minY = connDown ? 0f : INSET;
        float maxYFull = connUp ? 1f : (1f - INSET);

        // Scale Y by fill ratio
        float fluidHeight = maxYFull - minY;
        float maxY = minY + (fluidHeight * fillRatio);

        // Face culling: skip faces that connect to adjacent tanks (avoid double-alpha)
        boolean renderUp = !connUp || fillRatio < 1f;
        boolean renderDown = !connDown;
        boolean renderNorth = !connNorth;
        boolean renderSouth = !connSouth;
        boolean renderWest = !connWest;
        boolean renderEast = !connEast;

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var pose = poseStack.last();

        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite,
            minX, minY, minZ, maxX, maxY, maxZ,
            renderUp, renderDown, renderNorth, renderSouth, renderWest, renderEast);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
