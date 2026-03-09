/**
 * ============================================================
 * [ApiRenderer.java]
 * Description: Renderer pour le bloc Api avec scale dynamique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApiBlockEntity      | Données de scale     | getVisualScale()               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.api.ApiBlock;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Render le modèle Api avec scale dynamique basé sur le level.
 * Le modèle est un cube 10x10x10 centré (from 3,0,3 to 13,10,13).
 * Le scale est appliqué autour du centre du bloc (X=0.5, Z=0.5, Y=0).
 */
public class ApiRenderer implements BlockEntityRenderer<ApiBlockEntity> {

    public static final ModelResourceLocation API_MODEL_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api"));

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public ApiRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(ApiBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float scale = blockEntity.getVisualScale(partialTick);

        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(API_MODEL_LOC);

        if (model == null || model == modelManager.getMissingModel()) return;

        poseStack.pushPose();

        // Rotation selon FACING du blockstate + scale dynamique
        Direction facing = blockEntity.getBlockState().getValue(ApiBlock.FACING);
        float yRot = 180f - facing.toYRot();

        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);

        // tesselateBlock pour avoir l'ambient occlusion (ombres)
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), model, blockEntity.getBlockState(),
            blockEntity.getBlockPos(), poseStack, consumer, false,
            random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout()
        );

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ApiBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ApiBlockEntity blockEntity) {
        float scale = blockEntity.getCompletedScale();
        var pos = blockEntity.getBlockPos();
        double halfExtent = scale * 0.625;
        return new AABB(
            pos.getX() + 0.5 - halfExtent, pos.getY(), pos.getZ() + 0.5 - halfExtent,
            pos.getX() + 0.5 + halfExtent, pos.getY() + scale * 0.625, pos.getZ() + 0.5 + halfExtent
        );
    }
}
