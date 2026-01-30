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

import com.chapeau.beemancer.core.network.packets.StorageItemsSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gère l'agrégation des items du réseau, le dépôt, l'extraction,
 * et la synchronisation vers les joueurs qui visualisent le terminal.
 * Aucun état persistant (NBT) — tout est recalculé au chargement.
 */
public class StorageItemAggregator {
    private final StorageControllerBlockEntity parent;

    private List<ItemStack> aggregatedItems = new ArrayList<>();
    private int syncTimer = 0;
    private boolean needsSync = false;
    private final Map<UUID, BlockPos> playersViewing = new HashMap<>();

    private static final int SYNC_INTERVAL = 40;

    public StorageItemAggregator(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    // === Agrégation ===

    public List<ItemStack> getAggregatedItems() {
        return aggregatedItems;
    }

    /**
     * Force le recalcul des items agrégés depuis tous les coffres enregistrés.
     * Nettoie les coffres invalides au passage.
     */
    public void refreshAggregatedItems() {
        if (parent.getLevel() == null) return;

        StorageChestManager chestManager = parent.getChestManager();
        Set<BlockPos> chests = chestManager.getRegisteredChestsMutable();

        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : chests) {
            if (!chestManager.isChest(pos)) {
                toRemove.add(pos);
            }
        }
        if (!toRemove.isEmpty()) {
            chests.removeAll(toRemove);
            parent.setChanged();
            parent.syncToClient();
        }

        Map<ItemStackKey, Integer> itemCounts = new HashMap<>();
        for (BlockPos chestPos : chests) {
            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        ItemStackKey key = new ItemStackKey(stack);
                        itemCounts.merge(key, stack.getCount(), Integer::sum);
                    }
                }
            }
        }

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
     * Envoie la liste des items agrégés aux joueurs qui ont le terminal ouvert.
     */
    private void syncItemsToViewers() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        for (Map.Entry<UUID, BlockPos> entry : playersViewing.entrySet()) {
            ServerPlayer player = parent.getLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(entry.getValue(), aggregatedItems));
            }
        }
    }

    // === Deposit / Extract ===

    /**
     * Trouve un slot pour déposer un item.
     *
     * @return la position du coffre où déposer, ou null si aucun espace
     */
    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) {
        if (parent.getLevel() == null) return null;

        Set<BlockPos> chests = parent.getChestManager().getRegisteredChests();

        for (BlockPos chestPos : chests) {
            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, stack) &&
                        existing.getCount() < existing.getMaxStackSize()) {
                        return chestPos;
                    }
                }
            }
        }

        for (BlockPos chestPos : chests) {
            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
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

        Set<BlockPos> chests = parent.getChestManager().getRegisteredChests();
        ItemStack remaining = stack.copy();

        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                boolean hasItem = false;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (ItemStack.isSameItemSameComponents(container.getItem(i), remaining)) {
                        hasItem = true;
                        break;
                    }
                }

                if (hasItem) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack existing = container.getItem(i);
                        if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                            int space = existing.getMaxStackSize() - existing.getCount();
                            int toTransfer = Math.min(space, remaining.getCount());
                            if (toTransfer > 0) {
                                existing.grow(toTransfer);
                                remaining.shrink(toTransfer);
                                container.setChanged();
                            }
                        }
                        if (remaining.isEmpty()) break;
                    }

                    for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
                        if (container.getItem(i).isEmpty()) {
                            int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                            ItemStack toPlace = remaining.copy();
                            toPlace.setCount(toTransfer);
                            container.setItem(i, toPlace);
                            remaining.shrink(toTransfer);
                            container.setChanged();
                        }
                    }
                }
            }
        }

        for (BlockPos chestPos : chests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
                        int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                        ItemStack toPlace = remaining.copy();
                        toPlace.setCount(toTransfer);
                        container.setItem(i, toPlace);
                        remaining.shrink(toTransfer);
                        container.setChanged();
                    }
                    if (remaining.isEmpty()) break;
                }
            }
        }

        needsSync = true;
        return remaining;
    }

    /**
     * Extrait un item du réseau de stockage.
     *
     * @return l'item extrait (peut être moins que demandé)
     */
    public ItemStack extractItem(ItemStack template, int count) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        Set<BlockPos> chests = parent.getChestManager().getRegisteredChests();
        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (BlockPos chestPos : chests) {
            if (needed <= 0) break;

            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, template)) {
                        int toTake = Math.min(needed, existing.getCount());
                        existing.shrink(toTake);
                        result.grow(toTake);
                        needed -= toTake;
                        container.setChanged();

                        if (existing.isEmpty()) {
                            container.setItem(i, ItemStack.EMPTY);
                        }
                    }
                    if (needed <= 0) break;
                }
            }
        }

        needsSync = true;
        return result;
    }

    // === Viewer Management ===

    public void addViewer(UUID playerId, BlockPos terminalPos) {
        playersViewing.put(playerId, terminalPos);
        if (parent.getLevel() != null && !parent.getLevel().isClientSide()) {
            ServerPlayer player = parent.getLevel().getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                refreshAggregatedItems();
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(terminalPos, aggregatedItems));
            }
        }
    }

    public void removeViewer(UUID playerId) {
        playersViewing.remove(playerId);
    }

    // === Tick ===

    /**
     * Gère le timer de synchronisation périodique.
     */
    public void tickSync() {
        syncTimer++;

        boolean hasViewers = !playersViewing.isEmpty();
        boolean shouldSync = hasViewers || (syncTimer >= SYNC_INTERVAL && needsSync);

        if (shouldSync) {
            refreshAggregatedItems();
            syncTimer = 0;
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
    }

    /**
     * Supprime les viewers associés à un terminal spécifique.
     */
    public void removeViewersForTerminal(BlockPos terminalPos) {
        playersViewing.entrySet().removeIf(entry -> entry.getValue().equals(terminalPos));
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
