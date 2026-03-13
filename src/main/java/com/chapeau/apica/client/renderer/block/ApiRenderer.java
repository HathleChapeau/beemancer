/**
 * ============================================================
 * [ApiRenderer.java]
 * Description: Renderer pour Api avec parties articulees et animations
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApiBlockEntity      | Donnees de scale     | getVisualScale(), animState    |
 * | ApiAnimationState   | Enum animations      | IDLE, JUMP, HITSTOP, SLEEP     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
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
 * Chaque partie (body, bras, jambes) est rendue independamment avec ses propres rotations.
 */
public class ApiRenderer implements BlockEntityRenderer<ApiBlockEntity> {

    // Modeles des parties
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

    // Pivot de rotation (en pixels, centre du bloc = 8,8,8)
    private static final float PIVOT_X = 8f;
    private static final float PIVOT_Y = 2.75f;
    private static final float PIVOT_Z = 8.5f;

    // Position des membres (en pixels, relatif au pivot)
    private static final float ARM_LEFT_X = -8.5f;
    private static final float ARM_RIGHT_X = 8.5f;
    private static final float ARM_Y = 2.75f;
    private static final float ARM_Z = -5f;

    private static final float LEG_LEFT_X = -3.5f;
    private static final float LEG_RIGHT_X = 3.5f;
    private static final float LEG_Y = 2.75f;
    private static final float LEG_Z = 5f;

    // Constantes animation
    private static final float BASE_PITCH = 45f; // Inclinaison de base (degres)
    private static final float IDLE_CYCLE = 60f; // Ticks pour un cycle idle
    private static final float JUMP_DURATION = 80f; // 2 sauts de 40 ticks
    private static final float HITSTOP_DURATION = 120f;

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

        // Calcul du temps d'animation
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        float time = gameTime + partialTick;
        float animTime = time - be.getAnimStartTick();

        // Calcul des rotations selon l'etat d'animation
        AnimationFrame frame = calculateAnimation(be.getAnimState(), animTime);

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        // === BODY ===
        poseStack.pushPose();
        setupBaseTransform(poseStack, scale, yRot);
        poseStack.mulPose(Axis.XP.rotationDegrees(BASE_PITCH + frame.bodyPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(frame.bodyRoll));
        poseStack.translate(0, frame.bodyY / 16f, 0);
        renderModel(bodyModel, be, poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        // === ARM LEFT ===
        poseStack.pushPose();
        setupBaseTransform(poseStack, scale, yRot);
        poseStack.translate(ARM_LEFT_X / 16f, ARM_Y / 16f, ARM_Z / 16f);
        poseStack.mulPose(Axis.XP.rotationDegrees(BASE_PITCH + frame.armLeftPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(frame.armLeftRoll));
        renderModel(armLeftModel, be, poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        // === ARM RIGHT ===
        poseStack.pushPose();
        setupBaseTransform(poseStack, scale, yRot);
        poseStack.translate(ARM_RIGHT_X / 16f, ARM_Y / 16f, ARM_Z / 16f);
        poseStack.mulPose(Axis.XP.rotationDegrees(BASE_PITCH + frame.armRightPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(frame.armRightRoll));
        renderModel(armRightModel, be, poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        // === LEG LEFT ===
        poseStack.pushPose();
        setupBaseTransform(poseStack, scale, yRot);
        poseStack.translate(LEG_LEFT_X / 16f, LEG_Y / 16f, LEG_Z / 16f);
        poseStack.mulPose(Axis.XP.rotationDegrees(BASE_PITCH + frame.legLeftPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(frame.legLeftRoll));
        renderModel(legLeftModel, be, poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        // === LEG RIGHT ===
        poseStack.pushPose();
        setupBaseTransform(poseStack, scale, yRot);
        poseStack.translate(LEG_RIGHT_X / 16f, LEG_Y / 16f, LEG_Z / 16f);
        poseStack.mulPose(Axis.XP.rotationDegrees(BASE_PITCH + frame.legRightPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(frame.legRightRoll));
        renderModel(legRightModel, be, poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void setupBaseTransform(PoseStack poseStack, float scale, float yRot) {
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, PIVOT_Y / 16f, (PIVOT_Z - 8f) / 16f);
    }

    private void renderModel(BakedModel model, ApiBlockEntity be, PoseStack poseStack,
                             VertexConsumer consumer, int packedLight, int packedOverlay) {
        blockRenderer.getModelRenderer().tesselateBlock(
            be.getLevel(), model, be.getBlockState(),
            be.getBlockPos(), poseStack, consumer, false,
            random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout()
        );
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

        // 2 sauts de 40 ticks chacun
        float singleJump = 40f;
        int jumpNum = (int) (time / singleJump);
        float t = (time % singleJump) / singleJump;
        float fatigue = jumpNum >= 1 ? 0.75f : 1f;

        if (t < 0.25f) {
            // Anticipation
            float p = t / 0.25f;
            f.bodyPitch = 8f * p * fatigue;
            f.armLeftRoll = 0;
            f.armRightRoll = 0;
        } else if (t < 0.5f) {
            // Saut
            float p = (t - 0.25f) / 0.25f;
            f.bodyPitch = -5f * fatigue;
            f.bodyY = Mth.sin(p * Mth.PI) * 4f * fatigue;
            f.armLeftRoll = -20f * p * fatigue;
            f.armRightRoll = 20f * p * fatigue;
        } else if (t < 0.75f) {
            // Descente
            float p = (t - 0.5f) / 0.25f;
            f.bodyPitch = Mth.lerp(p, -5f, 10f) * fatigue;
            f.bodyY = (1f - p) * 4f * fatigue;
            f.armLeftRoll = Mth.lerp(p, -20f, 0f) * fatigue;
            f.armRightRoll = Mth.lerp(p, 20f, 0f) * fatigue;
        } else {
            // Atterrissage + tremblement
            float p = (t - 0.75f) / 0.25f;
            float shake = Mth.sin(p * 30f) * (1f - p) * 3f;
            f.bodyPitch = Mth.lerp(p, 10f, 0f) * fatigue + shake;
            f.armLeftRoll = shake;
            f.armRightRoll = -shake;
        }

        return f;
    }

    private AnimationFrame calculateHitstop(float time) {
        AnimationFrame f = new AnimationFrame();

        if (time < 60f) {
            // Phase effort avec tremblement
            float p = Math.min(1f, time / 10f);
            float shake = Mth.sin(time * 2.5f) * 2f;
            f.bodyPitch = 15f * p + shake;
            f.bodyRoll = Mth.cos(time * 3.1f) * 1.5f;
            f.armLeftPitch = 10f * p;
            f.armLeftRoll = -10f * p + shake;
            f.armRightPitch = 10f * p;
            f.armRightRoll = 10f * p - shake;
            f.legLeftRoll = -5f * p;
            f.legRightRoll = 5f * p;
        } else if (time < 80f) {
            // Abandon
            float p = (time - 60f) / 20f;
            f.bodyPitch = Mth.lerp(p, 15f, -5f);
            f.bodyRoll = Mth.lerp(p, 0f, 2f);
            f.armLeftRoll = Mth.lerp(p, -10f, 15f);
            f.armRightRoll = Mth.lerp(p, 10f, -15f);
        } else {
            // Epuisement
            float breathe = Mth.sin((time - 80f) * 0.15f) * 1f;
            f.bodyPitch = -5f + breathe;
            f.bodyRoll = 2f;
            f.armLeftRoll = 15f + breathe * 0.5f;
            f.armRightRoll = -15f - breathe * 0.3f;
        }

        return f;
    }

    private AnimationFrame calculateSleep(float time) {
        AnimationFrame f = new AnimationFrame();
        float p = Math.min(1f, time / 20f); // Transition 20 ticks

        f.bodyPitch = 5f * p;
        f.bodyRoll = 1f * p;
        f.armLeftRoll = 20f * p;
        f.armRightRoll = -20f * p;
        f.legLeftPitch = 3f * p;
        f.legRightPitch = 3f * p;

        return f;
    }

    // ==================== HELPERS ====================

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

    /** Frame d'animation avec toutes les rotations. */
    private static class AnimationFrame {
        float bodyPitch = 0, bodyRoll = 0, bodyY = 0;
        float armLeftPitch = 0, armLeftRoll = 0;
        float armRightPitch = 0, armRightRoll = 0;
        float legLeftPitch = 0, legLeftRoll = 0;
        float legRightPitch = 0, legRightRoll = 0;
    }
}
