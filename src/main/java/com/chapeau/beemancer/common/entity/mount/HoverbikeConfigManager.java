/**
 * ============================================================
 * [HoverbikeConfigManager.java]
 * Description: Chargement et gestion des configs JSON du hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeSettings   | Stats de base        | Charge depuis base_stats.json  |
 * | HoverbikeStatType   | Enum stats           | Mapping cles JSON              |
 * | HoverbikeStatObject | Modification stat    | Parsing stat_objects           |
 * | HoverbikeModifier   | Modifier complet     | Parsing statistics.json        |
 * | HoverbikeDefaultConfigs | Defaults V1      | Generation fichiers initiaux   |
 * | FMLPaths            | Chemin config        | Dossier config/beemancer/      |
 * | Gson                | Parsing JSON         | Lecture/ecriture fichiers      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Beemancer.java: Initialisation au demarrage
 * - HoverbikeEntity.java: Recuperation des stats de base
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire des fichiers de configuration JSON du hoverbike.
 * Gere 4 fichiers dans config/beemancer/hoverbike/:
 * - base_stats.json: statistiques de base
 * - tags.json: liste de tags pour les modifiers
 * - statistics.json: definitions des modifiers avec tiers T1-T8
 * - part_categories.json: categories de pieces et leurs stats associees
 */
public class HoverbikeConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HoverbikeConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int TIER_COUNT = 8;

    private static Path configDir;
    private static HoverbikeSettings baseStats = HoverbikeSettings.createDefaults();
    private static List<String> tags = new ArrayList<>();
    private static List<HoverbikeModifier> modifiers = new ArrayList<>();
    private static Map<String, List<String>> partCategories = new LinkedHashMap<>();

    /**
     * Initialise le dossier config et charge tous les fichiers.
     * Genere les fichiers par defaut avec contenu V1 s'ils n'existent pas.
     */
    public static void init() {
        configDir = FMLPaths.CONFIGDIR.get().resolve("beemancer").resolve("hoverbike");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create hoverbike config directory", e);
            return;
        }
        generateDefaultsIfMissing();
        loadAll();
    }

    private static void generateDefaultsIfMissing() {
        writeIfMissing("base_stats.json", GSON.toJson(HoverbikeSettings.createDefaults().toJsonObject()));
        writeIfMissing("tags.json", GSON.toJson(HoverbikeDefaultConfigs.defaultTags()));
        writeIfMissing("statistics.json", GSON.toJson(HoverbikeDefaultConfigs.defaultStatistics()));
        writeIfMissing("part_categories.json", GSON.toJson(HoverbikeDefaultConfigs.defaultPartCategories()));
    }

    private static void writeIfMissing(String filename, String content) {
        Path file = configDir.resolve(filename);
        if (Files.exists(file)) return;
        try {
            Files.writeString(file, content);
            LOGGER.info("Generated default hoverbike config: {}", filename);
        } catch (IOException e) {
            LOGGER.error("Failed to write default {}", filename, e);
        }
    }

    private static void loadAll() {
        loadBaseStats();
        loadTags();
        loadStatistics();
        loadPartCategories();
    }

    private static void loadBaseStats() {
        try {
            String content = Files.readString(configDir.resolve("base_stats.json"));
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            baseStats = HoverbikeSettings.fromJson(json);
            LOGGER.info("Loaded hoverbike base stats from config");
        } catch (Exception e) {
            LOGGER.error("Failed to load base_stats.json, using defaults", e);
            baseStats = HoverbikeSettings.createDefaults();
        }
    }

    private static void loadTags() {
        try {
            String content = Files.readString(configDir.resolve("tags.json"));
            JsonArray array = JsonParser.parseString(content).getAsJsonArray();
            tags = new ArrayList<>();
            for (JsonElement el : array) {
                tags.add(el.getAsString());
            }
            LOGGER.info("Loaded {} hoverbike tags", tags.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load tags.json", e);
            tags = new ArrayList<>();
        }
    }

    private static void loadStatistics() {
        try {
            String content = Files.readString(configDir.resolve("statistics.json"));
            JsonArray array = JsonParser.parseString(content).getAsJsonArray();
            modifiers = new ArrayList<>();
            for (JsonElement el : array) {
                modifiers.add(parseModifier(el.getAsJsonObject()));
            }
            LOGGER.info("Loaded {} hoverbike modifiers", modifiers.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load statistics.json", e);
            modifiers = new ArrayList<>();
        }
    }

    private static HoverbikeModifier parseModifier(JsonObject json) {
        String name = json.get("name").getAsString();
        boolean isPrefix = json.get("is_prefix").getAsBoolean();

        List<String> modTags = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("tags")) {
            modTags.add(el.getAsString());
        }

        int[] pools = new int[TIER_COUNT];
        for (int i = 1; i <= TIER_COUNT; i++) {
            String key = "T" + i + "_pool";
            pools[i - 1] = json.has(key) ? json.get(key).getAsInt() : 0;
        }

        List<HoverbikeStatObject> statObjects = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("stat_objects")) {
            statObjects.add(parseStatObject(el.getAsJsonObject()));
        }

        return new HoverbikeModifier(name, isPrefix, modTags, pools, statObjects);
    }

    private static HoverbikeStatObject parseStatObject(JsonObject json) {
        String statKey = json.get("statistic").getAsString();
        HoverbikeStatType stat = HoverbikeStatType.fromJsonKey(statKey);
        if (stat == null) {
            LOGGER.warn("Unknown hoverbike statistic '{}', defaulting to Hover_Max_Speed", statKey);
            stat = HoverbikeStatType.HOVER_MAX_SPEED;
        }
        String valueType = json.get("value_type").getAsString();

        double[][] ranges = new double[TIER_COUNT][2];
        for (int i = 1; i <= TIER_COUNT; i++) {
            String key = "T" + i;
            if (json.has(key)) {
                JsonArray arr = json.getAsJsonArray(key);
                ranges[i - 1][0] = arr.get(0).getAsDouble();
                ranges[i - 1][1] = arr.get(1).getAsDouble();
            }
        }

        return new HoverbikeStatObject(stat, valueType, ranges);
    }

    private static void loadPartCategories() {
        try {
            String content = Files.readString(configDir.resolve("part_categories.json"));
            JsonArray array = JsonParser.parseString(content).getAsJsonArray();
            partCategories = new LinkedHashMap<>();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String category = obj.get("category").getAsString();
                List<String> stats = new ArrayList<>();
                for (JsonElement s : obj.getAsJsonArray("stats")) {
                    stats.add(s.getAsString());
                }
                partCategories.put(category, stats);
            }
            LOGGER.info("Loaded {} hoverbike part categories", partCategories.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load part_categories.json", e);
            partCategories = new LinkedHashMap<>();
        }
    }

    // --- Getters ---

    public static HoverbikeSettings getBaseStats() {
        return baseStats;
    }

    public static List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public static List<HoverbikeModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    // --- Query ---

    /** Trouve un modifier par son nom exact, ou null. */
    public static HoverbikeModifier getModifier(String name) {
        for (HoverbikeModifier mod : modifiers) {
            if (mod.getName().equals(name)) return mod;
        }
        return null;
    }

    /** Retourne tous les modifiers qui possedent le tag donne. */
    public static List<HoverbikeModifier> getModifiersByTag(String tag) {
        List<HoverbikeModifier> result = new ArrayList<>();
        for (HoverbikeModifier mod : modifiers) {
            if (mod.getTags().contains(tag)) result.add(mod);
        }
        return result;
    }

    /** Retourne tous les prefixes. */
    public static List<HoverbikeModifier> getPrefixes() {
        List<HoverbikeModifier> result = new ArrayList<>();
        for (HoverbikeModifier mod : modifiers) {
            if (mod.isPrefix()) result.add(mod);
        }
        return result;
    }

    /** Retourne tous les suffixes. */
    public static List<HoverbikeModifier> getSuffixes() {
        List<HoverbikeModifier> result = new ArrayList<>();
        for (HoverbikeModifier mod : modifiers) {
            if (!mod.isPrefix()) result.add(mod);
        }
        return result;
    }

    /** Verifie si un tag existe dans la liste chargee. */
    public static boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /** Retourne toutes les categories de pieces avec leurs stats. */
    public static Map<String, List<String>> getPartCategories() {
        return Collections.unmodifiableMap(partCategories);
    }

    /** Retourne les noms de stats associes a une categorie, ou liste vide. */
    public static List<String> getCategoryStats(String category) {
        return partCategories.getOrDefault(category, List.of());
    }
}
