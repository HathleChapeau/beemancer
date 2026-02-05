/**
 * ============================================================
 * [RuneParticle.java]
 * Description: Particule de glyphe runique avec gravite inversee (monte)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerParticles      | Type enregistre      | RUNE                           |
 * | ConfigurableParticle    | Interface configurable| Overrides via ParticleEmitter  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement provider)
 * - MagicBreedingCrystalBlock.java (animateTick via ParticleEmitter)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particule de glyphe d'enchantement (SGA) avec gravite inversee.
 * Monte continuellement sans retomber. Sprites vanilla enchantement.
 * Configurable via {@link ParticleEmitter} et {@link ConfigurableParticle}.
 */
public class RuneParticle extends TextureSheetParticle implements ConfigurableParticle {

    private boolean fadeOut = true;

    protected RuneParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed,
                            SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.quadSize = 0.04f + this.random.nextFloat() * 0.02f;
        this.lifetime = 10 + this.random.nextInt(10);
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = -0.03f;
    }

    // =========================================================================
    // ConfigurableParticle
    // =========================================================================

    @Override
    public RuneParticle setParticleGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    @Override
    public RuneParticle setParticleScale(float scale) {
        this.quadSize = scale;
        return this;
    }

    @Override
    public RuneParticle setParticleAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    public RuneParticle setParticleFadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    @Override
    public RuneParticle setParticleColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        return this;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.95;
        this.zd *= 0.95;
        if (fadeOut) {
            this.alpha = 1.0f - ((float) this.age / (float) this.lifetime);
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            return new RuneParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
