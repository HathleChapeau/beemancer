/**
 * ============================================================
 * [BreedingManager.java]
 * Description: Gère le breeding et l'héritage des gènes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeGeneData         | Gènes parents/enfant | Création offspring             |
 * | GeneRegistry        | Gènes par défaut     | Héritage                       |
 * | GeneCategory        | Catégories           | Itération des traits           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity (breeding logic)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.breeding;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;

public class BreedingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreedingManager.class);
    
    // Config parameters
    public static final double BREEDING_CHANCE_PER_SECOND = 0.05; // 5% par seconde
    
    // Breeding combinations cache
    private static final Map<String, List<BreedingResult>> combinations = new HashMap<>();
    private static List<BreedingResult> defaultResults = new ArrayList<>();
    private static boolean loaded = false;
    
    /**
     * Charge les combinaisons depuis le fichier JSON
     */
    public static void loadCombinations(MinecraftServer server) {
        combinations.clear();
        defaultResults.clear();
        
        ResourceManager resourceManager = server.getResourceManager();
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "breeding/species_combinations.json");
        
        try {
            Optional<Resource> resource = resourceManager.getResource(location);
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    // Parse combinations
                    JsonArray combArray = json.getAsJsonArray("combinations");
                    for (JsonElement elem : combArray) {
                        JsonObject comb = elem.getAsJsonObject();
                        String parent1 = comb.get("parent1").getAsString();
                        String parent2 = comb.get("parent2").getAsString();
                        List<BreedingResult> results = parseResults(comb.getAsJsonArray("results"));
                        
                        // Store with normalized key (alphabetically sorted)
                        String key = createKey(parent1, parent2);
                        combinations.put(key, results);
                    }
                    
                    // Parse default
                    if (json.has("defaultResult")) {
                        JsonObject defaultObj = json.getAsJsonObject("defaultResult");
                        defaultResults = parseResults(defaultObj.getAsJsonArray("results"));
                    }
                    
                    LOGGER.info("Loaded {} breeding combinations", combinations.size());
                    loaded = true;
                }
            } else {
                LOGGER.warn("Breeding config not found: {}", location);
                setupDefaults();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load breeding config", e);
            setupDefaults();
        }
    }
    
    private static List<BreedingResult> parseResults(JsonArray arr) {
        List<BreedingResult> results = new ArrayList<>();
        for (JsonElement elem : arr) {
            JsonObject obj = elem.getAsJsonObject();
            String species = obj.get("species").getAsString();
            int chance = obj.get("chance").getAsInt();
            results.add(new BreedingResult(species, chance));
        }
        return results;
    }
    
    private static void setupDefaults() {
        defaultResults = List.of(
                new BreedingResult("meadow", 50),
                new BreedingResult("nothing", 50)
        );
        loaded = true;
    }
    
    /**
     * Crée une clé normalisée pour deux espèces (order-independent)
     */
    private static String createKey(String species1, String species2) {
        if (species1.compareTo(species2) <= 0) {
            return species1 + "+" + species2;
        }
        return species2 + "+" + species1;
    }
    
    /**
     * Résout l'espèce de l'offspring basé sur deux parents
     * @return ID de l'espèce résultante, ou "nothing" si pas d'offspring
     */
    public static String resolveOffspringSpecies(String species1, String species2, RandomSource random) {
        if (!loaded) {
            LOGGER.warn("BreedingManager not loaded, using defaults");
            setupDefaults();
        }
        
        String key = createKey(species1, species2);
        List<BreedingResult> results = combinations.getOrDefault(key, defaultResults);
        
        // Tirer au sort selon les probabilités
        int roll = random.nextInt(100);
        int cumulative = 0;
        
        for (BreedingResult result : results) {
            cumulative += result.chance();
            if (roll < cumulative) {
                return result.species();
            }
        }
        
        // Fallback
        return "nothing";
    }
    
    /**
     * Crée les données génétiques de l'offspring
     * 
     * Règles d'héritage:
     * - Si parents ont espèces différentes → traits par défaut pour l'offspring
     * - Si parents même espèce mais résultat différent → traits par défaut
     * - Si parents même espèce ET résultat même espèce → héritage aléatoire de chaque trait
     */
    public static BeeGeneData createOffspringGeneData(
            BeeGeneData parent1, 
            BeeGeneData parent2, 
            String offspringSpecies,
            RandomSource random) {
        
        BeeGeneData offspring = new BeeGeneData();
        
        Gene parent1Species = parent1.getGene(GeneCategory.SPECIES);
        Gene parent2Species = parent2.getGene(GeneCategory.SPECIES);
        
        boolean sameParentSpecies = parent1Species != null && parent2Species != null 
                && parent1Species.getId().equals(parent2Species.getId());
        boolean sameResultSpecies = parent1Species != null 
                && parent1Species.getId().equals(offspringSpecies);
        
        // Définir l'espèce de l'offspring
        Gene speciesGene = GeneRegistry.getGene(GeneCategory.SPECIES, offspringSpecies);
        if (speciesGene != null) {
            offspring.setGene(speciesGene);
        }
        
        // Traiter les autres traits
        for (GeneCategory category : GeneCategory.getAll().values()) {
            if (category == GeneCategory.SPECIES) continue; // Already set
            
            Gene resultGene;
            
            if (sameParentSpecies && sameResultSpecies) {
                // Héritage aléatoire d'un des parents
                Gene gene1 = parent1.getGene(category);
                Gene gene2 = parent2.getGene(category);
                resultGene = random.nextBoolean() ? gene1 : gene2;
            } else {
                // Traits par défaut
                resultGene = GeneRegistry.getDefaultGene(category);
            }
            
            if (resultGene != null) {
                offspring.setGene(resultGene);
            }
        }
        
        return offspring;
    }
    
    /**
     * Résultat de breeding avec probabilité
     */
    public record BreedingResult(String species, int chance) {}
}
