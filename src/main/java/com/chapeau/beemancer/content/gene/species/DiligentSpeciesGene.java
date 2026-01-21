/**
 * ============================================================
 * [DiligentSpeciesGene.java]
 * Description: Gène d'espèce diligente - vitesse améliorée
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.species;

import net.minecraft.world.entity.animal.Bee;

public class DiligentSpeciesGene extends SpeciesGene {
    public static final DiligentSpeciesGene INSTANCE = new DiligentSpeciesGene();

    private DiligentSpeciesGene() {
        super("diligent");
        setParameter("speedModifier", 1.5);
        setParameter("productionModifier", 0.8);
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Les abeilles diligentes sont plus rapides mais produisent moins
    }
}
