/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du HoverBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartVariants| Registre variantes  | Selection du modele a rendre   |
 * | AnimationController | Animations edit mode | Ecartement/retour des pieces   |
 * | MoveAnimation       | Translation animee   | Mouvement des pieces           |
 * | ApicaBeeModel       | Modele parent        | Type generique + rotation ailes|
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.MoveAnimation;
import com.chapeau.apica.client.animation.TimingType;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer de rendu qui itere sur toutes les parties du hoverbike et rend
 * la variante selectionnee de chaque partie. En edit mode, chaque partie
 * s'ecarte du centre avec une animation smooth.
 * L'etat d'animation est stocke par entite pour eviter les interferences
 * quand plusieurs HoverBees sont rendues simultanement.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> {

    private static final float EDIT_ANIM_DURATION = 15f;
    private static final String ANIM_PREFIX = "edit_";

    /** Tous les modeles bakes, par partie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partVariants = new EnumMap<>(HoverbikePart.class);

    /** Etat d'animation par entite (entity ID → state). */
    private final Map<Integer, PerEntityState> entityStates = new HashMap<>();

    /** Etat d'animation specifique a une entite. */
    private static class PerEntityState {
        final AnimationController controller = new AnimationController();
        boolean wasEditMode = false;
        boolean editExpanded = false;
    }

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, ApicaBeeModel<HoverbikeEntity>> parent,
                              EntityRendererProvider.Context context) {
        super(parent);

        for (HoverbikePart part : HoverbikePart.values()) {
            List<HoverbikePartVariants.VariantEntry> variants = HoverbikePartVariants.getVariants(part);
            List<HoverbikePartModel> models = new ArrayList<>();
            for (HoverbikePartVariants.VariantEntry entry : variants) {
                models.add(entry.factory().constructor().apply(
                        context.bakeLayer(entry.factory().layerLocation())));
            }
            partVariants.put(part, models);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        int entityId = entity.getId();
        PerEntityState state = entityStates.computeIfAbsent(entityId, k -> new PerEntityState());

        state.controller.tick(ageInTicks);

        boolean isEdit = entity.isEditMode();
        handleEditModeTransition(state, isEdit);

        ApicaBeeModel<HoverbikeEntity> beeModel = getParentModel();
        ModelPart rightWing = beeModel.getRightWing();
        ModelPart leftWing = beeModel.getLeftWing();

        for (HoverbikePart partType : HoverbikePart.values()) {
            List<HoverbikePartModel> models = partVariants.get(partType);
            if (models == null || models.isEmpty()) continue;

            int variantIndex = entity.getPartVariant(partType);
            int clampedIndex = Math.floorMod(variantIndex, models.size());
            HoverbikePartModel part = models.get(clampedIndex);

            part.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            // Synchronise la rotation des protections d'aile avec les ailes du modele parent
            if (partType == HoverbikePart.WING_PROTECTOR) {
                applyWingRotation(part, rightWing, leftWing);
            }

            String animName = ANIM_PREFIX + partType.name().toLowerCase();

            poseStack.pushPose();
            applyEditOffset(poseStack, part, animName, state);

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

        cleanupStaleEntities(entity);
    }

    /**
     * Applique la rotation des ailes du modele parent aux protections d'aile.
     * Les ModelParts "protector_right" et "protector_left" suivent le yRot (angle)
     * et le zRot (battement) de l'aile correspondante.
     */
    private void applyWingRotation(HoverbikePartModel part, ModelPart rightWing, ModelPart leftWing) {
        try {
            ModelPart protRight = part.root().getChild("protector_right");
            ModelPart protLeft = part.root().getChild("protector_left");
            protRight.yRot = rightWing.yRot;
            protRight.zRot = rightWing.zRot;
            protLeft.yRot = leftWing.yRot;
            protLeft.zRot = leftWing.zRot;
        } catch (Exception ignored) {
            // Variantes sans ces enfants : pas de synchro
        }
    }

    /**
     * Detecte les transitions d'edit mode et cree les animations d'ecartement/retour.
     * Etat stocke par entite pour eviter les interferences multi-entites.
     */
    private void handleEditModeTransition(PerEntityState state, boolean isEdit) {
        if (isEdit && !state.wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                state.controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(Vec3.ZERO).to(offset)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(false)
                        .build());
            }
            state.editExpanded = true;
        } else if (!isEdit && state.wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                state.controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(offset).to(Vec3.ZERO)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(true)
                        .build());
            }
            state.editExpanded = false;
        }
        state.wasEditMode = isEdit;
    }

    private void applyEditOffset(PoseStack poseStack, HoverbikePartModel part,
                                  String animName, PerEntityState state) {
        if (state.editExpanded && !state.controller.isAnimationPlaying(animName)) {
            Vec3 offset = part.getEditModeOffset();
            poseStack.translate(offset.x, offset.y, offset.z);
        } else {
            state.controller.applyAnimation(animName, poseStack);
        }
    }

    /**
     * Nettoie les etats des entites qui n'existent plus.
     * Appele periodiquement pour eviter les fuites memoire.
     */
    private void cleanupStaleEntities(HoverbikeEntity currentEntity) {
        if (currentEntity.tickCount % 200 != 0) return;
        entityStates.entrySet().removeIf(entry -> {
            if (entry.getKey() == currentEntity.getId()) return false;
            return currentEntity.level().getEntity(entry.getKey()) == null;
        });
    }
}
