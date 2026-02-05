/**
 * ============================================================
 * [MultiblockTankRenderer.java]
 * Description: Renderer pour le multiblock tank avec scale automatique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation               |
 * |-------------------------------|----------------------|---------------------------|
 * | MultiblockTankBlockEntity     | Données fluide/taille| getFluidTank(), getCubeSize |
 * | FluidCubeRenderer             | Rendu cube fluide    | renderFluidCube()         |
 * | IClientFluidTypeExtensions    | Texture atlas        | getStillTexture()         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.client.renderer.util.FluidCubeRenderer;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.phys.AABB;

/**
 * Renderer pour le multiblock tank.
 * Rendu uniquement sur le master (coin min X/Y/Z).
 * Scale automatiquement selon la taille du cube.
 * Le fluide remplit un cube simple de 0-1 (scalé par cubeSize).
 */
public class MultiblockTankRenderer implements BlockEntityRenderer<MultiblockTankBlockEntity> {

    private static final float INSET = 2f / 16f;

    public MultiblockTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MultiblockTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (!blockEntity.isMaster()) return;
        if (!blockEntity.isValidCuboid()) return;

        int cubeSize = blockEntity.getCubeSize();
        if (cubeSize < 2) return;

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

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        poseStack.pushPose();

        // Scale par la taille du cube
        poseStack.scale(cubeSize, cubeSize, cubeSize);

        // Cube de fluide simple (coordonnées normalisées 0-1)
        float minX = INSET;
        float minZ = INSET;
        float maxX = 1f - INSET;
        float maxZ = 1f - INSET;
        float minY = INSET;
        float maxY = INSET + (1f - 2f * INSET) * fillRatio;

        var pose = poseStack.last();
        FluidCubeRenderer.renderFluidCube(consumer, pose, sprite,
            minX, minY, minZ, maxX, maxY, maxZ,
            true, true, true, true, true, true);

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockTankBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MultiblockTankBlockEntity blockEntity) {
        if (!blockEntity.isMaster() || !blockEntity.isValidCuboid()) {
            return AABB.unitCubeFromLowerCorner(blockEntity.getBlockPos().getCenter());
        }

        int cubeSize = blockEntity.getCubeSize();
        if (cubeSize < 2) {
            return AABB.unitCubeFromLowerCorner(blockEntity.getBlockPos().getCenter());
        }

        var pos = blockEntity.getBlockPos();
        return new AABB(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + cubeSize, pos.getY() + cubeSize, pos.getZ() + cubeSize
        );
    }
}
