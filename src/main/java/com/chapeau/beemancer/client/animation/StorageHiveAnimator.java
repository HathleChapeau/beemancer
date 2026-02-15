/**
 * ============================================================
 * [StorageHiveAnimator.java]
 * Description: Gestionnaire d'animation per-hive pour l'oscillation verticale des Storage Hives
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimationController     | Gestion animations   | Named animations per-pos       |
 * | MoveAnimation           | Translation Y        | Bobbing up/down                |
 * | TimingType              | Courbe d'easing      | SLOW_IN_SLOW_OUT               |
 * | TimingEffect            | Effet temporel       | BOOMERANG (ping-pong)          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageHiveRenderer.java (tick, apply, cleanup)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Anime les Storage Hives avec un mouvement d'oscillation verticale (bobbing).
 * Le mouvement est un aller-retour smooth entre la position d'origine et +0.15 blocs.
 * L'animation ne joue que quand la hive est liee a un controller (LINKED ou ACTIVE).
 *
 * Utilise un cache per-position pour gerer chaque hive independamment.
 */
public class StorageHiveAnimator {

    private static final float BOB_HEIGHT = 0.15f;
    private static final float BOB_DURATION = 40f;
    private static final String BOB_CHANNEL = "bob";

    private static final Map<BlockPos, AnimState> states = new HashMap<>();

    private static class AnimState {
        final AnimationController controller;
        boolean active = false;

        AnimState() {
            this.controller = new AnimationController();
            this.controller.createAnimation(BOB_CHANNEL, MoveAnimation.builder()
                .from(0, 0, 0)
                .to(0, BOB_HEIGHT, 0)
                .duration(BOB_DURATION)
                .timingEffect(TimingEffect.BOOMERANG)
                .timingType(TimingType.SLOW_IN_SLOW_OUT)
                .resetAfterAnimation(false)
                .build());
        }
    }

    // === API publique ===

    /**
     * Retourne le controller d'animation pour cette position.
     */
    public static AnimationController getController(BlockPos pos) {
        return getOrCreateState(pos).controller;
    }

    /**
     * Tick l'animateur: avance le controller, gere start/stop de l'oscillation.
     *
     * @param pos           position du block entity
     * @param currentTime   gameTime + partialTick
     * @param shouldAnimate true si la hive est liee (LINKED ou ACTIVE)
     */
    public static void tick(BlockPos pos, float currentTime, boolean shouldAnimate) {
        AnimState state = getOrCreateState(pos);
        state.controller.tick(currentTime);

        if (shouldAnimate && !state.active) {
            state.active = true;
            // Decaler le startTime pour desynchroniser les hives entre elles
            float phaseOffset = Math.abs(pos.hashCode()) % BOB_DURATION;
            state.controller.tick(currentTime - phaseOffset);
            state.controller.playAnimation(BOB_CHANNEL);
            state.controller.tick(currentTime);
        } else if (!shouldAnimate && state.active) {
            state.active = false;
            state.controller.stopAnimation(BOB_CHANNEL);
        }
    }

    /**
     * Supprime l'etat d'animation pour cette position (quand le bloc est detruit).
     */
    public static void remove(BlockPos pos) {
        states.remove(pos);
    }

    // === Internal ===

    private static AnimState getOrCreateState(BlockPos pos) {
        return states.computeIfAbsent(pos, p -> new AnimState());
    }
}
