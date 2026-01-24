/**
 * ============================================================
 * [ProcessingRecipeInput.java]
 * Description: Input combine pour les recettes de processing (items + fluides)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RecipeInput         | Interface Minecraft  | Compatibilite systeme recettes |
 * | ItemStack           | Items d'entree       | Verification ingredients       |
 * | FluidStack          | Fluides d'entree     | Verification fluides           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ProcessingRecipe (classe abstraite)
 * - AbstractProcessingBlockEntity
 * - Toutes les machines de processing
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record ProcessingRecipeInput(List<ItemStack> items, List<FluidStack> fluids) implements RecipeInput {

    public static ProcessingRecipeInput empty() {
        return new ProcessingRecipeInput(List.of(), List.of());
    }

    public static ProcessingRecipeInput ofItem(ItemStack item) {
        return new ProcessingRecipeInput(List.of(item), List.of());
    }

    public static ProcessingRecipeInput ofFluid(FluidStack fluid) {
        return new ProcessingRecipeInput(List.of(), List.of(fluid));
    }

    public static ProcessingRecipeInput of(ItemStack item, FluidStack fluid) {
        return new ProcessingRecipeInput(List.of(item), List.of(fluid));
    }

    public static ProcessingRecipeInput of(List<ItemStack> items, List<FluidStack> fluids) {
        return new ProcessingRecipeInput(items, fluids);
    }

    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return items.size();
    }

    public FluidStack getFluid(int index) {
        if (index >= 0 && index < fluids.size()) {
            return fluids.get(index);
        }
        return FluidStack.EMPTY;
    }

    public int fluidSize() {
        return fluids.size();
    }

    public boolean hasItems() {
        return !items.isEmpty() && items.stream().anyMatch(s -> !s.isEmpty());
    }

    public boolean hasFluids() {
        return !fluids.isEmpty() && fluids.stream().anyMatch(s -> !s.isEmpty());
    }
}
