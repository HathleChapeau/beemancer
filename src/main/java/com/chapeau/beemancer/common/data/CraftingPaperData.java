/**
 * ============================================================
 * [CraftingPaperData.java]
 * Description: Donnees de recette inscrites sur un Crafting Paper
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DataComponents      | Stockage item data   | CUSTOM_DATA sur ItemStack      |
 * | CustomData          | Wrapper CompoundTag  | Serialisation sur items        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CraftingPaperItem (tooltip + inscription)
 * - CrafterBlockEntity (lecture recettes de la bibliotheque)
 * - CraftTaskManager (resolution des sous-crafts)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record CraftingPaperData(List<ItemStack> ingredients, ItemStack result) {

    private static final String ROOT_KEY = "CraftingPaper";
    private static final String INGREDIENTS_KEY = "Ingredients";
    private static final String RESULT_KEY = "Result";
    private static final int GRID_SIZE = 9;

    public static CompoundTag saveToTag(CraftingPaperData data, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        ListTag ingredientsList = new ListTag();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = i < data.ingredients.size() ? data.ingredients.get(i) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                ingredientsList.add(new CompoundTag());
            } else {
                ingredientsList.add(stack.save(registries));
            }
        }
        tag.put(INGREDIENTS_KEY, ingredientsList);

        if (!data.result.isEmpty()) {
            tag.put(RESULT_KEY, data.result.save(registries));
        }

        return tag;
    }

    public static CraftingPaperData loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        List<ItemStack> ingredients = new ArrayList<>(GRID_SIZE);
        if (tag.contains(INGREDIENTS_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(INGREDIENTS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < GRID_SIZE; i++) {
                if (i < list.size()) {
                    CompoundTag itemTag = list.getCompound(i);
                    ingredients.add(ItemStack.parseOptional(registries, itemTag));
                } else {
                    ingredients.add(ItemStack.EMPTY);
                }
            }
        } else {
            for (int i = 0; i < GRID_SIZE; i++) {
                ingredients.add(ItemStack.EMPTY);
            }
        }

        ItemStack result = ItemStack.EMPTY;
        if (tag.contains(RESULT_KEY, Tag.TAG_COMPOUND)) {
            result = ItemStack.parseOptional(registries, tag.getCompound(RESULT_KEY));
        }

        return new CraftingPaperData(ingredients, result);
    }

    public static void applyToStack(ItemStack stack, CraftingPaperData data, HolderLookup.Provider registries) {
        CompoundTag rootTag = getOrCreateRootTag(stack);
        rootTag.put(ROOT_KEY, saveToTag(data, registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    @Nullable
    public static CraftingPaperData readFromStack(ItemStack stack, HolderLookup.Provider registries) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag rootTag = customData.copyTag();
        if (!rootTag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return null;

        return loadFromTag(rootTag.getCompound(ROOT_KEY), registries);
    }

    public static boolean hasData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().contains(ROOT_KEY, Tag.TAG_COMPOUND);
    }

    private static CompoundTag getOrCreateRootTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return new CompoundTag();
    }
}
