/**
 * ============================================================
 * [MultiblockTankMenu.java]
 * Description: Menu pour le tank multibloc avec slot de bucket
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MultiblockTankMenu extends BeemancerMenu {
    protected final MultiblockTankBlockEntity blockEntity;
    protected final ContainerLevelAccess access;
    protected final ContainerData data;

    // Slot indices
    private static final int BUCKET_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    public MultiblockTankMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(4));
    }

    private static MultiblockTankBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        BlockEntity be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof MultiblockTankBlockEntity tank) {
            MultiblockTankBlockEntity master = tank.getMaster();
            return master != null ? master : tank;
        }
        return null;
    }

    public MultiblockTankMenu(int containerId, Inventory playerInv, MultiblockTankBlockEntity be, ContainerData data) {
        super(BeemancerMenus.MULTIBLOCK_TANK.get(), containerId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Bucket slot
        addSlot(new SlotItemHandler(blockEntity.getBucketSlot(), 0, 26, 35));

        addPlayerInventory(playerInv, 8, 88);
        addPlayerHotbar(playerInv, 8, 146);
    }

    public MultiblockTankBlockEntity getBlockEntity() { return blockEntity; }
    public int getFluidAmount() { return data.get(0); }
    public int getCapacity() { return data.get(1); }
    public int getBlockCount() { return data.get(2); }
    public boolean isValidCuboid() { return data.get(3) == 1; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // From bucket slot to player
            if (index == BUCKET_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player to bucket slot (if it's a valid bucket)
            else if (blockEntity.getBucketSlot().isItemValid(0, stack)) {
                if (!moveItemStackTo(stack, BUCKET_SLOT, BUCKET_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Move between inventory and hotbar
            else if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.MULTIBLOCK_TANK.get());
    }
}
