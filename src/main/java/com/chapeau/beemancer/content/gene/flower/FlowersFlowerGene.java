/**
 * ============================================================
 * [FlowersFlowerGene.java]
 * Description: Gene fleur pour fleurs classiques (BlockTags.FLOWERS)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FlowerGene          | Classe de base       | Extension comportement fleur   |
 * | BlockTags           | Tags Minecraft       | FLOWERS                        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - GeneInit.java (enregistrement dans GeneRegistry)
 *
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.flower;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;

public class FlowersFlowerGene extends FlowerGene {
    public static final FlowersFlowerGene INSTANCE = new FlowersFlowerGene();

    private FlowersFlowerGene() {
        super("flowers");
        setParameter("pollinationRange", 3);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return BlockTags.FLOWERS;
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
