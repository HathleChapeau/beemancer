/**
 * ============================================================
 * [InjectionConfigManager.java]
 * Description: Gestionnaire de configuration pour le systeme d'injection d'essence
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | Chemin des ressources          |
 * | Gson                | Parsing JSON         | Lecture des fichiers config    |
 * | EssenceValue        | Donnees essence      | Stockage valeurs par essence   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Apica.java (chargement au demarrage serveur)
 * - InjectorBlockEntity.java (lecture config pendant processing)
 *
 * ============================================================
 */
package com.chapeau.apica.core.config;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.essence.EssenceItem;
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
 * Charge et stocke la configuration du systeme d'injection d'essence.
 * Deux fichiers JSON: injection_config.json (global) et essence_values.json (par essence).
 */
public class InjectionConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionConfigManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "config/injection_config.json");
    private static final ResourceLocation VALUES_PATH = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "config/essence_values.json");

    private static int maxHunger = 500;
    private static int pointsPerLevel = 50;
    private static int processTimeTicks = 40;
    private static final Map<String, EssenceValue> essenceValues = new HashMap<>();
    private static boolean loaded = false;

    public static void load(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Server is null, using default injection config");
            setupDefaults();
            return;
        }
        load(server.getResourceManager());
    }

    public static void load(ResourceManager resourceManager) {
        essenceValues.clear();
        loadGlobalConfig(resourceManager);
        loadEssenceValues(resourceManager);
        loaded = true;
        LOGGER.info("Loaded injection config: maxHunger={}, pointsPerLevel={}, processTime={}, essences={}",
                maxHunger, pointsPerLevel, processTimeTicks, essenceValues.size());
    }

    private static void loadGlobalConfig(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(CONFIG_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("injection_config.json not found, using defaults");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                maxHunger = root.has("max_hunger") ? root.get("max_hunger").getAsInt() : 500;
                pointsPerLevel = root.has("points_per_level") ? root.get("points_per_level").getAsInt() : 50;
                processTimeTicks = root.has("process_time_ticks") ? root.get("process_time_ticks").getAsInt() : 40;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load injection_config.json", e);
        }
    }

    private static void loadEssenceValues(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(VALUES_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("essence_values.json not found, using defaults");
                setupDefaults();
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("essences")) {
                    JsonObject essences = root.getAsJsonObject("essences");
                    for (Map.Entry<String, JsonElement> entry : essences.entrySet()) {
                        JsonObject val = entry.getValue().getAsJsonObject();
                        int statPoints = val.has("stat_points") ? val.get("stat_points").getAsInt() : 5;
                        int hungerCost = val.has("hunger_cost") ? val.get("hunger_cost").getAsInt() : 10;
                        essenceValues.put(entry.getKey(), new EssenceValue(statPoints, hungerCost));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load essence_values.json", e);
            setupDefaults();
        }
    }

    private static void setupDefaults() {
        maxHunger = 500;
        pointsPerLevel = 50;
        processTimeTicks = 40;
        loaded = true;
    }

    // ========== API PUBLIQUE ==========

    public static EssenceValue getEssenceValue(EssenceItem.EssenceType type, EssenceItem.EssenceLevel level) {
        String key = type.name() + "_" + level.name();
        return essenceValues.getOrDefault(key, EssenceValue.DEFAULT);
    }

    public static int getMaxHunger() { return maxHunger; }
    public static int getPointsPerLevel() { return pointsPerLevel; }
    public static int getProcessTimeTicks() { return processTimeTicks; }
    public static boolean isLoaded() { return loaded; }
}
