/**
 * ============================================================
 * [RideableBeeSpawnItem.java]
 * Description: Item pour faire apparaître une RideableBeeEntity
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RideableBeeEntity       | Entité à spawn       | Création de l'entité           |
 * | BeemancerEntities       | Type d'entité        | Référence au type              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerItems.java: Enregistrement de l'item
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.mount;

import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Item pour faire apparaître une abeille chevauchable.
 * Clic droit sur un bloc pour spawn l'abeille au-dessus.
 */
public class RideableBeeSpawnItem extends Item {

    public RideableBeeSpawnItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos clickedPos = context.getClickedPos();
            BlockPos spawnPos = clickedPos.relative(context.getClickedFace());

            // Créer l'entité
            RideableBeeEntity bee = BeemancerEntities.RIDEABLE_BEE.get().create(serverLevel);
            if (bee != null) {
                // Positionner l'entité
                bee.moveTo(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    context.getPlayer() != null ? context.getPlayer().getYRot() : 0f,
                    0f
                );

                // Ajouter au monde
                serverLevel.addFreshEntity(bee);

                // Consommer l'item
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
