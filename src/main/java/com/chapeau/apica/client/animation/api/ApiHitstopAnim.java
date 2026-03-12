/**
 * ============================================================
 * [ApiHitstopAnim.java]
 * Description: Animation d'effort puis epuisement pour Api
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
import net.minecraft.util.Mth;

/**
 * Animation hitstop: effort intense avec tremblement puis abandon epuise.
 *
 * Principes appliques:
 * - Anticipation: tension avant l'effort
 * - Exaggeration: tremblement exagere pour montrer l'effort
 * - Follow Through: affaissement lent apres l'abandon
 * - Personnage maladroit: recuperation lente, mouvements desynchronises
 */
public class ApiHitstopAnim implements AnimationState {

    // Phases (en ticks)
    private static final float EFFORT_END = 60f;
    private static final float ABANDON_END = 80f;
    private static final float TOTAL_DURATION = 120f;

    // Rotation de base
    private static final float BASE_ANGLE = (float) (Math.PI / 4);

    // Poses effort
    private static final float EFFORT_LEAN = (float) Math.toRadians(15);
    private static final float ARM_FORWARD = (float) Math.toRadians(25);
    private static final float LEG_SPREAD = (float) Math.toRadians(8);

    // Poses epuisement
    private static final float EXHAUSTED_LEAN = (float) Math.toRadians(-5);
    private static final float ARM_DROOP = (float) Math.toRadians(15);

    private final ApiModel model;
    private float startTime;
    private boolean complete;

    public ApiHitstopAnim(ApiModel model) {
        this.model = model;
    }

    @Override
    public void onEnter(float currentTime) {
        this.startTime = currentTime;
        this.complete = false;
    }

    @Override
    public void onExit() {
        model.resetPose();
    }

    @Override
    public boolean update(float currentTime, float deltaTime) {
        float elapsed = currentTime - startTime;
        if (elapsed >= TOTAL_DURATION) {
            complete = true;
            return true;
        }
        return false;
    }

    @Override
    public void apply(float currentTime) {
        float elapsed = currentTime - startTime;

        if (elapsed < EFFORT_END) {
            applyEffortPhase(elapsed);
        } else if (elapsed < ABANDON_END) {
            applyAbandonPhase(elapsed - EFFORT_END, ABANDON_END - EFFORT_END);
        } else {
            applyExhaustedPhase(elapsed - ABANDON_END, TOTAL_DURATION - ABANDON_END);
        }
    }

    /**
     * Phase 1: Effort intense avec tremblement haute frequence.
     */
    private void applyEffortPhase(float phaseTime) {
        // Transition rapide vers pose d'effort (10 premiers ticks)
        float enterT = Math.min(1f, phaseTime / 10f);
        float enterEase = easeOut(enterT);

        // Tremblement haute frequence (shake)
        float shakeIntensity = 0.03f * (1f + 0.5f * Mth.sin(phaseTime * 0.3f)); // Intensite variable
        float shakeX = Mth.sin(phaseTime * 2.5f) * shakeIntensity;
        float shakeZ = Mth.cos(phaseTime * 3.1f) * shakeIntensity * 0.7f;

        // Body: penche en avant avec tremblement
        model.getBody().xRot = BASE_ANGLE + EFFORT_LEAN * enterEase + shakeX;
        model.getBody().zRot = shakeZ;

        // Arms: etendus vers l'avant/bas avec tremblement
        float armShake = Mth.sin(phaseTime * 2.8f) * shakeIntensity * 2f;
        model.getArmLeft().xRot = BASE_ANGLE + ARM_FORWARD * enterEase + armShake;
        model.getArmLeft().zRot = (float) Math.toRadians(-10) * enterEase;

        model.getArmRight().xRot = BASE_ANGLE + ARM_FORWARD * enterEase - armShake;
        model.getArmRight().zRot = (float) Math.toRadians(10) * enterEase;

        // Legs: ecartees et tendues
        model.getLegLeft().xRot = BASE_ANGLE + LEG_SPREAD * enterEase;
        model.getLegLeft().zRot = (float) Math.toRadians(-5) * enterEase;

        model.getLegRight().xRot = BASE_ANGLE + LEG_SPREAD * enterEase;
        model.getLegRight().zRot = (float) Math.toRadians(5) * enterEase;
    }

    /**
     * Phase 2: Abandon - transition lente vers pose epuisee.
     */
    private void applyAbandonPhase(float phaseTime, float phaseDuration) {
        float t = phaseTime / phaseDuration;
        float ease = easeInOut(t);

        // Dernier tremblement qui s'estompe
        float fadeShake = Mth.sin(phaseTime * 1.5f) * 0.02f * (1f - t);

        // Body: de penche avant vers penche arriere
        model.getBody().xRot = BASE_ANGLE + Mth.lerp(ease, EFFORT_LEAN, EXHAUSTED_LEAN) + fadeShake;
        model.getBody().zRot = Mth.lerp(ease, 0, (float) Math.toRadians(2)); // Leger affaissement lateral

        // Arms: de tendus vers pendants
        model.getArmLeft().xRot = BASE_ANGLE + Mth.lerp(ease, ARM_FORWARD, 0);
        model.getArmLeft().zRot = Mth.lerp(ease, (float) Math.toRadians(-10), ARM_DROOP);

        model.getArmRight().xRot = BASE_ANGLE + Mth.lerp(ease, ARM_FORWARD, 0);
        model.getArmRight().zRot = Mth.lerp(ease, (float) Math.toRadians(10), -ARM_DROOP);

        // Legs: relachement
        model.getLegLeft().xRot = BASE_ANGLE + Mth.lerp(ease, LEG_SPREAD, 0);
        model.getLegLeft().zRot = Mth.lerp(ease, (float) Math.toRadians(-5), 0);

        model.getLegRight().xRot = BASE_ANGLE + Mth.lerp(ease, LEG_SPREAD, 0);
        model.getLegRight().zRot = Mth.lerp(ease, (float) Math.toRadians(5), 0);
    }

    /**
     * Phase 3: Epuisement - pose statique affaissee avec micro-mouvements.
     */
    private void applyExhaustedPhase(float phaseTime, float phaseDuration) {
        // Micro-mouvements de respiration lourde
        float breathe = Mth.sin(phaseTime * 0.15f) * 0.01f;

        // Body: affaisse
        model.getBody().xRot = BASE_ANGLE + EXHAUSTED_LEAN + breathe;
        model.getBody().zRot = (float) Math.toRadians(2);

        // Arms: pendants, desynchronises
        model.getArmLeft().xRot = BASE_ANGLE;
        model.getArmLeft().zRot = ARM_DROOP + breathe * 0.5f;

        model.getArmRight().xRot = BASE_ANGLE;
        model.getArmRight().zRot = -ARM_DROOP - breathe * 0.3f; // Desync volontaire

        // Legs: relachees
        model.getLegLeft().xRot = BASE_ANGLE + breathe * 0.3f;
        model.getLegRight().xRot = BASE_ANGLE - breathe * 0.2f;
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }
}
