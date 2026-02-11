/**
 * ============================================================
 * [StorageItemAggregator.java]
 * Description: Agrégation, dépôt et extraction d'items dans le réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Parent BlockEntity   | Back-reference pour level/pos  |
 * | StorageChestManager           | Coffres enregistrés  | Liste des coffres à scanner    |
 * | StorageItemsSyncPacket        | Sync réseau          | Envoi items aux viewers        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (délégation)
 * - StorageDeliveryManager.java (setNeedsSync)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.core.util.ContainerHelper;
import com.chapeau.beemancer.core.util.StorageHelper;
import com.chapeau.beemancer.core.network.packets.StorageItemsSyncPacket;
import com.chapeau.beemancer.core.network.packets.StorageTasksSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gère l'agrégation des items du réseau, le dépôt, l'extraction,
 * et la synchronisation vers les joueurs qui visualisent le terminal.
 * Utilise un delta sync incrémental inspiré d'AE2: seuls les changements
 * (ajouts, suppressions, modifications de count) sont envoyés au client.
 * Les nouveaux viewers reçoivent un full sync initial.
 * Aucun état persistant (NBT) — tout est recalculé au chargement.
 */
public class StorageItemAggregator {
    private final StorageControllerBlockEntity parent;

    private List<ItemStack> aggregatedItems = new ArrayList<>();
    private Map<ItemStackKey, Integer> previousSnapshot = new HashMap<>();
    private final Set<UUID> fullSyncPending = new LinkedHashSet<>();
    private boolean needsSync = false;
    private boolean dirty = false;
    private final Map<UUID, BlockPos> playersViewing = new HashMap<>();

    // Versioned inventory cache: skip rescanning unchanged chests
    private final Map<BlockPos, Long> chestFingerprints = new HashMap<>();
    private final Map<BlockPos, Map<ItemStackKey, Integer>> cachedChestCounts = new HashMap<>();

    private static final int SYNC_INTERVAL = 40;

    public StorageItemAggregator(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    // === Agrégation ===

    public List<ItemStack> getAggregatedItems() {
        return Collections.unmodifiableList(aggregatedItems);
    }

    /**
     * Force le recalcul des items agreges depuis tous les coffres enregistres.
     * Nettoie les coffres invalides du registre au passage.
     */
    public void refreshAggregatedItems() {
        if (parent.getLevel() == null) return;

        // Nettoyage des coffres invalides du registre central
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        Set<BlockPos> allChests = new LinkedHashSet<>(registry.getAllChests());
        StorageChestManager chestManager = parent.getChestManager();

        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : allChests) {
            if (!parent.getLevel().hasChunkAt(pos)) continue;
            if (!chestManager.isChest(pos)) {
                toRemove.add(pos);
            }
        }
        if (!toRemove.isEmpty()) {
            for (BlockPos pos : toRemove) {
                registry.unregisterBlock(pos);
                allChests.remove(pos);
            }
            parent.setChanged();
            parent.syncNodeToClient();
        }
        Map<ItemStackKey, Integer> itemCounts = new HashMap<>();
        Set<BlockPos> activeChests = new LinkedHashSet<>();
        for (BlockPos chestPos : allChests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            activeChests.add(chestPos);

            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
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
            }
        }

        // Nettoyer le cache des coffres retires du reseau
        chestFingerprints.keySet().retainAll(activeChests);
        cachedChestCounts.keySet().retainAll(activeChests);

        aggregatedItems = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : itemCounts.entrySet()) {
            ItemStack stack = entry.getKey().toStack();
            stack.setCount(entry.getValue());
            aggregatedItems.add(stack);
        }

        aggregatedItems.sort(Comparator.comparing(
            stack -> stack.getHoverName().getString()
        ));

        syncItemsToViewers();
    }

    /**
     * Envoie les items agrégés et les tâches aux joueurs qui ont le terminal ouvert.
     * Nouveaux viewers reçoivent un full sync. Les autres reçoivent un delta incrémental.
     */
    private void syncItemsToViewers() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;
        if (playersViewing.isEmpty()) {
            updateSnapshot();
            return;
        }

        List<TaskDisplayData> taskData = parent.getDeliveryManager().getTaskDisplayData();
        Map<ItemStackKey, Integer> currentSnapshot = buildCurrentSnapshot();
        List<ItemStack> deltaItems = computeDelta(previousSnapshot, currentSnapshot);
        previousSnapshot = currentSnapshot;

        for (Map.Entry<UUID, BlockPos> entry : playersViewing.entrySet()) {
            ServerPlayer player = parent.getLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;

            if (fullSyncPending.remove(entry.getKey())) {
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(entry.getValue(), true, aggregatedItems));
            } else if (!deltaItems.isEmpty()) {
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(entry.getValue(), false, deltaItems));
            }
            PacketDistributor.sendToPlayer(player,
                new StorageTasksSyncPacket(entry.getValue(), taskData));
        }
    }

    /**
     * Construit un snapshot des counts actuels (ItemStackKey -> totalCount).
     */
    private Map<ItemStackKey, Integer> buildCurrentSnapshot() {
        Map<ItemStackKey, Integer> snapshot = new HashMap<>();
        for (ItemStack stack : aggregatedItems) {
            snapshot.put(new ItemStackKey(stack), stack.getCount());
        }
        return snapshot;
    }

    /**
     * Met à jour le snapshot sans envoyer de packets (quand pas de viewers).
     */
    private void updateSnapshot() {
        previousSnapshot = buildCurrentSnapshot();
    }

    /**
     * Compare le snapshot précédent avec le snapshot actuel et produit la liste des deltas.
     * Un delta est un ItemStack avec:
     * - count > 0: item ajouté ou count modifié (nouveau count absolu)
     * - count = 0: item supprimé du réseau
     */
    private List<ItemStack> computeDelta(Map<ItemStackKey, Integer> previous,
                                          Map<ItemStackKey, Integer> current) {
        List<ItemStack> deltas = new ArrayList<>();

        // Items modifiés ou ajoutés
        for (Map.Entry<ItemStackKey, Integer> entry : current.entrySet()) {
            Integer prevCount = previous.get(entry.getKey());
            if (prevCount == null || !prevCount.equals(entry.getValue())) {
                ItemStack delta = entry.getKey().toStack();
                delta.setCount(entry.getValue());
                deltas.add(delta);
            }
        }

        // Items supprimés (présents dans previous mais pas dans current)
        for (Map.Entry<ItemStackKey, Integer> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                ItemStack removed = entry.getKey().toStack();
                removed.setCount(0);
                deltas.add(removed);
            }
        }

        return deltas;
    }

    // === Deposit / Extract ===

    /**
     * Trouve un slot pour déposer un item avec priorité intelligente:
     * 1. Coffre contenant le même item exact (mergeable ou slot vide dans ce coffre)
     * 2. Coffre contenant un item partageant un tag commun (avec slot vide)
     * 3. Coffre contenant un item du même mod/namespace (avec slot vide)
     * 4. Coffre totalement vide
     * 5. N'importe quel coffre avec un slot vide
     *
     * @return la position du coffre où déposer, ou null si aucun espace
     */
    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) {
        if (parent.getLevel() == null) return null;

        Set<BlockPos> chests = parent.getAllNetworkChests();

        // Priorité 1: Coffre avec le même item exact (stack mergeable)
        for (BlockPos chestPos : chests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existing = handler.getStackInSlot(i);
                    if (ItemStack.isSameItemSameComponents(existing, stack) &&
                        existing.getCount() < existing.getMaxStackSize()) {
                        return chestPos;
                    }
                }
            }
        }

        // Priorité 1b: Coffre avec le même item (mais besoin d'un slot vide)
        for (BlockPos chestPos : chests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                boolean hasSameItem = false;
                boolean hasEmptySlot = false;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existing = handler.getStackInSlot(i);
                    if (existing.isEmpty()) {
                        hasEmptySlot = true;
                    } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        hasSameItem = true;
                    }
                }
                if (hasSameItem && hasEmptySlot) return chestPos;
            }
        }

        // Collecter les tags de l'item à déposer
        var stackTags = stack.getTags().toList();
        String stackNamespace = stack.getItem().builtInRegistryHolder().key().location().getNamespace();

        // Priorité 2: Coffre contenant un item partageant un tag commun
        if (!stackTags.isEmpty()) {
            for (BlockPos chestPos : chests) {
                if (!parent.getLevel().hasChunkAt(chestPos)) continue;
                IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
                if (handler != null) {
                    boolean hasSharedTag = false;
                    boolean hasEmptySlot = false;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack existing = handler.getStackInSlot(i);
                        if (existing.isEmpty()) {
                            hasEmptySlot = true;
                        } else if (!hasSharedTag) {
                            for (var tag : stackTags) {
                                if (existing.is(tag)) {
                                    hasSharedTag = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (hasSharedTag && hasEmptySlot) return chestPos;
                }
            }
        }

        // Priorité 3: Coffre contenant un item du même mod/namespace
        for (BlockPos chestPos : chests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                boolean hasSameNamespace = false;
                boolean hasEmptySlot = false;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existing = handler.getStackInSlot(i);
                    if (existing.isEmpty()) {
                        hasEmptySlot = true;
                    } else if (!hasSameNamespace) {
                        String existingNs = existing.getItem().builtInRegistryHolder()
                            .key().location().getNamespace();
                        if (existingNs.equals(stackNamespace)) {
                            hasSameNamespace = true;
                        }
                    }
                }
                if (hasSameNamespace && hasEmptySlot) return chestPos;
            }
        }

        // Priorité 4: Coffre totalement vide
        for (BlockPos chestPos : chests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                boolean allEmpty = true;
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (!handler.getStackInSlot(i).isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty && handler.getSlots() > 0) return chestPos;
            }
        }

        // Priorité 5: N'importe quel coffre avec un slot vide
        for (BlockPos chestPos : chests) {
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;
            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (handler.getStackInSlot(i).isEmpty()) {
                        return chestPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Dépose un item dans le réseau de stockage.
     * Priorise les coffres qui contiennent déjà l'item (fusion puis slots vides du même coffre).
     *
     * @return le reste non déposé (vide si tout a été déposé)
     */
    public ItemStack depositItem(ItemStack stack) {
        if (parent.getLevel() == null || stack.isEmpty()) return stack;

        Set<BlockPos> chests = parent.getAllNetworkChests();
        ItemStack remaining = stack.copy();

        // Pass 1: deposit in chests that already contain the same item
        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;

            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                if (ContainerHelper.countItem(handler, remaining) > 0) {
                    remaining = ContainerHelper.insertItem(handler, remaining);
                }
            }
        }

        // Pass 2: deposit in any chest with empty slots
        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;

            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                remaining = ContainerHelper.insertItem(handler, remaining);
            }
        }

        needsSync = true;
        dirty = true;
        return remaining;
    }

    /**
     * Extrait un item du réseau de stockage.
     *
     * @return l'item extrait (peut être moins que demandé)
     */
    public ItemStack extractItem(ItemStack template, int count) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        Set<BlockPos> chests = parent.getAllNetworkChests();
        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (BlockPos chestPos : chests) {
            if (needed <= 0) break;
            if (!parent.getLevel().hasChunkAt(chestPos)) continue;

            IItemHandler handler = StorageHelper.getItemHandler(parent.getLevel(), chestPos, null);
            if (handler != null) {
                ItemStack extracted = ContainerHelper.extractItem(handler, template, needed);
                if (!extracted.isEmpty()) {
                    result.grow(extracted.getCount());
                    needed -= extracted.getCount();
                }
            }
        }

        needsSync = true;
        dirty = true;
        return result;
    }

    // === Viewer Management ===

    public void addViewer(UUID playerId, BlockPos terminalPos) {
        playersViewing.put(playerId, terminalPos);
        fullSyncPending.add(playerId);
        if (parent.getLevel() != null && !parent.getLevel().isClientSide()) {
            ServerPlayer player = parent.getLevel().getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                refreshAggregatedItems();
            }
        }
    }

    public void removeViewer(UUID playerId) {
        playersViewing.remove(playerId);
        fullSyncPending.remove(playerId);
    }

    /**
     * Retourne les UUIDs des joueurs qui visualisent le terminal.
     */
    public Set<UUID> getViewerIds() {
        return Collections.unmodifiableSet(playersViewing.keySet());
    }

    // === Tick ===

    /**
     * Gère le timer de synchronisation périodique.
     * Ne recalcule l'agrégation que si le réseau a été muté (dirty flag).
     * N'envoie les packets sync que si des viewers sont connectés ou si un sync est nécessaire.
     */
    public void tickSync(long gameTick) {
        boolean hasViewers = !playersViewing.isEmpty();
        boolean shouldTick = ((gameTick + parent.getBlockPos().hashCode()) % SYNC_INTERVAL) == 0;
        if (!shouldTick) return;

        if (dirty) {
            refreshAggregatedItems();
            dirty = false;
            needsSync = false;
        } else if (hasViewers || needsSync) {
            syncItemsToViewers();
            needsSync = false;
        }
    }

    /**
     * Nettoie les joueurs déconnectés de la liste des viewers.
     */
    public void cleanupViewers() {
        if (parent.getLevel() != null && parent.getLevel().getServer() != null) {
            playersViewing.keySet().removeIf(uuid ->
                parent.getLevel().getServer().getPlayerList().getPlayer(uuid) == null);
        }
    }

    public void setNeedsSync(boolean value) {
        this.needsSync = value;
        if (value) {
            this.dirty = true;
        }
    }

    /**
     * Supprime les viewers associés à un terminal spécifique.
     */
    public void removeViewersForTerminal(BlockPos terminalPos) {
        playersViewing.entrySet().removeIf(entry -> entry.getValue().equals(terminalPos));
    }

    // === Versioned Inventory ===

    /**
     * Calcule un fingerprint du contenu d'un IItemHandler.
     * Combine le nombre de slots, les types d'items et leurs counts en un hash long.
     * Deux inventaires identiques produisent le meme fingerprint.
     */
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

    /**
     * Marque l'aggregateur comme necessitant un refresh.
     * Appele par les blocs intelligents du reseau (push model).
     */
    public void markDirty() {
        this.dirty = true;
    }

    // === Inner class ===

    /**
     * Clé d'agrégation pour les ItemStack (compare item + components, ignore count).
     */
    private static class ItemStackKey {
        private final ItemStack template;

        ItemStackKey(ItemStack stack) {
            this.template = stack.copyWithCount(1);
        }

        ItemStack toStack() {
            return template.copy();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemStackKey other)) return false;
            return ItemStack.isSameItemSameComponents(template, other.template);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(template);
        }
    }
}
