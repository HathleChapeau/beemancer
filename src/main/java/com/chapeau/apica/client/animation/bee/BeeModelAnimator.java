/**
 * ============================================================
 * [BeeModelAnimator.java]
 * Description: Gestionnaire d'animations pour ApicaBeeModel avec transitions fluides
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeAnimationState   | Etats d'animation    | Parametres d'animation         |
 * | ApicaBeeModel       | Modele cible         | Modification des ModelParts    |
 * | Mth                 | Math utilitaire      | Interpolation, cosinus         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBeeModel.java: Integration dans setupAnim
 * - MagicBeeRenderer.java: Changement d'etat
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.bee;

import com.chapeau.apica.client.model.ApicaBeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Anime un ApicaBeeModel selon un BeeAnimationState.
 * Gere les transitions fluides entre etats via interpolation lerp.
 *
 * Utilisation:
 * <pre>
 * BeeModelAnimator animator = new BeeModelAnimator();
 * animator.setState(BeeAnimationState.FLYING);
 * animator.animate(model, ageInTicks, partialTick);
 * </pre>
 */
public class BeeModelAnimator {

    /** Duree de transition entre etats en ticks. */
    private static final float TRANSITION_DURATION = 10f;

    /** Etat actuel de l'animation. */
    private BeeAnimationState currentState = BeeAnimationState.IDLE;

    /** Etat precedent (pour transitions). */
    private BeeAnimationState previousState = BeeAnimationState.IDLE;

    /** Progression de la transition (0 a 1). */
    private float transitionProgress = 1f;

    /** Tick de debut de transition. */
    private float transitionStartTick = 0f;

    // --- Valeurs interpolees ---
    private float currentWingSpeed;
    private float currentWingAmplitude;
    private float currentWingUpBias;
    private float currentBodyPitch;
    private float currentLegTuckAngle;

    public BeeModelAnimator() {
        applyStateValues(BeeAnimationState.IDLE);
    }

    /**
     * Change l'etat d'animation avec transition fluide.
     * Si l'etat est deja actif, ne fait rien.
     *
     * @param newState nouvel etat d'animation
     * @param currentTick tick actuel pour le timing
     */
    public void setState(BeeAnimationState newState, float currentTick) {
        if (newState == currentState && transitionProgress >= 1f) {
            return;
        }
        previousState = currentState;
        currentState = newState;
        transitionProgress = 0f;
        transitionStartTick = currentTick;
    }

    /**
     * Change l'etat immediatement sans transition.
     */
    public void setStateImmediate(BeeAnimationState newState) {
        previousState = newState;
        currentState = newState;
        transitionProgress = 1f;
        applyStateValues(newState);
    }

    /**
     * Retourne l'etat actuel.
     */
    public BeeAnimationState getState() {
        return currentState;
    }

    /**
     * Verifie si une transition est en cours.
     */
    public boolean isTransitioning() {
        return transitionProgress < 1f;
    }

    /**
     * Anime le modele selon l'etat actuel.
     * Appeler depuis le renderer avant le rendu.
     *
     * @param model modele a animer
     * @param ageInTicks temps d'animation
     * @param partialTick tick partiel pour interpolation
     * @param <T> type d'entite
     */
    public <T extends Entity> void animate(ApicaBeeModel<T> model, float ageInTicks, float partialTick) {
        updateTransition(ageInTicks);

        // Animation des ailes
        animateWings(model, ageInTicks);

        // Animation des pattes
        animateLegs(model);

        // Le pitch du corps est applique via le PoseStack dans le renderer
    }

    /**
     * Retourne le pitch actuel du corps (pour application dans le renderer).
     */
    public float getCurrentBodyPitch() {
        return currentBodyPitch;
    }

    /**
     * Retourne l'angle de repli des pattes actuel.
     */
    public float getCurrentLegTuckAngle() {
        return currentLegTuckAngle;
    }

    // --- Internal ---

    private void updateTransition(float currentTick) {
        if (transitionProgress >= 1f) {
            return;
        }

        float elapsed = currentTick - transitionStartTick;
        transitionProgress = Mth.clamp(elapsed / TRANSITION_DURATION, 0f, 1f);

        // Easing ease-out pour une transition naturelle
        float easedProgress = 1f - (1f - transitionProgress) * (1f - transitionProgress);

        // Interpolation des valeurs
        currentWingSpeed = Mth.lerp(easedProgress, previousState.wingSpeed, currentState.wingSpeed);
        currentWingAmplitude = Mth.lerp(easedProgress, previousState.wingAmplitude, currentState.wingAmplitude);
        currentWingUpBias = Mth.lerp(easedProgress, previousState.wingUpBias, currentState.wingUpBias);
        currentBodyPitch = Mth.lerp(easedProgress, previousState.bodyPitch, currentState.bodyPitch);
        currentLegTuckAngle = Mth.lerp(easedProgress, previousState.legTuckAngle, currentState.legTuckAngle);
    }

    private void applyStateValues(BeeAnimationState state) {
        currentWingSpeed = state.wingSpeed;
        currentWingAmplitude = state.wingAmplitude;
        currentWingUpBias = state.wingUpBias;
        currentBodyPitch = state.bodyPitch;
        currentLegTuckAngle = state.legTuckAngle;
    }

    private <T extends Entity> void animateWings(ApicaBeeModel<T> model, float ageInTicks) {
        model.animateWings(ageInTicks, currentWingSpeed, currentWingAmplitude, currentWingUpBias);
    }

    private <T extends Entity> void animateLegs(ApicaBeeModel<T> model) {
        model.animateLegs(currentLegTuckAngle);
    }

    /**
     * Applique le pitch du corps au ModelPart root/bone.
     * Doit etre appele avec le bon ModelPart (typiquement bone).
     *
     * @param bonePart le ModelPart bone a pivoter
     */
    public void applyBodyPitch(ModelPart bonePart) {
        bonePart.xRot = currentBodyPitch;
    }

    /**
     * Factory pour creer un animator deja en etat FLYING.
     */
    public static BeeModelAnimator createFlying() {
        BeeModelAnimator animator = new BeeModelAnimator();
        animator.setStateImmediate(BeeAnimationState.FLYING);
        return animator;
    }

    /**
     * Factory pour creer un animator en etat IDLE.
     */
    public static BeeModelAnimator createIdle() {
        return new BeeModelAnimator();
    }
}
