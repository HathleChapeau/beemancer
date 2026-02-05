/**
 * ============================================================
 * [ManualCentrifugeMenu.java]
 * Description: Menu pour la centrifugeuse manuelle
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
import com.chapeau.beemancer.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.common.quest.QuestEvents;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ManualCentrifugeMenu extends BeemancerMenu {
    private final ManualCentrifugeBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Slot indices
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 5;
    private static final int PLAYER_INV_START = 5;
    private static final int PLAYER_INV_END = 32;
    private static final int HOTBAR_START = 32;
    private static final int HOTBAR_END = 41;

    public ManualCentrifugeMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(3));
    }

    public ManualCentrifugeMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.MANUAL_CENTRIFUGE.get(), containerId);
        this.blockEntity = (ManualCentrifugeBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Input slot (gauche) - filtre par recette de centrifugation
        addSlot(BeemancerSlot.combInput(blockEntity.getInputSlot(), 0, 33, 35)
            .withFilter(stack -> blockEntity.isValidComb(stack)));

        // Output slots (droite, 2x2) avec callback pour les quêtes
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 0, 109, 26)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 1, 127, 26)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 2, 109, 44)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));
        addSlot(BeemancerSlot.output(blockEntity.getOutputSlots(), 3, 127, 44)
                .withOnExtract((p, s) -> QuestEvents.onMachineExtract(p, "manual_centrifuge", s)));

        // Player inventory
        addPlayerInventory(playerInv, 8, 88);
        addPlayerHotbar(playerInv, 8, 146);
    }

    public ManualCentrifugeBlockEntity getBlockEntity() { return blockEntity; }
    public int getProgress() { return data.get(0); }
    public int getFluidAmount() { return data.get(1); }
    public int getMaxProgress() { return data.get(2); }

    /**
     * Alias pour compatibilite avec ManualCentrifugeScreen
     */
    public int getSpinCount() { return data.get(0); }

    public float getProgressRatio() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }

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
                // Try to move to input slot if valid
                if (blockEntity.isValidComb(stack)) {
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.MANUAL_CENTRIFUGE.get());
    }
}
