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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

    // Pivot principal (centre de rotation du body) - en blocs
    private static final float PIVOT_X = 8f / 16f;
    private static final float PIVOT_Y = 2.75f / 16f;
    private static final float PIVOT_Z = 8.5f / 16f;

    // Pivots des membres (jonctions avec le body) - en blocs
    private static final float ARM_LEFT_PIVOT_X = 1f / 16f;
    private static final float ARM_LEFT_PIVOT_Y = 5.5f / 16f;
    private static final float ARM_LEFT_PIVOT_Z = 3.5f / 16f;

    private static final float ARM_RIGHT_PIVOT_X = 15f / 16f;
    private static final float ARM_RIGHT_PIVOT_Y = 5.5f / 16f;
    private static final float ARM_RIGHT_PIVOT_Z = 3.5f / 16f;

    private static final float LEG_LEFT_PIVOT_X = 4.5f / 16f;
    private static final float LEG_LEFT_PIVOT_Y = 5.5f / 16f;
    private static final float LEG_LEFT_PIVOT_Z = 12f / 16f;

    private static final float LEG_RIGHT_PIVOT_X = 11.5f / 16f;
    private static final float LEG_RIGHT_PIVOT_Y = 5.5f / 16f;
    private static final float LEG_RIGHT_PIVOT_Z = 12f / 16f;

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

        float bodyPitch = BASE_PITCH + frame.bodyPitch;
        float bodyRoll = frame.bodyRoll;

        // === BODY ===
        renderBody(bodyModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY);

        // === FACE ===
        renderFace(be, poseStack, buffer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY);

        // === LIMBS - rotation autour de leur propre pivot (jonction avec body) ===
        renderLimb(armLeftModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY,
                   ARM_LEFT_PIVOT_X, ARM_LEFT_PIVOT_Y, ARM_LEFT_PIVOT_Z,
                   frame.armLeftPitch, frame.armLeftRoll);

        renderLimb(armRightModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY,
                   ARM_RIGHT_PIVOT_X, ARM_RIGHT_PIVOT_Y, ARM_RIGHT_PIVOT_Z,
                   frame.armRightPitch, frame.armRightRoll);

        renderLimb(legLeftModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY,
                   LEG_LEFT_PIVOT_X, LEG_LEFT_PIVOT_Y, LEG_LEFT_PIVOT_Z,
                   frame.legLeftPitch, frame.legLeftRoll);

        renderLimb(legRightModel, be, poseStack, consumer, packedLight, packedOverlay,
                   scale, yRot, bodyPitch, bodyRoll, frame.bodyY,
                   LEG_RIGHT_PIVOT_X, LEG_RIGHT_PIVOT_Y, LEG_RIGHT_PIVOT_Z,
                   frame.legRightPitch, frame.legRightRoll);
    }

    private void renderBody(BakedModel model, ApiBlockEntity be, PoseStack poseStack,
                            VertexConsumer consumer, int packedLight, int packedOverlay,
                            float scale, float yRot, float pitch, float roll, float yOffset) {
        poseStack.pushPose();

        // 1. Centre du bloc pour facing
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);

        // 2. Pivot principal pour rotation body
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0, yOffset / 16f, 0);
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

    private void renderLimb(BakedModel model, ApiBlockEntity be, PoseStack poseStack,
                            VertexConsumer consumer, int packedLight, int packedOverlay,
                            float scale, float yRot, float bodyPitch, float bodyRoll, float bodyY,
                            float limbPivotX, float limbPivotY, float limbPivotZ,
                            float limbPitch, float limbRoll) {
        poseStack.pushPose();

        // 1. Centre du bloc pour facing
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);

        // 2. BODY rotation au pivot principal (le membre suit le corps)
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(bodyPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(bodyRoll));
        poseStack.translate(0, bodyY / 16f, 0);
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        // 3. LIMB rotation additionnelle au pivot du membre (articulation)
        if (limbPitch != 0 || limbRoll != 0) {
            poseStack.translate(limbPivotX, limbPivotY, limbPivotZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(limbPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(limbRoll));
            poseStack.translate(-limbPivotX, -limbPivotY, -limbPivotZ);
        }

        // Render
        blockRenderer.getModelRenderer().tesselateBlock(
            be.getLevel(), model, be.getBlockState(),
            be.getBlockPos(), poseStack, consumer, false,
            random, packedLight, packedOverlay,
            ModelData.EMPTY, RenderType.cutout()
        );

        poseStack.popPose();
    }

    // ==================== FACE RENDERING ====================

    private void renderFace(ApiBlockEntity be, PoseStack poseStack, MultiBufferSource buffer,
                            int packedLight, int packedOverlay,
                            float scale, float yRot, float pitch, float roll, float yOffset) {
        String faceName = be.getCurrentFace();
        ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "block/api/api_face_" + faceName);

        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(textureLoc);

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        poseStack.pushPose();

        // Memes transforms que le body
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, 0, -0.5);

        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0, yOffset / 16f, 0);
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        // Render face quad (position du JSON: from [0, 6.51, 2] to [16, 6.51, 18])
        PoseStack.Pose pose = poseStack.last();

        float minX = 0f / 16f;
        float maxX = 16f / 16f;
        float y = 6.52f / 16f; // Legèrement au-dessus pour éviter z-fighting
        float minZ = -2f / 16f;
        float maxZ = 14f / 16f;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Normales pre-calculees a 45° (BASE_PITCH)
        // cos(45°) = sin(45°) = 0.7071f
        float ny = 0.7071f;
        float nz = -0.7071f;

        // Quad face-up (normale inclinee a 45°)
        consumer.addVertex(pose, minX, y, minZ).setColor(255, 255, 255, 255)
            .setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, ny, nz);
        consumer.addVertex(pose, minX, y, maxZ).setColor(255, 255, 255, 255)
            .setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, ny, nz);
        consumer.addVertex(pose, maxX, y, maxZ).setColor(255, 255, 255, 255)
            .setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, ny, nz);
        consumer.addVertex(pose, maxX, y, minZ).setColor(255, 255, 255, 255)
            .setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, ny, nz);

        // Quad face-down (verso, normale inversee)
        consumer.addVertex(pose, maxX, y, minZ).setColor(255, 255, 255, 255)
            .setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, -ny, -nz);
        consumer.addVertex(pose, maxX, y, maxZ).setColor(255, 255, 255, 255)
            .setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, -ny, -nz);
        consumer.addVertex(pose, minX, y, maxZ).setColor(255, 255, 255, 255)
            .setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, -ny, -nz);
        consumer.addVertex(pose, minX, y, minZ).setColor(255, 255, 255, 255)
            .setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0, -ny, -nz);

        poseStack.popPose();
    }

    // ==================== ANIMATIONS ====================

    private AnimationFrame calculateAnimation(ApiAnimationState state, float time) {
        return switch (state) {
            case IDLE -> calculateIdle(time);
            case HITSTOP -> calculateHitstop(time);
            case HAPPY -> calculateHappy(time);
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

    private AnimationFrame calculateHappy(float time) {
        AnimationFrame f = new AnimationFrame();

        if (time >= 40f) {
            return calculateIdle(time);
        }

        // Phase effort seulement (copie de HITSTOP sans epuisement, 1.5x plus rapide)
        float p = Math.min(1f, time / 6.67f);
        float shake = Mth.sin(time * 0.75f) * 2f;
        f.bodyPitch = 15f * p + shake;
        f.bodyRoll = Mth.cos(time * 0.9f) * 1.5f;
        f.armLeftPitch = 18f * p;
        f.armLeftRoll = -18f * p + shake * 1.5f;
        f.armRightPitch = 18f * p;
        f.armRightRoll = 18f * p - shake * 1.5f;
        f.legLeftRoll = -10f * p;
        f.legRightRoll = 10f * p;

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
