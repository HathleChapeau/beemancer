/**
 * ============================================================
 * [PoweredCentrifugeMenu.java]
 * Description: Menu pour la centrifugeuse automatique
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

public class PoweredCentrifugeMenu extends AbstractContainerMenu {
    private final PoweredCentrifugeBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    // Client constructor
    public PoweredCentrifugeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()), 
             new SimpleContainerData(4));
    }

    // Server constructor
    public PoweredCentrifugeMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.POWERED_CENTRIFUGE.get(), containerId);
        this.blockEntity = (PoweredCentrifugeBlockEntity) be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        addDataSlots(data);

        // Input slots (left side)
        addSlotBox(blockEntity.getInputSlots(), 0, 26, 17, 2, 2, true);
        
        // Output slots (right side)
        addSlotBox(blockEntity.getOutputSlots(), 0, 116, 17, 2, 2, false);

        // Player inventory
        addPlayerInventory(playerInv, 8, 84);
        addPlayerHotbar(playerInv, 8, 142);
    }

    private void addSlotBox(IItemHandler handler, int startIndex, int x, int y, int cols, int rows, boolean isInput) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = startIndex + col + row * cols;
                int slotX = x + col * 18;
                int slotY = y + row * 18;
                if (isInput) {
                    // Input slots avec icone de comb et filtre
                    addSlot(BeemancerSlot.combInput(handler, index, slotX, slotY)
                        .withFilter(stack -> stack.is(Items.HONEYCOMB) ||
                                            stack.is(BeemancerItems.ROYAL_COMB.get()) ||
                                            stack.is(BeemancerItems.COMMON_COMB.get()) ||
                                            stack.is(BeemancerItems.NOBLE_COMB.get()) ||
                                            stack.is(BeemancerItems.DILIGENT_COMB.get())));
                } else {
                    // Output slots (extraction seulement)
                    addSlot(BeemancerSlot.output(handler, index, slotX, slotY));
                }
            }
        }
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

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return 100; }
    public int getFuelAmount() { return data.get(2); }
    public int getOutputAmount() { return data.get(3); }
    
    public float getProgressRatio() {
        return getMaxProgress() > 0 ? (float) getProgress() / getMaxProgress() : 0;
    }

    public PoweredCentrifugeBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // Slots: 0-3 input, 4-7 output, 8-34 inv, 35-43 hotbar
            if (index < 4) {
                // From input to player
                if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY;
            } else if (index < 8) {
                // From output to player
                if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY;
            } else {
                // From player to input
                if (stack.is(Items.HONEYCOMB) || stack.is(BeemancerItems.ROYAL_COMB.get())) {
                    if (!moveItemStackTo(stack, 0, 4, false)) return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.POWERED_CENTRIFUGE.get());
    }
}
