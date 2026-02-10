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
 * | ParticleHelper          | Beam particules      | beam() client-side             |
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
import com.chapeau.beemancer.core.util.ParticleHelper;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Renderer pour le Honey Altar multibloc.
 * Quand forme: rend 3 parties de structure + coeur rotatif + 4 conduits.
 * Pendant le craft: anime les conduits via le systeme d'animation + beam de particules.
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

    // Rotation Y par conduit pour orienter le modele vers l'exterieur
    // Modele de base pointe vers le nord (-Z)
    // N: 0°, S: 180°, E: 270°, W: 90°
    private static final float[] CONDUIT_Y_ROTATIONS = { 0f, 180f, 270f, 90f };

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
     * Quand le beam est actif, spawne des particules END_ROD du centre du conduit vers le coeur.
     */
    private void renderConduits(AnimationController ctrl, AltarHeartBlockEntity blockEntity,
                                 BlockState heartState, PoseStack poseStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay) {
        BlockPos blockPos = blockEntity.getBlockPos();
        Level level = blockEntity.getLevel();
        long gameTime = level != null ? level.getGameTime() : 0;
        boolean spawnBeam = AltarCraftAnimator.trySpawnBeamTick(blockPos, gameTime);

        for (int i = 0; i < AltarCraftAnimator.getConduitCount(); i++) {
            Vec3 staticPos = AltarCraftAnimator.getStaticPosition(i);

            poseStack.pushPose();
            ctrl.applyAnimation("orbit_" + i, poseStack);
            poseStack.translate(staticPos.x, staticPos.y, staticPos.z);
            ctrl.applyAnimation("pos_" + i, poseStack);
            ctrl.applyAnimation("rot_" + i, poseStack);

            // Rotation Y pour orienter le modele selon le facing du conduit
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(CONDUIT_Y_ROTATIONS[i]));
            poseStack.translate(-0.5, -0.5, -0.5);

            renderModel(CONDUIT_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);

            // Beam: particules du centre du conduit vers le coeur (1x par tick)
            if (spawnBeam && level != null) {
                Matrix4f mat = poseStack.last().pose();
                Vector4f center = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
                mat.transform(center);
                Vec3 conduitWorld = new Vec3(
                    blockPos.getX() + center.x(),
                    blockPos.getY() + center.y(),
                    blockPos.getZ() + center.z()
                );
                Vec3 heartWorld = Vec3.atCenterOf(blockPos);
                ParticleHelper.beam(level, ParticleTypes.END_ROD, conduitWorld, heartWorld, 4);
            }

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
