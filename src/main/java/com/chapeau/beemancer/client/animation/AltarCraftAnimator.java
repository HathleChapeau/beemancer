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
 * | RotateAnimation         | Rotation conduits    | Tilt, orbite, decel            |
 * | MoveAnimation           | Position conduits    | Expand/hold/return             |
 * | Sequence                | Timeline craft       | Action callbacks par phase     |
 * | SequenceEntry           | Entrees sequence     | action() pour transitions      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AltarHeartRenderer.java (obtient controller, tick, apply, beam state)
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
 * 3 canaux par conduit: orbit_i (orbite Y autour altar), pos_i (position), rot_i (tilt X/Z).
 *
 * Phases du craft:
 * 1. Horizontal spread (ease in) — conduits s'ecartent a l'horizontal
 * 2. Tilt + vertical move (ease in) — conduits se tournent et descendent
 * 3. Beam + orbit (ease in) — beam de particules + rotation orbitale (vitesse /3)
 * 4. Hold — rotation constante, beam actif
 * 5. Craft tick — execution du craft serveur
 * 6. Return — beam coupe, retour position, decel orbite
 */
public class AltarCraftAnimator {

    // === Timing (en ticks, 20 ticks = 1 seconde, gaps de 40t entre phases) ===
    public static final int HORIZ_END = 30;
    public static final int VERT_TILT_START = 70;
    public static final int VERT_TILT_END = 100;
    public static final int SPIN_START = 140;
    public static final int SPIN_END = 220;
    public static final int CRAFT_TICK = 250;
    public static final int BEAM_CUT = 260;
    public static final int RETURN_POS_START = 300;
    public static final int TOTAL_TICKS = 340;

    // === Tilt (rotation sur soi-meme, depend du facing) ===
    private static final float[] TILT_ANGLES = {
        90f,   // N → tilt vers le nord (exterieur)
        -90f,  // S → tilt vers le sud (exterieur)
        90f,   // E → tilt vers l'est (exterieur)
        -90f,  // W → tilt vers l'ouest (exterieur)
    };

    // === Orbite angles (rotation Y autour du centre altar, vitesse /3) ===
    private static final float ORBIT_SPIN_END = 840f;
    private static final float ORBIT_HOLD_END = 1290f;
    private static final float ORBIT_DECEL_END = 2100f;

    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);

    // Axe de tilt par conduit: N/S sur X, E/W sur Z
    private static final Axis[] TILT_AXES = {
        Axis.XP, Axis.XP, Axis.ZP, Axis.ZP,
    };

    // === Positions des conduits (relatives au coeur) ===
    private static final Vec3[] STATIC_POS = {
        new Vec3(0, 1, -1),
        new Vec3(0, 1, 1),
        new Vec3(1, 1, 0),
        new Vec3(-1, 1, 0),
    };

    // Offsets horizontaux (phase 1 — spread outward)
    private static final Vec3[] HORIZ_DELTA = {
        new Vec3(0, 0, -1),
        new Vec3(0, 0, 1),
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
    };

    // Offset vertical (phase 2 — descente au niveau du coeur)
    private static final Vec3 VERT_DELTA = new Vec3(0, -1, 0);

    // === Per-altar state ===
    private static final Map<BlockPos, AnimState> states = new HashMap<>();

    private static class AnimState {
        final AnimationController controller;
        boolean craftActive = false;
        Sequence craftSequence = null;
        boolean beamActive = false;
        long lastBeamTick = -1;

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

    /**
     * Retourne true si le beam est actif pour cet altar.
     * Utilise par le renderer pour savoir s'il faut spawner les particules.
     */
    public static boolean isBeamActive(BlockPos pos) {
        AnimState state = states.get(pos);
        return state != null && state.beamActive;
    }

    /**
     * Retourne true si le beam doit etre rendu CE tick (evite le spam par frame).
     * Retourne false si deja appele ce tick ou si le beam est inactif.
     */
    public static boolean trySpawnBeamTick(BlockPos pos, long gameTime) {
        AnimState state = states.get(pos);
        if (state == null || !state.beamActive) return false;
        if (state.lastBeamTick == gameTime) return false;
        state.lastBeamTick = gameTime;
        return true;
    }

    // === Creation controller ===

    private static AnimState getOrCreateState(BlockPos pos) {
        return states.computeIfAbsent(pos, p -> createState());
    }

    private static AnimState createState() {
        AnimationController ctrl = new AnimationController();

        ctrl.createAnimation("heart_y", RotateAnimation.builder()
            .axis(Axis.YP).startAngle(0).endAngle(360)
            .pivot(BLOCK_CENTER)
            .duration(360f).timingEffect(TimingEffect.LOOP)
            .build());
        ctrl.createAnimation("heart_x", RotateAnimation.builder()
            .axis(Axis.XP).startAngle(0).endAngle(360)
            .pivot(BLOCK_CENTER)
            .duration(514f).timingEffect(TimingEffect.LOOP)
            .build());
        ctrl.createAnimation("heart_z", RotateAnimation.builder()
            .axis(Axis.ZP).startAngle(0).endAngle(360)
            .pivot(BLOCK_CENTER)
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
        state.craftSequence = buildCraftSequence(state);
        state.controller.playSequence(state.craftSequence);
    }

    private static void stopCraft(AnimState state) {
        state.craftActive = false;
        state.beamActive = false;
        if (state.craftSequence != null) {
            state.controller.stopSequence(state.craftSequence);
            state.craftSequence = null;
        }
        for (int i = 0; i < STATIC_POS.length; i++) {
            state.controller.stopAnimation("orbit_" + i);
            state.controller.stopAnimation("pos_" + i);
            state.controller.stopAnimation("rot_" + i);
        }
    }

    private static Sequence buildCraftSequence(AnimState state) {
        AnimationController ctrl = state.controller;
        return new Sequence(
            // t=0: spread horizontal (ease in)
            SequenceEntry.action(0, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildHorizSpreadAnim(i));
                }
            }),
            // t=70: tilt + descente verticale (ease in)
            SequenceEntry.action(VERT_TILT_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildVertMoveAnim(i));
                    ctrl.replaceAnimation("rot_" + i, buildTiltAnim(i));
                }
            }),
            // t=140: beam on + hold pos/tilt + orbit spin
            SequenceEntry.action(SPIN_START, () -> {
                state.beamActive = true;
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildHoldPosAnim(i));
                    ctrl.replaceAnimation("rot_" + i, buildHoldTiltAnim(i));
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitSpinAnim());
                }
            }),
            // t=220: orbit hold (vitesse constante)
            SequenceEntry.action(SPIN_END, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitHoldAnim());
                }
            }),
            // t=260: beam off + orbit decel + untilt
            SequenceEntry.action(BEAM_CUT, () -> {
                state.beamActive = false;
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitDecelAnim());
                    ctrl.replaceAnimation("rot_" + i, buildUntiltAnim(i));
                }
            }),
            // t=300: retour position
            SequenceEntry.action(RETURN_POS_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildReturnAnim(i));
                }
            })
        );
    }

    // === Position factories ===

    /** Phase 1 : spread horizontal (ease in). */
    private static MoveAnimation buildHorizSpreadAnim(int conduitIndex) {
        return MoveAnimation.builder()
            .from(Vec3.ZERO).to(HORIZ_DELTA[conduitIndex])
            .duration(HORIZ_END).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Phase 2 : descente verticale (ease in). */
    private static MoveAnimation buildVertMoveAnim(int conduitIndex) {
        Vec3 from = HORIZ_DELTA[conduitIndex];
        Vec3 to = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(from).to(to)
            .duration(VERT_TILT_END - VERT_TILT_START).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Hold a la position finale (HORIZ + VERT = deplacement complet). */
    private static MoveAnimation buildHoldPosAnim(int conduitIndex) {
        Vec3 pos = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(pos).to(pos)
            .duration(20f).timingEffect(TimingEffect.LOOP)
            .build();
    }

    /** Retour a la position de repos. */
    private static MoveAnimation buildReturnAnim(int conduitIndex) {
        Vec3 pos = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(pos).to(Vec3.ZERO)
            .duration(TOTAL_TICKS - RETURN_POS_START)
            .timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }

    // === Tilt factories (rotation sur soi-meme, axe X ou Z selon le cote) ===

    /** Phase 2 : tilt vers l'exterieur (ease in). */
    private static RotateAnimation buildTiltAnim(int conduitIndex) {
        float angle = TILT_ANGLES[conduitIndex];
        return RotateAnimation.builder()
            .axis(TILT_AXES[conduitIndex]).startAngle(0).endAngle(angle)
            .pivot(BLOCK_CENTER)
            .duration(VERT_TILT_END - VERT_TILT_START).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Hold du tilt pendant le beam + orbite. */
    private static RotateAnimation buildHoldTiltAnim(int conduitIndex) {
        float angle = TILT_ANGLES[conduitIndex];
        return RotateAnimation.builder()
            .axis(TILT_AXES[conduitIndex]).startAngle(angle).endAngle(angle)
            .pivot(BLOCK_CENTER)
            .duration(20f).timingEffect(TimingEffect.LOOP)
            .build();
    }

    /** Retour du tilt a 0 (ease out). */
    private static RotateAnimation buildUntiltAnim(int conduitIndex) {
        float angle = TILT_ANGLES[conduitIndex];
        return RotateAnimation.builder()
            .axis(TILT_AXES[conduitIndex]).startAngle(angle).endAngle(0)
            .pivot(BLOCK_CENTER)
            .duration(TOTAL_TICKS - BEAM_CUT).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }

    // === Orbit factories (rotation Y autour du centre altar, vitesse /3) ===

    /** Acceleration de l'orbite (ease in). */
    private static RotateAnimation buildOrbitSpinAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(0).endAngle(ORBIT_SPIN_END)
            .pivot(BLOCK_CENTER)
            .duration(SPIN_END - SPIN_START).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Orbite a vitesse constante. */
    private static RotateAnimation buildOrbitHoldAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ORBIT_SPIN_END).endAngle(ORBIT_HOLD_END)
            .pivot(BLOCK_CENTER)
            .duration(BEAM_CUT - SPIN_END).timingType(TimingType.NORMAL)
            .resetAfterAnimation(false)
            .build();
    }

    /** Deceleration de l'orbite (ease out). */
    private static RotateAnimation buildOrbitDecelAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ORBIT_HOLD_END).endAngle(ORBIT_DECEL_END)
            .pivot(BLOCK_CENTER)
            .duration(TOTAL_TICKS - BEAM_CUT).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }
}
