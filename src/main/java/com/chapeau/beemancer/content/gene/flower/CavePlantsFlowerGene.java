package com.chapeau.beemancer.content.gene.flower;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;

public class CavePlantsFlowerGene extends FlowerGene {
    public static final CavePlantsFlowerGene INSTANCE = new CavePlantsFlowerGene();

    private CavePlantsFlowerGene() {
        super("cave_plants");
        setParameter("pollinationRange", 4);
        // Incompatible avec l'environnement normal (caves = nuit)
        addIncompatible("normal");
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return BlockTags.CAVE_VINES;
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
