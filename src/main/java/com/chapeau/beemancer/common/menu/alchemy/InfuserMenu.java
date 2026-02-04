/**
 * ============================================================
 * [InfuserMenu.java]
 * Description: Menu pour l'infuseur (standalone et multibloc)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | InfuserBlockEntity          | Standalone infuser   | Slots accesseurs      |
 * | InfuserHeartBlockEntity     | Multibloc infuser    | Slots accesseurs      |
 * | BeemancerBlocks             | Validation block     | stillValid            |
 * | BeemancerMenus              | Type menu            | Registration          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InfuserBlockEntity.java (createMenu)
 * - InfuserHeartBlockEntity.java (createMenu)
 * - InfuserScreen.java (client GUI)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.alchemy;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.blockentity.alchemy.InfuserBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.InfuserHeartBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class InfuserMenu extends BeemancerMenu {
    private final BlockEntity blockEntity;
    private final FluidTank honeyTank;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Slot indices
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    public InfuserMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()),
             new SimpleContainerData(3));
    }

    public InfuserMenu(int containerId, Inventory playerInv, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.INFUSER.get(), containerId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        ItemStackHandler inputHandler;
        ItemStackHandler outputHandler;
        if (be instanceof InfuserHeartBlockEntity heart) {
            inputHandler = heart.getInputSlot();
            outputHandler = heart.getOutputSlot();
            this.honeyTank = heart.getHoneyTank();
        } else {
            InfuserBlockEntity infuser = (InfuserBlockEntity) be;
            inputHandler = infuser.getInputSlot();
            outputHandler = infuser.getOutputSlot();
            this.honeyTank = infuser.getHoneyTank();
        }

        addDataSlots(data);

        // Input slot - accepte tout item valide pour une recette d'infusion
        addSlot(new BeemancerSlot(inputHandler, 0, 44, 35));

        // Output slot (extraction seulement)
        addSlot(BeemancerSlot.output(outputHandler, 0, 116, 35));

        addPlayerInventory(playerInv, 8, 88);
        addPlayerHotbar(playerInv, 8, 146);
    }

    public BlockEntity getBlockEntity() { return blockEntity; }
    public FluidTank getHoneyTank() { return honeyTank; }
    public int getProgress() { return data.get(0); }
    public int getProcessTime() { return data.get(1); }
    public int getHoneyAmount() { return data.get(2); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // From machine slots to player
            if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player to machine
            else {
                // Essayer de placer dans le slot d'input
                if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    // Si ca ne rentre pas, deplacer entre inventaire et hotbar
                    if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                        if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                        if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                            return ItemStack.EMPTY;
                        }
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
        return stillValid(access, player, BeemancerBlocks.INFUSER.get())
            || stillValid(access, player, BeemancerBlocks.INFUSER_HEART.get());
    }
}
