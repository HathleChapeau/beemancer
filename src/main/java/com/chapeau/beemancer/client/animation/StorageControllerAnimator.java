/**
 * ============================================================
 * [StorageControllerAnimator.java]
 * Description: Gestionnaire d'animation per-controller pour le coeur rotatif du Storage Controller
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimationController     | Gestion animations   | Named animations per-pos       |
 * | RotateAnimation         | Rotation coeur       | Quarter-turns ease-in          |
 * | TimingType              | Courbe d'easing      | EASE_IN pour acceleration      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerRenderer.java (tick, apply, cleanup)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Anime le coeur du Storage Controller avec des quarts de tour en ease-in.
 * L'axe de rotation change a chaque quart de tour: X -> Y -> Z -> X...
 * L'animation ne joue que si le multibloc est forme ET que le miel est > 0.
 *
 * Utilise 3 canaux permanents (heart_x, heart_y, heart_z) qui accumulent
 * les rotations au fil des cycles. Chaque cycle ajoute 90 degres sur chaque axe.
 */
public class StorageControllerAnimator {

    private static final Axis[] AXES = { Axis.XP, Axis.YP, Axis.ZP };
    private static final String[] CHANNEL_NAMES = { "heart_x", "heart_y", "heart_z" };
    private static final float QUARTER_TURN_DURATION = 40f;
    private static final Vec3 PIVOT = new Vec3(0.5, 0.5, 0.5);

    private static final Map<BlockPos, AnimState> states = new HashMap<>();

    private static class AnimState {
        final AnimationController controller;
        int currentAxis = 0;
        int cycleCount = 0;
        boolean active = false;

        AnimState(AnimationController controller) {
            this.controller = controller;
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
     * Tick l'animateur: avance le controller, gere les transitions entre quarts de tour.
     *
     * @param pos           position du block entity
     * @param currentTime   gameTime + partialTick
     * @param shouldAnimate true si le multibloc est forme ET honey > 0
     */
    public static void tick(BlockPos pos, float currentTime, boolean shouldAnimate) {
        AnimState state = getOrCreateState(pos);
        state.controller.tick(currentTime);

        if (shouldAnimate && !state.active) {
            startAnimation(state);
        } else if (!shouldAnimate && state.active) {
            stopAnimation(state);
        } else if (shouldAnimate && state.active) {
            advanceIfFinished(state);
        }
    }

    /**
     * Supprime l'etat d'animation pour cette position (quand le bloc est detruit ou deformed).
     */
    public static void remove(BlockPos pos) {
        states.remove(pos);
    }

    // === Internal ===

    private static AnimState getOrCreateState(BlockPos pos) {
        return states.computeIfAbsent(pos, p -> new AnimState(new AnimationController()));
    }

    /**
     * Demarre l'animation depuis zero: cycle 0, axe X.
     */
    private static void startAnimation(AnimState state) {
        state.active = true;
        state.currentAxis = 0;
        state.cycleCount = 0;
        startQuarterTurn(state);
    }

    /**
     * Arrete toutes les animations et reset l'etat.
     * Le coeur revient a sa pose par defaut (identity).
     */
    private static void stopAnimation(AnimState state) {
        state.active = false;
        for (String channel : CHANNEL_NAMES) {
            state.controller.stopAnimation(channel);
        }
    }

    /**
     * Verifie si le quart de tour en cours est termine.
     * Si oui, avance au prochain axe (et increment le cycle si necessaire).
     */
    private static void advanceIfFinished(AnimState state) {
        String channel = CHANNEL_NAMES[state.currentAxis];
        if (!state.controller.isAnimationPlaying(channel)) {
            state.currentAxis++;
            if (state.currentAxis >= AXES.length) {
                state.currentAxis = 0;
                state.cycleCount++;
            }
            startQuarterTurn(state);
        }
    }

    /**
     * Lance un quart de tour sur l'axe courant.
     * L'angle de depart depend du nombre de cycles precedents pour accumuler les rotations.
     */
    private static void startQuarterTurn(AnimState state) {
        String channel = CHANNEL_NAMES[state.currentAxis];
        float startAngle = state.cycleCount * 90f;
        float endAngle = startAngle + 90f;

        state.controller.replaceAnimation(channel, RotateAnimation.builder()
            .axis(AXES[state.currentAxis])
            .startAngle(startAngle).endAngle(endAngle)
            .pivot(PIVOT)
            .timingType(TimingType.EASE_IN)
            .duration(QUARTER_TURN_DURATION)
            .resetAfterAnimation(false)
            .build());
    }
}
