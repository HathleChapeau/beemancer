/**
 * ============================================================
 * [RequestManager.java]
 * Description: Orchestrateur des demandes du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Acces coffres, delivery   |
 * | RequestQueue                   | Collection demandes  | Stockage, index, requetes |
 * | RequestProcessor               | Traitement demandes  | Import/export/validation  |
 * | InterfaceRequest               | Demande unitaire     | Publication, annulation   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (delegation tick/NBT)
 * - StorageTerminalBlockEntity.java (publication demandes)
 * - StorageDeliveryManager.java (completion/echec taches)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.InterfaceRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrateur des demandes du reseau. Facade publique qui delegue:
 * - RequestQueue: stockage, index, requetes
 * - RequestProcessor: conversion PENDING en DeliveryTasks, validation, recheck
 *
 * Flux:
 * 1. Terminal publie via publishRequest()
 * 2. processRequests() convertit les PENDING en DeliveryTasks (via RequestProcessor)
 * 3. onTaskCompleted() retire la demande
 * 4. onTaskFailed() remet en PENDING
 * 5. cancelRequest() annule et rappelle la bee si en vol
 */
public class RequestManager {

    private static final int PROCESS_INTERVAL = 10;
    private static final int BLOCKED_RECHECK_INTERVAL = 60;
    private static final int SOURCE_VALIDATION_INTERVAL = 100;

    private final StorageControllerBlockEntity parent;
    private final RequestQueue queue = new RequestQueue();
    private final RequestProcessor processor;
    private boolean dirty = false;
    // [BN] Flag pour forcer un recheck des demandes bloquees au prochain tick
    private boolean needsRecheck = false;

    public RequestManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
        this.processor = new RequestProcessor(this);
    }

    // === Package-private accessors pour RequestProcessor ===

    StorageControllerBlockEntity getParent() { return parent; }
    RequestQueue getQueue() { return queue; }

    // === Publication ===

    /**
     * Publie une nouvelle demande. Si une demande identique existe deja, met a jour le count.
     */
    public void publishRequest(InterfaceRequest request) {
        InterfaceRequest existing = queue.findMergeable(request);
        if (existing != null) {
            existing.setCount(request.getCount());
            parent.setChanged();
            return;
        }

        queue.put(request);
        dirty = true;
        parent.setChanged();
    }

    /**
     * Annule une demande. Si une abeille est en vol, la tache est annulee aussi.
     * Pour les demandes preloaded non encore assignees, remet les items dans le reseau.
     */
    public void cancelRequest(UUID requestId) {
        InterfaceRequest request = queue.get(requestId);
        if (request == null) return;

        request.setStatus(InterfaceRequest.RequestStatus.CANCELLED);

        if (request.getAssignedTaskId() != null) {
            parent.getDeliveryManager().cancelTask(request.getAssignedTaskId());
        } else if (request.isPreloaded()) {
            parent.getItemAggregator().depositItem(
                request.getTemplate().copyWithCount(request.getCount()));
        }

        queue.remove(requestId);
        parent.setChanged();
    }

    public void cancelRequestsFromSource(BlockPos sourcePos) {
        List<UUID> toCancel = new ArrayList<>();
        for (InterfaceRequest request : queue.values()) {
            if (request.getSourcePos().equals(sourcePos)) {
                toCancel.add(request.getRequestId());
            }
        }
        for (UUID id : toCancel) { cancelRequest(id); }
    }

    public void cancelRequestsFromRequester(BlockPos requesterPos) {
        List<UUID> toCancel = new ArrayList<>();
        for (InterfaceRequest request : queue.values()) {
            if (request.getRequesterPos().equals(requesterPos)) {
                toCancel.add(request.getRequestId());
            }
        }
        for (UUID id : toCancel) { cancelRequest(id); }
    }

    public void updateRequestCount(UUID requestId, int newCount) {
        InterfaceRequest request = queue.get(requestId);
        if (request == null) return;
        if (newCount <= 0) { cancelRequest(requestId); return; }
        request.setCount(newCount);
        parent.setChanged();
    }

    // === Callbacks ===

    public void onTaskCompleted(UUID taskId) {
        for (InterfaceRequest request : new ArrayList<>(queue.values())) {
            if (taskId.equals(request.getAssignedTaskId())) {
                queue.remove(request.getRequestId());
                parent.setChanged();
                return;
            }
        }
    }

    public void onTaskFailed(UUID taskId) {
        for (InterfaceRequest request : queue.values()) {
            if (taskId.equals(request.getAssignedTaskId())) {
                request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                request.setAssignedTaskId(null);
                request.setBlockedReason("");
                dirty = true;
                parent.setChanged();
                return;
            }
        }
    }

    // === Processing (delegation) ===

    private void processRequests() {
        if (!dirty) return;
        dirty = false;
        StorageDeliveryManager delivery = parent.getDeliveryManager();
        for (InterfaceRequest request : queue.values()) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.PENDING) continue;
            if (request.getType() == InterfaceRequest.RequestType.IMPORT) {
                processor.processImportRequest(request, delivery);
            } else {
                processor.processExportRequest(request, delivery);
            }
        }
    }

    // === Query (delegation) ===

    public List<InterfaceRequest> getAllRequests() { return queue.getAllRequests(); }

    @javax.annotation.Nullable
    public InterfaceRequest findRequestByTaskId(UUID taskId) { return queue.findRequestByTaskId(taskId); }

    public boolean hasRequestFor(BlockPos sourcePos, InterfaceRequest.RequestType type, ItemStack template) {
        return queue.hasRequestFor(sourcePos, type, template);
    }

    public int getRequestedCount(BlockPos sourcePos, InterfaceRequest.RequestType type, ItemStack template) {
        return queue.getRequestedCount(sourcePos, type, template);
    }

    // === Tick ===

    /** [BN] Force un recheck des demandes bloquees au prochain tick. */
    public void requestRecheck() { needsRecheck = true; }

    public void tick(long gameTick) {
        long offset = parent.getBlockPos().hashCode();
        if ((gameTick + offset) % PROCESS_INTERVAL == 0) {
            processRequests();
        }
        if (needsRecheck || (gameTick + offset) % BLOCKED_RECHECK_INTERVAL == 0) {
            if (processor.recheckBlockedRequests()) { dirty = true; }
            needsRecheck = false;
        }
        if ((gameTick + offset) % SOURCE_VALIDATION_INTERVAL == 0) {
            processor.validateRequestSources();
        }
    }

    // === NBT ===

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag requestsTag = new ListTag();
        for (InterfaceRequest request : queue.values()) {
            if (request.getStatus() == InterfaceRequest.RequestStatus.CANCELLED) continue;
            CompoundTag requestTag = request.save(registries);
            if (request.getStatus() == InterfaceRequest.RequestStatus.ASSIGNED) {
                requestTag.putString("Status", InterfaceRequest.RequestStatus.PENDING.name());
                requestTag.remove("AssignedTaskId");
            }
            requestsTag.add(requestTag);
        }
        tag.put("Requests", requestsTag);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        queue.clear();
        if (tag.contains("Requests")) {
            ListTag requestsTag = tag.getList("Requests", Tag.TAG_COMPOUND);
            for (int i = 0; i < requestsTag.size(); i++) {
                try {
                    InterfaceRequest request = InterfaceRequest.load(requestsTag.getCompound(i), registries);
                    queue.put(request);
                } catch (Exception e) {
                    com.chapeau.apica.Apica.LOGGER.warn("Skipping corrupted request at index {}", i, e);
                }
            }
        }
        if (!queue.isEmpty()) { dirty = true; }
    }
}
