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
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

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
        String speciesId = getSpeciesId(stack);
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

    // =========================================================================
    // SPECIES DATA
    // =========================================================================

    /**
     * Stocke l'identifiant d'espece sur un item companion (companion_bee, bee_magnet, backpack).
     */
    public static void setSpeciesId(ItemStack stack, String speciesId) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString("SpeciesId", speciesId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Recupere l'identifiant d'espece stocke sur un item companion.
     * @return l'ID de l'espece (ex: "meadow") ou null si absent
     */
    @Nullable
    public static String getSpeciesId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("SpeciesId")) {
                return tag.getString("SpeciesId");
            }
        }
        return null;
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return new CompoundTag();
    }

    // =========================================================================
    // TOOLTIP
    // =========================================================================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String speciesId = getSpeciesId(stack);
        if (speciesId == null) {
            speciesId = "meadow";
        }
        tooltip.add(Component.translatable("species.apica." + speciesId).withStyle(ChatFormatting.GOLD));
    }

    // =========================================================================
    // BEE LIFECYCLE
    // =========================================================================

    /**
     * Spawne une abeille compagnon COMPANION pour le joueur au slot donne.
     * Utilisable depuis onEquip et depuis le login/respawn dans Apica.java.
     */
    public static void spawnCompanionBee(ServerPlayer player, int slot) {
        spawnCompanionBee(player, slot, "meadow");
    }

    /**
     * Spawne une abeille compagnon COMPANION pour le joueur au slot donne avec une espece specifique.
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
        bee.setCompanionType(CompanionBeeEntity.CompanionType.COMPANION);
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
            if (ItemStack.isSameItemSameComponents(data.getAccessory(i), stack)) {
                return i;
            }
        }
        return 0;
    }
}
