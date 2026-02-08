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
 * | AltarCraftAnimator      | Etat animation       | getController, updateCraftState |
 * | AnimationController     | Apply animations     | tick(), applyAnimation()       |
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
import com.chapeau.beemancer.client.animation.AnimationController;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand forme: rend 3 parties de structure + coeur rotatif + 4 conduits.
 * Pendant le craft: anime les conduits via le systeme d'animation.
 */
public class AltarHeartRenderer implements BlockEntityRenderer<AltarHeartBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

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

        if (!blockEntity.isFormed() || blockEntity.getLevel() == null) {
            AltarCraftAnimator.remove(blockEntity.getBlockPos());
            return;
        }

        float currentTime = blockEntity.getLevel().getGameTime() + partialTick;
        BlockState heartState = BeemancerBlocks.ALTAR_HEART.get().defaultBlockState();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());

        AltarCraftAnimator.updateCraftState(blockEntity.getBlockPos(), blockEntity.isCrafting());
        AnimationController ctrl = AltarCraftAnimator.getController(blockEntity.getBlockPos());
        ctrl.tick(currentTime);

        renderStructureParts(blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        renderHeart(ctrl, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        renderConduits(ctrl, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
    }

    /**
     * Rend les parties statiques de structure: pedestal (Y-2) et top (Y+1).
     */
    private void renderStructureParts(AltarHeartBlockEntity blockEntity, BlockState heartState,
                                       PoseStack poseStack, VertexConsumer vertexConsumer,
                                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0, -2, 0);
        renderModel(PEDESTAL_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        renderModel(TOP_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Rend le coeur (core) avec rotation permanente sur 3 axes via AnimationController.
     */
    private void renderHeart(AnimationController ctrl, AltarHeartBlockEntity blockEntity,
                              BlockState heartState, PoseStack poseStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay) {
        poseStack.pushPose();
        ctrl.applyAnimation("heart_y", poseStack);
        ctrl.applyAnimation("heart_x", poseStack);
        ctrl.applyAnimation("heart_z", poseStack);

        renderModel(CORE_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Rend les 4 conduits a leur position statique + animations craft.
     */
    private void renderConduits(AnimationController ctrl, AltarHeartBlockEntity blockEntity,
                                 BlockState heartState, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay) {
        for (int i = 0; i < AltarCraftAnimator.getConduitCount(); i++) {
            Vec3 staticPos = AltarCraftAnimator.getStaticPosition(i);

            poseStack.pushPose();
            poseStack.translate(staticPos.x, staticPos.y, staticPos.z);
            ctrl.applyAnimation("pos_" + i, poseStack);
            ctrl.applyAnimation("rot_" + i, poseStack);
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
            ModelData.EMPTY, RenderType.cutout());
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
