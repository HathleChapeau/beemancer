/**
 * ============================================================
 * [CentrifugeItemHandler.java]
 * Description: Item handler pour centrifuges avec filtrage combs
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IItemHandler        | Interface standard   | Implementation                 |
 * | ApicaTags           | Tag combs            | Filtrage input/output          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Apica.java (capability registration)
 *
 * ============================================================
 */
package com.chapeau.apica.core.util;

import com.chapeau.apica.core.registry.ApicaTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Wrapper IItemHandler pour les centrifuges.
 * - Input: accepte uniquement les items avec tag apica:combs
 * - Output: extrait uniquement les items SANS tag apica:combs
 */
public class CentrifugeItemHandler implements IItemHandler {

    private final ItemStackHandler inputSlot;
    private final ItemStackHandler outputSlots;

    public CentrifugeItemHandler(ItemStackHandler inputSlot, ItemStackHandler outputSlots) {
        this.inputSlot = inputSlot;
        this.outputSlots = outputSlots;
    }

    @Override
    public int getSlots() {
        return inputSlot.getSlots() + outputSlots.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < inputSlot.getSlots()) {
            return inputSlot.getStackInSlot(slot);
        }
        return outputSlots.getStackInSlot(slot - inputSlot.getSlots());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        // Only allow insertion into input slot (slot 0)
        if (slot != 0) return stack;
        // Only accept items with combs tag
        if (!stack.is(ApicaTags.Items.COMBS)) return stack;
        return inputSlot.insertItem(0, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        // Only allow extraction from output slots (slots 1+)
        if (slot < inputSlot.getSlots()) return ItemStack.EMPTY;
        int outputSlot = slot - inputSlot.getSlots();
        ItemStack stack = outputSlots.getStackInSlot(outputSlot);
        // Don't extract combs from output
        if (stack.is(ApicaTags.Items.COMBS)) return ItemStack.EMPTY;
        return outputSlots.extractItem(outputSlot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < inputSlot.getSlots()) {
            return inputSlot.getSlotLimit(slot);
        }
        return outputSlots.getSlotLimit(slot - inputSlot.getSlots());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // Only input slot accepts items, and only combs
        if (slot != 0) return false;
        return stack.is(ApicaTags.Items.COMBS);
    }
}
