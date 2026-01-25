/**
 * ============================================================
 * [BiomeTemperatureManager.java]
 * Description: Gestionnaire des temperatures des biomes pour les abeilles
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance           | Raison                | Utilisation                    |
 * |----------------------|----------------------|--------------------------------|
 * | Beemancer            | MOD_ID               | Chemin des ressources          |
 * | Gson                 | Parsing JSON         | Lecture des fichiers config    |
 * | BeeSpeciesManager    | Donnees especes      | Verification tolerance         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java - Verification compatibilite biome
 * - ForagingBehaviorGoal.java - Decision de travail
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.bee;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire des temperatures des biomes.
 * Charge les donnees depuis data/beemancer/bees/biomes.json
 * Temperature: -2 (frozen/end) a 2 (hot/nether)
 */
public class BiomeTemperatureManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeTemperatureManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "bees/biomes.json");

    private static final Map<String, Integer> biomeTemperatures = new HashMap<>();
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
        biomeTemperatures.clear();

        try {
            Optional<Resource> resource = resourceManager.getResource(CONFIG_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("Could not find biomes config at {}, using defaults", CONFIG_PATH);
                setupDefaults();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                if (root.has("temperature_levels")) {
                    JsonObject tempLevels = root.getAsJsonObject("temperature_levels");
                    for (Map.Entry<String, JsonElement> entry : tempLevels.entrySet()) {
                        int temperature = Integer.parseInt(entry.getKey());
                        JsonArray biomes = entry.getValue().getAsJsonArray();

                        for (JsonElement biomeElement : biomes) {
                            String biomeId = biomeElement.getAsString();
                            biomeTemperatures.put(biomeId, temperature);
                        }
                    }
                }

                loaded = true;
                LOGGER.info("Loaded temperature data for {} biomes", biomeTemperatures.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load biomes config", e);
            setupDefaults();
        }
    }

    private static void setupDefaults() {
        biomeTemperatures.clear();

        // Biomes chauds (2) - Nether
        biomeTemperatures.put("minecraft:nether_wastes", 2);
        biomeTemperatures.put("minecraft:basalt_deltas", 2);
        biomeTemperatures.put("minecraft:soul_sand_valley", 2);
        biomeTemperatures.put("minecraft:crimson_forest", 2);
        biomeTemperatures.put("minecraft:warped_forest", 2);

        // Biomes froids (-2) - End
        biomeTemperatures.put("minecraft:the_end", -2);
        biomeTemperatures.put("minecraft:end_barrens", -2);
        biomeTemperatures.put("minecraft:end_highlands", -2);
        biomeTemperatures.put("minecraft:end_midlands", -2);
        biomeTemperatures.put("minecraft:small_end_islands", -2);

        // Biomes froids (-2) - Overworld
        biomeTemperatures.put("minecraft:frozen_peaks", -2);
        biomeTemperatures.put("minecraft:ice_spikes", -2);
        biomeTemperatures.put("minecraft:snowy_plains", -2);

        // Biomes temperes froids (-1)
        biomeTemperatures.put("minecraft:taiga", -1);
        biomeTemperatures.put("minecraft:snowy_taiga", -1);

        // Biomes chauds (1)
        biomeTemperatures.put("minecraft:desert", 1);
        biomeTemperatures.put("minecraft:savanna", 1);
        biomeTemperatures.put("minecraft:badlands", 1);
        biomeTemperatures.put("minecraft:jungle", 1);

        loaded = true;
        LOGGER.info("Using default biome temperatures");
    }

    // ========== API PUBLIQUE ==========

    /**
     * Recupere la temperature d'un biome par son ID.
     * Retourne 0 (tempere) si le biome n'est pas trouve.
     */
    public static int getTemperature(String biomeId) {
        if (!loaded) {
            setupDefaults();
        }
        return biomeTemperatures.getOrDefault(biomeId, 0);
    }

    /**
     * Recupere la temperature d'un biome par sa ResourceLocation.
     */
    public static int getTemperature(ResourceLocation biomeLocation) {
        return getTemperature(biomeLocation.toString());
    }

    /**
     * Recupere la temperature d'un biome par son ResourceKey.
     */
    public static int getTemperature(ResourceKey<Biome> biomeKey) {
        return getTemperature(biomeKey.location().toString());
    }

    /**
     * Recupere la temperature d'un biome par son Holder.
     */
    public static int getTemperature(Holder<Biome> biomeHolder) {
        Optional<ResourceKey<Biome>> key = biomeHolder.unwrapKey();
        return key.map(BiomeTemperatureManager::getTemperature).orElse(0);
    }

    /**
     * Verifie si une abeille peut travailler dans un biome donne.
     * L'abeille peut travailler si la difference de temperature est <= tolerance.
     *
     * @param speciesId ID de l'espece
     * @param biomeTemperature Temperature du biome (-2 a 2)
     * @return true si l'abeille peut travailler
     */
    public static boolean canBeeWorkInBiome(String speciesId, int biomeTemperature) {
        BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(speciesId);
        if (speciesData == null) {
            return true; // Par defaut, permet le travail
        }

        int preferredTemp = speciesData.environment;
        int tolerance = speciesData.toleranceLevel;

        int tempDiff = Math.abs(biomeTemperature - preferredTemp);
        return tempDiff <= tolerance;
    }

    /**
     * Verifie si une abeille peut travailler dans un biome donne.
     *
     * @param speciesId ID de l'espece
     * @param biomeId ID du biome (ex: "minecraft:plains")
     * @return true si l'abeille peut travailler
     */
    public static boolean canBeeWorkInBiome(String speciesId, String biomeId) {
        return canBeeWorkInBiome(speciesId, getTemperature(biomeId));
    }

    /**
     * Verifie si le manager est charge.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
