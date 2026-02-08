/**
 * ============================================================
 * [SequenceEntry.java]
 * Description: Entree dans une sequence — seuil temporel + animation ou callback
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Animation                     | Animation a lancer   | play() au seuil atteint        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Sequence.java (liste d'entrees)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import javax.annotation.Nullable;

/**
 * Entree dans une sequence d'animations.
 * Contient un seuil temporel (en ticks) et soit une animation, soit un callback.
 * Quand le temps ecoule de la sequence depasse le threshold, l'action se declenche.
 *
 * Un threshold de -1 (STEP) met le chrono en pause et attend continueSequence().
 * Les animations deja en cours continuent a tourner pendant l'attente.
 */
public class SequenceEntry {

    /** Threshold special : met le chrono en pause, attend continueSequence(). */
    public static final float STEP_THRESHOLD = -1f;

    private final float threshold;
    @Nullable
    private final Animation animation;
    @Nullable
    private final Runnable action;

    private SequenceEntry(float threshold, @Nullable Animation animation, @Nullable Runnable action) {
        this.threshold = threshold;
        this.animation = animation;
        this.action = action;
    }

    /** Cree une entree qui lance une animation au seuil donne. */
    public static SequenceEntry animation(float threshold, Animation animation) {
        return new SequenceEntry(threshold, animation, null);
    }

    /** Cree une entree qui execute un callback au seuil donne. */
    public static SequenceEntry action(float threshold, Runnable action) {
        return new SequenceEntry(threshold, null, action);
    }

    /** Cree un step avec animation — le chrono se pause, attend continueSequence() pour lancer. */
    public static SequenceEntry step(Animation animation) {
        return new SequenceEntry(STEP_THRESHOLD, animation, null);
    }

    /** Cree un step avec callback — le chrono se pause, attend continueSequence() pour executer. */
    public static SequenceEntry stepAction(Runnable action) {
        return new SequenceEntry(STEP_THRESHOLD, null, action);
    }

    public float getThreshold() {
        return threshold;
    }

    @Nullable
    public Animation getAnimation() {
        return animation;
    }

    @Nullable
    public Runnable getAction() {
        return action;
    }

    public boolean isAnimation() {
        return animation != null;
    }

    public boolean isStep() {
        return threshold == STEP_THRESHOLD;
    }
}
