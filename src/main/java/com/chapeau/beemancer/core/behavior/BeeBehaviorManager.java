/**
 * ============================================================
 * [BeeBehaviorManager.java]
 * Description: Gestionnaire de configuration des comportements d'abeilles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeBehaviorConfig   | Config comportement  | Stockage par espèce            |
 * | BeeBehaviorType     | Type comportement    | Parsing depuis JSON            |
 * | LootEntry           | Entrée loot          | Parsing loots                  |
 * | SpeedThreshold      | Seuil vitesse        | Parsing seuils                 |
 * | Beemancer           | MOD_ID               | Chemin ressources              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java: Récupération config par espèce
 * - MagicHiveBlockEntity.java: Cooldowns et loot
 *
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
 * Charge les données depuis data/beemancer/behavior/species_behaviors.json
 */
public class BeeBehaviorManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BeeBehaviorManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "behavior/species_behaviors.json");
    
    private static final Map<String, BeeBehaviorConfig> configs = new HashMap<>();
    private static BeeBehaviorConfig defaultConfig = new BeeBehaviorConfig();
    private static BeeBehaviorConfig harvesterDefaults = new BeeBehaviorConfig();
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
        harvesterDefaults = new BeeBehaviorConfig();
        
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
                
                // Charger les harvester defaults
                if (root.has("harvesterDefaults")) {
                    parseConfig(root.getAsJsonObject("harvesterDefaults"), harvesterDefaults);
                }
                
                // Charger les configs par espèce
                if (root.has("species")) {
                    JsonObject speciesObj = root.getAsJsonObject("species");
                    for (Map.Entry<String, JsonElement> entry : speciesObj.entrySet()) {
                        String speciesId = entry.getKey();
                        JsonObject speciesConfig = entry.getValue().getAsJsonObject();
                        
                        BeeBehaviorConfig config = BeeBehaviorConfig.createWithDefaults(defaultConfig);
                        parseConfig(speciesConfig, config);
                        
                        // Si c'est un harvester, appliquer les defaults harvester
                        if (config.getBehaviorType() == BeeBehaviorType.HARVESTER) {
                            if (config.getHarvestingDuration() == defaultConfig.getHarvestingDuration()) {
                                config.setHarvestingDuration(harvesterDefaults.getHarvestingDuration());
                            }
                            if (config.getInventorySize() == defaultConfig.getInventorySize()) {
                                config.setInventorySize(harvesterDefaults.getInventorySize());
                            }
                            if (config.getReturnThreshold() == defaultConfig.getReturnThreshold()) {
                                config.setReturnThreshold(harvesterDefaults.getReturnThreshold());
                            }
                            if (config.getSpeedThresholds().isEmpty() && !harvesterDefaults.getSpeedThresholds().isEmpty()) {
                                for (SpeedThreshold threshold : harvesterDefaults.getSpeedThresholds()) {
                                    config.addSpeedThreshold(threshold);
                                }
                            }
                        }
                        
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
     * Configure les valeurs par défaut si le fichier JSON n'est pas disponible.
     */
    private static void setupDefaults() {
        configs.clear();
        defaultConfig = new BeeBehaviorConfig();
        
        // Config par défaut pour common
        BeeBehaviorConfig commonConfig = new BeeBehaviorConfig();
        commonConfig.setBehaviorType(BeeBehaviorType.FORAGER);
        commonConfig.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 1, 80));
        configs.put("common", commonConfig);
        
        // Config pour noble
        BeeBehaviorConfig nobleConfig = new BeeBehaviorConfig();
        nobleConfig.setBehaviorType(BeeBehaviorType.FORAGER);
        nobleConfig.setFlyingSpeed(0.5);
        nobleConfig.setHealth(12.0);
        nobleConfig.setForagingDuration(150);
        nobleConfig.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 2, 70));
        configs.put("noble", nobleConfig);
        
        // Config pour diligent
        BeeBehaviorConfig diligentConfig = new BeeBehaviorConfig();
        diligentConfig.setBehaviorType(BeeBehaviorType.FORAGER);
        diligentConfig.setFlyingSpeed(0.75);
        diligentConfig.setHealth(8.0);
        diligentConfig.setForagingDuration(60);
        diligentConfig.addLootEntry(new LootEntry("minecraft:honeycomb", 1, 1, 90));
        configs.put("diligent", diligentConfig);
        
        loaded = true;
        LOGGER.info("Using default behavior configs");
    }
    
    private static void parseConfig(JsonObject json, BeeBehaviorConfig config) {
        if (json.has("behaviorType")) {
            String type = json.get("behaviorType").getAsString();
            try {
                config.setBehaviorType(BeeBehaviorType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown behavior type: {}", type);
            }
        }
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
        if (json.has("harvestingDuration")) {
            config.setHarvestingDuration(json.get("harvestingDuration").getAsInt());
        }
        if (json.has("inventorySize")) {
            config.setInventorySize(json.get("inventorySize").getAsInt());
        }
        if (json.has("returnThreshold")) {
            config.setReturnThreshold(json.get("returnThreshold").getAsInt());
        }
        if (json.has("speedThresholds")) {
            JsonArray thresholdArray = json.getAsJsonArray("speedThresholds");
            for (JsonElement element : thresholdArray) {
                JsonObject thresholdObj = element.getAsJsonObject();
                int itemCount = thresholdObj.get("itemCount").getAsInt();
                double speedMultiplier = thresholdObj.get("speedMultiplier").getAsDouble();
                config.addSpeedThreshold(new SpeedThreshold(itemCount, speedMultiplier));
            }
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
     * Récupère la configuration pour une espèce donnée.
     * Retourne la config par défaut si l'espèce n'est pas trouvée.
     */
    public static BeeBehaviorConfig getConfig(String speciesId) {
        if (!loaded) {
            LOGGER.warn("BeeBehaviorManager not loaded, using defaults");
            setupDefaults();
        }
        return configs.getOrDefault(speciesId, defaultConfig);
    }
    
    /**
     * Récupère la configuration par défaut.
     */
    public static BeeBehaviorConfig getDefaultConfig() {
        if (!loaded) {
            setupDefaults();
        }
        return defaultConfig;
    }
    
    /**
     * Vérifie si une configuration existe pour une espèce.
     */
    public static boolean hasConfig(String speciesId) {
        if (!loaded) {
            setupDefaults();
        }
        return configs.containsKey(speciesId);
    }
    
    /**
     * Vérifie si les configurations sont chargées.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
