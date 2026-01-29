/**
 * ============================================================
 * [GeneInit.java]
 * Description: Initialise et enregistre tous les genes
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                         |
 * |-------------------------|--------------------------------|
 * | GeneRegistry            | Enregistrement des genes       |
 * | DataDrivenSpeciesGene   | Genes d'especes data-driven    |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.gene;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.content.gene.environment.*;
import com.chapeau.beemancer.content.gene.flower.*;
import com.chapeau.beemancer.content.gene.species.*;
import com.chapeau.beemancer.core.gene.GeneRegistry;

import java.util.*;

public class GeneInit {

    /**
     * Liste de toutes les especes d'abeilles du mod.
     * Correspond aux IDs dans bee_species.json
     * Utilise LinkedHashSet pour garantir l'ordre et eviter les doublons.
     */
    private static final Set<String> ALL_SPECIES = new LinkedHashSet<>(Arrays.asList(
            // Tier I - Base species (found in wild)
            "meadow", "forest", "rocky", "river", "dune", "nether", "end",

            // Tier II
            "monsty", "docile", "loam", "sediment", "spark", "cultural",

            // Tier III
            "carbon", "cupric", "festering", "skeletal", "silken", "arachnid",
            "tanner", "plume", "quercus", "boreal", "tropical", "paperbark",
            "reed", "karot", "tuber", "juicy", "gourd", "agrarian",

            // Tier IV
            "flower", "mushroom", "tree", "crystal", "argil", "umbra",
            "savanna", "frost", "volatile", "ashen", "igneous", "rose_stone",
            "prince", "infernalist",

            // Tier V
            "swift", "royal", "ferrous", "flux", "lazuli", "quartzose",
            "luminous", "grim", "columnar", "viscous", "inky", "fleece",
            "adobe", "geode", "marsh",

            // Tier VI
            "majestic", "steady", "wind", "thunder", "water", "fire",
            "auric", "magmatic", "spirit", "crimson", "mycelial", "humus",
            "floral",

            // Tier VII
            "treasure", "siphoning", "zephyr", "prismatic", "crystalline",
            "insomnia", "blazing",

            // Tier VIII
            "diamantine", "venerable", "volcanic", "xenolith", "traveler",
            "sorrow", "absorbent", "light", "dark", "imperial",

            // Tier IX
            "demonic", "paladin", "chorus", "ancient", "void",

            // Tier X
            "draconic", "stellar"
    ));

    private static boolean initialized = false;

    public static void registerAllGenes() {
        if (initialized) {
            Beemancer.LOGGER.warn("GeneInit.registerAllGenes() called multiple times");
            return;
        }

        registerSpeciesGenes();
        registerEnvironmentGenes();
        registerFlowerGenes();

        initialized = true;
        Beemancer.LOGGER.info("Registered {} species genes", ALL_SPECIES.size());
    }

    // =========================================================================
    // SPECIES GENES
    // =========================================================================

    private static void registerSpeciesGenes() {
        for (String speciesId : ALL_SPECIES) {
            GeneRegistry.register(new DataDrivenSpeciesGene(speciesId));
        }
    }

    // =========================================================================
    // ENVIRONMENT GENES
    // =========================================================================

    private static void registerEnvironmentGenes() {
        GeneRegistry.register(NormalEnvironmentGene.INSTANCE);
        GeneRegistry.register(RobustEnvironmentGene.INSTANCE);
        GeneRegistry.register(NocturnalEnvironmentGene.INSTANCE);
    }

    // =========================================================================
    // FLOWER GENES
    // =========================================================================

    private static void registerFlowerGenes() {
        GeneRegistry.register(FlowersFlowerGene.INSTANCE);
        GeneRegistry.register(CropsFlowerGene.INSTANCE);
        GeneRegistry.register(CavePlantsFlowerGene.INSTANCE);
        GeneRegistry.register(TreesFlowerGene.INSTANCE);
        GeneRegistry.register(MushroomsFlowerGene.INSTANCE);
        GeneRegistry.register(CrystalsFlowerGene.INSTANCE);
        GeneRegistry.register(NetherWartFlowerGene.INSTANCE);
    }

    /**
     * @return Liste immuable de tous les IDs d'especes
     */
    public static List<String> getAllSpeciesIds() {
        return List.copyOf(ALL_SPECIES);
    }

    /**
     * Verifie si une espece est enregistree.
     */
    public static boolean hasSpecies(String speciesId) {
        return ALL_SPECIES.contains(speciesId);
    }

    /**
     * @return Nombre total d'especes enregistrees
     */
    public static int getSpeciesCount() {
        return ALL_SPECIES.size();
    }
}
