/**
 * ============================================================
 * [NectarFilterMenu.java]
 * Description: Menu pour le filtre à nectar
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.NectarFilterBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NectarFilterMenu extends AbstractContainerMenu {
    private final NectarFilterBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public NectarFilterMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(2));
    }

    public NectarFilterMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.NECTAR_FILTER.get(), containerId);
        this.blockEntity = (NectarFilterBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;
        
        addDataSlots(data);

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

    public NectarFilterBlockEntity getBlockEntity() { return blockEntity; }
    public int getInputAmount() { return data.get(0); }
    public int getOutputAmount() { return data.get(1); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (!moveItemStackTo(stack, 0, 36, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.NECTAR_FILTER.get());
    }
}
