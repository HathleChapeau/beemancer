/**
 * ============================================================
 * [LiquidTrashCanMenu.java]
 * Description: Menu de la poubelle a liquides - 1 slot bucket
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.client.gui.widget.ApicaSlot;
import com.chapeau.apica.common.blockentity.storage.LiquidTrashCanBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LiquidTrashCanMenu extends ApicaMenu {
    private final LiquidTrashCanBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private static final int BUCKET_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    // Client constructor
    public LiquidTrashCanMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()));
    }

    // Server constructor
    public LiquidTrashCanMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ApicaMenus.LIQUID_TRASH_CAN.get(), containerId);
        this.blockEntity = (LiquidTrashCanBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // Bucket slot - accepts only buckets
        addSlot(new ApicaSlot(blockEntity.getBucketSlot(), 0, 87, 45)
                .withFilter(stack -> stack.getItem() instanceof BucketItem));

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

            if (index == BUCKET_SLOT) {
                // From bucket slot to player
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to bucket slot
                if (stackInSlot.getItem() instanceof BucketItem) {
                    if (!moveItemStackTo(stackInSlot, BUCKET_SLOT, BUCKET_SLOT + 1, false)) {
                        if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                            if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                } else {
                    return ItemStack.EMPTY;
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
        return stillValid(access, player, ApicaBlocks.LIQUID_TRASH_CAN.get());
    }
}
