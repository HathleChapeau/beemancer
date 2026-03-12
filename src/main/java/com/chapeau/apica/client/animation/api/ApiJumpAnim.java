/**
 * ============================================================
 * [ApiJumpAnim.java]
 * Description: Animation de 2 petits sauts pour Api
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
 * Animation jump: 2 petits sauts maladroits.
 *
 * Principes appliques:
 * - Anticipation: flexion avant chaque saut
 * - Squash & Stretch: ecrasement a l'atterrissage
 * - Exaggeration: mouvements exageres pour personnage maladroit
 * - Secondary Action: bras qui accompagnent le mouvement
 */
public class ApiJumpAnim implements AnimationState {

    // Duree totale: 2 sauts de 40 ticks chacun
    private static final float SINGLE_JUMP_DURATION = 40f;
    private static final float TOTAL_DURATION = SINGLE_JUMP_DURATION * 2;

    // Phases d'un saut (en ticks)
    private static final float ANTICIPATION_END = 10f;
    private static final float AIRBORNE_END = 20f;
    private static final float DESCENT_END = 30f;
    // 30-40: landing recovery

    // Rotation de base
    private static final float BASE_ANGLE = (float) (Math.PI / 4);

    // Amplitudes
    private static final float SQUAT_ANGLE = (float) Math.toRadians(8);
    private static final float STRETCH_ANGLE = (float) Math.toRadians(-5);
    private static final float ARM_UP_ANGLE = (float) Math.toRadians(20);
    private static final float SQUASH_RECOVERY = (float) Math.toRadians(10);

    private final ApiModel model;
    private float startTime;
    private boolean complete;

    public ApiJumpAnim(ApiModel model) {
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

        // Determiner quel saut (0 ou 1) et le temps dans ce saut
        int jumpIndex = (int) (elapsed / SINGLE_JUMP_DURATION);
        float jumpTime = elapsed % SINGLE_JUMP_DURATION;

        // Facteur de "fatigue" pour le 2eme saut (moins haut, plus maladroit)
        float fatigueFactor = jumpIndex == 0 ? 1.0f : 0.75f;

        applyJumpPhase(jumpTime, fatigueFactor);
    }

    private void applyJumpPhase(float jumpTime, float fatigue) {
        float bodyX, armZ, legX, bodyY;

        if (jumpTime < ANTICIPATION_END) {
            // Phase 1: Anticipation (flexion, preparation)
            float t = jumpTime / ANTICIPATION_END;
            float ease = easeInOut(t);

            bodyX = BASE_ANGLE + SQUAT_ANGLE * ease * fatigue;
            armZ = 0;
            legX = BASE_ANGLE + SQUAT_ANGLE * 0.5f * ease;
            bodyY = 0;

        } else if (jumpTime < AIRBORNE_END) {
            // Phase 2: Saut (etirement, bras vers le haut)
            float t = (jumpTime - ANTICIPATION_END) / (AIRBORNE_END - ANTICIPATION_END);
            float ease = easeOut(t);

            bodyX = BASE_ANGLE + STRETCH_ANGLE * ease * fatigue;
            armZ = -ARM_UP_ANGLE * ease * fatigue;
            legX = BASE_ANGLE;
            // Translation Y simulee par rotation supplementaire
            bodyY = Mth.sin(t * (float) Math.PI) * 4f * fatigue;

        } else if (jumpTime < DESCENT_END) {
            // Phase 3: Descente
            float t = (jumpTime - AIRBORNE_END) / (DESCENT_END - AIRBORNE_END);
            float ease = easeIn(t);

            bodyX = BASE_ANGLE + Mth.lerp(ease, STRETCH_ANGLE, SQUASH_RECOVERY) * fatigue;
            armZ = Mth.lerp(ease, -ARM_UP_ANGLE, 0) * fatigue;
            legX = BASE_ANGLE;
            bodyY = Mth.lerp(ease, 4f, 0) * fatigue;

        } else {
            // Phase 4: Atterrissage (squash, recovery, tremblement maladroit)
            float t = (jumpTime - DESCENT_END) / (SINGLE_JUMP_DURATION - DESCENT_END);
            float ease = easeOut(t);

            // Tremblement maladroit
            float shake = Mth.sin(t * 30f) * (1f - t) * 0.05f;

            bodyX = BASE_ANGLE + Mth.lerp(ease, SQUASH_RECOVERY, 0) * fatigue + shake;
            armZ = shake * 2f;
            legX = BASE_ANGLE + Mth.lerp(ease, SQUAT_ANGLE * 0.3f, 0);
            bodyY = 0;
        }

        // Application
        model.getBody().xRot = bodyX;
        model.getBody().y = 2.75f + bodyY; // bodyY est en pixels (4 = 4 pixels de hauteur)

        model.getArmLeft().xRot = BASE_ANGLE;
        model.getArmLeft().zRot = armZ;
        model.getArmRight().xRot = BASE_ANGLE;
        model.getArmRight().zRot = -armZ;

        model.getLegLeft().xRot = legX;
        model.getLegRight().xRot = legX;
    }

    // Fonctions d'easing
    private float easeIn(float t) {
        return t * t;
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
