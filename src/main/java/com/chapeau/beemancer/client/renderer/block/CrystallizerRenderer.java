/**
 * ============================================================
 * [CrystallizerRenderer.java]
 * Description: Renderer pour les cores animés du Crystallizer
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                   | Raison                | Utilisation               |
 * |-----------------------------|----------------------|---------------------------|
 * | CrystallizerBlockEntity     | Données progress     | getProgress(), hasOutput  |
 * | CrystallizerBlock           | ACTIVE property      | État actif                |
 * | StorageControllerRenderer   | Pattern rotation     | Même style d'animation    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.CrystallizerBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.CrystallizerBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Crystallizer.
 *
 * Rend 2 cores au centre du bloc:
 * - Processing: rotation XYZ (style StorageController) + scale de x1 a x2 selon progress
 * - Crystal produit: cores a x2, immobiles
 * - Idle: cores a x1, immobiles
 */
public class CrystallizerRenderer implements BlockEntityRenderer<CrystallizerBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/alchemy/crystallizer_core"));

    public CrystallizerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(CrystallizerBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        BakedModel coreModel = Minecraft.getInstance().getModelManager()
            .getModel(CORE_MODEL_LOC);

        BlockState state = blockEntity.getBlockState();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        boolean active = state.hasProperty(CrystallizerBlock.ACTIVE)
            && state.getValue(CrystallizerBlock.ACTIVE);
        boolean hasCrystal = blockEntity.hasOutputCrystal();

        float scale;
        boolean shouldRotate;

        if (active) {
            // Processing: scale de 1 a 2, rotation active
            int progress = blockEntity.getProgress();
            int processTime = blockEntity.getCurrentProcessTime();
            float ratio = processTime > 0 ? (float) progress / processTime : 0f;
            scale = 1.0f + ratio;
            shouldRotate = true;
        } else if (hasCrystal) {
            // Crystal produit: scale x2, pas de rotation
            scale = 2.0f;
            shouldRotate = false;
        } else {
            // Idle: scale x1, pas de rotation
            scale = 1.0f;
            shouldRotate = false;
        }

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        float time = gameTime + partialTick;

        // Core 1
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(scale, scale, scale);
        if (shouldRotate) {
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.5f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.1f));
        }
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );
        poseStack.popPose();

        // Core 2: phase decalee
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(scale, scale, scale);
        if (shouldRotate) {
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.0f + 90));
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.5f + 60));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.1f + 45));
        }
        poseStack.translate(-0.5, -0.5, -0.5);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );
        poseStack.popPose();
    }
}
