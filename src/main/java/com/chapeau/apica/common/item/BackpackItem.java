/**
 * ============================================================
 * [BackpackItem.java]
 * Description: Item sac a dos avec inventaire interne de 27 slots, spawne une abeille compagnon avec coffre
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Item                | Base Minecraft       | Classe parente                 |
 * | IAccessory          | Slot accessoire      | Interface equippable           |
 * | DataComponents      | Stockage items       | CONTAINER pour contenu         |
 * | BackpackTooltip     | Tooltip visuel       | Preview grille items           |
 * | MobEffects          | Effets               | Slowness en inventaire         |
 * | CompanionBeeEntity  | Abeille compagnon    | Spawn/despawn visuel           |
 * | ApicaEntities       | Registre entites     | Type entite compagnon          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - BackpackMenu.java (type check)
 * - BackpackOpenPacket.java (validation)
 * - AccessoryEquipPacket.java (equip en slot accessoire)
 * - Apica.java (lifecycle login/respawn)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.entity.companion.CompanionBeeEntity;
import com.chapeau.apica.common.item.accessory.IAccessory;
import com.chapeau.apica.core.network.packets.BackpackOpenPacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Item backpack portable. S'equipe en slot accessoire et s'ouvre via l'onglet Backpack.
 * L'inventaire (27 slots) est stocke dans DataComponents.CONTAINER sur l'ItemStack.
 * Quand equipe, spawne une abeille compagnon portant un coffre.
 * Applique Slowness si porte en inventaire regulier.
 */
public class BackpackItem extends Item implements IAccessory {

    private static final int CONTAINER_SLOTS = 27;

    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<ItemStack> nonEmpty = getContentItems(stack);
        if (nonEmpty.isEmpty()) return Optional.empty();
        List<ItemStack> display = nonEmpty.subList(0, Math.min(nonEmpty.size(), BackpackTooltip.MAX_DISPLAY));
        return Optional.of(new BackpackTooltip(display));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (level.getGameTime() % 20 != 0) return;

        int filledSlots = countFilledSlots(stack);
        if (filledSlots == 0) return;

        int amplifier;
        if (filledSlots <= 6) {
            amplifier = 0;
        } else if (filledSlots <= 12) {
            amplifier = 1;
        } else if (filledSlots <= 18) {
            amplifier = 2;
        } else {
            amplifier = 3;
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, amplifier, true, false));
    }

    // =========================================================================
    // ACCESSORY LIFECYCLE
    // =========================================================================

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
        return true;
    }

    @Override
    public void onInventoryTabClicked(int accessorySlot) {
        PacketDistributor.sendToServer(new BackpackOpenPacket(accessorySlot));
    }

    @Override
    public ItemStack getTabIcon() {
        return new ItemStack(Items.CHEST);
    }

    // =========================================================================
    // BEE LIFECYCLE
    // =========================================================================

    /**
     * Spawne une abeille compagnon BACKPACK pour le joueur au slot donne.
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
        bee.setCompanionType(CompanionBeeEntity.CompanionType.BACKPACK);
        serverLevel.addFreshEntity(bee);
    }

    /**
     * Despawn l'abeille compagnon backpack du joueur au slot donne.
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
                && bee.getCompanionType() == CompanionBeeEntity.CompanionType.BACKPACK) {
                bee.discard();
            }
        }
    }

    /**
     * Despawn TOUTES les abeilles compagnon backpack d'un joueur.
     */
    public static void despawnAllBackpackBees(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        AABB searchArea = player.getBoundingBox().inflate(64);
        List<CompanionBeeEntity> bees = serverLevel.getEntitiesOfClass(
            CompanionBeeEntity.class, searchArea);

        for (CompanionBeeEntity bee : bees) {
            Player owner = bee.getOwnerPlayer();
            if (owner != null && owner.getUUID().equals(player.getUUID())
                && bee.getCompanionType() == CompanionBeeEntity.CompanionType.BACKPACK) {
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

    /** Retourne la liste des items non-vides dans le backpack. */
    private static List<ItemStack> getContentItems(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return List.of();
        NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SLOTS, ItemStack.EMPTY);
        contents.copyInto(items);
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : items) {
            if (!item.isEmpty()) result.add(item);
        }
        return result;
    }

    /** Compte le nombre de slots occupes dans le backpack. */
    private static int countFilledSlots(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return 0;
        NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SLOTS, ItemStack.EMPTY);
        contents.copyInto(items);
        int count = 0;
        for (ItemStack item : items) {
            if (!item.isEmpty()) count++;
        }
        return count;
    }
}
