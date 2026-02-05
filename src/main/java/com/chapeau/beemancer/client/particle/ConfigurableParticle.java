/**
 * ============================================================
 * [ConfigurableParticle.java]
 * Description: Interface pour particules custom configurables via ParticleEmitter
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | (aucune)                |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ParticleEmitter.java (application des overrides)
 * - RuneParticle.java (implementation)
 * - HoneyPixelParticle.java (implementation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.particle;

/**
 * Interface implementee par les particules custom Beemancer.
 * Permet a {@link ParticleEmitter} de modifier les parametres post-creation
 * (gravity, scale, alpha, color, fadeOut) via un pattern fluent.
 */
public interface ConfigurableParticle {

    ConfigurableParticle setParticleGravity(float gravity);

    ConfigurableParticle setParticleScale(float scale);

    ConfigurableParticle setParticleAlpha(float alpha);

    ConfigurableParticle setParticleFadeOut(boolean fadeOut);

    ConfigurableParticle setParticleColor(float r, float g, float b);
}
