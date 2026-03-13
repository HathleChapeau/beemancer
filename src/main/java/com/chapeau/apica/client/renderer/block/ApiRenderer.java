/**
 * ============================================================
 * [ApiRenderer.java]
 * Description: Renderer pour Api avec parties articulees et animations
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.api.ApiAnimationState;
import com.chapeau.apica.common.block.api.ApiBlock;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Render Api avec parties separees pour animation.
 * Toutes les parties tournent autour du meme pivot (8, 2.75, 8.5) en pixels.
 */
public class ApiRenderer implements BlockEntityRenderer<ApiBlockEntity> {

    public static final ModelResourceLocation BODY_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api_body"));
    public static final ModelResourceLocation ARM_LEFT_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api_arm_left"));
    public static final ModelResourceLocation ARM_RIGHT_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api_arm_right"));
    public static final ModelResourceLocation LEG_LEFT_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api_leg_left"));
    public static final ModelResourceLocation LEG_RIGHT_LOC = ModelResourceLocation.standalone(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "block/machines/api_leg_right"));

    // Pivot de rotation partage (en pixels) - meme que le JSON original
    private static final float PIVOT_X = 8f / 16f;
    private static final float PIVOT_Y = 2.75f / 16f;
    private static final float PIVOT_Z = 8.5f / 16f;

    private static final float BASE_PITCH = 45f;
    private static final float IDLE_CYCLE = 60f;

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public ApiRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(ApiBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        var minecraft = Minecraft.getInstance();
        var modelManager = minecraft.getModelManager();

        BakedModel bodyModel = modelManager.getModel(BODY_LOC);
        BakedModel armLeftModel = modelManager.getModel(ARM_LEFT_LOC);
        BakedModel armRightModel = modelManager.getModel(ARM_RIGHT_LOC);
        BakedModel legLeftModel = modelManager.getModel(LEG_LEFT_LOC);
        BakedModel legRightModel = modelManager.getModel(LEG_RIGHT_LOC);

        if (bodyModel == modelManager.getMissingModel()) return;

        float scale = be.getVisualScale(partialTick);
        Direction facing = be.getBlockState().getValue(ApiBlock.FACING);
        float yRot = -facing.toYRot();

        // Temps d'animation avec interpolation smooth
        float gameTime = be.getLevel() != null ? be.getLevel().getGameTime() + partialTick : partialTick;
        float animTime = gameTime - be.getAnimStartTick();

        // Calcul des rotations
        AnimationFrame frame = calculateAnimation(be.getAnimState(), animTime);

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        // === BODY ===
        renderPart(bodyModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, BASE_PITCH + frame.bodyPitch, frame.bodyRoll, frame.bodyY);

        // === LIMBS - meme rotation que body pour l'instant (simplifie) ===
        // Les membres utilisent le meme pivot, donc ils bougent avec le corps
        // Animation supplementaire = rotation additionnelle autour de leur propre centre

        renderPart(armLeftModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, BASE_PITCH + frame.bodyPitch + frame.armLeftPitch,
                   frame.bodyRoll + frame.armLeftRoll, frame.bodyY);

        renderPart(armRightModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, BASE_PITCH + frame.bodyPitch + frame.armRightPitch,
                   frame.bodyRoll + frame.armRightRoll, frame.bodyY);

        renderPart(legLeftModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, BASE_PITCH + frame.bodyPitch + frame.legLeftPitch,
                   frame.bodyRoll + frame.legLeftRoll, frame.bodyY);

        renderPart(legRightModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, BASE_PITCH + frame.bodyPitch + frame.legRightPitch,
                   frame.bodyRoll + frame.legRightRoll, frame.bodyY);
    }

    private void renderPart(BakedModel model, ApiBlockEntity be, PoseStack poseStack,
                            VertexConsumer consumer, int packedLight, int packedOverlay,
                            float scale, float yRot, float pitch, float roll, float yOffset) {
        poseStack.pushPose();

        // 1. Translate au centre du bloc pour la rotation de facing
        poseStack.translate(0.5, 0, 0.5);

        // 2. Rotation facing
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

        // 3. Scale autour du centre
        poseStack.scale(scale, scale, scale);

        // 4. Retour a l'origine
        poseStack.translate(-0.5, 0, -0.5);

        // 5. Translate au pivot pour la rotation d'inclinaison
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);

        // 6. Rotations d'animation
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        // 7. Offset Y animation (saut)
        poseStack.translate(0, yOffset / 16f, 0);

        // 8. Retour du pivot
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        // Render
        blockRenderer.getModelRenderer().tesselateBlock(
            be.getLevel(), model, be.getBlockState(),
            be.getBlockPos(), poseStack, consumer, false,
            random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout()
        );

        poseStack.popPose();
    }

    // ==================== ANIMATIONS ====================

    private AnimationFrame calculateAnimation(ApiAnimationState state, float time) {
        return switch (state) {
            case IDLE -> calculateIdle(time);
            case JUMP -> calculateJump(time);
            case HITSTOP -> calculateHitstop(time);
            case SLEEP -> calculateSleep(time);
        };
    }

    private AnimationFrame calculateIdle(float time) {
        float cycle = (time % IDLE_CYCLE) / IDLE_CYCLE;
        float wave = Mth.sin(cycle * Mth.TWO_PI);
        float waveOffset = Mth.sin((cycle + 0.15f) * Mth.TWO_PI);

        AnimationFrame f = new AnimationFrame();
        f.bodyPitch = wave * 3f;
        f.armLeftRoll = waveOffset * 5f;
        f.armRightRoll = -waveOffset * 5f;
        f.legLeftPitch = wave * 2f;
        f.legRightPitch = -wave * 1.5f;
        return f;
    }

    private AnimationFrame calculateJump(float time) {
        AnimationFrame f = new AnimationFrame();

        float singleJump = 40f;
        int jumpNum = (int) (time / singleJump);
        float t = (time % singleJump) / singleJump;
        float fatigue = jumpNum >= 1 ? 0.75f : 1f;

        if (time >= 80f) {
            // Animation terminee, retour idle
            return calculateIdle(time);
        }

        if (t < 0.25f) {
            float p = easeInOut(t / 0.25f);
            f.bodyPitch = 8f * p * fatigue;
        } else if (t < 0.5f) {
            float p = easeOut((t - 0.25f) / 0.25f);
            f.bodyPitch = -5f * fatigue;
            f.bodyY = Mth.sin(p * Mth.PI) * 4f * fatigue;
            f.armLeftRoll = -20f * p * fatigue;
            f.armRightRoll = 20f * p * fatigue;
        } else if (t < 0.75f) {
            float p = easeIn((t - 0.5f) / 0.25f);
            f.bodyPitch = Mth.lerp(p, -5f, 10f) * fatigue;
            f.bodyY = (1f - p) * 4f * fatigue;
            f.armLeftRoll = Mth.lerp(p, -20f, 0f) * fatigue;
            f.armRightRoll = Mth.lerp(p, 20f, 0f) * fatigue;
        } else {
            float p = easeOut((t - 0.75f) / 0.25f);
            float shake = Mth.sin(time * 1.5f) * (1f - p) * 3f;
            f.bodyPitch = Mth.lerp(p, 10f, 0f) * fatigue + shake;
            f.armLeftRoll = shake;
            f.armRightRoll = -shake;
        }

        return f;
    }

    private AnimationFrame calculateHitstop(float time) {
        AnimationFrame f = new AnimationFrame();

        if (time >= 120f) {
            return calculateIdle(time);
        }

        if (time < 60f) {
            float p = Math.min(1f, time / 10f);
            float shake = Mth.sin(time * 0.5f) * 2f;
            f.bodyPitch = 15f * p + shake;
            f.bodyRoll = Mth.cos(time * 0.6f) * 1.5f;
            f.armLeftPitch = 10f * p;
            f.armLeftRoll = -10f * p + shake;
            f.armRightPitch = 10f * p;
            f.armRightRoll = 10f * p - shake;
            f.legLeftRoll = -5f * p;
            f.legRightRoll = 5f * p;
        } else if (time < 80f) {
            float p = easeInOut((time - 60f) / 20f);
            f.bodyPitch = Mth.lerp(p, 15f, -5f);
            f.bodyRoll = Mth.lerp(p, 0f, 2f);
            f.armLeftRoll = Mth.lerp(p, -10f, 15f);
            f.armRightRoll = Mth.lerp(p, 10f, -15f);
        } else {
            float breathe = Mth.sin((time - 80f) * 0.1f) * 1f;
            f.bodyPitch = -5f + breathe;
            f.bodyRoll = 2f;
            f.armLeftRoll = 15f + breathe * 0.5f;
            f.armRightRoll = -15f - breathe * 0.3f;
        }

        return f;
    }

    private AnimationFrame calculateSleep(float time) {
        AnimationFrame f = new AnimationFrame();
        float p = easeInOut(Math.min(1f, time / 20f));

        f.bodyPitch = 5f * p;
        f.bodyRoll = 1f * p;
        f.armLeftRoll = 20f * p;
        f.armRightRoll = -20f * p;
        f.legLeftPitch = 3f * p;
        f.legRightPitch = 3f * p;

        return f;
    }

    // Easing functions
    private float easeIn(float t) {
        return t * t;
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private float easeInOut(float t) {
        if (t < 0.5f) {
            return 2f * t * t;
        } else {
            float x = -2f * t + 2f;
            return 1f - (x * x) / 2f;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ApiBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ApiBlockEntity be) {
        float scale = be.getCompletedScale();
        var pos = be.getBlockPos();
        double halfExtent = scale * 0.625;
        return new AABB(
            pos.getX() + 0.5 - halfExtent, pos.getY(), pos.getZ() + 0.5 - halfExtent,
            pos.getX() + 0.5 + halfExtent, pos.getY() + scale * 0.625, pos.getZ() + 0.5 + halfExtent
        );
    }

    private static class AnimationFrame {
        float bodyPitch = 0, bodyRoll = 0, bodyY = 0;
        float armLeftPitch = 0, armLeftRoll = 0;
        float armRightPitch = 0, armRightRoll = 0;
        float legLeftPitch = 0, legLeftRoll = 0;
        float legRightPitch = 0, legRightRoll = 0;
    }
}
