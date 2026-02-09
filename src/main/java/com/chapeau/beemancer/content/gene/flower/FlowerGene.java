/**
 * ============================================================
 * [FlowerGene.java]
 * Description: Classe abstraite pour les genes de type fleur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AbstractGene        | Classe de base gene  | Extension comportement         |
 * | GeneCategory        | Categorie gene       | Classification FLOWER          |
 * | TagKey<Block>       | Tags Minecraft       | Identification fleurs valides  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - FlowersFlowerGene, CropsFlowerGene, CavePlantsFlowerGene
 * - GeneInit.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.flower;

import com.chapeau.beemancer.core.gene.AbstractGene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public abstract class FlowerGene extends AbstractGene {
    
    protected FlowerGene(String id) {
        super(id, GeneCategory.FLOWER);
    }

    public abstract TagKey<Block> getFlowerTag();
    
    public int getPollinationRange() {
        return getParameter("pollinationRange", 3);
    }
}
