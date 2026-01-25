/**
 * ============================================================
 * [BeeBehaviorManager.java]
 * Description: Gestionnaire de configuration des comportements d'abeilles
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

import com.chapeau.beemancer.Beemancer;
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
     * Retourne la config par defaut si l'espece n'est pas trouvee.
     */
    public static BeeBehaviorConfig getConfig(String speciesId) {
        if (!loaded) {
            LOGGER.warn("BeeBehaviorManager not loaded, using defaults");
            setupDefaults();
        }
        return configs.getOrDefault(speciesId, defaultConfig);
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
