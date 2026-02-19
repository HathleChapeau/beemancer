/**
 * ============================================================
 * [HoverbikePartLayer.java]
 * Description: RenderLayer qui rend toutes les parties modulaires du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartVariants| Registre variantes  | Selection du modele a rendre   |
 * | AnimationController | Animations edit mode | Ecartement/retour des pieces   |
 * | MoveAnimation       | Translation animee   | Mouvement des pieces           |
 * | HoverbikeModel      | Modele parent        | Type generique du renderer     |
 * | EditModeHandler     | Partie hover         | Glow sur partie survolee       |
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

import com.chapeau.apica.client.model.HoverbikeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Layer de rendu qui itere sur toutes les parties du hoverbike et rend
 * la variante selectionnee de chaque partie. En edit mode, chaque partie
 * s'ecarte du centre avec une animation smooth.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, HoverbikeModel> {

    private static final float EDIT_ANIM_DURATION = 15f;
    private static final String ANIM_PREFIX = "edit_";

    /** Tous les modeles bakes, par partie → liste des variantes. */
    private final Map<HoverbikePart, List<HoverbikePartModel>> partVariants = new EnumMap<>(HoverbikePart.class);

    private final AnimationController controller = new AnimationController();
    private boolean wasEditMode = false;
    private boolean editExpanded = false;

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, HoverbikeModel> parent,
                              EntityRendererProvider.Context context) {
        super(parent);

        // Bake toutes les variantes de chaque partie
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

        float currentTime = ageInTicks;
        controller.tick(currentTime);

        boolean isEdit = entity.isEditMode();
        handleEditModeTransition(isEdit);

        for (HoverbikePart partType : HoverbikePart.values()) {
            List<HoverbikePartModel> models = partVariants.get(partType);
            if (models == null || models.isEmpty()) continue;

            int variantIndex = entity.getPartVariant(partType);
            int clampedIndex = Math.floorMod(variantIndex, models.size());
            HoverbikePartModel part = models.get(clampedIndex);

            part.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            String animName = ANIM_PREFIX + partType.name().toLowerCase();

            poseStack.pushPose();
            applyEditOffset(poseStack, part, animName);

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);

            // Glow overlay retiree — interaction via InteractionMarkerEntity avec wireframe

            poseStack.popPose();
        }
    }

    /**
     * Detecte les transitions d'edit mode et cree les animations d'ecartement/retour.
     * Utilise le premier modele de chaque partie pour les offsets (tous les variantes
     * d'une meme partie partagent le meme offset).
     */
    private void handleEditModeTransition(boolean isEdit) {
        if (isEdit && !wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(Vec3.ZERO).to(offset)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(false)
                        .build());
            }
            editExpanded = true;
        } else if (!isEdit && wasEditMode) {
            for (HoverbikePart partType : HoverbikePart.values()) {
                List<HoverbikePartModel> models = partVariants.get(partType);
                if (models == null || models.isEmpty()) continue;
                String animName = ANIM_PREFIX + partType.name().toLowerCase();
                Vec3 offset = models.get(0).getEditModeOffset();
                controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(offset).to(Vec3.ZERO)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(true)
                        .build());
            }
            editExpanded = false;
        }
        wasEditMode = isEdit;
    }

    private void applyEditOffset(PoseStack poseStack, HoverbikePartModel part, String animName) {
        if (editExpanded && !controller.isAnimationPlaying(animName)) {
            Vec3 offset = part.getEditModeOffset();
            poseStack.translate(offset.x, offset.y, offset.z);
        } else {
            controller.applyAnimation(animName, poseStack);
        }
    }
}
