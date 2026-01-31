/**
 * ============================================================
 * [InterfaceFilter.java]
 * Description: POJO representant un filtre individuel d'une Import/Export Interface
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ItemStackHandler              | Ghost items          | 5 slots de filtre item         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceBlockEntity.java (liste de filtres)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Un filtre individuel dans l'interface Import/Export.
 *
 * Chaque filtre a:
 * - Un mode (ITEM ou TEXT)
 * - 5 ghost slots pour le mode ITEM
 * - Un champ texte pour le mode TEXT (#tag, @mod, texte libre)
 * - Un ensemble de slots cibles dans l'inventaire adjacent
 * - Une quantite cible (0 = illimite)
 */
public class InterfaceFilter {

    public enum FilterMode {
        ITEM,
        TEXT
    }

    public static final int SLOTS_PER_FILTER = 5;
    public static final int MAX_FILTERS = 3;

    private FilterMode mode = FilterMode.ITEM;
    private final ItemStackHandler items = new ItemStackHandler(SLOTS_PER_FILTER);
    private String textFilter = "";
    private final Set<Integer> selectedSlots = new HashSet<>();
    private int quantity = 0;

    public InterfaceFilter() { }

    // === Mode ===

    public FilterMode getMode() {
        return mode;
    }

    public void setMode(FilterMode mode) {
        this.mode = mode;
    }

    // === Items (ghost slots) ===

    public ItemStackHandler getItems() {
        return items;
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS_PER_FILTER) return;
        items.setStackInSlot(slot, stack.copyWithCount(1));
    }

    public void clearItem(int slot) {
        if (slot < 0 || slot >= SLOTS_PER_FILTER) return;
        items.setStackInSlot(slot, ItemStack.EMPTY);
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SLOTS_PER_FILTER) return ItemStack.EMPTY;
        return items.getStackInSlot(slot);
    }

    public boolean hasAnyItem() {
        for (int i = 0; i < SLOTS_PER_FILTER; i++) {
            if (!items.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    // === Text Filter ===

    public String getTextFilter() {
        return textFilter;
    }

    public void setTextFilter(String text) {
        this.textFilter = text != null ? text : "";
    }

    public boolean hasTextFilter() {
        return !textFilter.isEmpty();
    }

    // === Selected Slots ===

    public Set<Integer> getSelectedSlots() {
        return selectedSlots;
    }

    public void setSelectedSlots(Set<Integer> slots) {
        selectedSlots.clear();
        selectedSlots.addAll(slots);
    }

    // === Quantity ===

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    // === Matching ===

    /**
     * Verifie si un item correspond a ce filtre.
     * Retourne true si le filtre est vide et checkEmpty est true.
     */
    public boolean matches(ItemStack stack, boolean checkEmpty) {
        if (mode == FilterMode.ITEM) {
            return matchesItem(stack, checkEmpty);
        } else {
            return matchesText(stack, checkEmpty);
        }
    }

    private boolean matchesItem(ItemStack stack, boolean checkEmpty) {
        boolean hasAny = false;
        for (int i = 0; i < SLOTS_PER_FILTER; i++) {
            ItemStack filter = items.getStackInSlot(i);
            if (!filter.isEmpty()) {
                hasAny = true;
                if (ItemStack.isSameItemSameComponents(filter, stack)) {
                    return true;
                }
            }
        }
        return !hasAny && checkEmpty;
    }

    private boolean matchesText(ItemStack stack, boolean checkEmpty) {
        if (textFilter.isEmpty()) return checkEmpty;

        if (textFilter.startsWith("#")) {
            String tagName = textFilter.substring(1);
            return matchesTag(stack, tagName);
        } else if (textFilter.startsWith("@")) {
            String namespace = textFilter.substring(1);
            String itemNs = stack.getItem().builtInRegistryHolder()
                .key().location().getNamespace();
            return itemNs.equals(namespace);
        } else {
            String displayName = stack.getHoverName().getString().toLowerCase();
            return displayName.contains(textFilter.toLowerCase());
        }
    }

    private boolean matchesTag(ItemStack stack, String tagName) {
        var tags = stack.getTags().toList();
        for (var tag : tags) {
            String fullPath = tag.location().toString();
            String path = tag.location().getPath();
            if (path.equals(tagName) || fullPath.equals(tagName)
                || path.contains(tagName)) {
                return true;
            }
        }
        return false;
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        tag.put("Items", items.serializeNBT(registries));
        tag.putString("TextFilter", textFilter);
        tag.putInt("Quantity", quantity);

        if (!selectedSlots.isEmpty()) {
            tag.putIntArray("SelectedSlots",
                selectedSlots.stream().mapToInt(Integer::intValue).toArray());
        }

        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            mode = FilterMode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException e) {
            mode = FilterMode.ITEM;
        }

        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }

        textFilter = tag.getString("TextFilter");
        quantity = tag.getInt("Quantity");

        selectedSlots.clear();
        if (tag.contains("SelectedSlots")) {
            for (int s : tag.getIntArray("SelectedSlots")) {
                selectedSlots.add(s);
            }
        }
    }
}
