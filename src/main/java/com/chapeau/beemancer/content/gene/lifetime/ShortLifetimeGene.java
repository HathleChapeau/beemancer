/**
 * ============================================================
 * [ShortLifetimeGene.java]
 * Description: Gène de durée de vie courte (~5 minutes)
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.lifetime;

import net.minecraft.world.entity.animal.Bee;

public class ShortLifetimeGene extends LifetimeGene {
    public static final ShortLifetimeGene INSTANCE = new ShortLifetimeGene();

    private ShortLifetimeGene() {
        super("short");
        setParameter("maxLifetimeTicks", 6000); // 5 minutes
    }

    @Override
    public void applyBehavior(Bee bee) {
        // Comportement géré par MagicBeeEntity.tick()
    }
}
