/**
 * ============================================================
 * [LongLifetimeGene.java]
 * Description: Gène de durée de vie longue (~60 minutes)
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.lifetime;

import net.minecraft.world.entity.animal.Bee;

public class LongLifetimeGene extends LifetimeGene {
    public static final LongLifetimeGene INSTANCE = new LongLifetimeGene();

    private LongLifetimeGene() {
        super("long");
        setParameter("maxLifetimeTicks", 72000); // 60 minutes
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement géré par MagicBeeEntity.tick()
    }
}
