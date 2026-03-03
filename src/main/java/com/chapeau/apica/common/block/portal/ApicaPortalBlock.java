/**
 * ============================================================
 * [ApicaPortalBlock.java]
 * Description: Bloc portail vers la dimension Apica —
 *              téléporte le joueur en marchant dessus
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaDimension      | ResourceKey<Level>   | Déterminer la destination      |
 * | Portal              | Interface vanilla    | Mécanique de téléportation     |
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ApicaPortalBlock extends Block implements Portal {

    public ApicaPortalBlock(Properties properties) {
        super(properties);
    }

    // =========================================================================
    // PORTAL INTERFACE
    // =========================================================================

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 60;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        ResourceKey<Level> targetKey = ApicaDimension.isApicaDimension(level)
                ? Level.OVERWORLD
                : ApicaDimension.LEVEL_KEY;

        ServerLevel targetLevel = level.getServer().getLevel(targetKey);
        if (targetLevel == null) {
            return null;
        }

        Vec3 targetPos;
        if (targetKey == ApicaDimension.LEVEL_KEY) {
            // Vers Apica : spawn au centre, juste au-dessus du terrain
            targetPos = new Vec3(0.5, 52.0, 0.5);
        } else {
            // Retour overworld : spawn du monde
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            targetPos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5);
        }

        return new DimensionTransition(
                targetLevel,
                targetPos,
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
        );
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }
}
