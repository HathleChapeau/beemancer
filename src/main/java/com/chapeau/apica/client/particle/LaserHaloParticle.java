/**
 * ============================================================
 * [LaserHaloParticle.java]
 * Description: Particule halo lumineux pour le Mining Laser (point d'impact)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaParticles          | Type enregistré      | LASER_HALO                     |
 * | ConfigurableParticle    | Interface config     | Overrides via ParticleEmitter  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement provider)
 *
 * ============================================================
 */
package com.chapeau.apica.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particule halo billboard pour le Mining Laser.
 * Rotation continue, full bright, translucent avec fade out.
 * Utilisée au point d'impact du laser et comme effet au bout du canon.
 */
public class LaserHaloParticle extends TextureSheetParticle implements ConfigurableParticle {

    private boolean fadeOut = true;
    private boolean fullBright = true;
    private final float rotationSpeed;

    protected LaserHaloParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed,
                                 SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprites);
        this.rCol = 1.0f;
        this.gCol = 0.95f;
        this.bCol = 0.7f;
        this.alpha = 0.9f;
        this.quadSize = 0.2f;
        this.lifetime = 15;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = 0.0f;
        this.rotationSpeed = 0.15f + this.random.nextFloat() * 0.1f;
        this.oRoll = this.random.nextFloat() * (float) (Math.PI * 2);
        this.roll = this.oRoll;
    }

    // =========================================================================
    // ConfigurableParticle
    // =========================================================================

    @Override
    public LaserHaloParticle setParticleGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    @Override
    public LaserHaloParticle setParticleScale(float scale) {
        this.quadSize = scale;
        return this;
    }

    @Override
    public LaserHaloParticle setParticleAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    @Override
    public LaserHaloParticle setParticleFadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    @Override
    public LaserHaloParticle setParticleColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        return this;
    }

    @Override
    public LaserHaloParticle setParticleFullBright(boolean fullBright) {
        this.fullBright = fullBright;
        return this;
    }

    @Override
    public int getLightColor(float partialTick) {
        return fullBright ? 15728880 : super.getLightColor(partialTick);
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
        this.oRoll = this.roll;
        this.roll += this.rotationSpeed;
        this.move(this.xd, this.yd, this.zd);
        if (fadeOut) {
            this.alpha = 0.9f * (1.0f - ((float) this.age / (float) this.lifetime));
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
            return new LaserHaloParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
