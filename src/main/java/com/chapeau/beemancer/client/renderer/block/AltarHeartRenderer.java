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
import com.chapeau.beemancer.client.renderer.BeamRenderer;
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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
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
        renderConduits(ctrl, blockEntity, heartState, poseStack, buffer, vertexConsumer, partialTick, packedLight, packedOverlay);
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
     * Quand le beam est actif, rend un beam 3D style beacon du conduit vers le coeur.
     */
    private void renderConduits(AnimationController ctrl, AltarHeartBlockEntity blockEntity,
                                 BlockState heartState, PoseStack poseStack,
                                 MultiBufferSource buffer, VertexConsumer vertexConsumer,
                                 float partialTick, int packedLight, int packedOverlay) {
        BlockPos blockPos = blockEntity.getBlockPos();
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0;
        boolean beamActive = AltarCraftAnimator.isBeamActive(blockPos);
        boolean particleActive = AltarCraftAnimator.isCenterParticleActive(blockPos);
        int conduitCount = AltarCraftAnimator.getConduitCount();

        // Collecter les centres animes des conduits (pour beams et/ou particules)
        boolean needCenters = beamActive || particleActive;
        Vec3[] conduitCenters = needCenters ? new Vec3[conduitCount] : null;

        for (int i = 0; i < conduitCount; i++) {
            Vec3 staticPos = AltarCraftAnimator.getStaticPosition(i);

            poseStack.pushPose();
            ctrl.applyAnimation("orbit_" + i, poseStack);
            poseStack.translate(staticPos.x, staticPos.y, staticPos.z);
            ctrl.applyAnimation("pos_" + i, poseStack);
            ctrl.applyAnimation("rot_" + i, poseStack);

            // Self-spin Y + Rotation Y pour orienter le modele selon le facing
            ctrl.applyAnimation("spin_" + i, poseStack);
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(CONDUIT_Y_ROTATIONS[i]));
            poseStack.translate(-0.5, -0.5, -0.5);

            renderModel(CONDUIT_MODEL_LOC, blockEntity, heartState, poseStack, vertexConsumer, packedLight, packedOverlay);
            poseStack.popPose();

            if (needCenters) {
                conduitCenters[i] = computeConduitCenter(ctrl, i, staticPos);
            }
        }

        // Particules miel (boule de glow au centre de chaque conduit)
        if (conduitCenters != null
                && AltarCraftAnimator.trySpawnCenterParticleTick(blockPos, gameTime)
                && blockEntity.getLevel() != null) {
            DustParticleOptions honeyParticle = new DustParticleOptions(
                new Vector3f(1.0f, 0.75f, 0.1f), 1.2f);
            for (Vec3 center : conduitCenters) {
                Vec3 worldCenter = new Vec3(
                    blockPos.getX() + center.x,
                    blockPos.getY() + center.y,
                    blockPos.getZ() + center.z);
                for (int p = 0; p < 3; p++) {
                    Vec3 offset = new Vec3(
                        (random.nextDouble() - 0.5) * 0.25,
                        (random.nextDouble() - 0.5) * 0.25,
                        (random.nextDouble() - 0.5) * 0.25);
                    ParticleHelper.addParticle(blockEntity.getLevel(), honeyParticle,
                        worldCenter.add(offset), Vec3.ZERO);
                }
            }
        }

        // Beams convergents — 4 coins du conduit vers point de convergence, puis beam epais vers coeur
        if (conduitCenters != null && beamActive) {
            Vec3 heartCenter = new Vec3(0.5, 0.5, 0.5);
            for (Vec3 conduitCenter : conduitCenters) {
                renderConvergingBeams(poseStack, buffer, conduitCenter, heartCenter,
                    partialTick, gameTime);
            }
        }
    }

    /**
     * Calcule la position du centre du conduit en espace BE-relatif
     * en appliquant les animations d'orbite et de position (sans rotation).
     */
    private Vec3 computeConduitCenter(AnimationController ctrl, int index, Vec3 staticPos) {
        PoseStack calc = new PoseStack();
        ctrl.applyAnimation("orbit_" + index, calc);
        calc.translate(staticPos.x, staticPos.y, staticPos.z);
        ctrl.applyAnimation("pos_" + index, calc);
        Vector4f c = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        calc.last().pose().transform(c);
        return new Vec3(c.x(), c.y(), c.z());
    }

    /**
     * Rend 4 beams fins depuis les coins du conduit vers un point de convergence,
     * puis un beam epais du point de convergence vers le coeur.
     */
    private void renderConvergingBeams(PoseStack poseStack, MultiBufferSource buffer,
                                        Vec3 conduitCenter, Vec3 heartCenter,
                                        float partialTick, long gameTime) {
        Vec3 dir = heartCenter.subtract(conduitCenter);
        if (dir.lengthSqr() < 0.001) return;
        Vec3 dirNorm = dir.normalize();

        // Base perpendiculaire pour les 4 coins
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = dirNorm.cross(up);
        if (right.lengthSqr() < 0.001) {
            right = dirNorm.cross(new Vec3(1, 0, 0));
        }
        right = right.normalize();
        Vec3 perpUp = right.cross(dirNorm).normalize();

        // 4 coins autour du centre du conduit
        float cornerOffset = 0.25f;
        Vec3[] corners = {
            conduitCenter.add(right.scale(cornerOffset)).add(perpUp.scale(cornerOffset)),
            conduitCenter.add(right.scale(-cornerOffset)).add(perpUp.scale(cornerOffset)),
            conduitCenter.add(right.scale(cornerOffset)).add(perpUp.scale(-cornerOffset)),
            conduitCenter.add(right.scale(-cornerOffset)).add(perpUp.scale(-cornerOffset)),
        };

        // Point de convergence: 60% du chemin conduit -> coeur
        Vec3 convergence = conduitCenter.add(dir.scale(0.6));

        // 4 beams fins des coins vers le point de convergence
        for (Vec3 corner : corners) {
            BeamRenderer.renderBeam(poseStack, buffer, corner, convergence,
                partialTick, gameTime, 0.02f, 0.04f, 1.0f, 0.85f, 0.2f);
        }

        // 1 beam epais du point de convergence vers le coeur
        BeamRenderer.renderBeam(poseStack, buffer, convergence, heartCenter,
            partialTick, gameTime, 0.06f, 0.14f, 1.0f, 0.85f, 0.2f);
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
