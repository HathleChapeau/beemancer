/**
 * ============================================================
 * [NormalEnvironmentGene.java]
 * Description: Gene environnement normal (pas de pluie, pas de nuit)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | EnvironmentGene     | Classe de base       | Extension gene environnement   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - GeneInit.java (enregistrement dans GeneRegistry)
 *
 * ============================================================
 */
package com.chapeau.apica.content.gene.environment;

import net.minecraft.world.entity.animal.Bee;

public class NormalEnvironmentGene extends EnvironmentGene {
    public static final NormalEnvironmentGene INSTANCE = new NormalEnvironmentGene();

    private NormalEnvironmentGene() {
        super("normal");
        setParameter("canWorkInRain", false);
        setParameter("canWorkAtNight", false);
        setParameter("temperatureTolerance", 0);
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
