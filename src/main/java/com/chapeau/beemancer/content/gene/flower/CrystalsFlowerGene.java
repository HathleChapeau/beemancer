/**
 * ============================================================
 * [CrystalsFlowerGene.java]
 * Description: Gène de fleur pour les cristaux (améthyste, etc.)
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.flower;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class CrystalsFlowerGene extends FlowerGene {
    public static final CrystalsFlowerGene INSTANCE = new CrystalsFlowerGene();
    
    private static final TagKey<Block> CRYSTALS_TAG = TagKey.create(
            Registries.BLOCK, 
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "bee_flowers_crystals"));

    private CrystalsFlowerGene() {
        super("crystals");
        setParameter("pollinationRange", 4);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return CRYSTALS_TAG;
    }
}
