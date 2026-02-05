/**
 * ============================================================
 * [RuneParticle.java]
 * Description: Particule de rune qui monte avec gravite inversee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerParticles      | Type enregistre      | RUNE                           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement provider)
 * - MagicBreedingCrystalBlock.java (animateTick)
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
 * Particule de glyphe runique flottant vers le haut.
 * Utilise une gravite negative pour monter, avec un leger fade-out.
 * Couleur violette/magenta evoquant la magie.
 */
public class RuneParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected RuneParticle(ClientLevel level, double x, double y, double z,
                            double xSpeed, double ySpeed, double zSpeed,
                            SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.pickSprite(sprites);
        this.rCol = 0.6f + this.random.nextFloat() * 0.2f;
        this.gCol = 0.2f + this.random.nextFloat() * 0.15f;
        this.bCol = 0.8f + this.random.nextFloat() * 0.2f;
        this.alpha = 1.0f;
        this.quadSize = 0.06f + this.random.nextFloat() * 0.04f;
        this.lifetime = 20 + this.random.nextInt(20);
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = -0.02f;
    }

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
        this.setSpriteFromAge(this.sprites);
        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.96;
        this.zd *= 0.96;
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0f - progress;
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
