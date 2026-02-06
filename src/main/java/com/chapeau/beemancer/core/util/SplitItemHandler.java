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
 * - Beemancer.java (factories inputOnly/outputOnly pour capabilities Infuser)
 * - Tout multibloc nécessitant une séparation insert/extract ou un accès unidirectionnel
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Wrapper IItemHandler qui sépare les opérations d'insertion et d'extraction.
 * Chaque slot possède des flags canInsert et canExtract (défaut true).
 *
 * Par défaut, les input slots ont canInsert=true/canExtract=false,
 * et les output slots ont canInsert=false/canExtract=true.
 *
 * Les pipes et l'automation respectent ces flags.
 * Les interfaces avec un filtre et un slot sélectionné peuvent bypass
 * via forceInsertItem() et forceExtractItem().
 */
public class SplitItemHandler implements IItemHandler {

    private final IItemHandler inputHandler;
    private final IItemHandler outputHandler;
    private final int inputSlotCount;
    private final boolean[] canInsert;
    private final boolean[] canExtract;

    public SplitItemHandler(IItemHandler inputHandler, IItemHandler outputHandler) {
        this.inputHandler = inputHandler;
        this.outputHandler = outputHandler;
        this.inputSlotCount = inputHandler.getSlots();

        int total = inputSlotCount + outputHandler.getSlots();
        this.canInsert = new boolean[total];
        this.canExtract = new boolean[total];

        for (int i = 0; i < total; i++) {
            this.canInsert[i] = i < inputSlotCount;
            this.canExtract[i] = i >= inputSlotCount;
        }
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
        if (slot < 0 || slot >= getSlots()) return stack;
        if (!canInsert[slot]) return stack;
        if (slot < inputSlotCount) {
            return inputHandler.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        if (!canExtract[slot]) return ItemStack.EMPTY;
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
        if (slot < 0 || slot >= getSlots()) return false;
        if (!canInsert[slot]) return false;
        if (slot < inputSlotCount) {
            return inputHandler.isItemValid(slot, stack);
        }
        return false;
    }

    // --- Bypass pour interfaces avec filtre + slot sélectionné ---

    /**
     * Insère un item en ignorant le flag canInsert.
     * Utilisé par les interfaces d'import/export avec un filtre ciblant un slot spécifique.
     */
    public ItemStack forceInsertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= getSlots()) return stack;
        if (slot < inputSlotCount) {
            return inputHandler.insertItem(slot, stack, simulate);
        }
        return outputHandler.insertItem(slot - inputSlotCount, stack, simulate);
    }

    /**
     * Extrait un item en ignorant le flag canExtract.
     * Utilisé par les interfaces d'import/export avec un filtre ciblant un slot spécifique.
     */
    public ItemStack forceExtractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
        if (slot < inputSlotCount) {
            return inputHandler.extractItem(slot, amount, simulate);
        }
        return outputHandler.extractItem(slot - inputSlotCount, amount, simulate);
    }

    // --- Accesseurs flags ---

    public void setCanInsert(int slot, boolean value) {
        if (slot >= 0 && slot < canInsert.length) canInsert[slot] = value;
    }

    public void setCanExtract(int slot, boolean value) {
        if (slot >= 0 && slot < canExtract.length) canExtract[slot] = value;
    }

    public boolean getCanInsert(int slot) {
        return slot >= 0 && slot < canInsert.length && canInsert[slot];
    }

    public boolean getCanExtract(int slot) {
        return slot >= 0 && slot < canExtract.length && canExtract[slot];
    }

    public int getInputSlotCount() {
        return inputSlotCount;
    }

    // --- Factories unidirectionnelles ---

    /**
     * Cree un handler qui autorise uniquement insertItem() (insertion).
     * extractItem() retourne toujours EMPTY.
     * Utilise pour IOMode.INPUT sur un handler specifique.
     */
    public static IItemHandler inputOnly(IItemHandler handler) {
        return new IItemHandler() {
            @Override public int getSlots() { return handler.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return handler.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return handler.insertItem(slot, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
            @Override public int getSlotLimit(int slot) { return handler.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return handler.isItemValid(slot, stack); }
        };
    }

    /**
     * Cree un handler qui autorise uniquement extractItem() (extraction).
     * insertItem() retourne toujours le stack non modifie.
     * Utilise pour IOMode.OUTPUT sur un handler specifique.
     */
    public static IItemHandler outputOnly(IItemHandler handler) {
        return new IItemHandler() {
            @Override public int getSlots() { return handler.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return handler.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return handler.extractItem(slot, amount, simulate); }
            @Override public int getSlotLimit(int slot) { return handler.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
        };
    }
}
