/**
 * ============================================================
 * [ViewerSyncManager.java]
 * Description: Gestion des viewers et synchronisation delta/full vers les clients
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | StorageItemsSyncPacket     | Packet items          | Envoi full/delta sync          |
 * | StorageTasksSyncPacket     | Packet taches         | Envoi taches delivery          |
 * | StorageControllerBlockEntity | Parent              | Acces level, delivery manager  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageItemAggregator.java (delegation sync)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.core.network.packets.StorageItemsSyncPacket;
import com.chapeau.beemancer.core.network.packets.StorageTasksSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gere le suivi des joueurs qui visualisent le terminal
 * et la synchronisation des items/taches vers ces joueurs.
 * Utilise un delta sync incremental: seuls les changements sont envoyes.
 * Nouveaux viewers recoivent un full sync fragmente.
 */
class ViewerSyncManager {
    private static final int MAX_ITEMS_PER_PACKET = 100;

    private final Map<UUID, BlockPos> playersViewing = new HashMap<>();
    private final Set<UUID> fullSyncPending = new LinkedHashSet<>();
    private Map<ItemStackKey, Integer> previousSnapshot = new HashMap<>();

    boolean hasViewers() {
        return !playersViewing.isEmpty();
    }

    boolean hasFullSyncPending() {
        return !fullSyncPending.isEmpty();
    }

    void addViewer(UUID playerId, BlockPos terminalPos) {
        playersViewing.put(playerId, terminalPos);
        fullSyncPending.add(playerId);
    }

    void removeViewer(UUID playerId) {
        playersViewing.remove(playerId);
        fullSyncPending.remove(playerId);
    }

    Set<UUID> getViewerIds() {
        return Collections.unmodifiableSet(playersViewing.keySet());
    }

    /**
     * Envoie les items agreges et les taches aux joueurs qui ont le terminal ouvert.
     * Nouveaux viewers recoivent un full sync fragmente. Les autres recoivent un delta.
     */
    void syncToViewers(List<ItemStack> aggregatedItems, StorageControllerBlockEntity parent) {
        Level level = parent.getLevel();
        if (level == null || level.isClientSide()) return;
        if (playersViewing.isEmpty()) {
            updateSnapshot(aggregatedItems);
            return;
        }

        boolean tasksDirty = parent.getDeliveryManager().isTasksDirty();
        List<TaskDisplayData> taskData = null;
        Map<ItemStackKey, Integer> currentSnapshot = buildCurrentSnapshot(aggregatedItems);
        List<ItemStack> deltaItems = computeDelta(previousSnapshot, currentSnapshot);
        previousSnapshot = currentSnapshot;

        for (Map.Entry<UUID, BlockPos> entry : playersViewing.entrySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;

            boolean isFullSync = fullSyncPending.remove(entry.getKey());
            if (isFullSync) {
                sendFragmentedFullSync(player, entry.getValue(), aggregatedItems);
            } else if (!deltaItems.isEmpty()) {
                sendFragmentedDelta(player, entry.getValue(), deltaItems);
            }

            if (tasksDirty || isFullSync) {
                if (taskData == null) {
                    taskData = parent.getDeliveryManager().getTaskDisplayData();
                }
                PacketDistributor.sendToPlayer(player,
                    new StorageTasksSyncPacket(entry.getValue(), taskData));
            }
        }
        if (tasksDirty) {
            parent.getDeliveryManager().resetTasksDirty();
        }
    }

    void cleanupViewers(Level level) {
        if (level != null && level.getServer() != null) {
            playersViewing.keySet().removeIf(uuid ->
                level.getServer().getPlayerList().getPlayer(uuid) == null);
        }
    }

    void removeViewersForTerminal(BlockPos terminalPos) {
        playersViewing.entrySet().removeIf(entry -> entry.getValue().equals(terminalPos));
    }

    private void sendFragmentedDelta(ServerPlayer player, BlockPos terminalPos,
                                       List<ItemStack> deltaItems) {
        if (deltaItems.size() <= MAX_ITEMS_PER_PACKET) {
            PacketDistributor.sendToPlayer(player,
                new StorageItemsSyncPacket(terminalPos, false, true, deltaItems));
            return;
        }
        for (int offset = 0; offset < deltaItems.size(); offset += MAX_ITEMS_PER_PACKET) {
            int end = Math.min(offset + MAX_ITEMS_PER_PACKET, deltaItems.size());
            boolean last = (end >= deltaItems.size());
            PacketDistributor.sendToPlayer(player,
                new StorageItemsSyncPacket(terminalPos, false, last, deltaItems.subList(offset, end)));
        }
    }

    private void sendFragmentedFullSync(ServerPlayer player, BlockPos terminalPos,
                                         List<ItemStack> items) {
        int total = items.size();
        if (total <= MAX_ITEMS_PER_PACKET) {
            PacketDistributor.sendToPlayer(player,
                new StorageItemsSyncPacket(terminalPos, true, true, items));
            return;
        }
        for (int offset = 0; offset < total; offset += MAX_ITEMS_PER_PACKET) {
            int end = Math.min(offset + MAX_ITEMS_PER_PACKET, total);
            boolean last = (end >= total);
            PacketDistributor.sendToPlayer(player,
                new StorageItemsSyncPacket(terminalPos, true, last, items.subList(offset, end)));
        }
    }

    private Map<ItemStackKey, Integer> buildCurrentSnapshot(List<ItemStack> items) {
        Map<ItemStackKey, Integer> snapshot = new HashMap<>();
        for (ItemStack stack : items) {
            snapshot.put(new ItemStackKey(stack), stack.getCount());
        }
        return snapshot;
    }

    private void updateSnapshot(List<ItemStack> items) {
        previousSnapshot = buildCurrentSnapshot(items);
    }

    private List<ItemStack> computeDelta(Map<ItemStackKey, Integer> previous,
                                          Map<ItemStackKey, Integer> current) {
        List<ItemStack> deltas = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : current.entrySet()) {
            Integer prevCount = previous.get(entry.getKey());
            if (prevCount == null || !prevCount.equals(entry.getValue())) {
                ItemStack delta = entry.getKey().toStack();
                delta.setCount(entry.getValue());
                deltas.add(delta);
            }
        }
        for (Map.Entry<ItemStackKey, Integer> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                ItemStack removed = entry.getKey().toStack();
                removed.setCount(0);
                deltas.add(removed);
            }
        }
        return deltas;
    }
}
