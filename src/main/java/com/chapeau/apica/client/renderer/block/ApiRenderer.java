/**
 * ============================================================
 * [ApiRenderer.java]
 * Description: Renderer pour le bloc Api avec modele articule et animations
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApiBlockEntity      | Donnees de scale     | getVisualScale(), animState    |
 * | ApiModel            | Modele articule      | Rendu avec parties animees     |
 * | ApiAnimator         | Orchestration        | State machine animations       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.api.ApiAnimationState;
import com.chapeau.apica.client.animation.api.ApiAnimator;
import com.chapeau.apica.client.model.ApiModel;
import com.chapeau.apica.common.block.api.ApiBlock;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Render le modele articule Api avec animations.
 * Utilise ApiModel (Java) au lieu du BakedModel JSON pour permettre l'animation.
 * Chaque BlockEntity a son propre modele+animateur car les animations modifient
 * les rotations des ModelParts directement.
 */
public class ApiRenderer implements BlockEntityRenderer<ApiBlockEntity> {

    private final BlockEntityRendererProvider.Context context;

    // Cache des animateurs par BlockEntity (WeakHashMap pour cleanup automatique)
    private final Map<BlockPos, AnimatorState> animatorCache = new WeakHashMap<>();

    public ApiRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(ApiBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float scale = blockEntity.getVisualScale(partialTick);
        float currentTime = (blockEntity.getLevel() != null ?
                blockEntity.getLevel().getGameTime() : 0) + partialTick;

        // Obtenir ou creer l'animateur pour ce BE
        AnimatorState animState = getOrCreateAnimator(blockEntity);

        // Synchroniser l'etat d'animation du BE vers l'animateur
        ApiAnimationState beState = blockEntity.getAnimationState();
        if (animState.lastSyncedState != beState) {
            animState.animator.setState(beState, currentTime);
            animState.lastSyncedState = beState;
        }

        // Update et apply l'animation
        float deltaTime = currentTime - animState.lastTime;
        animState.animator.update(currentTime, deltaTime);
        animState.animator.apply(currentTime);
        animState.lastTime = currentTime;

        poseStack.pushPose();

        // Rotation selon FACING du blockstate
        Direction facing = blockEntity.getBlockState().getValue(ApiBlock.FACING);
        float yRot = -facing.toYRot();

        // Le modele est defini avec (0,0,0) = centre du bloc au sol
        // Coordonnees en pixels (16 pixels = 1 bloc)
        //
        // Transforms:
        // 1. Translate to block center at ground level (0.5, 0, 0.5)
        // 2. Apply Y rotation for facing
        // 3. Apply visual scale around this pivot
        // 4. Convert pixels to blocks (1/16)
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.scale(scale / 16f, scale / 16f, scale / 16f);

        // Rendu du modele (utiliser le modele de l'animateur, pas un modele partage)
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(ApiModel.TEXTURE));
        animState.animator.getModel().renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }

    /**
     * Obtient ou cree un animateur pour le BlockEntity.
     * Chaque BE a son propre modele car les animations modifient les ModelParts.
     */
    private AnimatorState getOrCreateAnimator(ApiBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        AnimatorState state = animatorCache.get(pos);
        if (state == null) {
            // Creer un nouveau modele pour ce BE
            ApiModel beModel = new ApiModel(context.bakeLayer(ApiModel.LAYER_LOCATION));
            state = new AnimatorState(new ApiAnimator(beModel));
            animatorCache.put(pos, state);
        }
        return state;
    }

    @Override
    public boolean shouldRenderOffScreen(ApiBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ApiBlockEntity blockEntity) {
        float scale = blockEntity.getCompletedScale();
        var pos = blockEntity.getBlockPos();
        double halfExtent = scale * 0.625;
        return new AABB(
            pos.getX() + 0.5 - halfExtent, pos.getY(), pos.getZ() + 0.5 - halfExtent,
            pos.getX() + 0.5 + halfExtent, pos.getY() + scale * 0.625, pos.getZ() + 0.5 + halfExtent
        );
    }

    /**
     * Etat de l'animateur pour un BlockEntity.
     */
    private static class AnimatorState {
        final ApiAnimator animator;
        ApiAnimationState lastSyncedState = ApiAnimationState.IDLE;
        float lastTime = 0;

        AnimatorState(ApiAnimator animator) {
            this.animator = animator;
        }
    }
}
