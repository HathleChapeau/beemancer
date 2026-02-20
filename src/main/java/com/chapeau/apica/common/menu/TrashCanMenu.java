/**
 * ============================================================
 * [TrashCanMenu.java]
 * Description: Menu de la poubelle a items - 1 slot trash
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.storage.TrashCanBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TrashCanMenu extends ApicaMenu {
    private final TrashCanBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private static final int TRASH_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    // Client constructor
    public TrashCanMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // Server constructor
    public TrashCanMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ApicaMenus.TRASH_CAN.get(), containerId);
        this.blockEntity = (TrashCanBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // Trash slot - center of the GUI
        addSlot(new ApicaSlot(blockEntity.getItemHandler(), 0, 87, 45));

        // Player inventory
        addPlayerInventory(playerInventory, 15, 107);
        addPlayerHotbar(playerInventory, 15, 165);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == TRASH_SLOT) {
                // From trash to player
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to trash: item goes in, old one is destroyed
                ItemStack oldItem = slots.get(TRASH_SLOT).getItem().copy();

                // Place new item (only 1)
                ItemStack toPlace = stackInSlot.copyWithCount(1);
                blockEntity.getItemHandler().setStackInSlot(0, toPlace);
                stackInSlot.shrink(stackInSlot.getCount()); // Destroy entire stack

                // Give back old item if it existed
                if (!oldItem.isEmpty()) {
                    if (!moveItemStackTo(oldItem, PLAYER_INV_START, HOTBAR_END, true)) {
                        player.drop(oldItem, false);
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ApicaBlocks.TRASH_CAN.get());
    }
}
