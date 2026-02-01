/**
 * ============================================================
 * [IncubatorMenu.java]
 * Description: Menu de l'incubateur avec slot larve unique
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
import com.chapeau.beemancer.common.menu.slot.LarvaSlot;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IncubatorMenu extends BeemancerMenu {
    private final Container container;
    private final ContainerData data;

    // Client constructor
    public IncubatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new SimpleContainer(IncubatorBlockEntity.SLOT_COUNT), new SimpleContainerData(2));
    }

    // Server constructor
    public IncubatorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(BeemancerMenus.INCUBATOR.get(), containerId);
        this.container = container;
        this.data = data;
        
        addDataSlots(data);
        
        // Single larva slot (centered)
        addSlot(new LarvaSlot(container, 0, 80, 35));
        
        // Player inventory
        addPlayerInventory(playerInventory, 8, 88);
        addPlayerHotbar(playerInventory, 8, 146);
    }

    public int getIncubationProgress() {
        return data.get(0);
    }

    public int getIncubationTime() {
        return data.get(1);
    }

    public float getProgressRatio() {
        int time = getIncubationTime();
        if (time <= 0) return 0;
        return (float) getIncubationProgress() / time;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            if (index == 0) {
                // From incubator to player
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to incubator
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
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
        return container.stillValid(player);
    }
}
