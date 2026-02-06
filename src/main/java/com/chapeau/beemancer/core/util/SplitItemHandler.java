/**
 * ============================================================
 * [SplitItemHandler.java]
 * Description: IItemHandler wrapper qui route insertItem() vers un handler input et extractItem() vers un handler output
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IItemHandler        | Interface implémentée| Exposition capability          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CentrifugeHeartBlockEntity (inputSlot insert, outputSlots extract)
 * - Tout multibloc nécessitant une séparation insert/extract
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Wrapper IItemHandler qui sépare les opérations d'insertion et d'extraction.
 * insertItem() est routé vers le handler d'entrée (ex: input slot).
 * extractItem() est routé vers le handler de sortie (ex: output slots).
 * Les slots logiques sont mappés : [0..inputCount-1] = input, [inputCount..total-1] = output.
 */
public class SplitItemHandler implements IItemHandler {

    private final IItemHandler inputHandler;
    private final IItemHandler outputHandler;
    private final int inputSlotCount;

    public SplitItemHandler(IItemHandler inputHandler, IItemHandler outputHandler) {
        this.inputHandler = inputHandler;
        this.outputHandler = outputHandler;
        this.inputSlotCount = inputHandler.getSlots();
    }

    @Override
    public int getSlots() {
        return inputSlotCount + outputHandler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < inputSlotCount) {
            return inputHandler.getStackInSlot(slot);
        }
        return outputHandler.getStackInSlot(slot - inputSlotCount);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < inputSlotCount) {
            return inputHandler.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= inputSlotCount) {
            return outputHandler.extractItem(slot - inputSlotCount, amount, simulate);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < inputSlotCount) {
            return inputHandler.getSlotLimit(slot);
        }
        return outputHandler.getSlotLimit(slot - inputSlotCount);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < inputSlotCount) {
            return inputHandler.isItemValid(slot, stack);
        }
        return false;
    }
}
