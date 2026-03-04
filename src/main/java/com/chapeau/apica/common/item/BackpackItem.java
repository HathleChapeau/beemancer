/**
 * ============================================================
 * [BackpackItem.java]
 * Description: Item sac a dos avec inventaire interne de 27 slots
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
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - BackpackMenu.java (type check)
 * - BackpackOpenPacket.java (validation)
 * - AccessoryEquipPacket.java (equip en slot accessoire)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.common.item.accessory.IAccessory;
import com.chapeau.apica.core.network.packets.BackpackOpenPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Item backpack portable. S'equipe en slot accessoire et s'ouvre via l'onglet Backpack.
 * L'inventaire (27 slots) est stocke dans DataComponents.CONTAINER sur l'ItemStack.
 * Applique Slowness si porte en inventaire regulier. Icone slowness via IItemDecorator (ClientSetup).
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

    @Override
    public void onEquip(Player player, ItemStack stack) {
        // Rien de special a l'equip pour l'instant
    }

    @Override
    public void onUnequip(Player player, ItemStack stack) {
        // Rien de special au desequip pour l'instant
    }

    @Override
    public boolean hasInventoryTab() {
        return true;
    }

    @Override
    public void onInventoryTabClicked(int accessorySlot) {
        PacketDistributor.sendToServer(new BackpackOpenPacket(accessorySlot));
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
