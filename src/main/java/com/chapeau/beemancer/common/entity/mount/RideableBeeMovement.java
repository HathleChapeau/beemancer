/**
 * ============================================================
 * [RideableBeeMovement.java]
 * Description: Calculs de vélocité et physique pour RideableBee
 * ============================================================
 *
 * PATTERN: Basé sur Cobblemon HorseBehaviour.kt + JetBehaviour.kt
 * - Vélocité en espace local (Z = avant, X = côté, Y = vertical)
 * - Conversion local→world basée sur yaw de l'entité
 *
 * UTILISÉ PAR:
 * - RideableBeeEntity.java: Calculs de mouvement
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Utilitaires statiques pour les calculs de mouvement de RideableBee.
 * Séparé de l'entité pour clarté et réutilisabilité.
 */
public final class RideableBeeMovement {

    // Constantes physiques (Cobblemon HorseBehaviour.kt lignes 340, 352)
    public static final double GRAVITY = (9.8 / 20.0) * 0.2 * 0.6;
    public static final double TERMINAL_VELOCITY = 2.0;
    public static final double GROUND_FRICTION = 0.03;

    private RideableBeeMovement() {} // Pas d'instanciation

    /**
     * Calcule la vélocité en mode WALK.
     * Mouvement libre basé sur la direction de la caméra du joueur.
     *
     * @param driver Le joueur qui contrôle
     * @param forward Input avant/arrière (W/S)
     * @param strafe Input gauche/droite (A/D)
     * @param currentVelY Vélocité Y actuelle (pour gravité)
     * @param onGround Si l'entité est au sol
     * @param jumping Si le joueur appuie sur saut
     * @param jumpCooldown Cooldown de saut actuel
     * @param settings Paramètres de riding
     * @return Vec3 vélocité en coordonnées MONDE (pas local)
     */
    public static Vec3 calculateWalkVelocity(
            Player driver,
            float forward,
            float strafe,
            double currentVelY,
            boolean onGround,
            boolean jumping,
            int jumpCooldown,
            RidingSettings settings
    ) {
        // Direction basée sur caméra du joueur
        float cameraYaw = driver.getYRot();
        float yawRad = (float) Math.toRadians(-cameraYaw);

        // Calcul mouvement horizontal relatif à la caméra
        double moveX = (strafe * Math.cos(yawRad) - forward * Math.sin(yawRad)) * settings.walkSpeed();
        double moveZ = (strafe * Math.sin(yawRad) + forward * Math.cos(yawRad)) * settings.walkSpeed();

        // Gravité (Cobblemon ligne 339-345)
        double velY = currentVelY;
        if (!onGround && jumpCooldown <= 0) {
            velY = Math.max(velY - GRAVITY, -TERMINAL_VELOCITY);
        } else if (onGround) {
            velY = 0;
        }

        // Saut
        if (jumping && onGround && jumpCooldown >= 0) {
            velY = settings.walkJumpStrength();
        }

        return new Vec3(moveX, velY, moveZ);
    }

    /**
     * Calcule la vélocité en mode RUN (style Jet).
     * Accélération/décélération avec A/D pour tourner.
     *
     * @param forward Input avant/arrière (W/S)
     * @param currentSpeed Vitesse actuelle (Z local)
     * @param currentVelY Vélocité Y actuelle
     * @param onGround Si l'entité est au sol
     * @param jumping Si le joueur appuie sur saut
     * @param jumpCooldown Cooldown de saut actuel
     * @param sprinting Si le joueur sprinte
     * @param settings Paramètres de riding
     * @return Vec3 vélocité en espace LOCAL (Z = avant)
     */
    public static Vec3 calculateRunVelocity(
            float forward,
            double currentSpeed,
            double currentVelY,
            boolean onGround,
            boolean jumping,
            int jumpCooldown,
            boolean sprinting,
            RidingSettings settings
    ) {
        double topSpeed = settings.maxRunSpeed();
        double minSpeed = settings.walkSpeed() * 0.5;
        double accel = topSpeed / (settings.acceleration() * 20.0);
        double deccel = accel * 0.5;

        double speed = currentSpeed;

        // Vitesse (JetBehaviour lignes 232-255)
        if (speed < minSpeed) {
            // Toujours accélérer vers minSpeed
            double accelMod = Math.max(1.0 - scaleToRange(speed, minSpeed, topSpeed), 0.0);
            speed = Math.min(speed + (accel * accelMod), topSpeed);
        } else if (forward > 0 && speed < topSpeed) {
            // W: accélérer vers topSpeed
            double accelMod = Math.max(1.0 - scaleToRange(speed, minSpeed, topSpeed), 0.0);
            speed = Math.min(speed + (accel * accelMod), topSpeed);
        } else if (forward < 0 && speed > minSpeed) {
            // S: décélérer vers minSpeed
            speed = Math.max(speed - deccel, minSpeed);
        } else if (speed > topSpeed) {
            // Au-dessus de topSpeed: friction
            speed *= 0.98;
        }

        // Gravité
        double velY = currentVelY;
        if (!onGround && jumpCooldown <= 0) {
            velY = Math.max(velY - GRAVITY, -TERMINAL_VELOCITY);
        } else if (onGround) {
            velY = 0;
        }

        // Leap (saut en RUN)
        if (jumping && onGround && jumpCooldown >= 0) {
            velY = settings.runLeapForce();
        }

        // Vélocité en espace local (X=0, Z=avant)
        return new Vec3(0, velY, speed);
    }

    /**
     * Calcule la rotation en mode RUN (style Jet).
     * A/D contrôle directement le yaw.
     *
     * @param strafe Input gauche/droite (A/D)
     * @param handlingYaw Degrés par seconde de rotation max
     * @return Changement de yaw à appliquer
     */
    public static float calculateRunYawDelta(float strafe, double handlingYaw) {
        // JetBehaviour ligne 290: yawForce = driver.xxa * handlingYaw * -1
        return (float) (strafe * (handlingYaw / 20.0) * -1.0);
    }

    /**
     * Calcule le yaw cible en mode WALK basé sur la direction de mouvement.
     *
     * @param moveX Mouvement X en monde
     * @param moveZ Mouvement Z en monde
     * @return Yaw cible en degrés, ou NaN si pas de mouvement
     */
    public static float calculateWalkTargetYaw(double moveX, double moveZ) {
        if (Math.abs(moveX) > 0.01 || Math.abs(moveZ) > 0.01) {
            return (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        }
        return Float.NaN;
    }

    /**
     * Convertit une vélocité locale (Z=avant) en vélocité monde.
     * Basé sur Cobblemon PokemonEntity.kt lignes 1829-1835.
     *
     * @param localVel Vélocité en espace local
     * @param entityYaw Yaw de l'entité en degrés
     * @return Vélocité en coordonnées monde
     */
    public static Vec3 localToWorldVelocity(Vec3 localVel, float entityYaw) {
        float yawRad = (float) Math.toRadians(entityYaw);
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);

        // Rotation du vecteur autour de Y
        double worldX = localVel.x * cosYaw - localVel.z * sinYaw;
        double worldZ = localVel.z * cosYaw + localVel.x * sinYaw;

        return new Vec3(worldX, localVel.y, worldZ);
    }

    /**
     * Utilitaire: scale une valeur dans un range [0, 1].
     * Copié de Cobblemon RidingBehaviour.kt ligne 140-141.
     */
    public static double scaleToRange(double x, double min, double max) {
        if ((max - min) < 0.01) return 0.0;
        return Mth.clamp((x - min) / (max - min), 0.0, 1.0);
    }
}
