/**
 * ============================================================
 * [HoneyTankMenu.java]
 * Description: Menu simple pour afficher le contenu du tank
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class HoneyTankMenu extends AbstractContainerMenu {
    private final HoneyTankBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public HoneyTankMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(1));
    }

    public HoneyTankMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.HONEY_TANK.get(), containerId);
        this.blockEntity = (HoneyTankBlockEntity) be;
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

    public HoneyTankBlockEntity getBlockEntity() { return blockEntity; }
    public int getFluidAmount() { return data.get(0); }

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
        return stillValid(access, player, BeemancerBlocks.HONEY_TANK.get());
    }
}
