/**
 * ============================================================
 * [Sequence.java]
 * Description: Enchainement d'animations declenche par seuils temporels
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | SequenceEntry                 | Entrees de sequence  | Seuils + animations/callbacks  |
 * | Animation                     | Animations a lancer  | play/apply dans tick           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AnimationController.java (gestion des sequences)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequence d'animations declenchees par seuils temporels.
 * Les entrees sont traitees dans l'ordre d'insertion.
 *
 * Supporte les steps (threshold -1) : le chrono se met en pause,
 * les animations deja lancees continuent, et on attend continueSequence()
 * pour declencher l'entree step et reprendre le chrono.
 */
public class Sequence {

    private final SequenceEntry[] entries;
    private final List<Animation> startedAnimations = new ArrayList<>();

    private boolean playing = false;
    private boolean paused = false;
    private boolean waiting = false;
    private float startTime = 0f;
    private float pauseTime = 0f;
    private float totalPausedDuration = 0f;
    private float waitStartTime = 0f;
    private int nextEntryIndex = 0;

    /**
     * Cree une sequence avec les entrees dans l'ordre donne.
     * Les entrees ne sont PAS triees — l'ordre d'insertion est respecte.
     */
    public Sequence(SequenceEntry... entries) {
        this.entries = entries.clone();
    }

    // --- Playback controls ---

    public void play(float currentTime) {
        if (paused && !waiting) {
            totalPausedDuration += currentTime - pauseTime;
            paused = false;
            for (Animation anim : startedAnimations) {
                anim.play(currentTime);
            }
            return;
        }
        if (playing) return;
        startTime = currentTime;
        totalPausedDuration = 0f;
        nextEntryIndex = 0;
        waiting = false;
        startedAnimations.clear();
        playing = true;
        paused = false;
    }

    public void pause(float currentTime) {
        if (!playing || paused || waiting) return;
        paused = true;
        pauseTime = currentTime;
        for (Animation anim : startedAnimations) {
            anim.pause(currentTime);
        }
    }

    public void stop() {
        for (Animation anim : startedAnimations) {
            anim.stop();
        }
        startedAnimations.clear();
        playing = false;
        paused = false;
        waiting = false;
        nextEntryIndex = 0;
        totalPausedDuration = 0f;
    }

    /**
     * Reprend le chrono apres un step.
     * Declenche l'entree step (animation ou callback) puis continue la sequence.
     * Ne fait rien si la sequence n'est pas en attente d'un step.
     */
    public void continueSequence(float currentTime) {
        if (!waiting) return;

        // Comptabiliser le temps d'attente
        totalPausedDuration += currentTime - waitStartTime;

        // Declencher l'entree step
        SequenceEntry entry = entries[nextEntryIndex];
        triggerEntry(entry, currentTime);
        nextEntryIndex++;

        waiting = false;
    }

    public boolean isPlaying() {
        if (playing) return true;
        for (Animation anim : startedAnimations) {
            if (anim.isPlaying()) return true;
        }
        return false;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isWaiting() {
        return waiting;
    }

    // --- Tick & Apply ---

    /**
     * Avance la sequence : verifie les seuils et declenche les entrees.
     * Si un step est rencontre, le chrono se met en pause.
     */
    public void tick(float currentTime) {
        if (!playing || paused || waiting) return;

        float elapsed = currentTime - startTime - totalPausedDuration;

        while (nextEntryIndex < entries.length) {
            SequenceEntry entry = entries[nextEntryIndex];

            // Step : pause le chrono, attend continueSequence()
            if (entry.isStep()) {
                waiting = true;
                waitStartTime = currentTime;
                return;
            }

            if (elapsed < entry.getThreshold()) break;

            triggerEntry(entry, currentTime);
            nextEntryIndex++;
        }

        // Auto-stop quand toutes les entrees sont declenchees et toutes les animations terminees
        if (nextEntryIndex >= entries.length) {
            boolean allDone = true;
            for (Animation anim : startedAnimations) {
                if (anim.isPlaying()) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                playing = false;
            }
        }
    }

    /**
     * Applique toutes les animations actives de la sequence au PoseStack.
     */
    public void apply(PoseStack poseStack, float currentTime) {
        for (Animation anim : startedAnimations) {
            anim.apply(poseStack, currentTime);
        }
    }

    // --- Internal ---

    private void triggerEntry(SequenceEntry entry, float currentTime) {
        if (entry.isAnimation() && entry.getAnimation() != null) {
            entry.getAnimation().play(currentTime);
            startedAnimations.add(entry.getAnimation());
        } else if (entry.getAction() != null) {
            entry.getAction().run();
        }
    }
}
