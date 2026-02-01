/**
 * ============================================================
 * [PoweredCentrifugeMenu.java]
 * Description: Menu pour la centrifugeuse automatique
 * ============================================================
 *
 * SLOTS:
 * - 1 slot entree (index 0)
 * - 4 slots sortie (index 1-4)
 * - 27 slots inventaire (index 5-31)
 * - 9 slots hotbar (index 32-40)
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
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

public class PoweredCentrifugeMenu extends BeemancerMenu {
    private final PoweredCentrifugeBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    // Slot indices
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 5;
    private static final int PLAYER_INV_START = 5;
    private static final int PLAYER_INV_END = 32;
    private static final int HOTBAR_START = 32;
    private static final int HOTBAR_END = 41;

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

        // Input slot (gauche)
        addSlot(BeemancerSlot.combInput(blockEntity.getInputSlot(), 0, 44, 35)
            .withFilter(stack -> stack.is(Items.HONEYCOMB) ||
                                stack.is(BeemancerItems.ROYAL_COMB.get()) ||
                                stack.is(BeemancerItems.COMMON_COMB.get()) ||
                                stack.is(BeemancerItems.NOBLE_COMB.get()) ||
                                stack.is(BeemancerItems.DILIGENT_COMB.get())));

        // Output slots (droite, 2x2)
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 0, 107, 26));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 1, 125, 26));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 2, 107, 44));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 3, 125, 44));

        // Player inventory
        addPlayerInventory(playerInv, 8, 88);
        addPlayerHotbar(playerInv, 8, 146);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getFuelAmount() { return data.get(2); }
    public int getOutputAmount() { return data.get(3); }

    public float getProgressRatio() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }

    public PoweredCentrifugeBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // From input slot to player
            if (index == INPUT_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From output slots to player
            else if (index >= OUTPUT_START && index < OUTPUT_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory
            else {
                // Try to move to input slot if valid comb
                if (isValidComb(stack)) {
                    if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private boolean isValidComb(ItemStack stack) {
        return stack.is(Items.HONEYCOMB) ||
               stack.is(BeemancerItems.ROYAL_COMB.get()) ||
               stack.is(BeemancerItems.COMMON_COMB.get()) ||
               stack.is(BeemancerItems.NOBLE_COMB.get()) ||
               stack.is(BeemancerItems.DILIGENT_COMB.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.POWERED_CENTRIFUGE.get());
    }
}
