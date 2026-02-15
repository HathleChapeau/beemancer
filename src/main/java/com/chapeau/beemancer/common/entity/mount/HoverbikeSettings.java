/**
 * ============================================================
 * [HoverbikeSettings.java]
 * Description: Constantes physiques du Hoverbike (chargees depuis config JSON)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeStatType   | Enum stats           | Cles JSON pour serialisation   |
 * | Gson (JsonObject)   | Serialisation        | toJson/fromJson                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePhysics.java: Tous les calculs physiques
 * - HoverbikeEntity.java: Reference aux settings
 * - HoverbikeDebugHud.java: Affichage des limites
 * - HoverbikeConfigManager.java: Chargement depuis base_stats.json
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import com.google.gson.JsonObject;

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
     * Settings par defaut du Hoverbike (valeurs hardcodees de fallback).
     */
    public static HoverbikeSettings createDefaults() {
        return new HoverbikeSettings(
                0.15,    // maxHoverSpeed (~3 blocs/sec)
                0.6,     // maxRunSpeed (~12 blocs/sec)
                0.1125,  // runThresholdSpeed (75% de maxHoverSpeed)
                0.008,   // hoverAcceleration
                0.012,   // runAcceleration
                0.003,   // deceleration (naturelle en run sans input)
                0.015,   // brakeDeceleration (S en run)
                0.002,   // hoverFriction (glace, tres faible)
                0.025,   // gravity (reduite vs 0.08 normal)
                0.5,     // terminalVelocity
                6.0,     // rotationSpeedMax (deg/tick a basse vitesse / hover)
                1.5,     // rotationSpeedMin (deg/tick a vitesse max run)
                0.01,    // gaugeFillRate (100 ticks = 5 sec pour remplir)
                0.0125,  // gaugeDrainRate (80 ticks = 4 sec de vol)
                0.06     // liftSpeed (montee douce, > gravity 0.025)
        );
    }

    /**
     * Serialise les settings en JsonObject pour base_stats.json.
     */
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty(HoverbikeStatType.HOVER_MAX_SPEED.getJsonKey(), maxHoverSpeed);
        json.addProperty(HoverbikeStatType.RUN_MAX_SPEED.getJsonKey(), maxRunSpeed);
        json.addProperty(HoverbikeStatType.RUN_THRESHOLD_SPEED.getJsonKey(), runThresholdSpeed);
        json.addProperty(HoverbikeStatType.HOVER_ACCELERATION.getJsonKey(), hoverAcceleration);
        json.addProperty(HoverbikeStatType.RUN_ACCELERATION.getJsonKey(), runAcceleration);
        json.addProperty(HoverbikeStatType.DECELERATION.getJsonKey(), deceleration);
        json.addProperty(HoverbikeStatType.BRAKE_DECELERATION.getJsonKey(), brakeDeceleration);
        json.addProperty(HoverbikeStatType.HOVER_FRICTION.getJsonKey(), hoverFriction);
        json.addProperty(HoverbikeStatType.GRAVITY.getJsonKey(), gravity);
        json.addProperty(HoverbikeStatType.TERMINAL_VELOCITY.getJsonKey(), terminalVelocity);
        json.addProperty(HoverbikeStatType.ROTATION_SPEED_MAX.getJsonKey(), rotationSpeedMax);
        json.addProperty(HoverbikeStatType.ROTATION_SPEED_MIN.getJsonKey(), rotationSpeedMin);
        json.addProperty(HoverbikeStatType.GAUGE_FILL_RATE.getJsonKey(), gaugeFillRate);
        json.addProperty(HoverbikeStatType.GAUGE_DRAIN_RATE.getJsonKey(), gaugeDrainRate);
        json.addProperty(HoverbikeStatType.LIFT_SPEED.getJsonKey(), liftSpeed);
        return json;
    }

    /**
     * Cree un HoverbikeSettings depuis un JsonObject (base_stats.json).
     * Utilise les valeurs par defaut pour les cles manquantes.
     */
    public static HoverbikeSettings fromJson(JsonObject json) {
        HoverbikeSettings defaults = createDefaults();
        return new HoverbikeSettings(
                getOr(json, HoverbikeStatType.HOVER_MAX_SPEED, defaults.maxHoverSpeed()),
                getOr(json, HoverbikeStatType.RUN_MAX_SPEED, defaults.maxRunSpeed()),
                getOr(json, HoverbikeStatType.RUN_THRESHOLD_SPEED, defaults.runThresholdSpeed()),
                getOr(json, HoverbikeStatType.HOVER_ACCELERATION, defaults.hoverAcceleration()),
                getOr(json, HoverbikeStatType.RUN_ACCELERATION, defaults.runAcceleration()),
                getOr(json, HoverbikeStatType.DECELERATION, defaults.deceleration()),
                getOr(json, HoverbikeStatType.BRAKE_DECELERATION, defaults.brakeDeceleration()),
                getOr(json, HoverbikeStatType.HOVER_FRICTION, defaults.hoverFriction()),
                getOr(json, HoverbikeStatType.GRAVITY, defaults.gravity()),
                getOr(json, HoverbikeStatType.TERMINAL_VELOCITY, defaults.terminalVelocity()),
                getOr(json, HoverbikeStatType.ROTATION_SPEED_MAX, defaults.rotationSpeedMax()),
                getOr(json, HoverbikeStatType.ROTATION_SPEED_MIN, defaults.rotationSpeedMin()),
                getOr(json, HoverbikeStatType.GAUGE_FILL_RATE, defaults.gaugeFillRate()),
                getOr(json, HoverbikeStatType.GAUGE_DRAIN_RATE, defaults.gaugeDrainRate()),
                getOr(json, HoverbikeStatType.LIFT_SPEED, defaults.liftSpeed())
        );
    }

    private static double getOr(JsonObject json, HoverbikeStatType stat, double defaultValue) {
        String key = stat.getJsonKey();
        return json.has(key) ? json.get(key).getAsDouble() : defaultValue;
    }
}
