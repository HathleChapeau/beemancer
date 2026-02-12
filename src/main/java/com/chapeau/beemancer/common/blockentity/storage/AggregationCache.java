/**
 * ============================================================
 * [AggregationCache.java]
 * Description: Caches d'agregation: totaux par type, index contenu coffres, fingerprints
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStackKey        | Cle d'agregation      | Maps de cache                  |
 * | StorageHelper       | Acces inventaires     | getItemHandler pour scan       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageItemAggregator.java (scan et coordination)
 * - DepositStrategy.java (lookups index, mises a jour cache)
 * - ViewerSyncManager.java (lecture aggregatedItems)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gere les caches d'agregation du reseau de stockage:
 * - Cache incrementiel des totaux par type d'item
 * - Index de contenu des coffres (quel coffre contient quel item)
 * - Fingerprints pour skip les coffres inchanges lors du scan
 */
class AggregationCache {

    private final Map<ItemStackKey, Integer> incrementalCache = new HashMap<>();
    private final Map<ItemStackKey, Set<BlockPos>> chestContentIndex = new HashMap<>();
    private final Map<BlockPos, Long> chestFingerprints = new HashMap<>();
    private final Map<BlockPos, Map<ItemStackKey, Integer>> cachedChestCounts = new HashMap<>();
    private List<ItemStack> aggregatedItems = new ArrayList<>();

    List<ItemStack> getAggregatedItems() {
        return Collections.unmodifiableList(aggregatedItems);
    }

    Set<BlockPos> getChestsContaining(ItemStackKey key) {
        return chestContentIndex.getOrDefault(key, Collections.emptySet());
    }

    /**
     * Scan complet de tous les coffres, reconstruit tous les caches.
     * Utilise les fingerprints pour sauter les coffres dont le contenu n'a pas change.
     */
    void scanAndRebuild(Set<BlockPos> validChests, Level level) {
        Map<ItemStackKey, Integer> itemCounts = new HashMap<>();
        Map<ItemStackKey, Set<BlockPos>> newContentIndex = new HashMap<>();
        Set<BlockPos> activeChests = new LinkedHashSet<>();

        for (BlockPos chestPos : validChests) {
            if (!level.hasChunkAt(chestPos)) continue;
            activeChests.add(chestPos);

            IItemHandler handler = StorageHelper.getItemHandler(level, chestPos, null);
            if (handler == null) continue;

            long fingerprint = computeFingerprint(handler);
            Long cached = chestFingerprints.get(chestPos);

            Map<ItemStackKey, Integer> chestCounts;
            if (cached != null && cached == fingerprint) {
                chestCounts = cachedChestCounts.get(chestPos);
            } else {
                chestCounts = new HashMap<>();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStackKey key = new ItemStackKey(stack);
                        chestCounts.merge(key, stack.getCount(), Integer::sum);
                    }
                }
                chestFingerprints.put(chestPos, fingerprint);
                cachedChestCounts.put(chestPos, chestCounts);
            }

            for (Map.Entry<ItemStackKey, Integer> chestEntry : chestCounts.entrySet()) {
                itemCounts.merge(chestEntry.getKey(), chestEntry.getValue(), Integer::sum);
                newContentIndex.computeIfAbsent(chestEntry.getKey(), k -> new HashSet<>()).add(chestPos);
            }
        }

        chestFingerprints.keySet().retainAll(activeChests);
        cachedChestCounts.keySet().retainAll(activeChests);

        incrementalCache.clear();
        incrementalCache.putAll(itemCounts);
        chestContentIndex.clear();
        chestContentIndex.putAll(newContentIndex);

        rebuildAggregatedList();
    }

    /**
     * Reconstruit aggregatedItems depuis le cache incrementiel. O(n_types).
     */
    void rebuildAggregatedList() {
        aggregatedItems = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : incrementalCache.entrySet()) {
            if (entry.getValue() > 0) {
                ItemStack stack = entry.getKey().toStack();
                stack.setCount(entry.getValue());
                aggregatedItems.add(stack);
            }
        }
    }

    void updateForDeposit(ItemStackKey key, int deposited, Set<BlockPos> affectedChests) {
        incrementalCache.merge(key, deposited, Integer::sum);
        for (BlockPos chestPos : affectedChests) {
            chestContentIndex.computeIfAbsent(key, k -> new HashSet<>()).add(chestPos);
        }
        rebuildAggregatedList();
    }

    void updateForExtract(ItemStackKey key, int extracted) {
        incrementalCache.computeIfPresent(key, (k, v) -> v - extracted > 0 ? v - extracted : null);
        rebuildAggregatedList();
    }

    void invalidateChest(BlockPos chestPos) {
        chestFingerprints.remove(chestPos);
        cachedChestCounts.remove(chestPos);
    }

    private long computeFingerprint(IItemHandler handler) {
        long hash = handler.getSlots();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                hash = hash * 31 + ItemStack.hashItemAndComponents(stack);
                hash = hash * 31 + stack.getCount();
            } else {
                hash = hash * 31;
            }
        }
        return hash;
    }
}
