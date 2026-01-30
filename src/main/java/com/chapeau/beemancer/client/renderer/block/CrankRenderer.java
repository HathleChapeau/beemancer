/**
 * ============================================================
 * [CrankRenderer.java]
 * Description: Renderer pour la rotation du Crank au-dessus de la centrifugeuse
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | CrankBlockEntity              | BlockEntity cible    | Rendu du crank                 |
 * | ManualCentrifugeBlock         | SPINNING property    | Detection etat actif           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.ManualCentrifugeBlock;
import com.chapeau.beemancer.common.blockentity.alchemy.CrankBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class CrankRenderer implements BlockEntityRenderer<CrankBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public static final ModelResourceLocation CRANK_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "block/alchemy/crank"));

    public CrankRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(CrankBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        Level level = blockEntity.getLevel();
        if (level == null) return;

        // Check if centrifuge below is spinning
        BlockState belowState = level.getBlockState(blockEntity.getBlockPos().below());
        boolean spinning = belowState.hasProperty(ManualCentrifugeBlock.SPINNING)
            && belowState.getValue(ManualCentrifugeBlock.SPINNING);

        BakedModel crankModel = Minecraft.getInstance().getModelManager()
            .getModel(CRANK_MODEL_LOC);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        poseStack.pushPose();

        if (spinning) {
            float time = level.getGameTime() + partialTick;
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 8.0f));
            poseStack.translate(-0.5, 0.0, -0.5);
        }

        blockRenderer.getModelRenderer().tesselateBlock(
            level, crankModel, blockEntity.getBlockState(), blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random,
            packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
        );

        poseStack.popPose();
    }
}
