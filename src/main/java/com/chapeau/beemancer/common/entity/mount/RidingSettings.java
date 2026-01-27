/**
 * ============================================================
 * [RidingSettings.java]
 * Description: Record immutable contenant les paramètres de déplacement configurables
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | Record autonome      | -                              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingSettingsLoader.java: Création depuis JSON
 * - RideableBeeEntity.java: Stockage des settings
 * - RideableBeeMovement.java: Calculs de vélocité
 * - RideableBeeController.java: Transitions de mode
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Paramètres de configuration pour l'abeille chevauchable.
 * Immutable - chargé depuis JSON au démarrage.
 *
 * @param walkSpeed Vitesse de déplacement en mode WALK (blocs/tick)
 * @param maxRunSpeed Vitesse maximale en mode RUN (blocs/tick)
 * @param acceleration Taux d'accélération en mode RUN (par tick)
 * @param deceleration Taux de décélération en mode RUN (par tick)
 * @param health Points de vie de l'abeille
 * @param walkJumpStrength Force du saut en mode WALK
 * @param runLeapForce Force du bond en mode RUN
 * @param turnInertia Facteur d'inertie pour la rotation (0-1, plus bas = plus lent)
 */
public record RidingSettings(
    float walkSpeed,
    float maxRunSpeed,
    float acceleration,
    float deceleration,
    float health,
    float walkJumpStrength,
    float runLeapForce,
    float turnInertia
) {

    /**
     * Valeurs par défaut utilisées si le JSON n'est pas disponible.
     */
    public static final RidingSettings DEFAULT = new RidingSettings(
        0.15f,   // walkSpeed
        0.45f,   // maxRunSpeed
        0.02f,   // acceleration
        0.03f,   // deceleration
        20.0f,   // health
        0.5f,    // walkJumpStrength
        1.2f,    // runLeapForce
        0.15f    // turnInertia
    );

    /**
     * Seuil de vitesse minimum en mode RUN.
     * En dessous de ce seuil, l'abeille repasse en mode WALK.
     */
    public float runToWalkThreshold() {
        return walkSpeed * 0.8f;
    }

    /**
     * Seuil de vitesse pour passer en mode RUN.
     * Au-dessus de ce seuil + sprint, l'abeille peut passer en RUN.
     */
    public float walkToRunThreshold() {
        return walkSpeed * 1.2f;
    }
}
