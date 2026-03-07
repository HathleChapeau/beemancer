/**
 * ============================================================
 * [MultiblockTankRenderer.java]
 * Description: Renderer pour le multiblock tank avec modèle JSON scalé
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                     | Raison                | Utilisation               |
 * |-------------------------------|----------------------|---------------------------|
 * | MultiblockTankBlockEntity     | Données fluide/taille| getFluidTank(), getCubeSize |
 * | FluidCubeRenderer             | Rendu cube fluide    | renderFluidCube()         |
 * | RenderHelper                  | Texture atlas fluide | getFluidSprite()          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.renderer.util.FluidCubeRenderer;
import com.chapeau.apica.client.renderer.util.RenderHelper;
import com.chapeau.apica.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Renderer pour le multiblock tank.
 * Rendu uniquement sur le master (coin min X/Y/Z).
 * Charge le modèle JSON et le scale selon la taille du cube.
 */
public class MultiblockTankRenderer implements BlockEntityRenderer<MultiblockTankBlockEntity> {

    public static final ModelResourceLocation FORMED_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/alchemy/multiblock_tank_formed"));

    public static final ModelResourceLocation SINGLE_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/alchemy/multiblock_tank_single"));

    public MultiblockTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MultiblockTankBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (!blockEntity.isFormed() || !blockEntity.isMaster()) return;

        int cubeSize = blockEntity.getCubeSize();
        if (cubeSize < 2) {
            renderModel(poseStack, buffer, packedLight, SINGLE_MODEL_LOC, 1f);
            return;
        }

        float scale = cubeSize / 2.0f;
        renderModel(poseStack, buffer, packedLight, FORMED_MODEL_LOC, scale);
        renderFluid(blockEntity, cubeSize, poseStack, buffer);
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                             ModelResourceLocation modelLoc, float scale) {
        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(modelLoc);

        if (model == null || model == modelManager.getMissingModel()) return;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(), consumer, null, model,
            1f, 1f, 1f, packedLight, OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY, RenderType.translucent()
        );

        poseStack.popPose();
    }

    private void renderFluid(MultiblockTankBlockEntity blockEntity, int cubeSize,
                             PoseStack poseStack, MultiBufferSource buffer) {
        FluidTank tank = blockEntity.getFluidTank();
        if (tank == null || tank.isEmpty()) return;

        FluidStack fluidStack = tank.getFluid();
        float fillRatio = (float) tank.getFluidAmount() / tank.getCapacity() / (float) (Math.pow(cubeSize, 3));
        if (fillRatio <= 0f) return;

        TextureAtlasSprite sprite = RenderHelper.getFluidSprite(fluidStack.getFluid());
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        float inset = 0.125f * cubeSize;
        float minX = inset;
        float maxX = cubeSize - inset;
        float minZ = inset;
        float maxZ = cubeSize - inset;

        float yStart = 0.32f * cubeSize;
        float yEnd = 0.97f * cubeSize;
        float minY = yStart;
        float maxY = yStart + (yEnd - yStart) * fillRatio;

        FluidCubeRenderer.renderFluidCube(consumer, poseStack.last(), sprite,
            minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(MultiblockTankBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MultiblockTankBlockEntity blockEntity) {
        if (!blockEntity.isMaster() || !blockEntity.isFormed()) {
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
