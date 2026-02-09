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
 * | HoverbikePartModel  | Modeles des parties  | Rendu geometrie                |
 * | ChassisPartModel    | Partie chassis       | Instanciation modele           |
 * | CoeurPartModel      | Partie coeur         | Instanciation modele           |
 * | PropulseurPartModel | Partie propulseur    | Instanciation modele           |
 * | RadiateurPartModel  | Partie radiateur     | Instanciation modele           |
 * | AnimationController | Animations edit mode | Ecartement/retour des pieces   |
 * | MoveAnimation       | Translation animee   | Mouvement des pieces           |
 * | HoverbikeModel      | Modele parent        | Type generique du renderer     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Ajout comme layer de rendu
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.client.animation.AnimationController;
import com.chapeau.beemancer.client.animation.MoveAnimation;
import com.chapeau.beemancer.client.animation.TimingType;
import com.chapeau.beemancer.client.model.HoverbikeModel;
import com.chapeau.beemancer.client.model.hoverbike.ChassisPartModel;
import com.chapeau.beemancer.client.model.hoverbike.CoeurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.beemancer.client.model.hoverbike.PropulseurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.RadiateurPartModel;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Layer de rendu qui itere sur toutes les parties du hoverbike et les rend
 * par-dessus le modele de base. En edit mode, chaque partie s'ecarte du centre
 * de la moto avec une animation smooth, puis revient a sa place en sortant.
 */
public class HoverbikePartLayer extends RenderLayer<HoverbikeEntity, HoverbikeModel> {

    private static final float EDIT_ANIM_DURATION = 15f;
    private static final String ANIM_PREFIX = "edit_";

    private final List<HoverbikePartModel> parts;
    private final AnimationController controller = new AnimationController();
    private boolean wasEditMode = false;
    private boolean editExpanded = false;

    public HoverbikePartLayer(RenderLayerParent<HoverbikeEntity, HoverbikeModel> parent,
                              EntityRendererProvider.Context context) {
        super(parent);

        this.parts = List.of(
                new ChassisPartModel(context.bakeLayer(ChassisPartModel.LAYER_LOCATION)),
                new CoeurPartModel(context.bakeLayer(CoeurPartModel.LAYER_LOCATION)),
                new PropulseurPartModel(context.bakeLayer(PropulseurPartModel.LAYER_LOCATION)),
                new RadiateurPartModel(context.bakeLayer(RadiateurPartModel.LAYER_LOCATION))
        );
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        float currentTime = ageInTicks;
        controller.tick(currentTime);

        boolean isEdit = entity.isEditMode();
        handleEditModeTransition(isEdit);

        for (HoverbikePartModel part : parts) {
            part.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            String animName = ANIM_PREFIX + part.getPartType().name().toLowerCase();

            poseStack.pushPose();
            applyEditOffset(poseStack, part, animName);

            VertexConsumer vertexConsumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(part.getTextureLocation()));

            part.renderToBuffer(poseStack, vertexConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /**
     * Detecte les transitions d'edit mode et cree les animations d'ecartement/retour.
     */
    private void handleEditModeTransition(boolean isEdit) {
        if (isEdit && !wasEditMode) {
            // Entree en edit mode : ecarter les pieces
            for (HoverbikePartModel part : parts) {
                String animName = ANIM_PREFIX + part.getPartType().name().toLowerCase();
                Vec3 offset = part.getEditModeOffset();
                controller.replaceAnimation(animName, MoveAnimation.builder()
                        .from(Vec3.ZERO).to(offset)
                        .duration(EDIT_ANIM_DURATION)
                        .timingType(TimingType.SLOW_IN_SLOW_OUT)
                        .resetAfterAnimation(false)
                        .build());
            }
            editExpanded = true;
        } else if (!isEdit && wasEditMode) {
            // Sortie d'edit mode : ramener les pieces
            for (HoverbikePartModel part : parts) {
                String animName = ANIM_PREFIX + part.getPartType().name().toLowerCase();
                Vec3 offset = part.getEditModeOffset();
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

    /**
     * Applique l'offset d'edit mode a la partie.
     * Si l'animation d'expansion est terminee (playing = false, editExpanded = true),
     * applique l'offset statiquement. Sinon, delegue a l'AnimationController.
     */
    private void applyEditOffset(PoseStack poseStack, HoverbikePartModel part, String animName) {
        if (editExpanded && !controller.isAnimationPlaying(animName)) {
            // Animation terminee, maintien statique a la position ecartee
            Vec3 offset = part.getEditModeOffset();
            poseStack.translate(offset.x, offset.y, offset.z);
        } else {
            // Animation en cours (expand ou contract) — ou pas d'edit mode
            controller.applyAnimation(animName, poseStack);
        }
    }
}
