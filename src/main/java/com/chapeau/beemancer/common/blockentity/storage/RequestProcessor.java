/**
 * ============================================================
 * [RequestProcessor.java]
 * Description: Traitement des demandes import/export et validation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | RequestManager                 | Orchestrateur        | Acces parent, delivery    |
 * | RequestQueue                   | Collection demandes  | Iteration, requetes       |
 * | InterfaceRequest               | Demande unitaire     | Etat, type, template      |
 * | DeliveryTask                   | Tache de livraison   | Creation taches           |
 * | DeliveryContainerOps           | Coffres reseau       | Recherche items/slots     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RequestManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Convertit les demandes PENDING en DeliveryTasks (import et export).
 * Gere aussi le recheck des demandes bloquees et la validation des sources.
 */
public class RequestProcessor {
    private final RequestManager manager;

    public RequestProcessor(RequestManager manager) {
        this.manager = manager;
    }

    /**
     * IMPORT: trouver un coffre source, creer une tache (coffre -> requester).
     */
    void processImportRequest(InterfaceRequest request, StorageDeliveryManager delivery) {
        List<DeliveryContainerOps.ChestItemInfo> chests =
            delivery.getContainerOps().findAllChestsWithItem(request.getTemplate());

        chests.removeIf(info -> info.pos().equals(request.getSourcePos()));

        if (chests.isEmpty()) {
            request.setStatus(InterfaceRequest.RequestStatus.BLOCKED);
            request.setBlockedReason("gui.beemancer.tasks.blocked.items_unavailable");
            return;
        }

        DeliveryTask.TaskOrigin taskOrigin = request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST
            ? DeliveryTask.TaskOrigin.REQUEST : DeliveryTask.TaskOrigin.AUTOMATION;

        int remaining = request.getCount();
        DeliveryTask rootTask = null;

        for (DeliveryContainerOps.ChestItemInfo chest : chests) {
            if (remaining <= 0) break;

            int toTake = Math.min(remaining, chest.count());
            UUID parentId = rootTask != null ? rootTask.getTaskId() : null;

            DeliveryTask task = DeliveryTask.builder(
                    request.getTemplate(), toTake, chest.pos(), request.getSourcePos(), taskOrigin)
                .requesterPos(request.getRequesterPos())
                .parentTaskId(parentId)
                .build();

            delivery.addDeliveryTask(task);
            remaining -= toTake;

            if (rootTask == null) {
                rootTask = task;
            }
        }

        request.setAssignedTaskId(rootTask.getTaskId());
        request.setStatus(InterfaceRequest.RequestStatus.ASSIGNED);
        request.setBlockedReason("");
        manager.getParent().setChanged();
    }

    /**
     * EXPORT: trouver un coffre destination, creer une tache (source -> coffre).
     * Pour les exports preloaded (terminal), les items sont pre-charges sur la bee.
     */
    void processExportRequest(InterfaceRequest request, StorageDeliveryManager delivery) {
        BlockPos destChest = manager.getParent().getItemAggregator().findSlotForItem(request.getTemplate());
        if (destChest == null) {
            request.setStatus(InterfaceRequest.RequestStatus.BLOCKED);
            request.setBlockedReason("gui.beemancer.tasks.blocked.no_space");
            return;
        }

        if (destChest.equals(request.getSourcePos())) return;

        DeliveryTask.TaskOrigin taskOrigin = request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST
            ? DeliveryTask.TaskOrigin.REQUEST : DeliveryTask.TaskOrigin.AUTOMATION;

        DeliveryTask task = DeliveryTask.builder(
                request.getTemplate(), request.getCount(), request.getSourcePos(), destChest, taskOrigin)
            .preloaded(request.isPreloaded())
            .requesterPos(request.getRequesterPos())
            .build();

        delivery.addDeliveryTask(task);
        request.setAssignedTaskId(task.getTaskId());
        request.setStatus(InterfaceRequest.RequestStatus.ASSIGNED);
        request.setBlockedReason("");
        manager.getParent().setChanged();
    }

    /**
     * Recheck les demandes BLOCKED: si la raison a disparu, repasser en PENDING.
     * @return true si au moins une demande a ete debloquee
     */
    boolean recheckBlockedRequests() {
        boolean unblocked = false;
        StorageDeliveryManager delivery = manager.getParent().getDeliveryManager();

        for (InterfaceRequest request : manager.getQueue().values()) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.BLOCKED) continue;

            if (request.getType() == InterfaceRequest.RequestType.IMPORT) {
                BlockPos chestPos = delivery.getContainerOps().findChestWithItem(request.getTemplate(), 1);
                if (chestPos != null) {
                    request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                    request.setBlockedReason("");
                    unblocked = true;
                }
            } else {
                BlockPos destChest = manager.getParent().getItemAggregator().findSlotForItem(request.getTemplate());
                if (destChest != null) {
                    request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                    request.setBlockedReason("");
                    unblocked = true;
                }
            }
        }
        return unblocked;
    }

    /**
     * Valide que les sources des demandes actives existent encore.
     * Annule les demandes dont le terminal source a ete detruit.
     */
    void validateRequestSources() {
        if (manager.getParent().getLevel() == null) return;

        List<UUID> toCancel = new ArrayList<>();
        Set<BlockPos> checkedPositions = new HashSet<>();

        for (InterfaceRequest request : manager.getQueue().values()) {
            if (request.getStatus() == InterfaceRequest.RequestStatus.CANCELLED) continue;

            BlockPos reqPos = request.getRequesterPos();
            if (checkedPositions.contains(reqPos)) continue;
            checkedPositions.add(reqPos);

            if (!manager.getParent().getLevel().hasChunkAt(reqPos)) continue;

            BlockEntity be = manager.getParent().getLevel().getBlockEntity(reqPos);

            if (be == null) {
                for (InterfaceRequest r : manager.getQueue().values()) {
                    if (r.getRequesterPos().equals(reqPos)
                            && r.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                        toCancel.add(r.getRequestId());
                    }
                }
            }
        }

        for (UUID id : toCancel) {
            manager.cancelRequest(id);
        }
    }
}
