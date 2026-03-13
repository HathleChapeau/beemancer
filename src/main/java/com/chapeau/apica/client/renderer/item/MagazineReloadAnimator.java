/**
 * ============================================================
 * [MagazineReloadAnimator.java]
 * Description: Animation de reload partagée pour tous les IMagazineHolder renderers
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Sequence            | Séquence animation   | Enchaînement tilt down/up      |
 * | RotateAnimation     | Rotation             | Tilt de l'item                 |
 * | AnimationTimer      | Temps client         | getRenderTime                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MiningLaserItemRenderer.java
 * - LeafBlowerItemRenderer.java
 * - ChopperHiveItemRenderer.java
 * - BuildingStaffItemRenderer.java
 * - RailgunItemRenderer.java
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.item;

import com.chapeau.apica.client.animation.RotateAnimation;
import com.chapeau.apica.client.animation.Sequence;
import com.chapeau.apica.client.animation.SequenceEntry;
import com.chapeau.apica.client.animation.TimingType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Gère l'animation de reload pour les items IMagazineHolder.
 * Séquence: tilt vers l'avant -> maintien -> remonte -> callback onComplete
 * Durée totale: 40 ticks (2 secondes)
 */
@OnlyIn(Dist.CLIENT)
public class MagazineReloadAnimator {

    public float TILT_DURATION = 8f;   // ticks pour descendre/remonter
    public float HOLD_DURATION = 24f;  // ticks en position basse
    public float TILT_ANGLE = -30f;    // degrés (négatif = vers l'avant)
    public Vec3 TILT_PIVOT = new Vec3(0.5, 0.5, 0.5);

    public final float SWEEP_DURATION = 30f; // 1.5 secondes de sweep shader

    /** Référence statique vers l'animateur actif (pour le HUD) */
    private static MagazineReloadAnimator activeAnimator = null;

    /** Retourne l'animateur actuellement actif, ou null */
    public static MagazineReloadAnimator getActiveAnimator() {
        return activeAnimator;
    }

    private Sequence reloadSequence;
    private float sweepStartTime = -1f;
    private float sweepEndTime = -1f;

    /**
     * Initialise et démarre l'animation de reload.
     * @param currentTime temps actuel (AnimationTimer.getRenderTime)
     * @param onComplete callback appelé à la fin (doit faire doReload + setReloading(false) + triggerSweep)
     */
    public void startReloadAnimation(float currentTime, Runnable onComplete) {
        RotateAnimation tiltDown = RotateAnimation.builder()
                .axis(Axis.XP)
                .startAngle(0f)
                .endAngle(TILT_ANGLE)
                .pivot(TILT_PIVOT)
                .duration(TILT_DURATION)
                .timingType(TimingType.EASE_OUT)
                .resetAfterAnimation(false)
                .build();

        RotateAnimation tiltUp = RotateAnimation.builder()
                .axis(Axis.XP)
                .startAngle(TILT_ANGLE)
                .endAngle(0f)
                .pivot(TILT_PIVOT)
                .duration(TILT_DURATION)
                .timingType(TimingType.EASE_IN)
                .resetAfterAnimation(true)
                .build();

        // Wrapper du callback pour clear l'animateur actif
        Runnable wrappedComplete = () -> {
            activeAnimator = null;
            if (onComplete != null) onComplete.run();
        };

        reloadSequence = new Sequence(
            SequenceEntry.animation(0f, tiltDown),
            SequenceEntry.animation(TILT_DURATION + HOLD_DURATION, tiltUp),
            SequenceEntry.action(TILT_DURATION + HOLD_DURATION + TILT_DURATION, wrappedComplete)
        );
        reloadSequence.play(currentTime);
        activeAnimator = this;
    }

    /** Tick l'animation de reload (appeler chaque frame) */
    public void tick(float currentTime) {
        if (reloadSequence != null) {
            reloadSequence.tick(currentTime);
        }
    }

    /** Applique l'animation au PoseStack (appeler avant le rendu du body) */
    public void apply(PoseStack poseStack, float currentTime) {
        if (reloadSequence != null && reloadSequence.isPlaying()) {
            reloadSequence.apply(poseStack, currentTime);
        }
    }

    /** Retourne true si l'animation de reload est en cours */
    public boolean isAnimating() {
        return reloadSequence != null && reloadSequence.isPlaying();
    }

    /** Stoppe l'animation (si besoin d'annuler) */
    public void stop() {
        if (reloadSequence != null) {
            reloadSequence.stop();
            reloadSequence = null;
        }
    }

    /** Retourne la progression de l'animation (0.0 à 1.0), ou -1 si pas en cours */
    public float getProgress(float currentTime) {
        if (reloadSequence == null || !reloadSequence.isPlaying()) return -1f;
        return reloadSequence.timelineRatio(currentTime);
    }

    /** Durée totale en ticks */
    public static float getTotalDuration() {
        if(activeAnimator == null) return 0;
        return activeAnimator.TILT_DURATION + activeAnimator.HOLD_DURATION + activeAnimator.TILT_DURATION;
    }

    // =========================================================================
    // Sweep shader (affiché temporairement après reload)
    // =========================================================================

    /** Démarre le sweep shader pour 1 seconde */
    public void triggerSweep(float currentTime) {
        sweepStartTime = currentTime;
        sweepEndTime = currentTime + SWEEP_DURATION;
    }

    /** Retourne true si le sweep shader doit être affiché */
    public boolean isSweepActive(float currentTime) {
        return sweepStartTime >= 0 && currentTime >= sweepStartTime && currentTime < sweepEndTime;
    }

    /** Retourne la progression du sweep (0.0 à 1.0) */
    public float getSweepProgress(float currentTime) {
        if (!isSweepActive(currentTime)) return -1f;
        return (currentTime - sweepStartTime) / SWEEP_DURATION;
    }
}
