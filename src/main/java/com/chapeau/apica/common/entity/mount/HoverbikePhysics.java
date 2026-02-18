/**
 * ============================================================
 * [HoverbikePhysics.java]
 * Description: Calculs de velocite, rotation, gravite et friction du Hoverbike
 * ============================================================
 *
 * PATTERN: Inspire de Cobblemon HorseBehaviour.kt (velocity/rotation)
 * - Velocite calculee en espace local (Z=avant, X=cote, Y=vertical)
 * - Rotation progressive vers le curseur du joueur
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeSettings   | Constantes physiques | Tous les calculs               |
 * | HoverbikeMode       | Mode courant         | Branchement logique            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoverbikeEntity.java: Appels depuis travel() et tickRidden()
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Methodes statiques pour tous les calculs physiques du Hoverbike.
 * Pas d'etat interne — tout est passe en parametre.
 */
public final class HoverbikePhysics {

    private HoverbikePhysics() {}

    /** Seuil en dessous duquel une composante de collision est traitee comme zero. */
    public static final double COLLISION_EPSILON = 0.015625;

    // =========================================================================
    // HOVER MODE — Velocite
    // =========================================================================

    /**
     * Calcule la velocite en mode HOVER.
     * 4 directions relatives a l'entite (WASD), friction glace, gravite reduite.
     *
     * @param forward     Input avant/arriere (W/S) normalise [-1, 1]
     * @param strafe      Input gauche/droite (A/D) normalise [-1, 1]
     * @param currentVel  Velocite locale precedente
     * @param onGround    Si l'entite touche le sol
     * @param settings    Constantes physiques
     * @return Nouvelle velocite en espace local
     */
    public static Vec3 calculateHoverVelocity(
            float forward, float strafe, Vec3 currentVel,
            boolean onGround, HoverbikeSettings settings
    ) {
        double vx = currentVel.x;
        double vy = currentVel.y;
        double vz = currentVel.z;

        double accel = settings.hoverAcceleration();
        double maxSpeed = settings.maxHoverSpeed();
        double friction = settings.hoverFriction();

        // Acceleration basee sur input
        boolean hasInput = Math.abs(forward) > 0.01f || Math.abs(strafe) > 0.01f;

        if (hasInput) {
            // Ajouter acceleration dans la direction input
            vz += forward * accel;
            vx += strafe * accel;

            // Clamp vitesse horizontale a maxHoverSpeed
            double horizSpeed = Math.sqrt(vx * vx + vz * vz);
            if (horizSpeed > maxSpeed) {
                double scale = maxSpeed / horizSpeed;
                vx *= scale;
                vz *= scale;
            }
        } else {
            // Friction glace : deceleration tres lente
            vx = applyFriction(vx, friction);
            vz = applyFriction(vz, friction);
        }

        // Gravite
        vy = applyGravity(vy, onGround, settings);

        return new Vec3(vx, vy, vz);
    }

    // =========================================================================
    // RUN MODE — Velocite
    // =========================================================================

    /**
     * Calcule la velocite en mode RUN.
     * Avant uniquement (Z), deceleration, frein, pas de strafe.
     *
     * @param forward     Input W/S normalise [-1, 1]
     * @param currentVel  Velocite locale precedente
     * @param onGround    Si l'entite touche le sol
     * @param settings    Constantes physiques
     * @return Nouvelle velocite en espace local
     */
    public static Vec3 calculateRunVelocity(
            float forward, Vec3 currentVel,
            boolean onGround, HoverbikeSettings settings
    ) {
        double vz = currentVel.z;
        double vy = currentVel.y;
        double maxSpeed = settings.maxRunSpeed();

        if (forward > 0) {
            // W: accelerer vers maxRunSpeed
            if (vz < maxSpeed) {
                vz += settings.runAcceleration();
                vz = Math.min(vz, maxSpeed);
            }
        } else if (forward < 0) {
            // S: freiner (brake)
            vz -= settings.brakeDeceleration();
            if (vz < 0) {
                vz = 0;
            }
        } else {
            // Pas d'input: deceleration naturelle
            vz -= settings.deceleration();
            if (vz < 0) {
                vz = 0;
            }
        }

        // Annuler progressivement le strafe residuel du hover
        double vx = currentVel.x * 0.9;
        if (Math.abs(vx) < 0.001) {
            vx = 0;
        }

        // Gravite
        vy = applyGravity(vy, onGround, settings);

        return new Vec3(vx, vy, vz);
    }

    // =========================================================================
    // ROTATION
    // =========================================================================

    /**
     * Calcule le delta de rotation (yaw) a appliquer ce tick.
     * En hover : vitesse constante (rotationSpeedMax).
     * En run : vitesse inversement proportionnelle a la speed.
     *
     * @param driverYaw   Yaw du joueur (direction du curseur)
     * @param entityYaw   Yaw actuel de l'entite
     * @param currentSpeed Vitesse forward actuelle (pour run)
     * @param mode        Mode courant
     * @param settings    Constantes
     * @return Delta yaw en degres a ajouter au yaw de l'entite
     */
    public static float calculateYawDelta(
            float driverYaw, float entityYaw,
            double currentSpeed, HoverbikeMode mode, HoverbikeSettings settings
    ) {
        // Difference angulaire normalisee [-180, 180]
        float diff = Mth.wrapDegrees(driverYaw - entityYaw);

        // Vitesse de rotation selon le mode
        double rotSpeed;
        if (mode == HoverbikeMode.RUN) {
            // Interpolation lineaire : plus la vitesse est haute, plus la rotation est lente
            double speedRatio = Mth.clamp(currentSpeed / settings.maxRunSpeed(), 0.0, 1.0);
            rotSpeed = Mth.lerp(speedRatio, settings.rotationSpeedMax(), settings.rotationSpeedMin());
        } else {
            rotSpeed = settings.rotationSpeedMax();
        }

        // Clamp le delta au max de rotation ET a la difference restante
        float maxDelta = (float) rotSpeed;
        return Mth.clamp(diff, -maxDelta, maxDelta);
    }

    // =========================================================================
    // MODE TRANSITIONS
    // =========================================================================

    /**
     * Verifie si on doit passer de HOVER a RUN.
     * Condition : sprint + W + vitesse forward > seuil.
     */
    public static boolean shouldTransitionToRun(
            float forward, boolean sprinting, double forwardSpeed, HoverbikeSettings settings
    ) {
        return sprinting && forward > 0 && forwardSpeed >= settings.runThresholdSpeed();
    }

    /**
     * Verifie si on doit passer de RUN a HOVER.
     * Condition : vitesse forward < seuil.
     */
    public static boolean shouldTransitionToHover(double forwardSpeed, HoverbikeSettings settings) {
        return forwardSpeed < settings.runThresholdSpeed();
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    /**
     * Convertit une velocite locale (Z=avant) en velocite monde via rotation yaw.
     * Pattern Cobblemon PokemonEntity.kt L1829-1835.
     */
    public static Vec3 localToWorld(Vec3 localVel, float entityYaw) {
        float yawRad = entityYaw * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);

        return new Vec3(
                localVel.x * cos - localVel.z * sin,
                localVel.y,
                localVel.z * cos + localVel.x * sin
        );
    }

    /**
     * Inverse de localToWorld : convertit une velocite monde en espace local.
     * Utilise apres collision pour synchroniser rideVelocity avec le mouvement reel.
     */
    public static Vec3 worldToLocal(Vec3 worldVel, float entityYaw) {
        float yawRad = entityYaw * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);

        return new Vec3(
                worldVel.x * cos + worldVel.z * sin,
                worldVel.y,
                worldVel.z * cos - worldVel.x * sin
        );
    }

    /**
     * Calcule la velocite Y pour le terrain following.
     * Interpole progressivement vers la hauteur de hover cible.
     * Montee rapide (franchir obstacles), descente lente (feeling hover).
     *
     * @param currentY    Position Y actuelle du bike
     * @param groundY     Hauteur du sol (via raycast)
     * @param hoverHeight Hauteur de hover au-dessus du sol
     * @param riseSpeed   Vitesse de montee max (blocs/tick)
     * @param fallSpeed   Vitesse de descente max (blocs/tick)
     * @return Velocite Y a appliquer pour se rapprocher du target
     */
    public static double calculateHoverY(
            double currentY, double groundY, double hoverHeight,
            double riseSpeed, double fallSpeed
    ) {
        double targetY = groundY + hoverHeight;
        double diff = targetY - currentY;
        if (Math.abs(diff) < 0.01) {
            return 0;
        }
        return diff > 0 ? Math.min(diff, riseSpeed) : Math.max(diff, -fallSpeed);
    }

    /**
     * Applique la gravite reduite.
     */
    private static double applyGravity(double vy, boolean onGround, HoverbikeSettings settings) {
        if (onGround) {
            return vy < 0 ? 0 : vy;
        }
        return Math.max(vy - settings.gravity(), -settings.terminalVelocity());
    }

    /**
     * Applique la friction (deceleration vers 0).
     */
    private static double applyFriction(double v, double friction) {
        if (Math.abs(v) < friction) {
            return 0;
        }
        return v - Math.signum(v) * friction;
    }
}
