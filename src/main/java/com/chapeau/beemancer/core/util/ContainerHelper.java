/**
 * ============================================================
 * [ContainerHelper.java]
 * Description: Utilitaire statique pour operations sur Container (insert, extract, count, space)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Container           | Interface Minecraft  | Operations inventaire          |
 * | ItemStack           | Items Minecraft      | Templates et transferts        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageDeliveryManager.java (depot/extraction coffres)
 * - DeliveryPhaseGoal.java (depot dans containers)
 * - ImportInterfaceBlockEntity.java (insertion slots filtres)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Operations generiques sur Container: insertion, extraction, comptage, espace disponible.
 * Remplace les patterns dupliques dans StorageDeliveryManager, DeliveryPhaseGoal,
 * ImportInterfaceBlockEntity.
 */
public final class ContainerHelper {

    private ContainerHelper() {}

    /**
     * Insere un ItemStack dans un Container sur tous les slots.
     * Merge d'abord dans les stacks existants, puis remplit les slots vides.
     *
     * @param container le container cible
     * @param stack l'item a inserer (non modifie)
     * @return le reste non insere (EMPTY si tout insere)
     */
    public static ItemStack insertItem(Container container, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int[] allSlots = allSlots(container);
        return insertItem(container, stack, allSlots);
    }

    /**
     * Insere un ItemStack dans un Container sur les slots specifies.
     * Merge d'abord dans les stacks existants, puis remplit les slots vides.
     *
     * @param container le container cible
     * @param stack l'item a inserer (non modifie)
     * @param slots les indices de slots autorises
     * @return le reste non insere (EMPTY si tout insere)
     */
    public static ItemStack insertItem(Container container, ItemStack stack, int[] slots) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();

        // Phase 1: merger dans les stacks existants du meme type
        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toTransfer = Math.min(space, remaining.getCount());
                if (toTransfer > 0) {
                    existing.grow(toTransfer);
                    remaining.shrink(toTransfer);
                }
            }
        }

        // Phase 2: remplir les slots vides
        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            if (container.getItem(slot).isEmpty()) {
                int toPlace = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                container.setItem(slot, remaining.copyWithCount(toPlace));
                remaining.shrink(toPlace);
            }
        }

        container.setChanged();
        return remaining;
    }

    /**
     * Extrait N items d'un type donne depuis un Container (tous les slots).
     *
     * @param container le container source
     * @param template le type d'item a extraire
     * @param count le nombre d'items a extraire
     * @return les items extraits (count peut etre inferieur a la demande si pas assez)
     */
    public static ItemStack extractItem(Container container, ItemStack template, int count) {
        if (template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                int toTake = Math.min(needed, existing.getCount());
                existing.shrink(toTake);
                result.grow(toTake);
                needed -= toTake;
                if (existing.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
            }
            if (needed <= 0) break;
        }

        container.setChanged();
        return result;
    }

    /**
     * Compte le nombre d'items d'un type dans un Container (tous les slots).
     *
     * @param container le container a analyser
     * @param template le type d'item a compter
     * @return le nombre total d'items de ce type
     */
    public static int countItem(Container container, ItemStack template) {
        if (template.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                count += existing.getCount();
            }
        }
        return count;
    }

    /**
     * Compte le nombre d'items d'un type dans des slots specifiques.
     *
     * @param container le container a analyser
     * @param template le type d'item a compter
     * @param slots les indices de slots a verifier
     * @return le nombre total d'items de ce type dans les slots donnes
     */
    public static int countItem(Container container, ItemStack template, int[] slots) {
        if (template.isEmpty()) return 0;
        int count = 0;
        for (int slot : slots) {
            ItemStack existing = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                count += existing.getCount();
            }
        }
        return count;
    }

    /**
     * Verifie si le container a de la place pour N items d'un type (tous les slots).
     *
     * @param container le container a verifier
     * @param template le type d'item
     * @param count le nombre d'items a inserer
     * @return true si au moins count items peuvent etre inseres
     */
    public static boolean hasSpaceFor(Container container, ItemStack template, int count) {
        return availableSpace(container, template) >= count;
    }

    /**
     * Calcule l'espace disponible total pour un type d'item dans un Container.
     *
     * @param container le container a analyser
     * @param template le type d'item
     * @return le nombre d'items de ce type qui peuvent encore etre inseres
     */
    public static int availableSpace(Container container, ItemStack template) {
        if (template.isEmpty()) return 0;
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty()) {
                space += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, template)) {
                space += existing.getMaxStackSize() - existing.getCount();
            }
        }
        return space;
    }

    /**
     * Genere un tableau de tous les indices de slots d'un Container.
     */
    private static int[] allSlots(Container container) {
        int[] slots = new int[container.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
