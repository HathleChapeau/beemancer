/**
 * ============================================================
 * [NetherWartFlowerGene.java]
 * Description: Gène de fleur pour le nether wart
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.flower;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class NetherWartFlowerGene extends FlowerGene {
    public static final NetherWartFlowerGene INSTANCE = new NetherWartFlowerGene();
    
    private static final TagKey<Block> NETHER_WART_TAG = TagKey.create(
            Registries.BLOCK, 
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "bee_flowers_nether_wart"));

    private NetherWartFlowerGene() {
        super("nether_wart");
        setParameter("pollinationRange", 5);
    }

    @Override
    public TagKey<Block> getFlowerTag() {
        return NETHER_WART_TAG;
    }
}
