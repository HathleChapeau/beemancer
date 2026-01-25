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

import com.chapeau.beemancer.content.gene.environment.*;
import com.chapeau.beemancer.content.gene.flower.*;
import com.chapeau.beemancer.content.gene.lifetime.*;
import com.chapeau.beemancer.content.gene.species.*;
import com.chapeau.beemancer.core.gene.GeneRegistry;

import java.util.Arrays;
import java.util.List;

public class GeneInit {

    /**
     * Liste de toutes les especes d'abeilles du mod.
     * Correspond aux IDs dans bee_species.json
     */
    private static final List<String> ALL_SPECIES = Arrays.asList(
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
            "draconic", "stellar",

            // Legacy species (for compatibility)
            "common", "noble", "diligent"
    );

    public static void registerAllGenes() {
        registerSpeciesGenes();
        registerEnvironmentGenes();
        registerFlowerGenes();
        registerLifetimeGenes();
    }

    // =========================================================================
    // SPECIES GENES
    // =========================================================================

    private static void registerSpeciesGenes() {
        // Register all species as data-driven genes
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

    // =========================================================================
    // LIFETIME GENES
    // =========================================================================

    private static void registerLifetimeGenes() {
        GeneRegistry.register(ShortLifetimeGene.INSTANCE);
        GeneRegistry.register(NormalLifetimeGene.INSTANCE);
        GeneRegistry.register(LongLifetimeGene.INSTANCE);
    }

    /**
     * @return Liste de tous les IDs d'especes
     */
    public static List<String> getAllSpeciesIds() {
        return ALL_SPECIES;
    }
}
