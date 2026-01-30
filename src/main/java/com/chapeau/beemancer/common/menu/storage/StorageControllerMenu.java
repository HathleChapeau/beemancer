/**
 * ============================================================
 * [StorageControllerMenu.java]
 * Description: Menu du Storage Controller (4 slots essence + 4 stats)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | BeemancerSlot                 | Slots filtrés        | Slots essence avec tag filter  |
 * | BeemancerTags                 | Tag ESSENCES         | Filtre des slots               |
 * | ContainerData                 | Sync stats           | 4 stats vers client            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (createMenu)
 * - StorageControllerScreen.java (affichage)
 * - ClientSetup.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class StorageControllerMenu extends AbstractContainerMenu {

    private final ContainerData data;

    private static final int ESSENCE_SLOTS = 4;
    private static final int ESSENCE_SLOT_START = 0;
    private static final int ESSENCE_SLOT_END = 4;
    private static final int PLAYER_INV_START = 4;
    private static final int PLAYER_INV_END = 31;
    private static final int HOTBAR_START = 31;
    private static final int HOTBAR_END = 40;

    public StorageControllerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, new ItemStackHandler(4), new SimpleContainerData(4));
    }

    public StorageControllerMenu(int containerId, Inventory playerInv,
                                  ItemStackHandler essenceSlots, ContainerData data) {
        super(BeemancerMenus.STORAGE_CONTROLLER.get(), containerId);
        this.data = data;

        addDataSlots(data);

        // 4 essence slots (centered at y=60, spaced 20px apart)
        int startX = 53;
        for (int i = 0; i < ESSENCE_SLOTS; i++) {
            addSlot(new BeemancerSlot(essenceSlots, i, startX + i * 20, 60)
                .withFilter(stack -> stack.is(BeemancerTags.Items.ESSENCES)));
        }

        addPlayerInventory(playerInv, 8, 84);
        addPlayerHotbar(playerInv, 8, 142);
    }

    private void addPlayerInventory(Inventory playerInv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInv, int x, int y) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, x + col * 18, y));
        }
    }

    public int getFlightSpeed() { return data.get(0); }
    public int getSearchSpeed() { return data.get(1); }
    public int getCraftSpeed() { return data.get(2); }
    public int getQuantity() { return data.get(3); }

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
}
