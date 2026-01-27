/**
 * ============================================================
 * [RideableBeeMovement.java]
 * Description: Calculs de vélocité et physique pour l'abeille chevauchable
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RidingMode          | Mode actuel          | Branche de calcul              |
 * | RidingSettings      | Paramètres           | Valeurs de vitesse/inertie     |
 * | RidingState         | État runtime         | Vitesse courante, yaw          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RideableBeeController.java: Calcul du mouvement à chaque tick
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Classe utilitaire pour les calculs de mouvement.
 * Tous les méthodes sont statiques et pures (pas d'effets de bord).
 */
public final class RideableBeeMovement {

    private RideableBeeMovement() {
        // Utility class
    }

    /**
     * Calcule la vélocité en mode WALK.
     * Mouvement libre dans toutes les directions selon l'orientation de la caméra.
     *
     * @param forward Input avant/arrière (-1 à 1)
     * @param strafe Input gauche/droite (-1 à 1)
     * @param cameraYaw Orientation de la caméra du joueur (degrés)
     * @param settings Paramètres de déplacement
     * @return Vecteur de vélocité en coordonnées monde
     */
    public static Vec3 calculateWalkVelocity(float forward, float strafe, float cameraYaw,
                                              RidingSettings settings) {
        // Pas d'input = pas de mouvement
        if (Math.abs(forward) < 0.01f && Math.abs(strafe) < 0.01f) {
            return Vec3.ZERO;
        }

        // Normaliser l'input pour éviter la diagonale plus rapide
        Vec3 inputVec = new Vec3(strafe, 0, forward);
        if (inputVec.lengthSqr() > 1.0) {
            inputVec = inputVec.normalize();
        }

        // Rotation selon le yaw de la caméra (en radians)
        float yawRad = (float) Math.toRadians(-cameraYaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double worldX = inputVec.x * cosYaw - inputVec.z * sinYaw;
        double worldZ = inputVec.x * sinYaw + inputVec.z * cosYaw;

        return new Vec3(worldX, 0, worldZ).scale(settings.walkSpeed());
    }

    /**
     * Calcule la vélocité en mode RUN.
     * Uniquement avant/arrière avec accélération/décélération.
     *
     * @param forward Input avant/arrière (-1 à 1)
     * @param settings Paramètres de déplacement
     * @param state État courant (vitesse, yaw)
     * @return Vecteur de vélocité en coordonnées monde
     */
    public static Vec3 calculateRunVelocity(float forward, RidingSettings settings, RidingState state) {
        float currentSpeed = state.getCurrentSpeed();
        float currentYaw = state.getCurrentYaw();

        // Direction avant basée sur le yaw de l'entité
        float yawRad = (float) Math.toRadians(-currentYaw);
        double dirX = Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);

        return new Vec3(dirX * currentSpeed, 0, dirZ * currentSpeed);
    }

    /**
     * Met à jour la vitesse en mode RUN avec accélération/décélération.
     *
     * @param forward Input avant/arrière (-1 à 1)
     * @param currentSpeed Vitesse actuelle
     * @param settings Paramètres de déplacement
     * @return Nouvelle vitesse
     */
    public static float updateRunSpeed(float forward, float currentSpeed, RidingSettings settings) {
        if (forward > 0.1f) {
            // Accélération (touche avant enfoncée)
            return Math.min(currentSpeed + settings.acceleration(), settings.maxRunSpeed());
        } else if (forward < -0.1f) {
            // Décélération active (touche arrière enfoncée)
            return Math.max(currentSpeed - settings.deceleration(), 0f);
        } else {
            // Décélération passive (aucune touche ou touche relâchée)
            // Décélère plus lentement que la décélération active
            return Math.max(currentSpeed - settings.deceleration() * 0.7f, 0f);
        }
    }

    /**
     * Met à jour la rotation avec inertie (pour mode RUN).
     *
     * @param strafe Input gauche/droite (-1 à 1)
     * @param currentYaw Yaw actuel
     * @param targetYaw Yaw cible
     * @param turnInertia Facteur d'inertie (0-1)
     * @param turnRate Vitesse de rotation de base (degrés/tick)
     * @return Nouveau yaw
     */
    public static float updateYawWithInertia(float strafe, float currentYaw, float targetYaw,
                                              float turnInertia, float turnRate) {
        // Mettre à jour le target yaw selon l'input
        float newTargetYaw = targetYaw - strafe * turnRate;

        // Interpoler vers le target avec inertie
        float yawDiff = Mth.wrapDegrees(newTargetYaw - currentYaw);
        float newYaw = currentYaw + yawDiff * turnInertia;

        return Mth.wrapDegrees(newYaw);
    }

    /**
     * Calcule le vecteur de bond (leap) pour le mode RUN.
     *
     * @param entityYaw Direction de l'entité
     * @param leapForce Force du bond
     * @return Vecteur de vélocité pour le bond
     */
    public static Vec3 calculateLeapVelocity(float entityYaw, float leapForce) {
        float yawRad = (float) Math.toRadians(-entityYaw);
        double dirX = Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);

        // Composante horizontale + verticale (0.5 de la force vers le haut)
        return new Vec3(
            dirX * leapForce,
            leapForce * 0.5,
            dirZ * leapForce
        );
    }

    /**
     * Calcule le vecteur de saut vertical (mode WALK).
     *
     * @param jumpStrength Force du saut
     * @return Vecteur de vélocité pour le saut
     */
    public static Vec3 calculateWalkJump(float jumpStrength) {
        return new Vec3(0, jumpStrength, 0);
    }

    /**
     * Vérifie si la vitesse est suffisante pour rester en mode RUN.
     */
    public static boolean shouldExitRunMode(float currentSpeed, RidingSettings settings) {
        return currentSpeed < settings.runToWalkThreshold();
    }

    /**
     * Vérifie si les conditions sont réunies pour entrer en mode RUN.
     * On entre en RUN si le joueur avance (forward > 0.5) ET sprint est pressé.
     *
     * @param forward Input avant (-1 à 1)
     * @param sprintPressed Sprint pressé (Ctrl ou double-tap W)
     * @param settings Non utilisé mais gardé pour cohérence API
     */
    public static boolean canEnterRunMode(float forward, boolean sprintPressed, RidingSettings settings) {
        return sprintPressed && forward > 0.5f;
    }
}
