/**
 * ============================================================
 * [ApiIdleAnim.java]
 * Description: Animation idle pour Api - respiration et balancement
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
 * Animation idle: respiration douce avec balancement.
 * Loop infini avec timing BOOMERANG implicite (sin oscillation).
 *
 * Principes appliques:
 * - Secondary Action: bras suivent le corps avec offset
 * - Slow In/Slow Out: oscillation sinusoidale naturelle
 * - Breathing: inhale plus long, exhale plus court (asymetrie legere)
 */
public class ApiIdleAnim implements AnimationState {

    private static final float CYCLE_DURATION = 60f; // ticks pour un cycle complet
    private static final float TWO_PI = (float) (Math.PI * 2);

    // Amplitudes (en radians)
    private static final float BODY_AMPLITUDE = (float) Math.toRadians(3);
    private static final float ARM_AMPLITUDE = (float) Math.toRadians(5);
    private static final float LEG_AMPLITUDE = (float) Math.toRadians(2);

    // Offsets de timing (fraction du cycle, 0-1)
    private static final float ARM_OFFSET = 0.15f;
    private static final float LEG_OFFSET = 0.1f;

    // Rotation de base (45 degres comme le modele)
    private static final float BASE_ANGLE = (float) (Math.PI / 4);

    private final ApiModel model;
    private float startTime;

    public ApiIdleAnim(ApiModel model) {
        this.model = model;
    }

    @Override
    public void onEnter(float currentTime) {
        this.startTime = currentTime;
    }

    @Override
    public void onExit() {
        // Rien a nettoyer
    }

    @Override
    public boolean update(float currentTime, float deltaTime) {
        // Idle ne se termine jamais
        return false;
    }

    @Override
    public void apply(float currentTime) {
        float elapsed = currentTime - startTime;
        float cycleProgress = (elapsed % CYCLE_DURATION) / CYCLE_DURATION;

        // Onde principale (corps)
        float bodyWave = Mth.sin(cycleProgress * TWO_PI);

        // Ondes secondaires avec offset (bras et jambes suivent)
        float armWave = Mth.sin((cycleProgress + ARM_OFFSET) * TWO_PI);
        float legWave = Mth.sin((cycleProgress + LEG_OFFSET) * TWO_PI);

        // Asymetrie respiratoire: compression plus rapide que expansion
        // On module legerement l'amplitude selon la phase
        float breathMod = 1f + 0.1f * Mth.sin(cycleProgress * TWO_PI * 2);

        // === Application aux ModelParts ===

        // Body: balancement avant/arriere
        model.getBody().xRot = BASE_ANGLE + bodyWave * BODY_AMPLITUDE * breathMod;

        // Arms: balancement lateral (Z) avec offset
        // Bras gauche et droit en opposition pour effet naturel
        model.getArmLeft().xRot = BASE_ANGLE;
        model.getArmLeft().zRot = armWave * ARM_AMPLITUDE;

        model.getArmRight().xRot = BASE_ANGLE;
        model.getArmRight().zRot = -armWave * ARM_AMPLITUDE;

        // Legs: leger balancement avant/arriere
        model.getLegLeft().xRot = BASE_ANGLE + legWave * LEG_AMPLITUDE;
        model.getLegRight().xRot = BASE_ANGLE - legWave * LEG_AMPLITUDE * 0.7f;
    }

    @Override
    public boolean isComplete() {
        // Idle ne se termine jamais (loop infini)
        return false;
    }
}
