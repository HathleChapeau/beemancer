/**
 * ============================================================
 * [StorageItemAggregator.java]
 * Description: Coordinateur d'agregation, depot, extraction et sync du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | AggregationCache             | Caches et scan        | Totaux items, fingerprints     |
 * | DepositStrategy              | Depot/extraction      | findSlotForItem, deposit       |
 * | ViewerSyncManager            | Sync viewers          | Delta/full sync aux clients    |
 * | StorageControllerBlockEntity | Parent BlockEntity    | Back-reference pour level/pos  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (delegation)
 * - StorageDeliveryManager.java (setNeedsSync)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinateur du systeme d'agregation d'items.
 * Orchestre le scan des coffres, le depot/extraction, et la sync vers les viewers.
 * Delegue le travail a AggregationCache, DepositStrategy et ViewerSyncManager.
 * Aucun etat persistant (NBT) — tout est recalcule au chargement.
 */
public class StorageItemAggregator {
    private final StorageControllerBlockEntity parent;
    private final AggregationCache cache;
    private final DepositStrategy depositStrategy;
    private final ViewerSyncManager viewerSync;

    private boolean dirty = false;
    private boolean needsSync = false;

    private static final int SYNC_INTERVAL = 20;
    private static final int EXTERNAL_CHANGE_CHECK_INTERVAL = 100;

    public StorageItemAggregator(StorageControllerBlockEntity parent) {
        this.parent = parent;
        this.cache = new AggregationCache();
        this.depositStrategy = new DepositStrategy(cache);
        this.viewerSync = new ViewerSyncManager();
    }

    // === Agregation ===

    public List<ItemStack> getAggregatedItems() {
        return cache.getAggregatedItems();
    }

    /**
     * Force le recalcul des items agreges depuis tous les coffres.
     * Nettoie les coffres invalides, reconstruit les caches, sync aux viewers.
     */
    public void refreshAggregatedItems() {
        if (parent.getLevel() == null) return;

        Set<BlockPos> validChests = cleanupInvalidChests();
        cache.scanAndRebuild(validChests, parent.getLevel());
        viewerSync.syncToViewers(cache.getAggregatedItems(), parent);
    }

    // === Deposit / Extract ===

    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) {
        if (parent.getLevel() == null) return null;
        return depositStrategy.findSlotForItem(stack, parent.getAllNetworkChests(), parent.getLevel());
    }

    public ItemStack depositItem(ItemStack stack) {
        if (parent.getLevel() == null || stack.isEmpty()) return stack;
        ItemStack remaining = depositStrategy.depositItem(stack, parent.getAllNetworkChests(), parent.getLevel());
        needsSync = true;
        return remaining;
    }

    public ItemStack extractItem(ItemStack template, int count) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack result = depositStrategy.extractItem(template, count, parent.getAllNetworkChests(), parent.getLevel());
        needsSync = true;
        return result;
    }

    // === Viewer Management ===

    public void addViewer(UUID playerId, BlockPos terminalPos) {
        viewerSync.addViewer(playerId, terminalPos);
        // Marquer dirty pour forcer un full scan+sync au prochain tickSync().
        // Ne PAS appeler refreshAggregatedItems() ici: le StorageItemsSyncPacket
        // serait envoye AVANT que le client ait cree le StorageTerminalMenu
        // (openMenu envoie le ContainerSetMenuPacket APRES le return de createMenu).
        // Le fullSyncPending serait consomme et le client ne recevrait jamais les items.
        dirty = true;
    }

    public void removeViewer(UUID playerId) {
        viewerSync.removeViewer(playerId);
    }

    public Set<UUID> getViewerIds() {
        return viewerSync.getViewerIds();
    }

    public void cleanupViewers() {
        viewerSync.cleanupViewers(parent.getLevel());
    }

    public void removeViewersForTerminal(BlockPos terminalPos) {
        viewerSync.removeViewersForTerminal(terminalPos);
    }

    // === Tick ===

    /**
     * Gere le timer de synchronisation periodique.
     * Refresh complet si dirty, sinon delta sync si viewers connectes.
     * Si un viewer attend un full sync (vient d'ouvrir le menu), force un refresh
     * des le prochain tick pour minimiser le delai d'affichage.
     */
    public void tickSync(long gameTick) {
        boolean hasViewers = viewerSync.hasViewers();

        if (hasViewers && ((gameTick + parent.getBlockPos().hashCode()) % EXTERNAL_CHANGE_CHECK_INTERVAL) == 0) {
            dirty = true;
        }

        // Si un viewer attend un full sync, forcer un refresh au prochain tick
        boolean hasFullSyncPending = viewerSync.hasFullSyncPending();
        boolean shouldTick = hasFullSyncPending
                || ((gameTick + parent.getBlockPos().hashCode()) % SYNC_INTERVAL) == 0;
        if (!shouldTick) return;

        if (dirty || hasFullSyncPending) {
            refreshAggregatedItems();
            dirty = false;
            needsSync = false;
        } else if (hasViewers || needsSync) {
            viewerSync.syncToViewers(cache.getAggregatedItems(), parent);
            needsSync = false;
        }
    }

    // === Flags ===

    public void markDirty() {
        this.dirty = true;
    }

    public void setNeedsSync(boolean value) {
        this.needsSync = value;
        if (value) {
            this.dirty = true;
        }
    }

    // === Internal ===

    /**
     * Nettoie les coffres invalides du registre central.
     * Retourne l'ensemble des coffres valides restants.
     */
    private Set<BlockPos> cleanupInvalidChests() {
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
        return allChests;
    }
}
