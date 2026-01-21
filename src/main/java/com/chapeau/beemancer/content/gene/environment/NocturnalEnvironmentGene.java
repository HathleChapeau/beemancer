package com.chapeau.beemancer.content.gene.environment;

import net.minecraft.world.entity.animal.Bee;

public class NocturnalEnvironmentGene extends EnvironmentGene {
    public static final NocturnalEnvironmentGene INSTANCE = new NocturnalEnvironmentGene();

    private NocturnalEnvironmentGene() {
        super("nocturnal");
        setParameter("canWorkInRain", false);
        setParameter("canWorkAtNight", true);
        setParameter("temperatureTolerance", 0);
    }

    @Override
    public void applyBehavior(Bee bee) {}
}
