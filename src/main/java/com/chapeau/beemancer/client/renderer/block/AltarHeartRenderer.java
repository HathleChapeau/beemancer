/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer pour le Honey Altar forme — 3 parties de structure
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed()            |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer + modeles additionnels)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
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
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand forme, rend 3 parties de structure (pedestal, core, top).
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    // Modeles des 3 parties de structure
    public static final ModelResourceLocation PEDESTAL_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_pedestal"));
    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_core"));
    public static final ModelResourceLocation TOP_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_top"));

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (!blockEntity.isFormed()) return;

        BlockState heartState = BeemancerBlocks.ALTAR_HEART.get().defaultBlockState();
        renderStructureParts(blockEntity, heartState, poseStack, buffer, packedLight, packedOverlay);
    }

    /**
     * Rend les 3 parties de structure du multibloc aux bons offsets Y.
     */
    private void renderStructureParts(AltarHeartBlockEntity blockEntity, BlockState heartState,
                                       PoseStack poseStack, MultiBufferSource buffer,
                                       int packedLight, int packedOverlay) {

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());

        // Pedestal: Y-2
        BakedModel pedestalModel = Minecraft.getInstance().getModelManager().getModel(PEDESTAL_MODEL_LOC);
        poseStack.pushPose();
        poseStack.translate(0, -2, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), pedestalModel, heartState, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.solid());
        poseStack.popPose();

        // Core: Y+0 (pas d'offset)
        BakedModel coreModel = Minecraft.getInstance().getModelManager().getModel(CORE_MODEL_LOC);
        poseStack.pushPose();
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), coreModel, heartState, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.solid());
        poseStack.popPose();

        // Top: Y+1
        BakedModel topModel = Minecraft.getInstance().getModelManager().getModel(TOP_MODEL_LOC);
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), topModel, heartState, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.solid());
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AltarHeartBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
