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
