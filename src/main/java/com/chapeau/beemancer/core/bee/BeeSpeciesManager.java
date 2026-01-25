/**
 * ============================================================
 * [BeeSpeciesManager.java]
 * Description: Gestionnaire des especes d'abeilles et leurs statistiques
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance           | Raison                | Utilisation                    |
 * |----------------------|----------------------|--------------------------------|
 * | Beemancer            | MOD_ID               | Chemin des ressources          |
 * | Gson                 | Parsing JSON         | Lecture des fichiers config    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java - Recuperation des stats des abeilles
 * - BreedingManager.java - Verification des parents valides
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.bee;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Gestionnaire central pour les especes d'abeilles.
 * Charge les donnees depuis data/beemancer/bees/bee_species.json et bee_stats.json
 */
public class BeeSpeciesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeeSpeciesManager.class);
    private static final ResourceLocation SPECIES_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "bees/bee_species.json");
    private static final ResourceLocation STATS_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "bees/bee_stats.json");

    private static final Map<String, BeeSpeciesData> species = new HashMap<>();
    private static final Map<String, Double> levelModifiers = new HashMap<>();
    private static BeeBaseStats baseStats = new BeeBaseStats();
    private static boolean loaded = false;

    /**
     * Charge les configurations depuis le serveur.
     */
    public static void load(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Server is null, using default configs");
            setupDefaults();
            return;
        }
        load(server.getResourceManager());
    }

    /**
     * Charge les configurations depuis les ressources.
     */
    public static void load(ResourceManager resourceManager) {
        species.clear();
        levelModifiers.clear();
        baseStats = new BeeBaseStats();

        // Charger les stats de base et modificateurs
        loadStats(resourceManager);

        // Charger les especes
        loadSpecies(resourceManager);

        loaded = true;
        LOGGER.info("Loaded {} bee species with {} level modifiers", species.size(), levelModifiers.size());
    }

    private static void loadStats(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(STATS_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("Could not find stats config at {}, using defaults", STATS_PATH);
                setupDefaultStats();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                // Charger les stats de base
                if (root.has("base_stats")) {
                    JsonObject stats = root.getAsJsonObject("base_stats");
                    baseStats = new BeeBaseStats(
                            stats.has("attack") ? stats.get("attack").getAsDouble() : 2.0,
                            stats.has("health") ? stats.get("health").getAsDouble() : 10.0,
                            stats.has("drop_rate") ? stats.get("drop_rate").getAsDouble() : 1.0,
                            stats.has("flying_speed") ? stats.get("flying_speed").getAsDouble() : 0.6,
                            stats.has("foraging_duration") ? stats.get("foraging_duration").getAsInt() : 100,
                            stats.has("tolerance") ? stats.get("tolerance").getAsInt() : 1
                    );
                }

                // Charger les modificateurs de niveau
                if (root.has("level_modifiers")) {
                    JsonObject modifiers = root.getAsJsonObject("level_modifiers");
                    for (Map.Entry<String, JsonElement> entry : modifiers.entrySet()) {
                        levelModifiers.put(entry.getKey(), entry.getValue().getAsDouble());
                    }
                }

                LOGGER.debug("Loaded base stats and {} level modifiers", levelModifiers.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load stats config", e);
            setupDefaultStats();
        }
    }

    private static void loadSpecies(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(SPECIES_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("Could not find species config at {}, using defaults", SPECIES_PATH);
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                if (root.has("species")) {
                    JsonObject speciesObj = root.getAsJsonObject("species");
                    for (Map.Entry<String, JsonElement> entry : speciesObj.entrySet()) {
                        String speciesId = entry.getKey();
                        JsonObject speciesJson = entry.getValue().getAsJsonObject();

                        BeeSpeciesData data = parseSpecies(speciesId, speciesJson);
                        species.put(speciesId, data);
                        LOGGER.debug("Loaded species: {}", speciesId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load species config", e);
        }
    }

    private static BeeSpeciesData parseSpecies(String id, JsonObject json) {
        BeeSpeciesData data = new BeeSpeciesData(id);

        data.tier = json.has("tier") ? json.get("tier").getAsString() : "I";
        data.environment = json.has("environment") ? json.get("environment").getAsInt() : 0;
        data.flowerType = json.has("flower_type") ? json.get("flower_type").getAsString() : "flower";
        data.dayNight = json.has("day_night") ? json.get("day_night").getAsString() : "day";
        data.texture = json.has("texture") ? json.get("texture").getAsString() : id + "_Bee";
        data.pollen = json.has("pollen") && !json.get("pollen").isJsonNull()
                ? json.get("pollen").getAsString() : null;

        // Parents
        if (json.has("parents") && !json.get("parents").isJsonNull()) {
            List<String> parents = new ArrayList<>();
            for (JsonElement parent : json.getAsJsonArray("parents")) {
                if (!parent.isJsonNull()) {
                    parents.add(parent.getAsString());
                }
            }
            data.parents = parents;
        }

        // Stats (niveaux 1-4)
        if (json.has("stats")) {
            JsonObject stats = json.getAsJsonObject("stats");
            data.attackLevel = stats.has("attack") ? stats.get("attack").getAsInt() : 1;
            data.healthLevel = stats.has("health") ? stats.get("health").getAsInt() : 1;
            data.dropLevel = stats.has("drop_level") ? stats.get("drop_level").getAsInt() : 1;
            data.flyingSpeedLevel = stats.has("flying_speed") ? stats.get("flying_speed").getAsInt() : 1;
            data.foragingDurationLevel = stats.has("foraging_duration") ? stats.get("foraging_duration").getAsInt() : 1;
            data.toleranceLevel = stats.has("tolerance") ? stats.get("tolerance").getAsInt() : 1;
        }

        // Aggression
        if (json.has("aggression")) {
            JsonObject aggro = json.getAsJsonObject("aggression");
            data.aggressiveToPlayers = aggro.has("players") && aggro.get("players").getAsBoolean();
            data.aggressiveToPassiveMobs = aggro.has("passive_mobs") && aggro.get("passive_mobs").getAsBoolean();
            data.aggressiveToHostileMobs = aggro.has("hostile_mobs") && aggro.get("hostile_mobs").getAsBoolean();
        }

        // Loot
        if (json.has("loot")) {
            JsonObject loot = json.getAsJsonObject("loot");
            data.lootItem = loot.has("item") ? loot.get("item").getAsString() : "minecraft:honeycomb";
            data.inComb = loot.has("in_comb") && loot.get("in_comb").getAsBoolean();
        }

        return data;
    }

    private static void setupDefaults() {
        setupDefaultStats();
        loaded = true;
    }

    private static void setupDefaultStats() {
        baseStats = new BeeBaseStats();
        levelModifiers.put("1", 1.0);
        levelModifiers.put("2", 1.2);
        levelModifiers.put("3", 1.5);
        levelModifiers.put("4", 2.0);
    }

    // ========== API PUBLIQUE ==========

    /**
     * Recupere les donnees d'une espece.
     */
    public static BeeSpeciesData getSpecies(String speciesId) {
        if (!loaded) {
            LOGGER.warn("BeeSpeciesManager not loaded");
            setupDefaults();
        }
        return species.get(speciesId);
    }

    /**
     * Verifie si une espece existe.
     */
    public static boolean hasSpecies(String speciesId) {
        if (!loaded) {
            setupDefaults();
        }
        return species.containsKey(speciesId);
    }

    /**
     * Recupere toutes les especes.
     */
    public static Collection<BeeSpeciesData> getAllSpecies() {
        if (!loaded) {
            setupDefaults();
        }
        return species.values();
    }

    /**
     * Recupere les IDs de toutes les especes.
     */
    public static Set<String> getAllSpeciesIds() {
        if (!loaded) {
            setupDefaults();
        }
        return species.keySet();
    }

    /**
     * Calcule la stat finale pour une espece et un type de stat donne.
     */
    public static double calculateStat(String speciesId, StatType statType) {
        BeeSpeciesData data = getSpecies(speciesId);
        if (data == null) {
            return getBaseStat(statType);
        }

        int level = data.getStatLevel(statType);
        double modifier = levelModifiers.getOrDefault(String.valueOf(level), 1.0);
        double baseStat = getBaseStat(statType);

        return baseStat * modifier;
    }

    /**
     * Recupere la stat de base sans modificateur.
     */
    public static double getBaseStat(StatType statType) {
        return switch (statType) {
            case ATTACK -> baseStats.attack;
            case HEALTH -> baseStats.health;
            case DROP_RATE -> baseStats.dropRate;
            case FLYING_SPEED -> baseStats.flyingSpeed;
            case FORAGING_DURATION -> baseStats.foragingDuration;
            case TOLERANCE -> baseStats.tolerance;
        };
    }

    /**
     * Recupere le modificateur pour un niveau donne.
     */
    public static double getLevelModifier(int level) {
        return levelModifiers.getOrDefault(String.valueOf(level), 1.0);
    }

    /**
     * Verifie si le manager est charge.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    // ========== CLASSES INTERNES ==========

    public enum StatType {
        ATTACK, HEALTH, DROP_RATE, FLYING_SPEED, FORAGING_DURATION, TOLERANCE
    }

    public static class BeeBaseStats {
        public final double attack;
        public final double health;
        public final double dropRate;
        public final double flyingSpeed;
        public final int foragingDuration;
        public final int tolerance;

        public BeeBaseStats() {
            this(2.0, 10.0, 1.0, 0.6, 100, 1);
        }

        public BeeBaseStats(double attack, double health, double dropRate,
                          double flyingSpeed, int foragingDuration, int tolerance) {
            this.attack = attack;
            this.health = health;
            this.dropRate = dropRate;
            this.flyingSpeed = flyingSpeed;
            this.foragingDuration = foragingDuration;
            this.tolerance = tolerance;
        }
    }

    public static class BeeSpeciesData {
        public final String id;
        public String tier = "I";
        public List<String> parents = null;
        public int environment = 0;
        public String flowerType = "flower";
        public String dayNight = "day";
        public String texture;
        public String pollen = null;

        // Stats (niveaux 1-4)
        public int attackLevel = 1;
        public int healthLevel = 1;
        public int dropLevel = 1;
        public int flyingSpeedLevel = 1;
        public int foragingDurationLevel = 1;
        public int toleranceLevel = 1;

        // Aggression
        public boolean aggressiveToPlayers = false;
        public boolean aggressiveToPassiveMobs = false;
        public boolean aggressiveToHostileMobs = false;

        // Loot
        public String lootItem = "minecraft:honeycomb";
        public boolean inComb = true;

        public BeeSpeciesData(String id) {
            this.id = id;
            this.texture = id.substring(0, 1).toUpperCase() + id.substring(1) + "_Bee";
        }

        public int getStatLevel(StatType statType) {
            return switch (statType) {
                case ATTACK -> attackLevel;
                case HEALTH -> healthLevel;
                case DROP_RATE -> dropLevel;
                case FLYING_SPEED -> flyingSpeedLevel;
                case FORAGING_DURATION -> foragingDurationLevel;
                case TOLERANCE -> toleranceLevel;
            };
        }

        public boolean canWorkDuringDay() {
            return "day".equals(dayNight) || "both".equals(dayNight);
        }

        public boolean canWorkDuringNight() {
            return "night".equals(dayNight) || "both".equals(dayNight);
        }
    }
}
