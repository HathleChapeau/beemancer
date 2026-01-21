package com.chapeau.beemancer.content.gene.flower;

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
