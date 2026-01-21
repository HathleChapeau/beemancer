/**
 * ============================================================
 * [CommonSpeciesGene.java]
 * Description: Gène d'espèce commune (par défaut)
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.species;

import net.minecraft.world.entity.animal.Bee;

public class CommonSpeciesGene extends SpeciesGene {
    public static final CommonSpeciesGene INSTANCE = new CommonSpeciesGene();

    private CommonSpeciesGene() {
        super("common");
        setParameter("speedModifier", 1.0);
        setParameter("productionModifier", 1.0);
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement standard, pas de modification
    }
}
