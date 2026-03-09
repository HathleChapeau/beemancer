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
package com.chapeau.apica.common.entity.mount;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Construit les JsonArray par defaut pour tags.json, statistics.json et part_categories.json.
 * Les valeurs sont des placeholders equilibres, ajustables via les fichiers config.
 */
public class HoverbikeDefaultConfigs {

    static JsonArray defaultPartCategories() {
        JsonArray a = new JsonArray();
        a.add(cat("saddle", "Hover_Max_Speed", "Run_Max_Speed"));
        a.add(cat("wing_protector", "Hover_Acceleration", "Gravity"));
        a.add(cat("control_left", "Rotation_Speed_Max", "Rotation_Speed_Min"));
        a.add(cat("control_right", "Gauge_Fill_Rate", "Gauge_Drain_Rate"));
        return a;
    }

    private static JsonObject cat(String category, String... stats) {
        JsonObject o = new JsonObject();
        o.addProperty("category", category);
        JsonArray arr = new JsonArray();
        for (String s : stats) arr.add(s);
        o.add("stats", arr);
        return o;
    }

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

    /**
     * Base stats par variant pour chaque catégorie de pièce.
     * Chaque variant peut avoir des types et valeurs de stats totalement différents.
     */
    static JsonObject defaultPartBaseStats() {
        JsonObject root = new JsonObject();
        root.add("saddle", variantArray(
                variant(0, "Hover_Max_Speed", 0.15, "Run_Max_Speed", 0.60),
                variant(1, "Hover_Max_Speed", -0.005, "Run_Max_Speed", 0.01),
                variant(2, "Hover_Max_Speed", 0.01, "Run_Max_Speed", -0.005)
        ));
        root.add("wing_protector", variantArray(
                variant(0, "Hover_Acceleration", 0.004, "Gravity", 0.0),
                variant(1, "Hover_Acceleration", -0.001, "Gravity", -0.005),
                variant(2, "Hover_Acceleration", 0.002, "Gravity", 0.002)
        ));
        root.add("control_left", variantArray(
                variant(0, "Rotation_Speed_Max", 0.0, "Rotation_Speed_Min", 0.0),
                variant(1, "Rotation_Speed_Max", -0.2, "Rotation_Speed_Min", 0.1),
                variant(2, "Rotation_Speed_Max", 0.3, "Rotation_Speed_Min", -0.05)
        ));
        root.add("control_right", variantArray(
                variant(0, "Gauge_Fill_Rate", 0.0, "Gauge_Drain_Rate", 0.0),
                variant(1, "Gauge_Fill_Rate", 0.002, "Gauge_Drain_Rate", -0.001),
                variant(2, "Gauge_Fill_Rate", -0.001, "Gauge_Drain_Rate", 0.002)
        ));
        return root;
    }

    private static JsonArray variantArray(JsonObject... variants) {
        JsonArray a = new JsonArray();
        for (JsonObject v : variants) a.add(v);
        return a;
    }

    private static JsonObject variant(int index, String stat1, double val1, String stat2, double val2) {
        JsonObject o = new JsonObject();
        o.addProperty("variant", index);
        o.addProperty("base_stat_1", stat1);
        o.addProperty("base_stat_1_value", val1);
        o.addProperty("base_stat_2", stat2);
        o.addProperty("base_stat_2_value", val2);
        return o;
    }
}
