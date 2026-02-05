/**
 * ============================================================
 * [ParticleEmitter.java]
 * Description: Builder fluent client-side pour spawner et configurer des particules
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | Minecraft               | ParticleEngine       | Creation particules client      |
 * | ParticleOptions          | Type de particule    | Parametre du spawn             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBreedingCrystalBlock.java (animateTick)
 * - Tout bloc/renderer necessitant des particules client-side configurables
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Builder fluent pour spawner des particules cote client avec configuration post-creation.
 *
 * Utilisation:
 *   new ParticleEmitter(RUNE.get())
 *       .at(x, y, z)
 *       .speed(0, 0.03, 0)
 *       .lifetime(15)
 *       .gravity(-0.03f)
 *       .scale(0.05f)
 *       .fadeOut()
 *       .spawn(level);
 *
 * Pour spawner plusieurs particules avec spread aleatoire:
 *   new ParticleEmitter(RUNE.get())
 *       .at(x, y, z)
 *       .count(3)
 *       .spread(0.5, 0.3, 0.5)
 *       .speedVariance(0.02, 0.01, 0.02)
 *       .spawn(level);
 */
public class ParticleEmitter {

    private final ParticleOptions type;
    private double x, y, z;
    private double xSpeed, ySpeed, zSpeed;
    private int count = 1;
    private double spreadX, spreadY, spreadZ;
    private double speedVarX, speedVarY, speedVarZ;

    private Integer lifetimeOverride;
    private Float gravityOverride;
    private Float scaleOverride;
    private Float alphaOverride;
    private boolean fadeOut;
    private float[] colorOverride;

    public ParticleEmitter(ParticleOptions type) {
        this.type = type;
    }

    public ParticleEmitter at(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ParticleEmitter speed(double xSpeed, double ySpeed, double zSpeed) {
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
        this.zSpeed = zSpeed;
        return this;
    }

    public ParticleEmitter count(int count) {
        this.count = count;
        return this;
    }

    public ParticleEmitter spread(double x, double y, double z) {
        this.spreadX = x;
        this.spreadY = y;
        this.spreadZ = z;
        return this;
    }

    public ParticleEmitter speedVariance(double x, double y, double z) {
        this.speedVarX = x;
        this.speedVarY = y;
        this.speedVarZ = z;
        return this;
    }

    public ParticleEmitter lifetime(int ticks) {
        this.lifetimeOverride = ticks;
        return this;
    }

    public ParticleEmitter gravity(float gravity) {
        this.gravityOverride = gravity;
        return this;
    }

    public ParticleEmitter scale(float scale) {
        this.scaleOverride = scale;
        return this;
    }

    public ParticleEmitter alpha(float alpha) {
        this.alphaOverride = alpha;
        return this;
    }

    public ParticleEmitter fadeOut() {
        this.fadeOut = true;
        return this;
    }

    public ParticleEmitter color(float r, float g, float b) {
        this.colorOverride = new float[]{r, g, b};
        return this;
    }

    /**
     * Spawne les particules cote client et applique les overrides de configuration.
     * Les overrides (lifetime, gravity, scale, color, fadeOut) ne s'appliquent
     * qu'aux particules implementant {@link ConfigurableParticle}.
     */
    public void spawn(Level level) {
        if (!level.isClientSide()) return;
        var engine = Minecraft.getInstance().particleEngine;
        RandomSource random = level.getRandom();

        for (int i = 0; i < count; i++) {
            double px = x + randomOffset(random, spreadX);
            double py = y + randomOffset(random, spreadY);
            double pz = z + randomOffset(random, spreadZ);
            double sx = xSpeed + randomOffset(random, speedVarX);
            double sy = ySpeed + randomOffset(random, speedVarY);
            double sz = zSpeed + randomOffset(random, speedVarZ);

            Particle p = engine.createParticle(type, px, py, pz, sx, sy, sz);
            if (p == null) continue;

            if (lifetimeOverride != null) {
                p.setLifetime(lifetimeOverride);
            }

            if (p instanceof ConfigurableParticle cp) {
                if (gravityOverride != null) cp.setParticleGravity(gravityOverride);
                if (scaleOverride != null) cp.setParticleScale(scaleOverride);
                if (alphaOverride != null) cp.setParticleAlpha(alphaOverride);
                if (fadeOut) cp.setParticleFadeOut(true);
                if (colorOverride != null) cp.setParticleColor(colorOverride[0], colorOverride[1], colorOverride[2]);
            }
        }
    }

    private static double randomOffset(RandomSource random, double range) {
        if (range == 0) return 0;
        return (random.nextDouble() - 0.5) * 2.0 * range;
    }
}
