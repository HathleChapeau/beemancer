/**
 * ============================================================
 * [SpeciesGene.java]
 * Description: Classe de base pour les gènes d'espèce
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.species;

import com.chapeau.beemancer.core.gene.AbstractGene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import net.minecraft.world.entity.animal.Bee;

public abstract class SpeciesGene extends AbstractGene {
    
    protected SpeciesGene(String id) {
        super(id, GeneCategory.SPECIES);
    }

    /**
     * @return Multiplicateur de vitesse de l'abeille
     */
    public double getSpeedModifier() {
        return getParameter("speedModifier", 1.0);
    }

    /**
     * @return Multiplicateur de production
     */
    public double getProductionModifier() {
        return getParameter("productionModifier", 1.0);
    }
}
