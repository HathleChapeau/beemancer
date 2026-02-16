/**
 * ============================================================
 * [RotatingModelHelper.java]
 * Description: Helper pour rendre des modeles avec rotation autour du centre
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance            | Raison                | Utilisation               |
 * |-----------------------|----------------------|---------------------------|
 * | RenderHelper          | Tessellation model   | tesselateModel()          |
 * | Axis                  | Rotations XYZ        | mulPose()                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CentrifugeHeartRenderer, InfuserHeartRenderer
 * - CrystallizerRenderer, StorageControllerRenderer
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rend un BakedModel avec rotation autour du centre du bloc (0.5, 0.5, 0.5).
 * Deux modes: rotation Y seule (centrifuge) et rotation XYZ (crystallizer, storage controller).
 */
public final class RotatingModelHelper {

    private RotatingModelHelper() {}

    /**
     * Rend un modele avec rotation Y autour du centre du bloc.
     * Utilise par CentrifugeHeart et InfuserHeart.
     */
    public static void renderWithYRotation(BlockRenderDispatcher blockRenderer, BakedModel model,
                                            Level level, BlockState state, BlockPos pos,
                                            PoseStack poseStack, VertexConsumer consumer,
                                            RandomSource random, int light, int overlay,
                                            RenderType renderType, float yRotation) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        RenderHelper.tesselateModel(blockRenderer, model, level, state, pos,
            poseStack, consumer, random, light, overlay, renderType);
        poseStack.popPose();
    }

    /**
     * Rend un modele avec rotation XYZ et scale autour du centre du bloc.
     * Utilise par Crystallizer et StorageController.
     *
     * @param xRot  rotation axe X en degres
     * @param yRot  rotation axe Y en degres
     * @param zRot  rotation axe Z en degres
     * @param scale echelle uniforme (1.0 = normal)
     */
    public static void renderWithXYZRotation(BlockRenderDispatcher blockRenderer, BakedModel model,
                                              Level level, BlockState state, BlockPos pos,
                                              PoseStack poseStack, VertexConsumer consumer,
                                              RandomSource random, int light, int overlay,
                                              RenderType renderType,
                                              float xRot, float yRot, float zRot, float scale) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        if (scale != 1.0f) poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        poseStack.translate(-0.5, -0.5, -0.5);
        RenderHelper.tesselateModel(blockRenderer, model, level, state, pos,
            poseStack, consumer, random, light, overlay, renderType);
        poseStack.popPose();
    }
}
