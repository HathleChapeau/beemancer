/**
 * ============================================================
 * [ItemFilterData.java]
 * Description: Donnees de filtrage pour les item pipes (ghost slots, texte, mode, priority)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStackHandler    | Stockage ghost items | 9 slots de filtrage            |
 * | TextFilterMatcher   | Matching texte       | @mod, #tag, substring          |
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

import com.chapeau.apica.core.util.TextFilterMatcher;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * POJO contenant les donnees de filtrage d'un item pipe.
 * Supporte deux modes d'input : ghost slots (SLOT) ou champ texte (TEXT).
 * Le mode Accept/Deny s'applique dans les deux cas.
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
    private InputMode inputMode = InputMode.SLOT;
    private String textFilter = "";
    private int priority = 0;

    public enum FilterMode {
        ACCEPT, DENY
    }

    public enum InputMode {
        SLOT, TEXT
    }

    /**
     * Verifie si un ItemStack passe le filtre.
     * Dispatch selon inputMode (SLOT ou TEXT), puis applique Accept/Deny.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (inputMode == InputMode.TEXT) {
            return matchesTextMode(stack);
        }
        return matchesSlotMode(stack);
    }

    /**
     * Mode SLOT : matching par ghost slots.
     * Accept: l'item doit etre dans les slots. Si aucun slot rempli → rien ne passe.
     * Deny: l'item ne doit PAS etre dans les slots. Si aucun slot rempli → tout passe.
     */
    private boolean matchesSlotMode(ItemStack stack) {
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

    /**
     * Mode TEXT : matching par TextFilterMatcher (@mod, #tag, substring).
     * Accept: l'item doit matcher le texte. Texte vide → rien ne passe.
     * Deny: l'item ne doit PAS matcher le texte. Texte vide → tout passe.
     */
    private boolean matchesTextMode(ItemStack stack) {
        if (textFilter.isEmpty()) {
            return mode == FilterMode.DENY;
        }
        boolean textMatch = TextFilterMatcher.matches(stack, textFilter);
        return mode == FilterMode.ACCEPT ? textMatch : !textMatch;
    }

    // === Ghost Items ===

    public ItemStackHandler getGhostItems() {
        return ghostItems;
    }

    public void setGhostSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        ghostItems.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    // === Filter Mode (Accept/Deny) ===

    public FilterMode getMode() {
        return mode;
    }

    public void setMode(FilterMode mode) {
        this.mode = mode;
    }

    public void toggleMode() {
        this.mode = (mode == FilterMode.ACCEPT) ? FilterMode.DENY : FilterMode.ACCEPT;
    }

    // === Input Mode (Slot/Text) ===

    public InputMode getInputMode() {
        return inputMode;
    }

    public void setInputMode(InputMode inputMode) {
        this.inputMode = inputMode;
    }

    public void toggleInputMode() {
        this.inputMode = (inputMode == InputMode.SLOT) ? InputMode.TEXT : InputMode.SLOT;
    }

    // === Text Filter ===

    public String getTextFilter() {
        return textFilter;
    }

    public void setTextFilter(String text) {
        this.textFilter = text != null ? text : "";
    }

    // === Priority ===

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("GhostItems", ghostItems.serializeNBT(registries));
        tag.putString("Mode", mode.name());
        tag.putString("InputMode", inputMode.name());
        tag.putString("TextFilter", textFilter);
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
        if (tag.contains("InputMode")) {
            try {
                inputMode = InputMode.valueOf(tag.getString("InputMode"));
            } catch (IllegalArgumentException e) {
                inputMode = InputMode.SLOT;
            }
        }
        textFilter = tag.contains("TextFilter") ? tag.getString("TextFilter") : "";
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
