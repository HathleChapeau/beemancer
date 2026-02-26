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
 */
public final class MiningLaserBlockBreaker {

    /** Hardness maximale destructible (obsidienne = 50, bedrock = -1) */
    private static final float MAX_HARDNESS = 50.0f;

    /**
     * Tente de détruire le premier bloc solide sur le rayon du laser.
     *
     * @param level  Le ServerLevel dans lequel opérer
     * @param player Le joueur qui tire
     * @param range  Portée maximale en blocs
     * @return La position du bloc touché, ou null si aucun bloc destructible trouvé
     */
    public static BlockPos tryBreakBlock(ServerLevel level, Player player, int range) {
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

        level.destroyBlock(hitPos, true, player);
        spawnBreakParticles(level, hitPos);

        return hitPos;
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
     */
    private static void spawnBreakParticles(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        ParticleHelper.burst(level, center, ParticleTypes.ELECTRIC_SPARK, 8);
    }

    private MiningLaserBlockBreaker() {
    }
}
