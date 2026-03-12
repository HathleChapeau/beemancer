/**
 * ============================================================
 * [ApiAnimator.java]
 * Description: Orchestrateur des animations Api via state machine
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | StateMachine        | Gestion etats        | Transitions, blending          |
 * | ApiAnimationState   | Enum des etats       | IDLE, JUMP, HITSTOP, SLEEP     |
 * | ApiModel            | Modele cible         | Passage aux animations         |
 * | Api*Anim            | Implementations      | Animations specifiques         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApiRenderer.java (tick et rendu)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.api;

import com.chapeau.apica.client.animation.state.AnimationState;
import com.chapeau.apica.client.animation.state.StateMachine;
import com.chapeau.apica.client.model.ApiModel;

/**
 * Orchestre les animations d'Api.
 * Gere la state machine et les transitions entre animations.
 */
public class ApiAnimator {

    private final ApiModel model;
    private final StateMachine<ApiAnimationState> stateMachine;

    // References aux animations pour callbacks
    private final ApiIdleAnim idleAnim;
    private final ApiJumpAnim jumpAnim;
    private final ApiHitstopAnim hitstopAnim;
    private final ApiSleepAnim sleepAnim;

    public ApiAnimator(ApiModel model) {
        this.model = model;
        this.stateMachine = new StateMachine<>(ApiAnimationState.class, ApiAnimationState.IDLE);

        // Creer les animations
        this.idleAnim = new ApiIdleAnim(model);
        this.jumpAnim = new ApiJumpAnim(model);
        this.hitstopAnim = new ApiHitstopAnim(model);
        this.sleepAnim = new ApiSleepAnim(model);

        // Enregistrer dans la state machine
        stateMachine.registerState(ApiAnimationState.IDLE, idleAnim);
        stateMachine.registerState(ApiAnimationState.JUMP, jumpAnim);
        stateMachine.registerState(ApiAnimationState.HITSTOP, hitstopAnim);
        stateMachine.registerState(ApiAnimationState.SLEEP, sleepAnim);

        // Configurer le blend (10 ticks comme demande)
        stateMachine.setBlendDuration(10f);
    }

    /**
     * Met a jour l'animateur. Appeler chaque tick.
     *
     * @param currentTime temps en ticks (gameTime ou partialTick)
     * @param deltaTime   temps ecoule depuis le dernier update
     */
    public void update(float currentTime, float deltaTime) {
        stateMachine.update(currentTime, deltaTime);

        // Retour automatique a IDLE apres animations one-shot
        AnimationState current = getCurrentAnimationState();
        if (current != null && current.isComplete()) {
            if (stateMachine.getCurrentState() == ApiAnimationState.JUMP ||
                stateMachine.getCurrentState() == ApiAnimationState.HITSTOP) {
                stateMachine.setState(ApiAnimationState.IDLE, currentTime);
            }
        }
    }

    /**
     * Applique l'animation courante au modele.
     * Appeler juste avant le rendu.
     *
     * @param currentTime temps en ticks avec partialTick
     */
    public void apply(float currentTime) {
        // Reset pose avant application pour eviter accumulation
        model.resetPose();
        stateMachine.apply(currentTime);
    }

    /**
     * Change l'etat d'animation.
     */
    public void setState(ApiAnimationState state, float currentTime) {
        stateMachine.setState(state, currentTime);
    }

    /**
     * Cycle vers l'etat suivant (pour debug/interaction).
     */
    public void cycleState(float currentTime) {
        stateMachine.cycleToNextState(currentTime);
    }

    /**
     * Retourne l'etat courant.
     */
    public ApiAnimationState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    /**
     * Retourne l'implementation de l'animation courante.
     */
    private AnimationState getCurrentAnimationState() {
        return switch (stateMachine.getCurrentState()) {
            case IDLE -> idleAnim;
            case JUMP -> jumpAnim;
            case HITSTOP -> hitstopAnim;
            case SLEEP -> sleepAnim;
        };
    }

    /**
     * Retourne le modele associe.
     */
    public ApiModel getModel() {
        return model;
    }
}
