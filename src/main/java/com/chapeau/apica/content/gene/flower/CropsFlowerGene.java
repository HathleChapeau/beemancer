/**
 * ============================================================
 * [CropsFlowerGene.java]
 * Description: Gene fleur pour cultures (BlockTags.CROPS)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FlowerGene          | Classe de base       | Extension comportement fleur   |
 * | BlockTags           | Tags Minecraft       | CROPS                          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - GeneInit.java (enregistrement dans GeneRegistry)
 *
 * ============================================================
 */
package com.chapeau.apica.content.gene.flower;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;

public class CropsFlowerGene extends FlowerGene {
    public static final CropsFlowerGene INSTANCE = new CropsFlowerGene();

    private CropsFlowerGene() {
        super("crops");
        setParameter("pollinationRange", 5);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return BlockTags.CROPS;
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
