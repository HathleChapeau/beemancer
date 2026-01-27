/**
 * ============================================================
 * [RidingSettingsLoader.java]
 * Description: Chargeur de configuration JSON pour les paramètres de montage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RidingSettings      | Record à créer       | Résultat du chargement         |
 * | Beemancer           | MOD_ID               | Chemin des ressources          |
 * | Gson                | Parsing JSON         | Lecture du fichier config      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java: Chargement au démarrage serveur
 * - RideableBeeEntity.java: Récupération des settings
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.Optional;

/**
 * Charge les paramètres de montage depuis data/beemancer/mount/rideable_bee.json.
 * Pattern similaire à BeeBehaviorManager.
 */
public class RidingSettingsLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RidingSettingsLoader.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "mount/rideable_bee.json");

    private static RidingSettings settings = RidingSettings.DEFAULT;
    private static boolean loaded = false;

    /**
     * Charge les settings depuis le serveur.
     */
    public static void load(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Server is null, using default riding settings");
            settings = RidingSettings.DEFAULT;
            loaded = true;
            return;
        }
        load(server.getResourceManager());
    }

    /**
     * Charge les settings depuis les ressources.
     */
    public static void load(ResourceManager resourceManager) {
        try {
            Optional<Resource> resource = resourceManager.getResource(CONFIG_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("Could not find riding config at {}, using defaults", CONFIG_PATH);
                settings = RidingSettings.DEFAULT;
                loaded = true;
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                settings = parseSettings(json);
                loaded = true;
                LOGGER.info("Loaded riding settings from {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load riding settings, using defaults", e);
            settings = RidingSettings.DEFAULT;
            loaded = true;
        }
    }

    /**
     * Parse les settings depuis le JSON.
     */
    private static RidingSettings parseSettings(JsonObject json) {
        return new RidingSettings(
            getFloat(json, "walkSpeed", RidingSettings.DEFAULT.walkSpeed()),
            getFloat(json, "maxRunSpeed", RidingSettings.DEFAULT.maxRunSpeed()),
            getFloat(json, "acceleration", RidingSettings.DEFAULT.acceleration()),
            getFloat(json, "deceleration", RidingSettings.DEFAULT.deceleration()),
            getFloat(json, "health", RidingSettings.DEFAULT.health()),
            getFloat(json, "walkJumpStrength", RidingSettings.DEFAULT.walkJumpStrength()),
            getFloat(json, "runLeapForce", RidingSettings.DEFAULT.runLeapForce()),
            getFloat(json, "turnInertia", RidingSettings.DEFAULT.turnInertia())
        );
    }

    /**
     * Récupère une valeur float du JSON avec fallback.
     */
    private static float getFloat(JsonObject json, String key, float defaultValue) {
        if (json.has(key)) {
            return json.get(key).getAsFloat();
        }
        return defaultValue;
    }

    /**
     * Récupère les settings chargés.
     */
    public static RidingSettings getSettings() {
        if (!loaded) {
            LOGGER.warn("RidingSettingsLoader not loaded, using defaults");
            return RidingSettings.DEFAULT;
        }
        return settings;
    }

    /**
     * Vérifie si les settings sont chargés.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
