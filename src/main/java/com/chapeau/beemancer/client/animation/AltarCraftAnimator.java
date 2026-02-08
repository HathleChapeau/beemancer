/**
 * ============================================================
 * [AltarCraftAnimator.java]
 * Description: Calcul des positions et rotations des conduits pendant le craft de l'altar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | (aucune externe)              | Math standalone      | -                              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AltarHeartRenderer.java (rendu des conduits animes)
 * - AltarHeartBlockEntity.java (constantes de timing)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Calculateur d'animation pour le craft de l'altar.
 * Gere 5 phases pour les 4 conduits:
 *
 * Phase 1 (0-2s):   Conduits s'ecartent en diagonal bas + rotation 90 deg
 * Phase 2 (2-7s):   Conduits accelerent leur rotation (spin-up, ease-in)
 * Phase 3 (7-8s):   Conduits maintiennent la vitesse max (hold)
 * Phase 4 (8-11s):  Conduits decelerent la rotation (ease-out vers 0 deg visuel)
 * Phase 5 (9-11s):  Conduits reviennent a leur position d'origine (chevauche phase 4)
 */
public class AltarCraftAnimator {

    // === Timing (en ticks, 20 ticks = 1 seconde) ===
    public static final int EXPAND_END = 40;
    public static final int SPIN_END = 140;
    public static final int CRAFT_TICK = 160;
    public static final int RETURN_POS_START = 180;
    public static final int TOTAL_TICKS = 220;

    // === Rotation ===
    private static final float INITIAL_ROT = 90f;
    private static final float SPIN_TOTAL = 2520f;
    private static final float SPIN_DURATION = 100f;
    private static final float SPIN_END_SPEED = 2f * SPIN_TOTAL / SPIN_DURATION;
    private static final float HOLD_DURATION = 20f;
    private static final float HOLD_ADDITIONAL = SPIN_END_SPEED * HOLD_DURATION;
    private static final float ANGLE_AT_HOLD_END = INITIAL_ROT + SPIN_TOTAL + HOLD_ADDITIONAL;
    private static final float RETURN_ROT_TOTAL = 1422f;
    private static final float RETURN_ROT_DURATION = 60f;

    // === Positions des conduits (relatives au coeur) ===
    private static final Vec3[] STATIC_POS = {
        new Vec3(0, 1, -1),
        new Vec3(0, 1, 1),
        new Vec3(1, 1, 0),
        new Vec3(-1, 1, 0),
    };

    private static final Vec3[] TARGET_POS = {
        new Vec3(0, 0, -2),
        new Vec3(0, 0, 2),
        new Vec3(2, 0, 0),
        new Vec3(-2, 0, 0),
    };

    /**
     * Calcule la position d'un conduit pendant l'animation de craft.
     * @param conduitIndex index du conduit (0=N, 1=S, 2=E, 3=W)
     * @param craftTick temps ecoule depuis le debut du craft (fractionnaire pour partialTick)
     */
    public static Vec3 computePosition(int conduitIndex, float craftTick) {
        Vec3 from = STATIC_POS[conduitIndex];
        Vec3 to = TARGET_POS[conduitIndex];

        if (craftTick < EXPAND_END) {
            float p = easeOut(craftTick / EXPAND_END);
            return lerpVec(p, from, to);
        } else if (craftTick < RETURN_POS_START) {
            return to;
        } else if (craftTick < TOTAL_TICKS) {
            float p = easeOut((craftTick - RETURN_POS_START) / (TOTAL_TICKS - RETURN_POS_START));
            return lerpVec(p, to, from);
        }
        return from;
    }

    /**
     * Calcule la rotation Y d'un conduit pendant l'animation de craft.
     * La rotation est continue a travers toutes les phases.
     * @param craftTick temps ecoule depuis le debut du craft
     * @return angle de rotation en degres
     */
    public static float computeRotation(float craftTick) {
        if (craftTick < EXPAND_END) {
            float p = easeOut(craftTick / EXPAND_END);
            return p * INITIAL_ROT;
        } else if (craftTick < SPIN_END) {
            float p = (craftTick - EXPAND_END) / SPIN_DURATION;
            return INITIAL_ROT + easeIn(p) * SPIN_TOTAL;
        } else if (craftTick < CRAFT_TICK) {
            float elapsed = craftTick - SPIN_END;
            return INITIAL_ROT + SPIN_TOTAL + SPIN_END_SPEED * elapsed;
        } else if (craftTick < CRAFT_TICK + RETURN_ROT_DURATION) {
            float p = (craftTick - CRAFT_TICK) / RETURN_ROT_DURATION;
            return ANGLE_AT_HOLD_END + easeOut(p) * RETURN_ROT_TOTAL;
        }
        return 0f;
    }

    /**
     * Retourne la position statique d'un conduit (hors craft).
     */
    public static Vec3 getStaticPosition(int conduitIndex) {
        return STATIC_POS[conduitIndex];
    }

    public static int getConduitCount() {
        return STATIC_POS.length;
    }

    // === Math helpers ===

    private static float easeIn(float t) {
        return t * t;
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private static Vec3 lerpVec(float t, Vec3 a, Vec3 b) {
        return new Vec3(
            Mth.lerp(t, a.x, b.x),
            Mth.lerp(t, a.y, b.y),
            Mth.lerp(t, a.z, b.z)
        );
    }
}
