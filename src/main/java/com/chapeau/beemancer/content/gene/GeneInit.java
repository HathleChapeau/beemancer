/**
 * ============================================================
 * [GeneInit.java]
 * Description: Initialise et enregistre tous les gènes
 * ============================================================
 */
package com.chapeau.beemancer.content.gene;

import com.chapeau.beemancer.content.gene.environment.*;
import com.chapeau.beemancer.content.gene.flower.*;
import com.chapeau.beemancer.content.gene.lifetime.*;
import com.chapeau.beemancer.content.gene.species.*;
import com.chapeau.beemancer.core.gene.GeneRegistry;

public class GeneInit {
    
    public static void registerAllGenes() {
        // Species genes
        GeneRegistry.register(CommonSpeciesGene.INSTANCE);
        GeneRegistry.register(NobleSpeciesGene.INSTANCE);
        GeneRegistry.register(DiligentSpeciesGene.INSTANCE);

        // Environment genes
        GeneRegistry.register(NormalEnvironmentGene.INSTANCE);
        GeneRegistry.register(RobustEnvironmentGene.INSTANCE);
        GeneRegistry.register(NocturnalEnvironmentGene.INSTANCE);

        // Flower genes
        GeneRegistry.register(FlowersFlowerGene.INSTANCE);
        GeneRegistry.register(CropsFlowerGene.INSTANCE);
        GeneRegistry.register(CavePlantsFlowerGene.INSTANCE);

        // Lifetime genes
        GeneRegistry.register(ShortLifetimeGene.INSTANCE);
        GeneRegistry.register(NormalLifetimeGene.INSTANCE);
        GeneRegistry.register(LongLifetimeGene.INSTANCE);
    }
}
