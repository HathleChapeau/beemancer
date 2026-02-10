/**
 * ============================================================
 * [PartCraftingPaperData.java]
 * Description: Donnees de recette machine inscrites sur un Part Crafting Paper
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
 * - PartCraftingPaperItem (tooltip + inscription)
 * - NetworkInterfaceBlockEntity (craft mode filter)
 * - CraftTaskManager (resolution crafts machine)
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
import java.util.UUID;

public record PartCraftingPaperData(PartMode mode, UUID craftId, List<ItemStack> items) {

    private static final String ROOT_KEY = "PartCraftingPaper";
    private static final String MODE_KEY = "Mode";
    private static final String CRAFT_ID_KEY = "CraftId";
    private static final String ITEMS_KEY = "Items";

    public enum PartMode {
        INPUT,
        OUTPUT
    }

    public static CompoundTag saveToTag(PartCraftingPaperData data, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        tag.putString(MODE_KEY, data.mode.name());
        tag.putUUID(CRAFT_ID_KEY, data.craftId);

        ListTag itemsList = new ListTag();
        for (ItemStack stack : data.items) {
            if (stack.isEmpty()) {
                itemsList.add(new CompoundTag());
            } else {
                itemsList.add(stack.save(registries));
            }
        }
        tag.put(ITEMS_KEY, itemsList);

        return tag;
    }

    public static PartCraftingPaperData loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        PartMode mode = PartMode.INPUT;
        if (tag.contains(MODE_KEY, Tag.TAG_STRING)) {
            try {
                mode = PartMode.valueOf(tag.getString(MODE_KEY));
            } catch (IllegalArgumentException ignored) {
                // Default to INPUT if corrupt
            }
        }

        UUID craftId = tag.hasUUID(CRAFT_ID_KEY) ? tag.getUUID(CRAFT_ID_KEY) : UUID.randomUUID();

        List<ItemStack> items = new ArrayList<>();
        if (tag.contains(ITEMS_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ITEMS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }

        return new PartCraftingPaperData(mode, craftId, items);
    }

    public static void applyToStack(ItemStack stack, PartCraftingPaperData data, HolderLookup.Provider registries) {
        CompoundTag rootTag = getOrCreateRootTag(stack);
        rootTag.put(ROOT_KEY, saveToTag(data, registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    @Nullable
    public static PartCraftingPaperData readFromStack(ItemStack stack, HolderLookup.Provider registries) {
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
