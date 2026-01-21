/**
 * ============================================================
 * [LifetimeGene.java]
 * Description: Classe de base pour les gènes de durée de vie
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.lifetime;

import com.chapeau.beemancer.core.gene.AbstractGene;
import com.chapeau.beemancer.core.gene.GeneCategory;

public abstract class LifetimeGene extends AbstractGene {
    
    protected LifetimeGene(String id) {
        super(id, GeneCategory.LIFETIME);
    }

    /**
     * @return Durée de vie maximale en ticks (20 ticks = 1 seconde)
     */
    public int getMaxLifetimeTicks() {
        return getParameter("maxLifetimeTicks", 24000); // 20 minutes par défaut
    }

    /**
     * @return Durée de vie en secondes (pour affichage)
     */
    public int getMaxLifetimeSeconds() {
        return getMaxLifetimeTicks() / 20;
    }
}
