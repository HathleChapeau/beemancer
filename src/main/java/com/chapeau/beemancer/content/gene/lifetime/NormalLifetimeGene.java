/**
 * ============================================================
 * [NormalLifetimeGene.java]
 * Description: Gène de durée de vie normale (~20 minutes)
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.lifetime;

import net.minecraft.world.entity.animal.Bee;

public class NormalLifetimeGene extends LifetimeGene {
    public static final NormalLifetimeGene INSTANCE = new NormalLifetimeGene();

    private NormalLifetimeGene() {
        super("normal");
        setParameter("maxLifetimeTicks", 24000); // 20 minutes
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement géré par MagicBeeEntity.tick()
    }
}
