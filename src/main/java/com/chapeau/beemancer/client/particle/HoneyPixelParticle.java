/**
 * ============================================================
 * [HoneyPixelParticle.java]
 * Description: Particule carrée minuscule couleur miel
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerParticles      | Type enregistré      | HONEY_PIXEL                    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
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
 * Particule pixel carrée couleur jaune miel.
 * Taille fixe très petite, pas de fade, durée de vie courte.
 */
public class HoneyPixelParticle extends TextureSheetParticle {

    protected HoneyPixelParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.rCol = 1.0f;
        this.gCol = 0.75f;
        this.bCol = 0.1f;
        this.alpha = 1.0f;
        this.quadSize = 0.04f;
        this.lifetime = 6;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.0f;
    }

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
