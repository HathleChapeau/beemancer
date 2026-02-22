/**
 * ============================================================
 * [HoverbikePartVariants.java]
 * Description: Registre dynamique des variantes de modeles du Hoverbike (charge depuis JSON)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePart       | Enum des parties     | Cle du registre                |
 * | HoverbikePartModel  | Type de base         | Factory de modeles             |
 * | *PartModel[B/C]     | Variantes concretes  | Enregistrement factories       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Selection du modele a rendre
 * - HoverbikePartScreen.java: Preview + nom de la variante
 * - ClientSetup.java: Enregistrement de tous les layers
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.hoverbike;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registre central pour les variantes de modeles du hoverbike.
 * Les factories de modeles sont enregistrees statiquement en Java (geometrie).
 * Les variantes sont chargees depuis assets/apica/hoverbike/parts/{category}.json.
 */
public final class HoverbikePartVariants {

    private static final Logger LOGGER = LoggerFactory.getLogger(HoverbikePartVariants.class);
    private static final String PARTS_DIR = "hoverbike/parts/";

    /** Factory de modele enregistree en Java (geometrie + constructeur). */
    public record ModelFactory(
            ModelLayerLocation layerLocation,
            Supplier<LayerDefinition> layerDefinition,
            Function<ModelPart, HoverbikePartModel> constructor
    ) {}

    /** Variante chargee depuis le JSON (donnees + factory Java). */
    public record VariantEntry(
            String modelKey, String name, ModelFactory factory,
            String stat1Name, double stat1Value,
            String stat2Name, double stat2Value
    ) {}

    /** Factories Java enregistrees par cle de modele (lazy-loaded). */
    private static volatile Map<String, ModelFactory> MODEL_FACTORIES;

    /** Variantes chargees depuis JSON, par partie. */
    private static Map<HoverbikePart, List<VariantEntry>> loadedVariants = null;

    private static Map<String, ModelFactory> getModelFactories() {
        if (MODEL_FACTORIES == null) {
            Map<String, ModelFactory> map = new HashMap<>();

            // --- Chassis ---
            map.put("chassis", new ModelFactory(ChassisPartModel.LAYER_LOCATION,
                    ChassisPartModel::createLayerDefinition, ChassisPartModel::new));
            map.put("chassis_b", new ModelFactory(ChassisPartModelB.LAYER_LOCATION,
                    ChassisPartModelB::createLayerDefinition, ChassisPartModelB::new));
            map.put("chassis_c", new ModelFactory(ChassisPartModelC.LAYER_LOCATION,
                    ChassisPartModelC::createLayerDefinition, ChassisPartModelC::new));

            // --- Coeur ---
            map.put("coeur", new ModelFactory(CoeurPartModel.LAYER_LOCATION,
                    CoeurPartModel::createLayerDefinition, CoeurPartModel::new));
            map.put("coeur_b", new ModelFactory(CoeurPartModelB.LAYER_LOCATION,
                    CoeurPartModelB::createLayerDefinition, CoeurPartModelB::new));
            map.put("coeur_c", new ModelFactory(CoeurPartModelC.LAYER_LOCATION,
                    CoeurPartModelC::createLayerDefinition, CoeurPartModelC::new));

            // --- Propulseur ---
            map.put("propulseur", new ModelFactory(PropulseurPartModel.LAYER_LOCATION,
                    PropulseurPartModel::createLayerDefinition, PropulseurPartModel::new));
            map.put("propulseur_b", new ModelFactory(PropulseurPartModelB.LAYER_LOCATION,
                    PropulseurPartModelB::createLayerDefinition, PropulseurPartModelB::new));
            map.put("propulseur_c", new ModelFactory(PropulseurPartModelC.LAYER_LOCATION,
                    PropulseurPartModelC::createLayerDefinition, PropulseurPartModelC::new));

            // --- Radiateur ---
            map.put("radiateur", new ModelFactory(RadiateurPartModel.LAYER_LOCATION,
                    RadiateurPartModel::createLayerDefinition, RadiateurPartModel::new));
            map.put("radiateur_b", new ModelFactory(RadiateurPartModelB.LAYER_LOCATION,
                    RadiateurPartModelB::createLayerDefinition, RadiateurPartModelB::new));
            map.put("radiateur_c", new ModelFactory(RadiateurPartModelC.LAYER_LOCATION,
                    RadiateurPartModelC::createLayerDefinition, RadiateurPartModelC::new));

            MODEL_FACTORIES = map;
        }
        return MODEL_FACTORIES;
    }

    /** Retourne toutes les factories enregistrees (pour ClientSetup layer registration). */
    public static Map<String, ModelFactory> getAllModelFactories() {
        return getModelFactories();
    }

    /** Charge les variantes depuis les JSONs par categorie si pas encore fait. */
    private static void ensureLoaded() {
        if (loadedVariants != null) return;
        loadedVariants = new EnumMap<>(HoverbikePart.class);

        try {
            Minecraft mc = Minecraft.getInstance();
            for (HoverbikePart part : HoverbikePart.values()) {
                String categoryKey = part.name().toLowerCase();
                ResourceLocation partFile = ResourceLocation.fromNamespaceAndPath(
                        Apica.MOD_ID, PARTS_DIR + categoryKey + ".json");

                Optional<Resource> resource = mc.getResourceManager().getResource(partFile);
                if (resource.isEmpty()) {
                    LOGGER.warn("Part file not found: {}", partFile);
                    continue;
                }

                try (InputStream is = resource.get().open();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                    List<VariantEntry> entries = parsePartArray(array);
                    if (!entries.isEmpty()) {
                        loadedVariants.put(part, entries);
                    }
                }
            }
            LOGGER.info("Loaded hoverbike parts for {} categories", loadedVariants.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load hoverbike parts, using defaults", e);
            buildDefaults();
        }
    }

    /** Parse un tableau JSON de variantes pour une categorie. */
    private static List<VariantEntry> parsePartArray(JsonArray array) {
        List<VariantEntry> entries = new ArrayList<>();
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String modelKey = obj.get("model").getAsString();
            String stat1Name = obj.has("stat_1") ? obj.get("stat_1").getAsString() : "";
            double stat1Value = obj.has("stat_1_value") ? obj.get("stat_1_value").getAsDouble() : 0.0;
            String stat2Name = obj.has("stat_2") ? obj.get("stat_2").getAsString() : "";
            double stat2Value = obj.has("stat_2_value") ? obj.get("stat_2_value").getAsDouble() : 0.0;

            ModelFactory factory = getModelFactories().get(modelKey);
            if (factory == null) {
                LOGGER.warn("Unknown model key '{}' for part '{}', skipping", modelKey, name);
                continue;
            }

            entries.add(new VariantEntry(modelKey, name, factory, stat1Name, stat1Value, stat2Name, stat2Value));
        }
        return entries;
    }

    /** Fallback : une seule variante par partie (le modele A). */
    private static void buildDefaults() {
        for (HoverbikePart part : HoverbikePart.values()) {
            String key = part.name().toLowerCase();
            ModelFactory factory = MODEL_FACTORIES.get(key);
            if (factory != null) {
                loadedVariants.put(part, List.of(
                        new VariantEntry(key, "Default", factory, "", 0.0, "", 0.0)));
            }
        }
    }

    /** Force le rechargement depuis les JSONs (utile lors d'un resource reload). */
    public static void reload() {
        loadedVariants = null;
    }

    /** Retourne toutes les variantes d'une partie. */
    public static List<VariantEntry> getVariants(HoverbikePart part) {
        ensureLoaded();
        return loadedVariants.getOrDefault(part, List.of());
    }

    /** Retourne la variante a l'index donne (modulo le nombre). */
    public static VariantEntry getVariant(HoverbikePart part, int index) {
        List<VariantEntry> variants = getVariants(part);
        if (variants.isEmpty()) return null;
        return variants.get(Math.floorMod(index, variants.size()));
    }

    /** Retourne le nombre de variantes pour une partie. */
    public static int getVariantCount(HoverbikePart part) {
        return getVariants(part).size();
    }

    private HoverbikePartVariants() {}
}
