/**
 * ============================================================
 * [InterfaceFilterManager.java]
 * Description: Gere les InterfaceFilters et les globalSelectedSlots pour une interface
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | InterfaceFilter               | Filtre individuel    | Liste de filtres geree         |
 * | NetworkInterfaceBlockEntity   | Parent               | setChanged, syncToClient       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceBlockEntity.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsule la liste des InterfaceFilters et les globalSelectedSlots.
 * Chaque modification trigger needsScan + setChanged + syncToClient via le parent.
 */
public class InterfaceFilterManager {

    private final List<InterfaceFilter> filters = new ArrayList<>();
    private final Set<Integer> globalSelectedSlots = new HashSet<>();
    private final NetworkInterfaceBlockEntity parent;

    public InterfaceFilterManager(NetworkInterfaceBlockEntity parent) {
        this.parent = parent;
    }

    // === Filter Accessors ===

    public List<InterfaceFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public int getFilterCount() {
        return filters.size();
    }

    @Nullable
    public InterfaceFilter getFilter(int index) {
        if (index < 0 || index >= filters.size()) return null;
        return filters.get(index);
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    // === Filter Mutations ===

    public void addFilter() {
        if (filters.size() >= InterfaceFilter.MAX_FILTERS) return;
        filters.add(new InterfaceFilter());
        notifyChanged();
    }

    public void removeFilter(int index) {
        if (index < 0 || index >= filters.size()) return;
        filters.remove(index);
        notifyChanged();
    }

    public void setFilterItem(int filterIdx, int slot, ItemStack stack) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setItem(slot, stack);
        notifyChanged();
    }

    public void clearFilterItem(int filterIdx, int slot) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.clearItem(slot);
        notifyChanged();
    }

    public void setFilterMode(int filterIdx, InterfaceFilter.FilterMode mode) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setMode(mode);
        notifyChanged();
    }

    public void setFilterText(int filterIdx, String text) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setTextFilter(text);
        notifyChanged();
    }

    public void setFilterQuantity(int filterIdx, int quantity) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setQuantity(quantity);
        notifyChanged();
    }

    public void setFilterSelectedSlots(int filterIdx, Set<Integer> slots) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setSelectedSlots(slots);
        notifyChanged();
    }

    public void setFilterInverted(int filterIdx, boolean inverted) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setInverted(inverted);
        notifyChanged();
    }

    // === Global Selected Slots ===

    public Set<Integer> getGlobalSelectedSlots() {
        return Collections.unmodifiableSet(globalSelectedSlots);
    }

    public void setGlobalSelectedSlots(Set<Integer> slots) {
        globalSelectedSlots.clear();
        globalSelectedSlots.addAll(slots);
        notifyChanged();
    }

    // === Matching ===

    /**
     * Verifie si un item correspond a au moins un filtre actif.
     * Si aucun filtre, retourne checkEmpty.
     */
    public boolean matchesAnyFilter(ItemStack stack, boolean checkEmpty) {
        if (filters.isEmpty()) return checkEmpty;
        for (InterfaceFilter filter : filters) {
            if (filter.matches(stack, false)) return true;
        }
        return false;
    }

    // === NBT ===

    public void save(CompoundTag parentTag, HolderLookup.Provider registries) {
        ListTag filtersTag = new ListTag();
        for (InterfaceFilter filter : filters) {
            filtersTag.add(filter.save(registries));
        }
        parentTag.put("Filters", filtersTag);

        if (!globalSelectedSlots.isEmpty()) {
            parentTag.putIntArray("GlobalSelectedSlots",
                globalSelectedSlots.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public void load(CompoundTag parentTag, HolderLookup.Provider registries) {
        filters.clear();
        if (parentTag.contains("Filters")) {
            ListTag filtersTag = parentTag.getList("Filters", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(filtersTag.size(), InterfaceFilter.MAX_FILTERS); i++) {
                try {
                    InterfaceFilter filter = new InterfaceFilter();
                    filter.load(filtersTag.getCompound(i), registries);
                    filters.add(filter);
                } catch (Exception e) {
                    com.chapeau.apica.Apica.LOGGER.warn("Skipping corrupted filter at index {}", i, e);
                }
            }
        }

        globalSelectedSlots.clear();
        if (parentTag.contains("GlobalSelectedSlots")) {
            for (int s : parentTag.getIntArray("GlobalSelectedSlots")) {
                globalSelectedSlots.add(s);
            }
        }
    }

    // === Internal ===

    private void notifyChanged() {
        parent.markNeedsScan();
        parent.setChanged();
        parent.syncToClient();
    }
}
