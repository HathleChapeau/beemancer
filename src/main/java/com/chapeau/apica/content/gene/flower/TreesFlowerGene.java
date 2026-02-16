/**
 * ============================================================
 * [TreesFlowerGene.java]
 * Description: Gène de fleur pour les arbres (logs et feuilles)
 * ============================================================
 */
package com.chapeau.apica.content.gene.flower;

import com.chapeau.apica.Apica;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class TreesFlowerGene extends FlowerGene {
    public static final TreesFlowerGene INSTANCE = new TreesFlowerGene();
    
    private static final TagKey<Block> TREES_TAG = TagKey.create(
            Registries.BLOCK, 
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "bee_flowers_trees"));

    private TreesFlowerGene() {
        super("trees");
        setParameter("pollinationRange", 8);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return TREES_TAG;
    }
}
