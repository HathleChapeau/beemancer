/**
 * ============================================================
 * [StorageControllerMenu.java]
 * Description: Menu du Storage Controller (8 slots essence dynamiques + stats)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | BeemancerSlot                 | Slots filtres        | Slots essence avec tag filter  |
 * | BeemancerTags                 | Tag ESSENCES         | Filtre des slots               |
 * | ContainerData                 | Sync stats           | 8 indices vers client          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (createMenu)
 * - StorageControllerScreen.java (affichage)
 * - ClientSetup.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class StorageControllerMenu extends BeemancerMenu {

    private final ContainerData data;

    private static final int BASE_ESSENCE_SLOTS = 4;
    private static final int TOTAL_ESSENCE_SLOTS = 8;
    private static final int ESSENCE_SLOT_START = 0;
    private static final int ESSENCE_SLOT_END = 8;
    private static final int PLAYER_INV_START = 8;
    private static final int PLAYER_INV_END = 35;
    private static final int HOTBAR_START = 35;
    private static final int HOTBAR_END = 44;

    public StorageControllerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, new ItemStackHandler(TOTAL_ESSENCE_SLOTS), new SimpleContainerData(8));
    }

    public StorageControllerMenu(int containerId, Inventory playerInv,
                                  ItemStackHandler essenceSlots, ContainerData data) {
        super(BeemancerMenus.STORAGE_CONTROLLER.get(), containerId);
        this.data = data;

        addDataSlots(data);

        // 4 base essence slots (always active, row at y=55)
        int startX = 53;
        for (int i = 0; i < BASE_ESSENCE_SLOTS; i++) {
            addSlot(new BeemancerSlot(essenceSlots, i, startX + i * 20, 55)
                .withFilter(stack -> stack.is(BeemancerTags.Items.ESSENCES)));
        }

        // 4 bonus essence slots (active per linked hive, row at y=75)
        for (int i = 0; i < BASE_ESSENCE_SLOTS; i++) {
            int slotIndex = BASE_ESSENCE_SLOTS + i;
            int requiredHives = i + 1;
            addSlot(new DynamicEssenceSlot(essenceSlots, slotIndex, startX + i * 20, 75, requiredHives));
        }

        addPlayerInventory(playerInv, 8, 94);
        addPlayerHotbar(playerInv, 8, 152);
    }

    public int getFlightSpeed() { return data.get(0); }
    public int getSearchSpeed() { return data.get(1); }
    public int getCraftSpeed() { return data.get(2); }
    public int getQuantity() { return data.get(3); }
    public int getHoneyConsumption() { return data.get(4); }
    public int getHoneyEfficiency() { return data.get(5); }
    public int getLinkedHiveCount() { return data.get(6); }
    public int getMaxDeliveryBees() { return data.get(7); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // From essence slots to player
            if (index >= ESSENCE_SLOT_START && index < ESSENCE_SLOT_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player to essence slots
            else {
                if (stack.is(BeemancerTags.Items.ESSENCES)) {
                    if (!moveItemStackTo(stack, ESSENCE_SLOT_START, ESSENCE_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                    if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                    if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Slot essence dynamique: actif seulement quand assez de hives sont liees.
     * requiredHives=1 signifie qu'il faut au moins 1 hive pour activer ce slot.
     */
    private class DynamicEssenceSlot extends BeemancerSlot {
        private final int requiredHives;

        public DynamicEssenceSlot(IItemHandler handler, int index, int x, int y, int requiredHives) {
            super(handler, index, x, y);
            this.requiredHives = requiredHives;
            withFilter(stack -> stack.is(BeemancerTags.Items.ESSENCES));
        }

        @Override
        public boolean isActive() {
            return data.get(6) >= requiredHives;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive()) return false;
            return super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!isActive()) return false;
            return super.mayPickup(player);
        }
    }
}
