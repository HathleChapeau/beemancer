/**
 * ============================================================
 * [RobustEnvironmentGene.java]
 * Description: Gene environnement robuste (tolere la pluie)
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
package com.chapeau.beemancer.content.gene.environment;

import net.minecraft.world.entity.animal.Bee;

public class RobustEnvironmentGene extends EnvironmentGene {
    public static final RobustEnvironmentGene INSTANCE = new RobustEnvironmentGene();

    private RobustEnvironmentGene() {
        super("robust");
        setParameter("canWorkInRain", true);
        setParameter("canWorkAtNight", false);
        setParameter("temperatureTolerance", 1);
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
