/**
 * ============================================================
 * [BeeInventory.java]
 * Description: Inventaire interne pour les abeilles récolteuses
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java: Stockage des items récoltés
 * - HarvestingBehaviorGoal.java: Ajout/récupération des items
 * - MagicHiveBlockEntity.java: Dépôt des items
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventaire interne pour les abeilles récolteuses.
 * L'inventaire n'est pas visible par le joueur.
 * Si l'abeille meurt, l'inventaire est détruit (pas de drop).
 */
public class BeeInventory {
    
    private final NonNullList<ItemStack> items;
    private final int maxSize;
    
    public BeeInventory(int maxSize) {
        this.maxSize = maxSize;
        this.items = NonNullList.withSize(maxSize, ItemStack.EMPTY);
    }
    
    /**
     * Compte le nombre total d'items dans l'inventaire.
     */
    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Compte le nombre de stacks non vides.
     */
    public int getFilledSlotCount() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Vérifie si l'inventaire est vide.
     */
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Vérifie si l'inventaire est plein.
     */
    public boolean isFull() {
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Tente d'ajouter un item à l'inventaire.
     * @return Le reste qui n'a pas pu être ajouté (EMPTY si tout ajouté)
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack toInsert = stack.copy();
        
        // D'abord essayer de stack avec des items existants
        for (int i = 0; i < maxSize && !toInsert.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, toInsert.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    toInsert.shrink(toAdd);
                }
            }
        }
        
        // Puis chercher des slots vides
        for (int i = 0; i < maxSize && !toInsert.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int toAdd = Math.min(toInsert.getMaxStackSize(), toInsert.getCount());
                items.set(i, toInsert.copyWithCount(toAdd));
                toInsert.shrink(toAdd);
            }
        }
        
        return toInsert;
    }
    
    /**
     * Récupère tous les items et vide l'inventaire.
     */
    public List<ItemStack> extractAll() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                result.add(stack.copy());
                items.set(i, ItemStack.EMPTY);
            }
        }
        return result;
    }
    
    /**
     * Récupère un item spécifique (pour replanter par exemple).
     * @return L'item retiré ou EMPTY si non trouvé
     */
    public ItemStack extractItem(java.util.function.Predicate<ItemStack> predicate, int count) {
        for (int i = 0; i < maxSize; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && predicate.test(stack)) {
                int toExtract = Math.min(count, stack.getCount());
                ItemStack extracted = stack.copyWithCount(toExtract);
                stack.shrink(toExtract);
                if (stack.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Vérifie si l'inventaire contient un item correspondant au prédicat.
     */
    public boolean contains(java.util.function.Predicate<ItemStack> predicate) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Vide l'inventaire (utilisé quand l'abeille meurt).
     */
    public void clear() {
        for (int i = 0; i < maxSize; i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
    
    /**
     * Sauvegarde l'inventaire en NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        
        for (int i = 0; i < maxSize; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                listTag.add(stack.save(itemTag));
            }
        }
        
        tag.put("Items", listTag);
        tag.putInt("MaxSize", maxSize);
        return tag;
    }
    
    /**
     * Charge l'inventaire depuis NBT.
     */
    public void load(CompoundTag tag) {
        clear();
        
        if (tag.contains("Items")) {
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < maxSize) {
                    items.set(slot, ItemStack.parse(itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }
    }
    
    public int getMaxSize() {
        return maxSize;
    }
}
