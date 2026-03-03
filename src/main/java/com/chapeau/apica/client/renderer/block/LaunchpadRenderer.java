/**
 * ============================================================
 * [LaunchpadRenderer.java]
 * Description: Renderer pour la plaque du Launchpad avec rotation dynamique
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | LaunchpadBlockEntity          | BlockEntity cible    | Rendu de la plaque             |
 * | LaunchpadBlock                | FACING, ANGLE props  | Orientation et inclinaison     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.artifacts.LaunchpadBlock;
import com.chapeau.apica.common.blockentity.artifacts.LaunchpadBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LaunchpadRenderer implements BlockEntityRenderer<LaunchpadBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation PLATE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/artifacts/launchpad_plate"));

    public LaunchpadRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(LaunchpadBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LaunchpadBlock.FACING);
        int angleIndex = state.getValue(LaunchpadBlock.ANGLE);
        float tiltDegrees = angleIndex * 10f;

        BakedModel plateModel = Minecraft.getInstance().getModelManager()
            .getModel(PLATE_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();

        // Facing rotation (rotate entire plate to match block orientation)
        float facingYRot = switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot));
        poseStack.translate(-0.5, 0.0, -0.5);

        // Tilt rotation around front-bottom edge of plate (pivot at front, plate rises at back)
        if (tiltDegrees > 0) {
            float pivotY = 4f / 16f;
            float pivotZ = 1f / 16f;
            poseStack.translate(0.5, pivotY, pivotZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(-tiltDegrees));
            poseStack.translate(-0.5, -pivotY, -pivotZ);
        }

        blockRenderer.getModelRenderer().tesselateBlock(
            level, plateModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );

        poseStack.popPose();
    }
}
