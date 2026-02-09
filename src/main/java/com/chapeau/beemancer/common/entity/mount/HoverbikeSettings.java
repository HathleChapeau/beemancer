/**
 * ============================================================
 * [HoverbikeSettings.java]
 * Description: Constantes physiques du Hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoverbikePhysics.java: Tous les calculs physiques
 * - HoverbikeEntity.java: Reference aux settings
 * - HoverbikeDebugHud.java: Affichage des limites
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Record contenant toutes les constantes physiques du Hoverbike.
 * Une seule instance creee au demarrage, partagee par toutes les entites.
 */
public record HoverbikeSettings(
        // --- Vitesses ---
        double maxHoverSpeed,
        double maxRunSpeed,
        double runThresholdSpeed,

        // --- Acceleration ---
        double hoverAcceleration,
        double runAcceleration,
        double deceleration,
        double brakeDeceleration,
        double hoverFriction,

        // --- Gravite ---
        double gravity,
        double terminalVelocity,

        // --- Rotation (degres/tick) ---
        double rotationSpeedMax,
        double rotationSpeedMin,

        // --- Jauge d'envol ---
        double gaugeFillRate,
        double gaugeDrainRate,
        double liftSpeed
) {

    /**
     * Settings par defaut du Hoverbike.
     * Tous les parametres sont ajustables ici.
     */
    public static HoverbikeSettings createDefaults() {
        return new HoverbikeSettings(
                // Vitesses
                0.15,    // maxHoverSpeed (~3 blocs/sec)
                0.6,     // maxRunSpeed (~12 blocs/sec)
                0.1125,  // runThresholdSpeed (75% de maxHoverSpeed)

                // Acceleration
                0.008,   // hoverAcceleration
                0.012,   // runAcceleration
                0.003,   // deceleration (naturelle en run sans input)
                0.015,   // brakeDeceleration (S en run)
                0.002,   // hoverFriction (glace, tres faible)

                // Gravite
                0.025,   // gravity (reduite vs 0.08 normal)
                0.5,     // terminalVelocity

                // Rotation
                6.0,     // rotationSpeedMax (deg/tick a basse vitesse / hover)
                1.5,     // rotationSpeedMin (deg/tick a vitesse max run)

                // Jauge d'envol
                0.01,    // gaugeFillRate (100 ticks = 5 sec pour remplir)
                0.0125,  // gaugeDrainRate (80 ticks = 4 sec de vol)
                0.06     // liftSpeed (montee douce, > gravity 0.025)
        );
    }
}
