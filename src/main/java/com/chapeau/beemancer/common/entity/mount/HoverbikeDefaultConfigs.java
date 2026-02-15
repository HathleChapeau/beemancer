/**
 * ============================================================
 * [HoverbikeDefaultConfigs.java]
 * Description: Generation des fichiers JSON par defaut du hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson (JsonObject)   | Construction JSON    | Build des defaults             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeConfigManager.java: Generation des configs si absentes
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Construit les JsonArray par defaut pour tags.json et statistics.json.
 * Les valeurs sont des placeholders equilibres, ajustables via les fichiers config.
 */
public class HoverbikeDefaultConfigs {

    static JsonArray defaultTags() {
        JsonArray a = new JsonArray();
        for (String t : new String[]{"speed", "acceleration", "handling", "flight", "engine", "weight", "aerodynamics"}) {
            a.add(t);
        }
        return a;
    }

    static JsonArray defaultStatistics() {
        JsonArray a = new JsonArray();

        a.add(mod("Swift", true, new String[]{"speed"},
                new int[]{5, 10, 20, 30, 45, 60, 80, 100},
                stat("Hover_Max_Speed", "+", tiers(0.065, 0.08, 0.05, 0.065, 0.04, 0.05, 0.03, 0.04, 0.02, 0.03, 0.015, 0.02, 0.01, 0.015, 0.005, 0.01)),
                stat("Run_Max_Speed", "+", tiers(0.15, 0.20, 0.12, 0.15, 0.09, 0.12, 0.07, 0.09, 0.05, 0.07, 0.035, 0.05, 0.02, 0.035, 0.01, 0.02))
        ));

        a.add(mod("Turbocharged", true, new String[]{"engine", "acceleration"},
                new int[]{5, 10, 20, 30, 45, 60, 80, 100},
                stat("Hover_Acceleration", "+", tiers(0.010, 0.012, 0.008, 0.010, 0.006, 0.008, 0.005, 0.006, 0.004, 0.005, 0.003, 0.004, 0.002, 0.003, 0.001, 0.002)),
                stat("Run_Acceleration", "+", tiers(0.012, 0.015, 0.010, 0.012, 0.008, 0.010, 0.006, 0.008, 0.005, 0.006, 0.004, 0.005, 0.003, 0.004, 0.001, 0.003))
        ));

        a.add(mod("Agile", true, new String[]{"handling"},
                new int[]{8, 15, 25, 35, 50, 65, 80, 100},
                stat("Rotation_Speed_Max", "+", tiers(3.0, 3.5, 2.5, 3.0, 2.0, 2.5, 1.6, 2.0, 1.2, 1.6, 0.8, 1.2, 0.5, 0.8, 0.2, 0.5)),
                stat("Rotation_Speed_Min", "+", tiers(1.5, 2.0, 1.2, 1.5, 1.0, 1.2, 0.8, 1.0, 0.6, 0.8, 0.4, 0.6, 0.2, 0.4, 0.1, 0.2))
        ));

        a.add(mod("Soaring", true, new String[]{"flight"},
                new int[]{5, 10, 20, 30, 45, 60, 80, 100},
                stat("Gauge_Fill_Rate", "%", tiers(25, 30, 20, 25, 16, 20, 12, 16, 8, 12, 5, 8, 3, 5, 1, 3)),
                stat("Lift_Speed", "+", tiers(0.04, 0.05, 0.03, 0.04, 0.025, 0.03, 0.02, 0.025, 0.015, 0.02, 0.01, 0.015, 0.005, 0.01, 0.002, 0.005))
        ));

        a.add(mod("of Stability", false, new String[]{"weight", "aerodynamics"},
                new int[]{8, 15, 25, 35, 50, 65, 80, 100},
                stat("Gravity", "%", tiers(-25, -20, -20, -16, -16, -12, -12, -8, -8, -5, -5, -3, -3, -2, -2, -1)),
                stat("Hover_Friction", "%", tiers(-30, -25, -25, -20, -20, -16, -16, -12, -12, -8, -8, -5, -5, -3, -3, -1))
        ));

        a.add(mod("of Precision", false, new String[]{"handling", "acceleration"},
                new int[]{8, 15, 25, 35, 50, 65, 80, 100},
                stat("Brake_Deceleration", "+", tiers(0.012, 0.015, 0.010, 0.012, 0.008, 0.010, 0.006, 0.008, 0.005, 0.006, 0.004, 0.005, 0.002, 0.004, 0.001, 0.002)),
                stat("Deceleration", "%", tiers(-25, -20, -20, -16, -16, -12, -12, -8, -8, -5, -5, -3, -3, -2, -2, -1))
        ));

        return a;
    }

    // --- Helpers compacts ---

    /** Construit un modifier JSON complet. */
    private static JsonObject mod(String name, boolean isPrefix, String[] tags, int[] pools, JsonObject... stats) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("is_prefix", isPrefix);
        JsonArray tagsArr = new JsonArray();
        for (String t : tags) tagsArr.add(t);
        o.add("tags", tagsArr);
        for (int i = 0; i < 8; i++) o.addProperty("T" + (i + 1) + "_pool", pools[i]);
        JsonArray statsArr = new JsonArray();
        for (JsonObject s : stats) statsArr.add(s);
        o.add("stat_objects", statsArr);
        return o;
    }

    /** Construit un stat_object JSON. */
    private static JsonObject stat(String statistic, String valueType, double[][] tierRanges) {
        JsonObject o = new JsonObject();
        o.addProperty("statistic", statistic);
        o.addProperty("value_type", valueType);
        for (int i = 0; i < 8; i++) {
            JsonArray arr = new JsonArray();
            arr.add(tierRanges[i][0]);
            arr.add(tierRanges[i][1]);
            o.add("T" + (i + 1), arr);
        }
        return o;
    }

    /** Convertit 16 doubles (T1min,T1max,...,T8min,T8max) en double[8][2]. */
    private static double[][] tiers(double... vals) {
        double[][] r = new double[8][2];
        for (int i = 0; i < 8; i++) {
            r[i][0] = vals[i * 2];
            r[i][1] = vals[i * 2 + 1];
        }
        return r;
    }
}
