/**
 * ============================================================
 * [HoverbikeSpawnItem.java]
 * Description: Item pour spawner un Hoverbike dans le monde
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaEntities   | Registre entites     | Type du Hoverbike              |
 * | HoverbikeEntity     | Entite a spawner     | Creation de l'instance         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java: Enregistrement
 * - ApicaCreativeTabs.java: Onglet creatif
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Clic droit sur un bloc pour spawner un Hoverbike au-dessus.
 */
public class HoverbikeSpawnItem extends Item {

    public HoverbikeSpawnItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos pos = context.getClickedPos().above();
        Vec3 spawnPos = Vec3.atBottomCenterOf(pos);

        HoverbikeEntity hoverbike = ApicaEntities.HOVERBIKE.get().create(serverLevel);
        if (hoverbike != null) {
            hoverbike.setPos(spawnPos);
            hoverbike.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0);
            if (context.getPlayer() != null) {
                hoverbike.setOwner(context.getPlayer());
            }
            serverLevel.addFreshEntity(hoverbike);

            // Consommer l'item
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.CONSUME;
    }
}
