/**
 * ============================================================
 * [MushroomsFlowerGene.java]
 * Description: Gène de fleur pour les champignons
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.flower;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class MushroomsFlowerGene extends FlowerGene {
    public static final MushroomsFlowerGene INSTANCE = new MushroomsFlowerGene();
    
    private static final TagKey<Block> MUSHROOMS_TAG = TagKey.create(
            Registries.BLOCK, 
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "bee_flowers_mushrooms"));

    private MushroomsFlowerGene() {
        super("mushrooms");
        setParameter("pollinationRange", 5);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return MUSHROOMS_TAG;
    }
}
