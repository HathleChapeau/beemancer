/**
 * ============================================================
 * [AnimationController.java]
 * Description: Gestionnaire d'animations nommees et de sequences
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | Animation                     | Animations nommees   | play/pause/stop/apply          |
 * | Sequence                      | Sequences actives    | play/pause/stop/tick/apply     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IAnimatable.java (interface pour renderers)
 * - Tout renderer implementant IAnimatable
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire central d'animations pour un renderer.
 * Supporte deux modes : animations nommees (createAnimation/playAnimation)
 * et sequences (playSequence).
 *
 * Appeler tick() une fois par frame, puis applyAnimation() ou applySequence()
 * la ou les transforms doivent etre appliques dans le render.
 */
public class AnimationController {

    private final Map<String, Animation> namedAnimations = new LinkedHashMap<>();
    private final List<Sequence> activeSequences = new ArrayList<>();
    private float currentTime = 0f;

    // --- Named Animations ---

    /**
     * Enregistre une animation sous un nom.
     */
    public void createAnimation(String name, Animation animation) {
        namedAnimations.put(name, animation);
    }

    /**
     * Lance l'animation nommee.
     */
    public void playAnimation(String name) {
        Animation anim = namedAnimations.get(name);
        if (anim != null) {
            anim.play(currentTime);
        }
    }

    /**
     * Met en pause l'animation nommee.
     */
    public void pauseAnimation(String name) {
        Animation anim = namedAnimations.get(name);
        if (anim != null) {
            anim.pause(currentTime);
        }
    }

    /**
     * Arrete l'animation nommee.
     */
    public void stopAnimation(String name) {
        Animation anim = namedAnimations.get(name);
        if (anim != null) {
            anim.stop();
        }
    }

    /**
     * Applique l'animation nommee au PoseStack (si active).
     */
    public void applyAnimation(String name, PoseStack poseStack) {
        Animation anim = namedAnimations.get(name);
        if (anim != null) {
            anim.apply(poseStack, currentTime);
        }
    }

    /**
     * Verifie si une animation nommee est en cours de lecture.
     */
    public boolean isAnimationPlaying(String name) {
        Animation anim = namedAnimations.get(name);
        return anim != null && anim.isPlaying();
    }

    // --- Sequences ---

    /**
     * Lance une sequence.
     */
    public void playSequence(Sequence sequence) {
        if (!activeSequences.contains(sequence)) {
            activeSequences.add(sequence);
        }
        sequence.play(currentTime);
    }

    /**
     * Met en pause une sequence.
     */
    public void pauseSequence(Sequence sequence) {
        sequence.pause(currentTime);
    }

    /**
     * Arrete et retire une sequence.
     */
    public void stopSequence(Sequence sequence) {
        sequence.stop();
        activeSequences.remove(sequence);
    }

    /**
     * Reprend une sequence en attente d'un step.
     * Ne fait rien si la sequence n'est pas en attente.
     */
    public void continueSequence(Sequence sequence) {
        sequence.continueSequence(currentTime);
    }

    /**
     * Applique toutes les animations actives d'une sequence au PoseStack.
     */
    public void applySequence(Sequence sequence, PoseStack poseStack) {
        sequence.apply(poseStack, currentTime);
    }

    // --- Tick ---

    /**
     * Met a jour le temps interne et avance toutes les sequences actives.
     * Appeler une fois au debut de chaque render frame.
     *
     * @param currentTime temps actuel (gameTime + partialTick)
     */
    public void tick(float currentTime) {
        this.currentTime = currentTime;

        Iterator<Sequence> it = activeSequences.iterator();
        while (it.hasNext()) {
            Sequence seq = it.next();
            seq.tick(currentTime);
            if (!seq.isPlaying() && !seq.isPaused()) {
                it.remove();
            }
        }
    }

    public float getCurrentTime() {
        return currentTime;
    }
}
