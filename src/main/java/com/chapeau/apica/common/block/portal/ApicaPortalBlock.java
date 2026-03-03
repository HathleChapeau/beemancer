/**
 * ============================================================
 * [ApicaPortalBlock.java]
 * Description: Bloc portail vers la dimension Apica —
 *              téléporte le joueur au clic droit
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaDimension      | ResourceKey<Level>   | Déterminer la destination      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 * - ApicaItems.java (block item)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.portal;

import com.chapeau.apica.content.dimension.ApicaDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ApicaPortalBlock extends Block {

    public ApicaPortalBlock(Properties properties) {
        super(properties);
    }

    // =========================================================================
    // TELEPORTATION AU CLIC DROIT
    // =========================================================================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        ResourceKey<Level> targetKey = ApicaDimension.isApicaDimension(level)
                ? Level.OVERWORLD
                : ApicaDimension.LEVEL_KEY;

        ServerLevel targetLevel = serverLevel.getServer().getLevel(targetKey);
        if (targetLevel == null) {
            return InteractionResult.FAIL;
        }

        Vec3 targetPos;
        if (targetKey == ApicaDimension.LEVEL_KEY) {
            targetPos = new Vec3(0.5, 52.0, 0.5);
        } else {
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            targetPos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5);
        }

        DimensionTransition transition = new DimensionTransition(
                targetLevel,
                targetPos,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
        );

        serverPlayer.changeDimension(transition);

        return InteractionResult.SUCCESS;
    }
}
