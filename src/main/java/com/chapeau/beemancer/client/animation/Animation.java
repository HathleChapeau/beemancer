/**
 * ============================================================
 * [Animation.java]
 * Description: Classe abstraite parent de toutes les animations
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | TimingType                    | Courbe d'easing      | apply() dans computeProgress   |
 * | TimingEffect                  | Effet temporel       | apply() dans computeProgress   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MoveAnimation.java (sous-classe)
 * - RotateAnimation.java (sous-classe)
 * - AnimationController.java (gestion)
 * - Sequence.java (enchainement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Classe abstraite pour toute animation renderer.
 * Gere le cycle play/pause/stop, le calcul du progress avec easing et effets temporels.
 *
 * Pipeline: rawDelta -> TimingEffect -> TimingType -> doApply(progress)
 */
public abstract class Animation {

    protected final TimingType timingType;
    protected final TimingEffect timingEffect;
    protected final float easePower;
    protected final float duration;
    protected final boolean resetAfterAnimation;

    private boolean playing = false;
    private boolean paused = false;
    private float startTime = 0f;
    private float pauseTime = 0f;
    private float totalPausedDuration = 0f;

    protected Animation(TimingType timingType, TimingEffect timingEffect,
                         float easePower, float duration, boolean resetAfterAnimation) {
        this.timingType = timingType;
        this.timingEffect = timingEffect;
        this.easePower = easePower;
        this.duration = duration;
        this.resetAfterAnimation = resetAfterAnimation;
    }

    // --- Playback controls ---

    /**
     * Demarre ou reprend l'animation.
     * @param currentTime temps actuel (gameTime + partialTick)
     */
    public void play(float currentTime) {
        if (paused) {
            totalPausedDuration += currentTime - pauseTime;
            paused = false;
            return;
        }
        if (playing) return;
        startTime = currentTime;
        totalPausedDuration = 0f;
        playing = true;
        paused = false;
    }

    /**
     * Met l'animation en pause.
     * @param currentTime temps actuel
     */
    public void pause(float currentTime) {
        if (!playing || paused) return;
        paused = true;
        pauseTime = currentTime;
    }

    /**
     * Arrete l'animation et reset l'etat interne.
     */
    public void stop() {
        playing = false;
        paused = false;
        startTime = 0f;
        pauseTime = 0f;
        totalPausedDuration = 0f;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Verifie si l'animation est terminee (uniquement pour TimingEffect.NONE).
     */
    public boolean isFinished(float currentTime) {
        if (!playing || paused || timingEffect != TimingEffect.NONE) return false;
        float rawDelta = computeRawDelta(currentTime);
        return rawDelta >= 1.0f;
    }

    // --- Application ---

    /**
     * Applique l'animation au PoseStack si elle est active.
     *
     * @param poseStack le PoseStack du renderer
     * @param currentTime temps actuel (gameTime + partialTick)
     */
    public void apply(PoseStack poseStack, float currentTime) {
        if (!playing || paused) return;

        float rawDelta = computeRawDelta(currentTime);

        // Gestion fin d'animation pour TimingEffect.NONE
        if (timingEffect == TimingEffect.NONE && rawDelta >= 1.0f) {
            if (!resetAfterAnimation) {
                doApply(poseStack, 1.0f);
            }
            playing = false;
            return;
        }

        float effectDelta = timingEffect.apply(rawDelta);
        float progress = timingType.apply(effectDelta, easePower);
        doApply(poseStack, progress);
    }

    // --- Internal ---

    private float computeRawDelta(float currentTime) {
        if (duration <= 0f) return 1f;
        return (currentTime - startTime - totalPausedDuration) / duration;
    }

    /**
     * Applique la transformation specifique a cette animation.
     *
     * @param poseStack le PoseStack
     * @param progress  valeur easee entre 0.0 et 1.0
     */
    protected abstract void doApply(PoseStack poseStack, float progress);

    // --- Builder base ---

    /**
     * Builder abstrait pour les sous-classes d'Animation.
     * Chaque sous-classe definit son propre builder qui etend celui-ci.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        protected TimingType timingType = TimingType.NORMAL;
        protected TimingEffect timingEffect = TimingEffect.NONE;
        protected float easePower = 2.0f;
        protected float duration = 20f;
        protected boolean resetAfterAnimation = true;

        public T timingType(TimingType timingType) {
            this.timingType = timingType;
            return (T) this;
        }

        public T timingEffect(TimingEffect timingEffect) {
            this.timingEffect = timingEffect;
            return (T) this;
        }

        public T easePower(float easePower) {
            this.easePower = easePower;
            return (T) this;
        }

        public T duration(float duration) {
            this.duration = duration;
            return (T) this;
        }

        public T resetAfterAnimation(boolean reset) {
            this.resetAfterAnimation = reset;
            return (T) this;
        }

        public abstract Animation build();
    }
}
