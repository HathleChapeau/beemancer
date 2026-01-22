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
 * | HolderLookup        | Serialization 1.21   | Sauvegarde NBT des Items       |
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

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Inventaire interne pour les abeilles récolteuses.
 * Adapté pour Minecraft 1.21+ (DataComponents).
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

    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

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

        // 1. Essayer de merger avec les stacks existants
        for (int i = 0; i < maxSize && !toInsert.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            // isSameItemSameComponents est CRUCIAL en 1.21 pour vérifier les composants (ex: enchantements différents)
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, toInsert.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    toInsert.shrink(toAdd);
                }
            }
        }

        // 2. Remplir les slots vides
        for (int i = 0; i < maxSize && !toInsert.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                // On clone pour éviter les références partagées
                items.set(i, toInsert.copy());
                toInsert.setCount(0); // Tout a été ajouté
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
     * Récupère un item spécifique.
     */
    public ItemStack extractItem(Predicate<ItemStack> predicate, int count) {
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

    public boolean contains(Predicate<ItemStack> predicate) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        items.clear(); // NonNullList supporte clear() pour remettre à EMPTY
    }

    // ============================================================
    // GESTION NBT (Mise à jour 1.21)
    // ============================================================

    /**
     * Sauvegarde l'inventaire en NBT.
     * @param registries Indispensable en 1.21 pour sauvegarder les DataComponents.
     */
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();

        for (int i = 0; i < maxSize; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                // Utilisation de la méthode save avec registries
                listTag.add(stack.save(registries, itemTag));
            }
        }

        tag.put("Items", listTag);
        tag.putInt("MaxSize", maxSize);
        return tag;
    }

    /**
     * Charge l'inventaire depuis NBT.
     * @param registries Indispensable en 1.21 pour charger les DataComponents.
     */
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        clear();

        if (tag.contains("Items")) {
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");

                if (slot >= 0 && slot < maxSize) {
                    // Utilisation de parseOptional pour éviter les crashs sur des items corrompus
                    ItemStack stack = ItemStack.parseOptional(registries, itemTag);
                    items.set(slot, stack);
                }
            }
        }
    }

    public int getMaxSize() {
        return maxSize;
    }
}