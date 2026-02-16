/**
 * ============================================================
 * [DepositStrategy.java]
 * Description: Strategie de depot et extraction d'items dans le reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AggregationCache    | Cache et index        | Lookups O(1) et mises a jour   |
 * | ContainerHelper     | Manipulation items    | Insert/extract dans handlers   |
 * | StorageHelper       | Acces inventaires     | getItemHandler                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageItemAggregator.java (delegation deposit/extract)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.core.util.ContainerHelper;
import com.chapeau.apica.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Gere le depot et l'extraction d'items dans le reseau.
 * Utilise un systeme de priorites a 5 niveaux pour le placement intelligent.
 */
class DepositStrategy {
    private final AggregationCache cache;

    DepositStrategy(AggregationCache cache) {
        this.cache = cache;
    }

    /**
     * Trouve un slot pour deposer un item avec priorite intelligente:
     * 1. Coffre contenant le meme item exact (via index O(1))
     * 2. Coffre contenant un item partageant un tag commun
     * 3. Coffre contenant un item du meme mod/namespace
     * 4. Coffre totalement vide
     * 5. N'importe quel coffre avec un slot vide
     */
    @Nullable
    BlockPos findSlotForItem(ItemStack stack, Set<BlockPos> chests, Level level) {
        // Priorite 1: Index de contenu pour meme item (O(1) lookup)
        ItemStackKey key = new ItemStackKey(stack);
        Set<BlockPos> indexedChests = cache.getChestsContaining(key);
        for (BlockPos chestPos : indexedChests) {
            if (!chests.contains(chestPos) || !level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler == null) continue;
            boolean hasEmptySlot = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack existing = handler.getStackInSlot(i);
                if (existing.isEmpty()) {
                    hasEmptySlot = true;
                } else if (ItemStack.isSameItemSameComponents(existing, stack)
                           && existing.getCount() < existing.getMaxStackSize()) {
                    return chestPos;
                }
            }
            if (hasEmptySlot) return chestPos;
        }

        // Priorite 2: Tag commun
        var stackTags = stack.getTags().toList();
        if (!stackTags.isEmpty()) {
            for (BlockPos chestPos : chests) {
                if (!level.hasChunkAt(chestPos)) continue;
                IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
                if (handler == null) continue;
                boolean hasSharedTag = false;
                boolean hasEmptySlot = false;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existing = handler.getStackInSlot(i);
                    if (existing.isEmpty()) {
                        hasEmptySlot = true;
                    } else if (!hasSharedTag) {
                        for (var tag : stackTags) {
                            if (existing.is(tag)) { hasSharedTag = true; break; }
                        }
                    }
                }
                if (hasSharedTag && hasEmptySlot) return chestPos;
            }
        }

        // Priorite 3: Meme namespace
        String stackNamespace = stack.getItem().builtInRegistryHolder().key().location().getNamespace();
        for (BlockPos chestPos : chests) {
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler == null) continue;
            boolean hasSameNamespace = false;
            boolean hasEmptySlot = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack existing = handler.getStackInSlot(i);
                if (existing.isEmpty()) {
                    hasEmptySlot = true;
                } else if (!hasSameNamespace) {
                    String ns = existing.getItem().builtInRegistryHolder().key().location().getNamespace();
                    if (ns.equals(stackNamespace)) hasSameNamespace = true;
                }
            }
            if (hasSameNamespace && hasEmptySlot) return chestPos;
        }

        // Priorite 4: Coffre totalement vide
        for (BlockPos chestPos : chests) {
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler == null) continue;
            boolean allEmpty = true;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) { allEmpty = false; break; }
            }
            if (allEmpty && handler.getSlots() > 0) return chestPos;
        }

        // Priorite 5: N'importe quel slot vide
        for (BlockPos chestPos : chests) {
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).isEmpty()) return chestPos;
            }
        }

        return null;
    }

    /**
     * Depose un item dans le reseau.
     * Pass 1: coffres contenant deja l'item. Pass 2: tout coffre avec slot vide.
     */
    ItemStack depositItem(ItemStack stack, Set<BlockPos> chests, Level level) {
        if (stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();
        Set<BlockPos> affectedChests = new HashSet<>();

        // Pass 1: coffres contenant le meme item
        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler != null && ContainerHelper.countItem(handler, remaining) > 0) {
                int before = remaining.getCount();
                remaining = ContainerHelper.insertItem(handler, remaining);
                if (remaining.getCount() < before) {
                    cache.invalidateChest(chestPos);
                    affectedChests.add(chestPos);
                }
            }
        }

        // Pass 2: tout coffre avec slot vide
        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler != null) {
                int before = remaining.getCount();
                remaining = ContainerHelper.insertItem(handler, remaining);
                if (remaining.getCount() < before) {
                    cache.invalidateChest(chestPos);
                    affectedChests.add(chestPos);
                }
            }
        }

        int deposited = stack.getCount() - remaining.getCount();
        if (deposited > 0) {
            cache.updateForDeposit(new ItemStackKey(stack), deposited, affectedChests);
        }
        return remaining;
    }

    /**
     * Extrait un item du reseau.
     */
    ItemStack extractItem(ItemStack template, int count, Set<BlockPos> chests, Level level) {
        if (template.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (BlockPos chestPos : chests) {
            if (needed <= 0) break;
            if (!level.hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler != null) {
                ItemStack extracted = ContainerHelper.extractItem(handler, template, needed);
                if (!extracted.isEmpty()) {
                    result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                    cache.invalidateChest(chestPos);
                }
            }
        }

        int totalExtracted = result.getCount();
        if (totalExtracted > 0) {
            cache.updateForExtract(new ItemStackKey(template), totalExtracted);
        }
        return result;
    }
}
