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
import com.chapeau.beemancer.client.animation.AnimationTimer;
import com.chapeau.beemancer.common.block.alchemy.CrystallizerBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.CrystallizerBlockEntity;
import com.chapeau.beemancer.client.renderer.util.RotatingModelHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());

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

        float time = AnimationTimer.getRenderTime(partialTick);

        float xRot1 = shouldRotate ? time * 3.0f : 0;
        float yRot1 = shouldRotate ? time * 4.5f : 0;
        float zRot1 = shouldRotate ? time * 2.1f : 0;

        // Core 1
        RotatingModelHelper.renderWithXYZRotation(blockRenderer, coreModel,
            blockEntity.getLevel(), state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, random, packedLight, packedOverlay,
            RenderType.translucent(), xRot1, yRot1, zRot1, scale);

        // Core 2: phase decalee
        RotatingModelHelper.renderWithXYZRotation(blockRenderer, coreModel,
            blockEntity.getLevel(), state, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, random, packedLight, packedOverlay,
            RenderType.translucent(),
            xRot1 + 90, yRot1 + 60, zRot1 + 45, scale);
    }
}
