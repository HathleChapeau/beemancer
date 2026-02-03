/**
 * ============================================================
 * [ParticleHelper.java]
 * Description: Utilitaires statiques pour spawner des particules (server et client)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                      |
 * |-------------------------|----------------------|----------------------------------|
 * | ServerLevel             | Spawn server-side    | sendParticles()                  |
 * | ParticleTypes           | Types vanilla        | Particules prédéfinies           |
 * | BuiltInRegistries       | Lookup par RL        | Résolution ResourceLocation      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - AltarHeartBlockEntity.java (potentiel)
 * - BeePathfinding.java (potentiel)
 * - Tout système nécessitant des effets de particules
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticleHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleHelper.class);

    // ==================== EffectType ====================

    public enum EffectType {
        SUCCESS(ParticleTypes.HAPPY_VILLAGER, ParticleTypes.END_ROD),
        FAILURE(ParticleTypes.SMOKE),
        MAGIC(ParticleTypes.ENCHANT),
        HEAL(ParticleTypes.HEART),
        HONEY(ParticleTypes.DRIPPING_HONEY, ParticleTypes.FALLING_HONEY),
        SOUL(ParticleTypes.SOUL_FIRE_FLAME),
        PORTAL(ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL),
        NATURE(ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.CHERRY_LEAVES),
        FLAME(ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME),
        ELECTRIC(ParticleTypes.ELECTRIC_SPARK),
        SCULK(ParticleTypes.SCULK_SOUL);

        private final ParticleOptions[] particles;

        EffectType(ParticleOptions... particles) {
            this.particles = particles;
        }

        public ParticleOptions[] getParticles() {
            return particles;
        }

        public ParticleOptions primary() {
            return particles[0];
        }
    }

    // ==================== Effets haut niveau (EffectType) ====================

    public static void burst(ServerLevel level, Vec3 center, EffectType type, int count) {
        for (ParticleOptions particle : type.getParticles()) {
            spawnParticles(level, particle, center, count, 0.5, 0.1);
        }
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, EffectType type, int count) {
        for (ParticleOptions particle : type.getParticles()) {
            spawnRing(level, particle, center, radius, count);
        }
    }

    public static void spiral(ServerLevel level, Vec3 base, double radius, double height, EffectType type, int count) {
        for (ParticleOptions particle : type.getParticles()) {
            spawnSpiral(level, particle, base, radius, height, count);
        }
    }

    public static void line(ServerLevel level, Vec3 start, Vec3 end, EffectType type, int count) {
        for (ParticleOptions particle : type.getParticles()) {
            spawnLine(level, particle, start, end, count);
        }
    }

    public static void sphere(ServerLevel level, Vec3 center, double radius, EffectType type, int count) {
        for (ParticleOptions particle : type.getParticles()) {
            spawnSphere(level, particle, center, radius, count);
        }
    }

    // ==================== Effets haut niveau (ParticleOptions) ====================

    public static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count) {
        spawnParticles(level, particle, center, count, 0.5, 0.1);
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int count) {
        spawnRing(level, particle, center, radius, count);
    }

    public static void spiral(ServerLevel level, Vec3 base, double radius, double height, ParticleOptions particle, int count) {
        spawnSpiral(level, particle, base, radius, height, count);
    }

    public static void line(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int count) {
        spawnLine(level, particle, start, end, count);
    }

    public static void sphere(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int count) {
        spawnSphere(level, particle, center, radius, count);
    }

    // ==================== Génériques (ParticleOptions) ====================

    public static void spawnParticles(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public static void spawnWithMotion(ServerLevel level, ParticleOptions particle, Vec3 pos, Vec3 motion, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, 0, motion.x, motion.y, motion.z, speed);
    }

    public static void spawnRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        double angleStep = (2 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnSpiral(ServerLevel level, ParticleOptions particle, Vec3 base, double radius, double height, int count) {
        double angleStep = (4 * Math.PI) / count;
        double heightStep = height / count;
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            double x = base.x + Math.cos(angle) * radius;
            double y = base.y + (i * heightStep);
            double z = base.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnLine(ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, int count) {
        Vec3 direction = end.subtract(start);
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count;
            Vec3 pos = start.add(direction.scale(t));
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Spawne des particules orbitant autour d'un centre, position basée sur le temps.
     * Les particules tournent en continu à chaque appel grâce au gameTime.
     *
     * @param level     ServerLevel
     * @param particle  Type de particule
     * @param center    Centre de l'orbite
     * @param radius    Rayon de l'orbite
     * @param count     Nombre de particules uniformément réparties sur l'orbite
     * @param speed     Vitesse angulaire (radians par tick)
     */
    public static void orbitingRing(ServerLevel level, ParticleOptions particle, Vec3 center,
                                     double radius, int count, double speed) {
        double time = level.getGameTime() * speed;
        double angleStep = (2 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            double angle = time + (i * angleStep);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnSphere(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(1 - 2.0 * i / count);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i;
            double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.y + radius * Math.cos(phi);
            double z = center.z + radius * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    // ==================== Surcharges ResourceLocation ====================

    public static void spawnParticles(ServerLevel level, ResourceLocation particleId, Vec3 pos, int count, double spread, double speed) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnParticles(level, particle, pos, count, spread, speed);
        }
    }

    public static void spawnWithMotion(ServerLevel level, ResourceLocation particleId, Vec3 pos, Vec3 motion, double speed) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnWithMotion(level, particle, pos, motion, speed);
        }
    }

    public static void spawnRing(ServerLevel level, ResourceLocation particleId, Vec3 center, double radius, int count) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnRing(level, particle, center, radius, count);
        }
    }

    public static void spawnSpiral(ServerLevel level, ResourceLocation particleId, Vec3 base, double radius, double height, int count) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnSpiral(level, particle, base, radius, height, count);
        }
    }

    public static void spawnLine(ServerLevel level, ResourceLocation particleId, Vec3 start, Vec3 end, int count) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnLine(level, particle, start, end, count);
        }
    }

    public static void spawnSphere(ServerLevel level, ResourceLocation particleId, Vec3 center, double radius, int count) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            spawnSphere(level, particle, center, radius, count);
        }
    }

    // ==================== Client-side ====================

    public static void addParticle(Level level, ParticleOptions particle, Vec3 pos, Vec3 motion) {
        if (level.isClientSide) {
            level.addParticle(particle, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        }
    }

    public static void addParticle(Level level, ResourceLocation particleId, Vec3 pos, Vec3 motion) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            addParticle(level, particle, pos, motion);
        }
    }

    public static void addAlwaysVisible(Level level, ParticleOptions particle, Vec3 pos, Vec3 motion) {
        if (level.isClientSide) {
            level.addAlwaysVisibleParticle(particle, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        }
    }

    public static void addAlwaysVisible(Level level, ResourceLocation particleId, Vec3 pos, Vec3 motion) {
        ParticleOptions particle = resolveParticle(particleId);
        if (particle != null) {
            addAlwaysVisible(level, particle, pos, motion);
        }
    }

    // ==================== Résolution interne ====================

    private static ParticleOptions resolveParticle(ResourceLocation particleId) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
        if (type instanceof ParticleOptions options) {
            return options;
        }
        LOGGER.warn("Particle '{}' not found or not a simple particle type", particleId);
        return null;
    }

    private ParticleHelper() {
    }
}
