/**
 * ============================================================
 * [MiningLaserBlockBreaker.java]
 * Description: Logique server-side de destruction de blocs par le Mining Laser
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ParticleHelper      | Effets visuels       | Particules d'impact            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MiningLaserItem.java (appel lors du tir)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gère le raycast et la destruction de blocs pour le Mining Laser.
 * Le rayon part de la position des yeux du joueur dans la direction du regard,
 * détruit le premier bloc solide (hardness 0-50, exclut bedrock).
 * Le chargeLevel détermine le rayon AoE :
 * - 0 : bloc ciblé uniquement
 * - 1 : sphère rayon 1 (voisins directs)
 * - 2 : sphère rayon 2
 * - 3 : sphère rayon 3
 */
public final class MiningLaserBlockBreaker {

    /** Hardness maximale destructible (obsidienne = 50, bedrock = -1) */
    private static final float MAX_HARDNESS = 50.0f;

    /**
     * Tente de détruire le premier bloc solide sur le rayon du laser,
     * plus les blocs voisins en sphère selon le chargeLevel.
     *
     * @param level       Le ServerLevel dans lequel opérer
     * @param player      Le joueur qui tire
     * @param range       Portée maximale en blocs
     * @param chargeLevel Niveau de charge (0-3), détermine le rayon AoE
     * @return La position du bloc touché, ou null si aucun bloc touché
     */
    public static BlockPos tryBreakBlock(ServerLevel level, Player player, int range, int chargeLevel) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        BlockHitResult hitResult = level.clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(hitPos);

        if (state.isAir()) {
            return null;
        }

        float hardness = state.getDestroySpeed(level, hitPos);
        if (hardness < 0 || hardness > MAX_HARDNESS) {
            spawnImpactParticles(level, hitResult);
            return hitPos;
        }

        // Détruire le bloc central
        level.destroyBlock(hitPos, true, player);

        // Détruire les blocs voisins en sphère si chargeLevel > 0
        if (chargeLevel > 0) {
            destroySphere(level, player, hitPos, chargeLevel);
        }

        spawnBreakParticles(level, hitPos, chargeLevel);
        return hitPos;
    }

    /**
     * Détruit tous les blocs destructibles dans une sphère autour du centre.
     *
     * @param level  Le ServerLevel
     * @param player Le joueur (pour les drops)
     * @param center Centre de la sphère
     * @param radius Rayon en blocs (1, 2 ou 3)
     */
    private static void destroySphere(ServerLevel level, Player player, BlockPos center, int radius) {
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) continue;

                    float h = state.getDestroySpeed(level, pos);
                    if (h < 0 || h > MAX_HARDNESS) continue;

                    level.destroyBlock(pos, true, player);
                }
            }
        }
    }

    /**
     * Particules d'impact quand le laser touche un bloc indestructible.
     */
    private static void spawnImpactParticles(ServerLevel level, BlockHitResult hitResult) {
        Vec3 impactPos = hitResult.getLocation();
        ParticleHelper.burst(level, impactPos, ParticleTypes.SMOKE, 5);
    }

    /**
     * Particules de destruction quand un bloc est cassé par le laser.
     * Plus de particules selon le chargeLevel.
     */
    private static void spawnBreakParticles(ServerLevel level, BlockPos pos, int chargeLevel) {
        Vec3 center = Vec3.atCenterOf(pos);
        int count = 8 + chargeLevel * 6;
        double spread = 0.5 + chargeLevel * 0.5;
        ParticleHelper.spawnParticles(level, ParticleTypes.ELECTRIC_SPARK, center, count, spread, 0.1);
    }

    private MiningLaserBlockBreaker() {
    }
}
