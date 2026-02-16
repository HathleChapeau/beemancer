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
 * | RotateAnimation         | Rotation conduits    | Tilt, orbite, spin, decel      |
 * | MoveAnimation           | Position conduits    | Expand/hold/return             |
 * | Sequence                | Timeline craft       | Action callbacks par phase     |
 * | SequenceEntry           | Entrees sequence     | action() pour transitions      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AltarHeartRenderer.java (obtient controller, tick, apply, beam/particle state)
 * - AltarHeartBlockEntity.java (constantes de timing pour serverTick)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation;

import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'etat d'animation per-altar.
 * 5 canaux par conduit: orbit_i, pos_i, rot_i, spin_i + flags beam/particule.
 *
 * Position de repos (startpos):
 * Les conduits sont decales a mi-chemin vers le centre de l'altar.
 * Cette position est le depart et la fin de toute la sequence de craft.
 *
 * Phases du craft:
 * 1. Horizontal spread — conduits s'ecartent depuis startpos vers l'exterieur
 * 2. Tilt + vertical descent — conduits se tournent et descendent
 * 3. Self-spin Y — conduits tournent sur eux-memes (loop)
 * 4. Center particle — particule au centre du coeur
 * 5. Untilt — conduits pivotent 90 vers le centre
 * 6. Beam + orbit — beam actif, conduits orbitent autour de l'altar
 * 7. Decel + return — tout ralentit, retour a la position startpos
 */
public class AltarCraftAnimator {

    // === Timing (en ticks, 20 ticks = 1 seconde) ===

    /** Phase 1: spread horizontal (ease in) */
    public static final int DELAY_START = 25;

    public static final int HORIZ_DURATION = 40;

    /** Phase 2: tilt outward + descente verticale */
    public static final int TILT_START = 80;
    public static final int TILT_DURATION = 40;

    /** Phase 3: self-spin Y (rotation sur soi-meme, loop) */
    public static final int SELF_SPIN_START = 150;
    public static final int SELF_SPIN_PERIOD = 100;

    /** Phase 4: particule centre apparait */
    public static final int CENTER_PARTICLE_START = 165;

    /** Phase 5: untilt 90 vers le centre (slow in slow out) */
    public static final int UNTILT_START = 255;
    public static final int UNTILT_DURATION = 60;

    /** Phase 6: beam ON + orbite acceleree */
    public static final int BEAM_ORBIT_START = 300;
    public static final int ORBIT_ACCEL_DURATION = 100;

    /** Phase 6b: orbite vitesse constante */
    public static final int ORBIT_HOLD_START = 400;
    public static final int ORBIT_HOLD_DURATION = 200;

    /** Craft execution (server-side) */
    public static final int CRAFT_TICK = 590;

    /** Phase 7: deceleration + retour */
    public static final int DECEL_START = 610;
    public static final int DECEL_DURATION = 100;
    public static final int TOTAL_TICKS = 710;

    // === Orbit angles ===
    private static final float ORBIT_ACCEL_END = 300f;
    private static final float ORBIT_HOLD_END = 700f;
    private static final float ORBIT_DECEL_END = 950f;

    // === Tilt (rotation sur soi-meme, depend du facing) ===
    private static final float[] TILT_ANGLES = {
        90f,   // N
        -90f,  // S
        90f,   // E
        -90f,  // W
    };

    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);

    private static final Axis[] TILT_AXES = {
        Axis.XP, Axis.XP, Axis.ZP, Axis.ZP,
    };

    // Axe de self-spin par conduit: N/S sur Z (face le long de Z), E/W sur X (face le long de X)
    private static final Axis[] SPIN_AXES = {
        Axis.ZP, Axis.ZP, Axis.XP, Axis.XP,
    };

    // === Positions des conduits (relatives au coeur) ===
    private static final Vec3[] STATIC_POS = {
        new Vec3(0, 1, -1),
        new Vec3(0, 1, 1),
        new Vec3(1, 1, 0),
        new Vec3(-1, 1, 0),
    };

    private static final Vec3[] HORIZ_DELTA = {
        new Vec3(0, 0, -1),
        new Vec3(0, 0, 1),
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
    };

    private static final Vec3 VERT_DELTA = new Vec3(0, -1.2, 0);

    // Position de repos: conduits decales a mi-chemin vers le centre
    //private static final double STARTPOS_SCALE = 0.5;
    private static final Vec3[] STARTPOS_OFFSET = {
            new Vec3(0, 0.2, 0.4),
            new Vec3(0, 0.2, -0.4),
            new Vec3(-0.4, 0.2, 0),
            new Vec3(0.4, 0.2, 0),
    };

    // === Per-altar state ===
    private static final Map<BlockPos, AnimState> states = new HashMap<>();

    private static class AnimState {
        final AnimationController controller;
        boolean craftActive = false;
        Sequence craftSequence = null;
        boolean beamActive = false;
        boolean centerParticleActive = false;
        long lastBeamTick = -1;
        long lastParticleTick = -1;

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

    public static boolean isBeamActive(BlockPos pos) {
        AnimState state = states.get(pos);
        return state != null && state.beamActive;
    }

    public static boolean trySpawnBeamTick(BlockPos pos, long gameTime) {
        AnimState state = states.get(pos);
        if (state == null || !state.beamActive) return false;
        if (state.lastBeamTick == gameTime) return false;
        state.lastBeamTick = gameTime;
        return true;
    }

    /**
     * Retourne true si les particules de conduit sont actives pour cet altar.
     */
    public static boolean isCenterParticleActive(BlockPos pos) {
        AnimState state = states.get(pos);
        return state != null && state.centerParticleActive;
    }

    /**
     * Retourne true si la particule centre doit etre spawnee CE tick.
     * Evite le spam par frame (une seule spawn par tick).
     */
    public static boolean trySpawnCenterParticleTick(BlockPos pos, long gameTime) {
        AnimState state = states.get(pos);
        if (state == null || !state.centerParticleActive) return false;
        if (state.lastParticleTick == gameTime) return false;
        state.lastParticleTick = gameTime;
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

        // Conduits en position de repos (startpos): decales vers le centre
        for (int i = 0; i < STATIC_POS.length; i++) {
            ctrl.createAnimation("pos_" + i, buildStartPosHoldAnim(i));
            ctrl.playAnimation("pos_" + i);
        }

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
        state.centerParticleActive = false;
        if (state.craftSequence != null) {
            state.controller.stopSequence(state.craftSequence);
            state.craftSequence = null;
        }
        for (int i = 0; i < STATIC_POS.length; i++) {
            state.controller.stopAnimation("orbit_" + i);
            state.controller.stopAnimation("rot_" + i);
            state.controller.stopAnimation("spin_" + i);
            // Restaurer pos_i a la position de repos (startpos)
            state.controller.replaceAnimation("pos_" + i, buildStartPosHoldAnim(i));
        }
    }

    private static Sequence buildCraftSequence(AnimState state) {
        AnimationController ctrl = state.controller;
        return new Sequence(
            // t=0: spread horizontal (ease in)
            SequenceEntry.action(DELAY_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildHorizSpreadAnim(i));
                }
            }),
            // t=40: tilt outward + descente verticale
            SequenceEntry.action(TILT_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("pos_" + i, buildVertMoveAnim(i));
                    ctrl.replaceAnimation("rot_" + i, buildTiltAnim(i));
                }
            }),
            // t=75: self-spin Y demarre (loop, vitesse moyenne)
            SequenceEntry.action(SELF_SPIN_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("spin_" + i, buildSelfSpinAnim(i));
                }
            }),
            // t=115: particule centre apparait
            SequenceEntry.action(CENTER_PARTICLE_START, () -> {
                state.centerParticleActive = true;
            }),
            // t=155: untilt 90 vers le centre (smooth)
            SequenceEntry.action(UNTILT_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("rot_" + i, buildUntiltAnim(i));
                }
            }),
            // t=215: beam ON + orbite acceleree + hold pos (spin continue)
            SequenceEntry.action(BEAM_ORBIT_START, () -> {
                state.beamActive = true;
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitAccelAnim());
                    ctrl.replaceAnimation("pos_" + i, buildHoldPosAnim(i));
                }
            }),
            // t=275: orbite vitesse constante
            SequenceEntry.action(ORBIT_HOLD_START, () -> {
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitHoldAnim());
                }
            }),
            // t=315: decel, beam OFF, particule OFF, spin OFF, retour position
            SequenceEntry.action(DECEL_START, () -> {
                state.beamActive = false;
                state.centerParticleActive = false;
                for (int i = 0; i < STATIC_POS.length; i++) {
                    ctrl.stopAnimation("spin_" + i);
                    ctrl.replaceAnimation("orbit_" + i, buildOrbitDecelAnim());
                    ctrl.replaceAnimation("pos_" + i, buildReturnAnim(i));
                }
            })
        );
    }

    // === Position factories ===

    /** Position de repos: conduit decale vers le centre (hold permanent). */
    private static MoveAnimation buildStartPosHoldAnim(int conduitIndex) {
        Vec3 offset = STARTPOS_OFFSET[conduitIndex];
        return MoveAnimation.builder()
            .from(offset).to(offset)
            .duration(1f)
            .resetAfterAnimation(false)
            .build();
    }

    /** Phase 1: spread depuis startpos vers l'exterieur (ease in, hold). */
    private static MoveAnimation buildHorizSpreadAnim(int conduitIndex) {
        return MoveAnimation.builder()
            .from(STARTPOS_OFFSET[conduitIndex]).to(HORIZ_DELTA[conduitIndex])
            .duration(HORIZ_DURATION).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Phase 2: descente verticale (ease in, hold). */
    private static MoveAnimation buildVertMoveAnim(int conduitIndex) {
        Vec3 from = HORIZ_DELTA[conduitIndex];
        Vec3 to = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(from).to(to)
            .duration(TILT_DURATION).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Hold position pendant orbite (loop). */
    private static MoveAnimation buildHoldPosAnim(int conduitIndex) {
        Vec3 pos = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(pos).to(pos)
            .duration(20f).timingEffect(TimingEffect.LOOP)
            .build();
    }

    /** Retour a la position de repos startpos (ease out, hold). */
    private static MoveAnimation buildReturnAnim(int conduitIndex) {
        Vec3 pos = HORIZ_DELTA[conduitIndex].add(VERT_DELTA);
        return MoveAnimation.builder()
            .from(pos).to(STARTPOS_OFFSET[conduitIndex])
            .duration(DECEL_DURATION)
            .timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(false)
            .build();
    }

    // === Tilt factories ===

    /** Phase 2: tilt vers l'exterieur (ease in, hold). */
    private static RotateAnimation buildTiltAnim(int conduitIndex) {
        float angle = TILT_ANGLES[conduitIndex];
        return RotateAnimation.builder()
            .axis(TILT_AXES[conduitIndex]).startAngle(0).endAngle(angle)
            .pivot(BLOCK_CENTER)
            .duration(TILT_DURATION).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Phase 5: untilt vers le centre (smooth, hold). */
    private static RotateAnimation buildUntiltAnim(int conduitIndex) {
        float angle = TILT_ANGLES[conduitIndex];
        return RotateAnimation.builder()
            .axis(TILT_AXES[conduitIndex]).startAngle(angle).endAngle(0)
            .pivot(BLOCK_CENTER)
            .duration(UNTILT_DURATION).timingType(TimingType.SLOW_IN_SLOW_OUT)
            .resetAfterAnimation(false)
            .build();
    }

    // === Self-spin factory ===

    /** Phase 3: rotation sur soi-meme (loop, vitesse moyenne). Axe selon le facing du conduit. */
    private static RotateAnimation buildSelfSpinAnim(int conduitIndex) {
        return RotateAnimation.builder()
            .axis(SPIN_AXES[conduitIndex]).startAngle(0).endAngle(360)
            .pivot(BLOCK_CENTER)
            .duration(SELF_SPIN_PERIOD)//.timingEffect(TimingEffect.LOOP)
            .build();
    }

    // === Orbit factories ===

    /** Acceleration de l'orbite (ease in, hold). */
    private static RotateAnimation buildOrbitAccelAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(0).endAngle(ORBIT_ACCEL_END)
            .pivot(BLOCK_CENTER)
            .duration(ORBIT_ACCEL_DURATION).timingType(TimingType.EASE_IN)
            .resetAfterAnimation(false)
            .build();
    }

    /** Orbite a vitesse constante (normal, hold). */
    private static RotateAnimation buildOrbitHoldAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ORBIT_ACCEL_END).endAngle(ORBIT_HOLD_END)
            .pivot(BLOCK_CENTER)
            .duration(ORBIT_HOLD_DURATION).timingType(TimingType.NORMAL)
            .resetAfterAnimation(false)
            .build();
    }

    /** Deceleration de l'orbite (ease out, reset). */
    private static RotateAnimation buildOrbitDecelAnim() {
        return RotateAnimation.builder()
            .axis(Axis.YP).startAngle(ORBIT_HOLD_END).endAngle(ORBIT_DECEL_END)
            .pivot(BLOCK_CENTER)
            .duration(DECEL_DURATION).timingType(TimingType.EASE_OUT)
            .resetAfterAnimation(true)
            .build();
    }
}
