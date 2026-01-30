/**
 * ============================================================
 * [BeeBehaviorManager.java]
 * Description: Gestionnaire de configuration des comportements d'abeilles
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.google.gson.JsonArray;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire central pour les configurations de comportement des abeilles.
 * Charge les donnees depuis data/beemancer/behavior/species_behaviors.json
 */
public class BeeBehaviorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeeBehaviorManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "behavior/species_behaviors.json");

    private static final Map<String, BeeBehaviorConfig> configs = new HashMap<>();
    private static BeeBehaviorConfig defaultConfig = new BeeBehaviorConfig();
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
        configs.clear();
        defaultConfig = new BeeBehaviorConfig();

        try {
            Optional<Resource> resource = resourceManager.getResource(CONFIG_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("Could not find behavior config at {}, using defaults", CONFIG_PATH);
                setupDefaults();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                // Charger les defaults
                if (root.has("defaults")) {
                    parseConfig(root.getAsJsonObject("defaults"), defaultConfig);
                }

                // Charger les configs par espece
                if (root.has("species")) {
                    JsonObject speciesObj = root.getAsJsonObject("species");
                    for (Map.Entry<String, JsonElement> entry : speciesObj.entrySet()) {
                        String speciesId = entry.getKey();
                        JsonObject speciesConfig = entry.getValue().getAsJsonObject();

                        BeeBehaviorConfig config = BeeBehaviorConfig.createWithDefaults(defaultConfig);
                        parseConfig(speciesConfig, config);

                        configs.put(speciesId, config);
                        LOGGER.debug("Loaded behavior config for species: {}", speciesId);
                    }
                }

                loaded = true;
                LOGGER.info("Loaded {} bee behavior configs", configs.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load behavior configs", e);
            setupDefaults();
        }
    }

    /**
     * Configure les valeurs par defaut si le fichier JSON n'est pas disponible.
     */
    private static void setupDefaults() {
        configs.clear();
        defaultConfig = new BeeBehaviorConfig();
        defaultConfig.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 1, 80));

        // Config par defaut pour meadow (Tier I de base)
        BeeBehaviorConfig meadowConfig = new BeeBehaviorConfig();
        meadowConfig.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 1, 80));
        configs.put("meadow", meadowConfig);

        loaded = true;
        LOGGER.info("Using default behavior configs");
    }

    private static void parseConfig(JsonObject json, BeeBehaviorConfig config) {
        if (json.has("flyingSpeed")) {
            config.setFlyingSpeed(json.get("flyingSpeed").getAsDouble());
        }
        if (json.has("enragedFlyingSpeed")) {
            config.setEnragedFlyingSpeed(json.get("enragedFlyingSpeed").getAsDouble());
        }
        if (json.has("health")) {
            config.setHealth(json.get("health").getAsDouble());
        }
        if (json.has("attackDamage")) {
            config.setAttackDamage(json.get("attackDamage").getAsDouble());
        }
        if (json.has("regenerationRate")) {
            config.setRegenerationRate(json.get("regenerationRate").getAsInt());
        }
        if (json.has("aggressiveToPlayers")) {
            config.setAggressiveToPlayers(json.get("aggressiveToPlayers").getAsBoolean());
        }
        if (json.has("aggressiveToPassiveMobs")) {
            config.setAggressiveToPassiveMobs(json.get("aggressiveToPassiveMobs").getAsBoolean());
        }
        if (json.has("aggressiveToHostileMobs")) {
            config.setAggressiveToHostileMobs(json.get("aggressiveToHostileMobs").getAsBoolean());
        }
        if (json.has("restCooldownMin")) {
            config.setRestCooldownMin(json.get("restCooldownMin").getAsInt());
        }
        if (json.has("restCooldownMax")) {
            config.setRestCooldownMax(json.get("restCooldownMax").getAsInt());
        }
        if (json.has("areaOfEffect")) {
            config.setAreaOfEffect(json.get("areaOfEffect").getAsInt());
        }
        if (json.has("foragingDuration")) {
            config.setForagingDuration(json.get("foragingDuration").getAsInt());
        }
        if (json.has("pollinationLoot")) {
            JsonArray lootArray = json.getAsJsonArray("pollinationLoot");
            for (JsonElement element : lootArray) {
                JsonObject lootObj = element.getAsJsonObject();
                String itemId = lootObj.get("item").getAsString();
                int minQty = lootObj.has("minQty") ? lootObj.get("minQty").getAsInt() : 1;
                int maxQty = lootObj.has("maxQty") ? lootObj.get("maxQty").getAsInt() : minQty;
                int chance = lootObj.has("chance") ? lootObj.get("chance").getAsInt() : 100;
                config.addLootEntry(new LootEntry(itemId, minQty, maxQty, chance));
            }
        }
    }

    /**
     * Recupere la configuration pour une espece donnee.
     * Si l'espece n'a pas de config explicite dans species_behaviors.json,
     * genere automatiquement une config a partir des donnees de BeeSpeciesManager.
     */
    public static BeeBehaviorConfig getConfig(String speciesId) {
        if (!loaded) {
            LOGGER.warn("BeeBehaviorManager not loaded, using defaults");
            setupDefaults();
        }

        BeeBehaviorConfig config = configs.get(speciesId);
        if (config != null) {
            return config;
        }

        // Auto-generer depuis les donnees d'espece
        config = generateConfigFromSpecies(speciesId);
        if (config != null) {
            configs.put(speciesId, config);
            return config;
        }

        return defaultConfig;
    }

    /**
     * Genere une BeeBehaviorConfig a partir des donnees de BeeSpeciesManager.
     * Utilise le champ lootItem de l'espece pour configurer le loot de pollinisation.
     */
    private static BeeBehaviorConfig generateConfigFromSpecies(String speciesId) {
        BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesId);
        if (speciesData == null) {
            return null;
        }

        BeeBehaviorConfig config = BeeBehaviorConfig.createWithDefaults(defaultConfig);

        // Appliquer les stats de l'espece
        config.setFlyingSpeed(BeeSpeciesManager.calculateStat(speciesId, BeeSpeciesManager.StatType.FLYING_SPEED));
        config.setHealth(BeeSpeciesManager.calculateStat(speciesId, BeeSpeciesManager.StatType.HEALTH));
        config.setAttackDamage(BeeSpeciesManager.calculateStat(speciesId, BeeSpeciesManager.StatType.ATTACK));

        // Aggression
        config.setAggressiveToPlayers(speciesData.aggressiveToPlayers);
        config.setAggressiveToPassiveMobs(speciesData.aggressiveToPassiveMobs);
        config.setAggressiveToHostileMobs(speciesData.aggressiveToHostileMobs);

        // Configurer le loot a partir de l'item defini dans bee_species.json
        String lootItem = speciesData.lootItem;
        if (lootItem != null && !lootItem.isEmpty()) {
            if (lootItem.startsWith("#")) {
                // Loot base sur un tag (ex: "#minecraft:small_flowers")
                config.addLootEntry(new LootEntry(lootItem, 1, 1, 80));
            } else {
                config.addLootEntry(new LootEntry(lootItem, 1, 1, 80));
            }
        } else {
            config.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 1, 80));
        }

        LOGGER.debug("Auto-generated behavior config for species: {} (loot: {})", speciesId, lootItem);
        return config;
    }

    /**
     * Recupere la configuration par defaut.
     */
    public static BeeBehaviorConfig getDefaultConfig() {
        if (!loaded) {
            setupDefaults();
        }
        return defaultConfig;
    }

    /**
     * Verifie si une configuration existe pour une espece.
     */
    public static boolean hasConfig(String speciesId) {
        if (!loaded) {
            setupDefaults();
        }
        return configs.containsKey(speciesId);
    }

    /**
     * Verifie si les configurations sont chargees.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
