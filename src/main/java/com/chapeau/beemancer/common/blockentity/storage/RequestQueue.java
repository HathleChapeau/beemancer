/**
 * ============================================================
 * [RequestQueue.java]
 * Description: Collection et index des demandes du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | InterfaceRequest               | Demande unitaire     | Stockage, index, requetes |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RequestManager.java (delegation)
 * - RequestProcessor.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Couche de donnees pour les demandes du reseau: Map indexee + requetes O(1).
 * Encapsule le stockage (LinkedHashMap) et l'index composite (source, type, item).
 */
public class RequestQueue {

    private final Map<UUID, InterfaceRequest> activeRequests = new LinkedHashMap<>();

    // Index composite pour lookup O(1) par (source, type, item)
    private record RequestKey(BlockPos source, InterfaceRequest.RequestType type, int itemHash) {}
    private final Map<RequestKey, Set<UUID>> requestIndex = new HashMap<>();

    // === Index management ===

    private RequestKey keyFor(InterfaceRequest request) {
        return new RequestKey(request.getSourcePos(), request.getType(),
            ItemStack.hashItemAndComponents(request.getTemplate()));
    }

    void indexAdd(InterfaceRequest request) {
        requestIndex.computeIfAbsent(keyFor(request), k -> new HashSet<>()).add(request.getRequestId());
    }

    void indexRemove(InterfaceRequest request) {
        RequestKey key = keyFor(request);
        Set<UUID> ids = requestIndex.get(key);
        if (ids != null) {
            ids.remove(request.getRequestId());
            if (ids.isEmpty()) requestIndex.remove(key);
        }
    }

    // === Collection operations ===

    void put(InterfaceRequest request) {
        activeRequests.put(request.getRequestId(), request);
        indexAdd(request);
    }

    void remove(UUID requestId) {
        InterfaceRequest request = activeRequests.remove(requestId);
        if (request != null) {
            indexRemove(request);
        }
    }

    InterfaceRequest get(UUID requestId) {
        return activeRequests.get(requestId);
    }

    Collection<InterfaceRequest> values() {
        return activeRequests.values();
    }

    boolean isEmpty() {
        return activeRequests.isEmpty();
    }

    void clear() {
        activeRequests.clear();
        requestIndex.clear();
    }

    // === Queries ===

    /**
     * Retourne toutes les demandes actives (pour affichage GUI).
     */
    public List<InterfaceRequest> getAllRequests() {
        return new ArrayList<>(activeRequests.values());
    }

    /**
     * Trouve la demande assignee a une tache de livraison donnee.
     */
    @javax.annotation.Nullable
    public InterfaceRequest findRequestByTaskId(UUID taskId) {
        for (InterfaceRequest request : activeRequests.values()) {
            if (taskId.equals(request.getAssignedTaskId())) {
                return request;
            }
        }
        return null;
    }

    /**
     * Verifie si une demande existe pour une source + type + item donnes.
     */
    public boolean hasRequestFor(BlockPos sourcePos, InterfaceRequest.RequestType type, ItemStack template) {
        RequestKey key = new RequestKey(sourcePos, type, ItemStack.hashItemAndComponents(template));
        Set<UUID> indexed = requestIndex.get(key);
        if (indexed == null || indexed.isEmpty()) return false;
        for (UUID id : indexed) {
            InterfaceRequest request = activeRequests.get(id);
            if (request != null
                    && ItemStack.isSameItemSameComponents(request.getTemplate(), template)
                    && request.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retourne le nombre total d'items en demande pour un type + item donnes depuis une source.
     */
    public int getRequestedCount(BlockPos sourcePos, InterfaceRequest.RequestType type, ItemStack template) {
        int total = 0;
        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getSourcePos().equals(sourcePos)
                    && request.getType() == type
                    && ItemStack.isSameItemSameComponents(request.getTemplate(), template)
                    && request.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                total += request.getCount();
            }
        }
        return total;
    }

    /**
     * Cherche une demande existante fusionnable (meme source, type, item, non annulee).
     */
    @javax.annotation.Nullable
    InterfaceRequest findMergeable(InterfaceRequest incoming) {
        RequestKey key = keyFor(incoming);
        Set<UUID> indexed = requestIndex.get(key);
        if (indexed == null) return null;
        for (UUID id : indexed) {
            InterfaceRequest existing = activeRequests.get(id);
            if (existing != null
                    && ItemStack.isSameItemSameComponents(existing.getTemplate(), incoming.getTemplate())
                    && existing.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                if (existing.getStatus() == InterfaceRequest.RequestStatus.PENDING
                        || existing.getStatus() == InterfaceRequest.RequestStatus.BLOCKED
                        || existing.getStatus() == InterfaceRequest.RequestStatus.ASSIGNED) {
                    return existing;
                }
            }
        }
        return null;
    }
}
