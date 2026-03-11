/**
 * ============================================================
 * [HoverbikeEditModeHandler.java]
 * Description: Gestion des animations du mode edition HoverBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AnimationController | Controleur anim      | Gestion des animations         |
 * | MoveAnimation       | Type d'animation     | Ecartement/retour des pieces   |
 * | TimingType          | Easing               | SLOW_IN_SLOW_OUT               |
 * | HoverbikePartModel  | Modele partie        | getEditModeOffset()            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Transitions edit mode
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.MoveAnimation;
import com.chapeau.apica.client.animation.TimingType;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Gere les animations de transition en mode edition:
 * - Ecartement des pieces quand edit mode s'active
 * - Retour des pieces quand edit mode se desactive
 */
public final class HoverbikeEditModeHandler {

    private HoverbikeEditModeHandler() {}

    private static final float EDIT_ANIM_DURATION = 15f;
    private static final String ANIM_PREFIX = "edit_";

    /**
     * Detecte les transitions d'edit mode et cree les animations d'ecartement/retour.
     *
     * @param state Etat de l'entite
     * @param isEdit Mode edition actuel
     * @param partVariants Map des variantes par partie
     */
    public static void handleTransition(EditModeState state, boolean isEdit,
                                         Map<HoverbikePart, List<HoverbikePartModel>> partVariants) {
        if (isEdit && !state.wasEditMode) {
            // Entree en mode edition: ecarter les pieces
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
            // Sortie du mode edition: retour des pieces
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

    /**
     * Applique l'offset d'animation ou statique a la pose stack.
     *
     * @param poseStack Stack de transformation
     * @param part Modele de la partie
     * @param partType Type de partie
     * @param state Etat de l'entite
     * @param flipX Si true, inverse l'offset X (pour controle droit)
     */
    public static void applyOffset(PoseStack poseStack, HoverbikePartModel part,
                                    HoverbikePart partType, EditModeState state, boolean flipX) {
        String animName = ANIM_PREFIX + partType.name().toLowerCase();

        if (state.editExpanded && !state.controller.isAnimationPlaying(animName)) {
            // Animation terminee, appliquer offset statique
            Vec3 offset = part.getEditModeOffset();
            double ox = flipX ? -offset.x : offset.x;
            poseStack.translate(ox, offset.y, offset.z);
        } else {
            // Animation en cours
            state.controller.applyAnimation(animName, poseStack);
        }
    }

    /**
     * Etat du mode edition pour une entite.
     */
    public static class EditModeState {
        public final AnimationController controller = new AnimationController();
        public boolean wasEditMode = false;
        public boolean editExpanded = false;

        public void tick(float ageInTicks) {
            controller.tick(ageInTicks);
        }
    }
}
