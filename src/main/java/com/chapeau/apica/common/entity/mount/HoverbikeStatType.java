/**
 * ============================================================
 * [HoverbikeStatType.java]
 * Description: Enum des statistiques configurables du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeStatObject.java: Reference la statistique ciblee
 * - HoverbikeConfigManager.java: Parsing JSON base_stats et statistics
 * - HoverbikeSettings.java: Mapping champ -> enum pour serialisation
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

/**
 * Enum de toutes les statistiques du hoverbike.
 * Chaque valeur correspond a une cle JSON dans base_stats.json
 * et a un champ du record HoverbikeSettings.
 */
public enum HoverbikeStatType {

    HOVER_MAX_SPEED("Hover_Max_Speed"),
    RUN_MAX_SPEED("Run_Max_Speed"),
    RUN_THRESHOLD_SPEED("Run_Threshold_Speed"),
    HOVER_ACCELERATION("Hover_Acceleration"),
    RUN_ACCELERATION("Run_Acceleration"),
    DECELERATION("Deceleration"),
    BRAKE_DECELERATION("Brake_Deceleration"),
    HOVER_FRICTION("Hover_Friction"),
    GRAVITY("Gravity"),
    TERMINAL_VELOCITY("Terminal_Velocity"),
    ROTATION_SPEED_MAX("Rotation_Speed_Max"),
    ROTATION_SPEED_MIN("Rotation_Speed_Min"),
    GAUGE_FILL_RATE("Gauge_Fill_Rate"),
    GAUGE_DRAIN_RATE("Gauge_Drain_Rate"),
    LIFT_SPEED("Lift_Speed");

    private final String jsonKey;

    HoverbikeStatType(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String getJsonKey() {
        return jsonKey;
    }

    /**
     * Trouve le type de stat a partir de sa cle JSON.
     * Retourne null si la cle ne correspond a aucune stat.
     */
    public static HoverbikeStatType fromJsonKey(String key) {
        for (HoverbikeStatType type : values()) {
            if (type.jsonKey.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
