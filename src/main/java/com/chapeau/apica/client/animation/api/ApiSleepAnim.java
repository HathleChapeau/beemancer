/**
 * ============================================================
 * [ApiSleepAnim.java]
 * Description: Animation de sommeil statique pour Api
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AnimationState      | Interface etat       | Implementation                 |
 * | ApiModel            | Modele cible         | Modification des ModelParts    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApiAnimator.java (enregistrement dans state machine)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.api;

import com.chapeau.apica.client.animation.state.AnimationState;
import com.chapeau.apica.client.model.ApiModel;

/**
 * Animation sleep: pose statique endormie.
 * Bras et jambes pendants vers le bas, corps legerement affaisse.
 * Reste dans cet etat jusqu'a changement manuel.
 */
public class ApiSleepAnim implements AnimationState {

    // Rotation de base
    private static final float BASE_ANGLE = (float) (Math.PI / 4);

    // Pose endormie
    private static final float SLEEP_LEAN = (float) Math.toRadians(5); // Leger penche avant
    private static final float ARM_DROOP = (float) Math.toRadians(20); // Bras pendants
    private static final float LEG_RELAX = (float) Math.toRadians(3); // Jambes detendues

    // Duree de transition vers la pose (ticks)
    private static final float TRANSITION_DURATION = 20f;

    private final ApiModel model;
    private float startTime;

    public ApiSleepAnim(ApiModel model) {
        this.model = model;
    }

    @Override
    public void onEnter(float currentTime) {
        this.startTime = currentTime;
    }

    @Override
    public void onExit() {
        model.resetPose();
    }

    @Override
    public boolean update(float currentTime, float deltaTime) {
        // Sleep ne se termine jamais automatiquement
        return false;
    }

    @Override
    public void apply(float currentTime) {
        float elapsed = currentTime - startTime;

        // Transition douce vers la pose de sommeil
        float t = Math.min(1f, elapsed / TRANSITION_DURATION);
        float ease = easeInOut(t);

        // Body: legerement penche en avant (affaissement)
        model.getBody().xRot = BASE_ANGLE + SLEEP_LEAN * ease;
        model.getBody().zRot = (float) Math.toRadians(1) * ease; // Tres leger tilt

        // Arms: pendants vers le bas (rotation Z positive = vers l'exterieur)
        model.getArmLeft().xRot = BASE_ANGLE;
        model.getArmLeft().zRot = ARM_DROOP * ease;

        model.getArmRight().xRot = BASE_ANGLE;
        model.getArmRight().zRot = -ARM_DROOP * ease;

        // Legs: detendues, legerement vers l'avant
        model.getLegLeft().xRot = BASE_ANGLE + LEG_RELAX * ease;
        model.getLegLeft().zRot = (float) Math.toRadians(2) * ease;

        model.getLegRight().xRot = BASE_ANGLE + LEG_RELAX * ease;
        model.getLegRight().zRot = (float) Math.toRadians(-2) * ease;
    }

    private float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }

    @Override
    public boolean isComplete() {
        // Sleep ne se termine jamais (reste jusqu'a changement manuel)
        return false;
    }
}
