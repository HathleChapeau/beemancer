/**
 * ============================================================
 * [ItemFilterData.java]
 * Description: Donnees de filtrage pour les item pipes (ghost slots, mode, priority)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStackHandler    | Stockage ghost items | 9 slots de filtrage            |
 * | CompoundTag         | Serialisation NBT    | Sauvegarde/chargement          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ItemPipeBlockEntity.java (stockage du filtre)
 * - ItemFilterMenu.java (GUI du filtre)
 * - PipeNetwork.java (routage item-aware)
 *
 * ============================================================
 */
package com.chapeau.apica.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * POJO contenant les donnees de filtrage d'un item pipe.
 * 9 ghost slots, mode Accept/Deny, priority.
 */
public class ItemFilterData {

    public static final int SLOT_COUNT = 9;

    private final ItemStackHandler ghostItems = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private FilterMode mode = FilterMode.DENY;
    private int priority = 0;

    public enum FilterMode {
        ACCEPT, DENY
    }

    /**
     * Verifie si un ItemStack passe le filtre.
     * - Accept: l'item doit etre dans les slots. Si aucun slot rempli → rien ne passe.
     * - Deny: l'item ne doit PAS etre dans les slots. Si aucun slot rempli → tout passe.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        boolean hasAnySlotFilled = false;
        boolean foundInSlots = false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack ghost = ghostItems.getStackInSlot(i);
            if (!ghost.isEmpty()) {
                hasAnySlotFilled = true;
                if (ItemStack.isSameItemSameComponents(ghost, stack)) {
                    foundInSlots = true;
                    break;
                }
            }
        }

        if (mode == FilterMode.ACCEPT) {
            return hasAnySlotFilled && foundInSlots;
        } else {
            return !foundInSlots;
        }
    }

    public ItemStackHandler getGhostItems() {
        return ghostItems;
    }

    public void setGhostSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        ghostItems.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public FilterMode getMode() {
        return mode;
    }

    public void setMode(FilterMode mode) {
        this.mode = mode;
    }

    public void toggleMode() {
        this.mode = (mode == FilterMode.ACCEPT) ? FilterMode.DENY : FilterMode.ACCEPT;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("GhostItems", ghostItems.serializeNBT(registries));
        tag.putString("Mode", mode.name());
        tag.putInt("Priority", priority);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("GhostItems")) {
            ghostItems.deserializeNBT(registries, tag.getCompound("GhostItems"));
        }
        if (tag.contains("Mode")) {
            try {
                mode = FilterMode.valueOf(tag.getString("Mode"));
            } catch (IllegalArgumentException e) {
                mode = FilterMode.DENY;
            }
        }
        if (tag.contains("Priority")) {
            priority = tag.getInt("Priority");
        }
    }

    public static ItemFilterData fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        ItemFilterData data = new ItemFilterData();
        data.load(tag, registries);
        return data;
    }
}
