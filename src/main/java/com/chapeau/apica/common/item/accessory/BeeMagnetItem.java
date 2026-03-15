/**
 * ============================================================
 * [BeeMagnetItem.java]
 * Description: Accessoire magnet qui spawne une abeille compagnon
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
 * - AccessoryEquipPacket.java (validation IAccessory)
 * - Apica.java (lifecycle login/respawn)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.accessory;

import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Accessoire magnet. Quand equipe dans un slot accessoire, spawne une CompanionBeeEntity
 * qui hover pres de l'epaule du joueur et ramasse les items droppes au sol.
 * Pas de tab d'inventaire (contrairement au backpack).
 */
public class BeeMagnetItem extends Item implements IAccessory {

    public BeeMagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int slot = findAccessorySlot(serverPlayer, stack);
        String speciesId = CompanionBeeItem.getSpeciesId(stack);
        spawnCompanionBee(serverPlayer, slot, speciesId != null ? speciesId : "meadow");
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String speciesId = CompanionBeeItem.getSpeciesId(stack);
        if (speciesId == null) {
            speciesId = "meadow"; // default species
        }
        tooltip.add(Component.translatable("tooltip.apica.species")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("species.apica." + speciesId).withStyle(ChatFormatting.GOLD)));
    }

    // =========================================================================
    // BEE LIFECYCLE
    // =========================================================================

    /**
     * Spawne une abeille compagnon pour le joueur au slot donne.
     * Utilisable depuis onEquip et depuis le login/respawn dans Apica.java.
     */
    public static void spawnCompanionBee(ServerPlayer player, int slot) {
        spawnCompanionBee(player, slot, "meadow");
    }

    /**
     * Spawne une abeille compagnon pour le joueur au slot donne avec une espece specifique.
     */
    public static void spawnCompanionBee(ServerPlayer player, int slot, String speciesId) {
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
        bee.setSpeciesId(speciesId != null ? speciesId : "meadow");
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
                && bee.getAccessorySlot() == slot) {
                bee.discard();
            }
        }
    }

    /**
     * Despawn TOUTES les abeilles compagnon d'un joueur.
     */
    public static void despawnAllCompanionBees(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        AABB searchArea = player.getBoundingBox().inflate(64);
        List<CompanionBeeEntity> bees = serverLevel.getEntitiesOfClass(
            CompanionBeeEntity.class, searchArea);

        for (CompanionBeeEntity bee : bees) {
            Player owner = bee.getOwnerPlayer();
            if (owner != null && owner.getUUID().equals(player.getUUID())) {
                bee.discard();
            }
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    /**
     * Trouve dans quel slot accessoire cet item se trouve.
     * Retourne 0 par defaut si non trouve.
     */
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
