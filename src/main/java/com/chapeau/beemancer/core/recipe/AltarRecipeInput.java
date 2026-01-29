/**
 * ============================================================
 * [AltarRecipeInput.java]
 * Description: Input pour les recettes d'Altar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RecipeInput         | Interface Minecraft  | Input pour Recipe.matches()    |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;
import java.util.Map;

public record AltarRecipeInput(
    ItemStack centerItem,
    List<ItemStack> pedestalItems,
    Map<Item, Integer> availablePollen
) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        if (index == 0) {
            return centerItem;
        }
        int pedestalIndex = index - 1;
        if (pedestalIndex >= 0 && pedestalIndex < pedestalItems.size()) {
            return pedestalItems.get(pedestalIndex);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1 + pedestalItems.size();
    }
}
