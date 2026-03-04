/**
 * ============================================================
 * [CompanionBeeItem.java]
 * Description: Accessoire simple qui spawne une abeille compagnon decorative
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | IAccessory              | Interface accessoire | Equip/unequip callbacks        |
 * | CompanionBeeEntity      | Entite compagnon     | Spawn/despawn                  |
 * | AccessoryPlayerData     | Donnees joueur       | Detection slot                 |
 * | ApicaEntities           | Registre entites     | Type entite compagnon          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - Apica.java (lifecycle login/respawn)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.accessory;

import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Accessoire compagnon simple. Spawne une abeille decorative qui suit le joueur.
 * Pas de comportement special (pas de magnet, pas de coffre).
 */
public class CompanionBeeItem extends Item implements IAccessory {

    public CompanionBeeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int slot = findAccessorySlot(serverPlayer, stack);
        spawnCompanionBee(serverPlayer, slot);
    }

    @Override
    public void onUnequip(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int slot = findAccessorySlot(serverPlayer, stack);
        despawnCompanionBee(serverPlayer, slot);
    }

    @Override
    public boolean hasInventoryTab() {
        return false;
    }

    // =========================================================================
    // BEE LIFECYCLE
    // =========================================================================

    /**
     * Spawne une abeille compagnon COMPANION pour le joueur au slot donne.
     * Utilisable depuis onEquip et depuis le login/respawn dans Apica.java.
     */
    public static void spawnCompanionBee(ServerPlayer player, int slot) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        despawnCompanionBee(player, slot);

        CompanionBeeEntity bee = ApicaEntities.COMPANION_BEE.get().create(serverLevel);
        if (bee == null) return;

        double angle = Math.toRadians(player.yBodyRot);
        double offsetX = (slot == 0) ? -0.6 : 0.6;
        double x = player.getX() + offsetX * Math.cos(angle);
        double y = player.getY() + 1.8;
        double z = player.getZ() + offsetX * Math.sin(angle);

        bee.moveTo(x, y, z, player.getYRot(), 0);
        bee.setOwnerUuid(player.getUUID());
        bee.setAccessorySlot(slot);
        bee.setCompanionType(CompanionBeeEntity.CompanionType.COMPANION);
        serverLevel.addFreshEntity(bee);
    }

    /**
     * Despawn l'abeille compagnon du joueur au slot donne.
     */
    public static void despawnCompanionBee(ServerPlayer player, int slot) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        AABB searchArea = player.getBoundingBox().inflate(64);
        List<CompanionBeeEntity> bees = serverLevel.getEntitiesOfClass(
            CompanionBeeEntity.class, searchArea);

        for (CompanionBeeEntity bee : bees) {
            Player owner = bee.getOwnerPlayer();
            if (owner != null && owner.getUUID().equals(player.getUUID())
                && bee.getAccessorySlot() == slot
                && bee.getCompanionType() == CompanionBeeEntity.CompanionType.COMPANION) {
                bee.discard();
            }
        }
    }

    /**
     * Despawn TOUTES les abeilles compagnon COMPANION d'un joueur.
     */
    public static void despawnAllCompanionBees(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        AABB searchArea = player.getBoundingBox().inflate(64);
        List<CompanionBeeEntity> bees = serverLevel.getEntitiesOfClass(
            CompanionBeeEntity.class, searchArea);

        for (CompanionBeeEntity bee : bees) {
            Player owner = bee.getOwnerPlayer();
            if (owner != null && owner.getUUID().equals(player.getUUID())
                && bee.getCompanionType() == CompanionBeeEntity.CompanionType.COMPANION) {
                bee.discard();
            }
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private int findAccessorySlot(ServerPlayer player, ItemStack stack) {
        AccessoryPlayerData data = player.getData(ApicaAttachments.ACCESSORY_DATA);
        for (int i = 0; i < AccessoryPlayerData.SLOT_COUNT; i++) {
            if (data.getAccessory(i) == stack) {
                return i;
            }
        }
        return 0;
    }
}
