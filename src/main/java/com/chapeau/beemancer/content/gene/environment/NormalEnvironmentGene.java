package com.chapeau.beemancer.content.gene.environment;

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
