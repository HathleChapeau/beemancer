/**
 * ============================================================
 * [AltarHeartRenderer.java]
 * Description: Renderer pour le Honey Altar forme — coeur anime, conduits, 3 parties de structure
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AltarHeartBlockEntity   | Donnees a rendre     | isFormed(), isCrafting()       |
 * | AltarCraftAnimator      | Animation craft      | computePosition/Rotation       |
 * | BlockEntityRenderer     | Interface renderer   | Rendu custom                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer + modeles additionnels)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.block;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.animation.AltarCraftAnimator;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand forme: rend 3 parties de structure + coeur rotatif + 4 conduits.
 * Pendant le craft: anime les conduits (deplacement, rotation acceleree).
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    // Modeles des parties de structure
    public static final ModelResourceLocation PEDESTAL_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_pedestal"));
    public static final ModelResourceLocation CORE_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_core"));
    public static final ModelResourceLocation TOP_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_top"));
    public static final ModelResourceLocation CONDUIT_MODEL_LOC =
        ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_formed_conduit"));

    public AltarHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(AltarHeartBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        if (!blockEntity.isFormed() || blockEntity.getLevel() == null) return;

        BlockState heartState = BeemancerBlocks.ALTAR_HEART.get().defaultBlockState();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        float currentTime = blockEntity.getLevel().getGameTime() + partialTick;

        renderStructureParts(blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        renderHeart(blockEntity, heartState, currentTime, poseStack, vertexConsumer, packedLight, packedOverlay);
        renderConduits(blockEntity, heartState, currentTime, poseStack, vertexConsumer, packedLight, packedOverlay);
    }

    /**
     * Rend les parties statiques de structure: pedestal (Y-2) et top (Y+1).
     */
    private void renderStructureParts(AltarHeartBlockEntity blockEntity, BlockState heartState,
                                       PoseStack poseStack, VertexConsumer vertexConsumer,
                                       int packedLight, int packedOverlay) {
        // Pedestal: Y-2
        poseStack.pushPose();
        poseStack.translate(0, -2, 0);
        renderModel(PEDESTAL_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();

        // Top: Y+1
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        renderModel(TOP_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Rend le coeur (core) avec rotation permanente sur 3 axes.
     */
    private void renderHeart(AltarHeartBlockEntity blockEntity, BlockState heartState, float currentTime,
                              PoseStack poseStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(currentTime * 1.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(currentTime * 0.7f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentTime * 0.3f));
        poseStack.translate(-0.5, -0.5, -0.5);
        renderModel(CORE_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Rend les 4 conduits — statiques ou animes pendant le craft.
     */
    private void renderConduits(AltarHeartBlockEntity blockEntity, BlockState heartState, float currentTime,
                                 PoseStack poseStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay) {

        boolean isCrafting = blockEntity.isCrafting();
        float craftTick = isCrafting
            ? (currentTime - blockEntity.getCraftStartGameTime())
            : -1f;
        boolean animating = isCrafting && craftTick >= 0 && craftTick < AltarCraftAnimator.TOTAL_TICKS;

        for (int i = 0; i < AltarCraftAnimator.getConduitCount(); i++) {
            Vec3 pos;
            float rotation;

            if (animating) {
                pos = AltarCraftAnimator.computePosition(i, craftTick);
                rotation = AltarCraftAnimator.computeRotation(craftTick);
            } else {
                pos = AltarCraftAnimator.getStaticPosition(i);
                rotation = 0f;
            }

            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, pos.z);
            if (rotation != 0f) {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
                poseStack.translate(-0.5, -0.5, -0.5);
            }
            renderModel(CONDUIT_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    /**
     * Rend un BakedModel a la position courante du PoseStack.
     */
    private void renderModel(ModelResourceLocation modelLoc, AltarHeartBlockEntity blockEntity,
                              BlockState heartState, PoseStack poseStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLoc);
        blockRenderer.getModelRenderer().tesselateBlock(
            blockEntity.getLevel(), model, heartState, blockEntity.getBlockPos(),
            poseStack, vertexConsumer, false, random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.solid());
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
