/**
 * ============================================================
 * [NobleSpeciesGene.java]
 * Description: Gène d'espèce noble - production améliorée
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.species;

import net.minecraft.world.entity.animal.Bee;

public class NobleSpeciesGene extends SpeciesGene {
    public static final NobleSpeciesGene INSTANCE = new NobleSpeciesGene();

    private NobleSpeciesGene() {
        super("noble");
        setParameter("speedModifier", 0.9);
        setParameter("productionModifier", 1.5);
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Les abeilles nobles sont plus lentes mais produisent plus
    }
}
