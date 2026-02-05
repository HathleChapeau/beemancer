/**
 * ============================================================
 * [HoneyPixelParticle.java]
 * Description: Particule carree minuscule couleur miel
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerParticles      | Type enregistre      | HONEY_PIXEL                    |
 * | ConfigurableParticle    | Interface configurable| Overrides via ParticleEmitter  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement provider)
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
 * Particule pixel carree couleur jaune miel.
 * Taille fixe tres petite, pas de fade, duree de vie courte.
 * Configurable via {@link ParticleEmitter} et {@link ConfigurableParticle}.
 */
public class HoneyPixelParticle extends TextureSheetParticle implements ConfigurableParticle {

    private boolean fadeOut = false;

    protected HoneyPixelParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.rCol = 1.0f;
        this.gCol = 0.75f;
        this.bCol = 0.1f;
        this.alpha = 1.0f;
        this.quadSize = 0.02f;
        this.lifetime = 6;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.0f;
    }

    // =========================================================================
    // ConfigurableParticle
    // =========================================================================

    @Override
    public HoneyPixelParticle setParticleGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    @Override
    public HoneyPixelParticle setParticleScale(float scale) {
        this.quadSize = scale;
        return this;
    }

    @Override
    public HoneyPixelParticle setParticleAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    public HoneyPixelParticle setParticleFadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    @Override
    public HoneyPixelParticle setParticleColor(float r, float g, float b) {
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
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
        this.move(this.xd, this.yd, this.zd);
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
            return new HoneyPixelParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
