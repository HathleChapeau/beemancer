/**
 * ============================================================
 * [EnvironmentGene.java]
 * Description: Classe de base pour les gènes d'environnement
 * ============================================================
 */
package com.chapeau.beemancer.content.gene.environment;

import com.chapeau.beemancer.core.gene.AbstractGene;
import com.chapeau.beemancer.core.gene.GeneCategory;

public abstract class EnvironmentGene extends AbstractGene {
    
    protected EnvironmentGene(String id) {
        super(id, GeneCategory.ENVIRONMENT);
    }

    public boolean canWorkInRain() {
        return getParameter("canWorkInRain", false);
    }

    public boolean canWorkAtNight() {
        return getParameter("canWorkAtNight", false);
    }

    public int getTemperatureTolerance() {
        return getParameter("temperatureTolerance", 0);
    }
}
