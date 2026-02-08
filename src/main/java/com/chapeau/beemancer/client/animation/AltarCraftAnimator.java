/**
 * ============================================================
 * [AltarCraftAnimator.java]
 * Description: Gestionnaire d'etat d'animation per-altar utilisant le systeme Animation/Sequence
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AnimationController     | Gestion animations   | Named animations + sequences   |
 * | RotateAnimation         | Rotation conduits    | Phases spin/decel              |
 * | MoveAnimation           | Position conduits    | Expand/hold/return             |
 * | Sequence                | Timeline craft       | Action callbacks par phase     |
 * | SequenceEntry           | Entrees sequence     | action() pour transitions      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AltarHeartRenderer.java (obtient controller, tick, apply)
 * - AltarHeartBlockEntity.java (constantes de timing pour serverTick)
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
 * Gestionnaire d'etat d'animation per-altar.
 * Stocke un AnimationController par BlockPos d'altar.
 * Gere la rotation permanente du coeur et la sequence de craft des conduits.
 */
public class AltarCraftAnimator {

    // === Timing (en ticks, 20 ticks = 1 seconde) ===
    public static final int EXPAND_END = 40;
    public static final int SPIN_END = 140;
    public static final int CRAFT_TICK = 160;
    public static final int RETURN_POS_START = 180;
    public static final int TOTAL_TICKS = 220;

    // === Rotation angles ===
    private static final float INITIAL_ROT = 90f;
    private static final float ANGLE_AT_SPIN_END = 2610f;
    private static final float ANGLE_AT_HOLD_END = 3618f;
    private static final float DECEL_END_ANGLE = 5040f;

    private static final Vec3 CONDUIT_PIVOT = new Vec3(0.5, 0.5, 0.5);

    // === Positions des conduits (relatives au coeur) ===
    private static final Vec3[] STATIC_POS = {
        new Vec3(0, 1, -1),
        new Vec3(0, 1, 1),
        new Vec3(1, 1, 0),
        new Vec3(-1, 1, 0),
    };

    // Offsets de deplacement: TARGET - STATIC
    private static final Vec3[] DELTA_POS = {
        new Vec3(0, -1, -1),
        new Vec3(0, -1, 1),
        new Vec3(1, -1, 0),
        new Vec3(-1, -1, 0),
    };

    // === Per-altar state ===
    private static final Map<BlockPos, AnimState> states = new HashMap<>();

    private static class AnimState {
        final AnimationController controller;
        boolean craftActive = false;
        Sequence craftSequence = null;

        AnimState(AnimationController controller) {
            this.controller = controller;
        }
    }

    // === API publique ===

    public static AnimationController getController(BlockPos pos) {
        return getOrCreateState(pos).controller;
    }

    public static void updateCraftState(BlockPos pos, boolean isCrafting) {
        AnimState state = getOrCreateState(pos);
        if (isCrafting && !state.craftActive) {
            startCraft(state);
        } else if (!isCrafting && state.craftActive) {
            stopCraft(state);
        }
    }

    public static void remove(BlockPos pos) {
        states.remove(pos);
    }

    public static Vec3 getStaticPosition(int index) {
        return STATIC_POS[index];
    }

    public static int getConduitCount() {
        return STATIC_POS.length;
    }

    // === Creation controller ===

    private static AnimState getOrCreateState(BlockPos pos) {
        return states.computeIfAbsent(pos, p -> createState());
    }

    private static AnimState createState() {
        AnimationController ctrl = new AnimationController();

        // Heart rotation permanente sur 3 axes
        ctrl.createAnimation("heart_y", RotateAnimation.builder()
            .axis(Axis.YP).startAngle(0).endAngle(360)
            .pivot(CONDUIT_PIVOT)
            .duration(360f).timingEffect(TimingEffect.LOOP)
            .build());
        ctrl.createAnimation("heart_x", RotateAnimation.builder()
            .axis(Axis.XP).startAngle(0).endAngle(360)
            .pivot(CONDUIT_PIVOT)
            .duration(514f).timingEffect(TimingEffect.LOOP)
            .build());
        ctrl.createAnimation("heart_z", RotateAnimation.builder()
            .axis(Axis.ZP).startAngle(0).endAngle(360)
            .pivot(CONDUIT_PIVOT)
            .duration(1200f).timingEffect(TimingEffect.LOOP)
            .build());

        ctrl.playAnimation("heart_y");
        ctrl.playAnimation("heart_x");
        ctrl.playAnimation("heart_z");

        return new AnimState(ctrl);
    }

    // === Craft sequence ===

    private static void startCraft(AnimState state) {
        state.craftActive = true;
        state.craftSequence = buildCraftSequence(state.controller);
        state.controller.playSequence(state.craftSequence);
    }

    private static void stopCraft(AnimState state) {
        state.craftActive = false;
        if (state.craftSequence != null) {
            state.controller.stopSequence(state.craftSequence);
            state.craftSequence = null;
        }
        for (int i = 0; i < STATIC_POS.length; i++) {
            state.controller.stopAnimation("pos_" + i);
            state.controller.stopAnimation("rot_" + i);
        }
    }

    private static Sequence buildCraftSequence(AnimationController ctrl) {
        return new Sequence(
            // t=0: conduits s'ecartent + rotation initiale 90 deg
            SequenceEntry.action(0, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildExpandAnim(i));
                    ctrl.replaceAnimation("rot_" + i, buildInitialRotAnim());
                }
            }),
            // t=40: hold position + spin acceleration
            SequenceEntry.action(EXPAND_END, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildHoldPosAnim(i));
                    ctrl.replaceAnimation("rot_" + i, buildSpinAnim());
                }
            }),
            // t=140: hold rotation a vitesse constante
            SequenceEntry.action(SPIN_END, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("rot_" + i, buildHoldRotAnim());
                }
            }),
            // t=160: deceleration rotation
            SequenceEntry.action(CRAFT_TICK, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("rot_" + i, buildDecelAnim());
                }
            }),
            // t=180: retour position
            SequenceEntry.action(RETURN_POS_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildReturnAnim(i));
                }
            })
        );
    }

    // === Animation factories ===

    private static MoveAnimation buildExpandAnim(int conduitIndex) {
        return MoveAnimation.builder()
            .from(Vec3.ZERO).to(DELTA_POS[conduitIndex])
            .duration(EXPAND_END).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(false)
            .build();
    }

    private static MoveAnimation buildHoldPosAnim(int conduitIndex) {
        Vec3 delta = DELTA_POS[conduitIndex];
        return MoveAnimation.builder()
            .from(delta).to(delta)
            .duration(20f).timingEffect(TimingEffect.LOOP)
            .build();
    }

    private static MoveAnimation buildReturnAnim(int conduitIndex) {
        return MoveAnimation.builder()
            .from(DELTA_POS[conduitIndex]).to(Vec3.ZERO)
            .duration(TOTAL_TICKS - RETURN_POS_START)
            .timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }

    private static RotateAnimation buildInitialRotAnim() {
        return RotateAnimation.builder()
            .axis(Axis.XP).startAngle(0).endAngle(INITIAL_ROT)
            .pivot(CONDUIT_PIVOT)
            .duration(EXPAND_END).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(false)
            .build();
    }

    private static RotateAnimation buildSpinAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(INITIAL_ROT).endAngle(ANGLE_AT_SPIN_END)
            .pivot(CONDUIT_PIVOT)
            .duration(SPIN_END - EXPAND_END).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    private static RotateAnimation buildHoldRotAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ANGLE_AT_SPIN_END).endAngle(ANGLE_AT_HOLD_END)
            .pivot(CONDUIT_PIVOT)
            .duration(CRAFT_TICK - SPIN_END).timingType(TimingType.NORMAL)
            .resetAfterAnimation(false)
            .build();
    }

    private static RotateAnimation buildDecelAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ANGLE_AT_HOLD_END).endAngle(DECEL_END_ANGLE)
            .pivot(CONDUIT_PIVOT)
            .duration(TOTAL_TICKS - CRAFT_TICK).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }
}
