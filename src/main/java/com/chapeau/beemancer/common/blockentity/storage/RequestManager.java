/**
 * ============================================================
 * [RequestManager.java]
 * Description: Gestionnaire central des demandes du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Parent controller    | Acces coffres, delivery        |
 * | InterfaceRequest              | Demande unitaire     | Publication, suivi, annulation |
 * | DeliveryTask                  | Tache de livraison   | Conversion demande → tache     |
 * | StorageDeliveryManager        | Gestion livraison    | Ajout taches, verification     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (delegation tick/NBT)
 * - ImportInterfaceBlockEntity.java (publication demandes)
 * - ExportInterfaceBlockEntity.java (publication demandes)
 * - StorageTerminalBlockEntity.java (publication demandes)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Recoit les demandes des interfaces et terminaux, les convertit en DeliveryTasks.
 *
 * Flux:
 * 1. Interface/terminal publie une InterfaceRequest via publishRequest()
 * 2. processRequests() (chaque tick) convertit les PENDING en DeliveryTasks
 * 3. Quand la tache est terminee, onTaskCompleted() retire la demande
 * 4. Si la tache echoue, onTaskFailed() remet la demande en PENDING
 * 5. cancelRequest() annule une demande et rappelle l'abeille si en vol
 */
public class RequestManager {

    private static final int PROCESS_INTERVAL = 10;
    private static final int BLOCKED_RECHECK_INTERVAL = 60;
    private static final int SOURCE_VALIDATION_INTERVAL = 100;

    private final StorageControllerBlockEntity parent;
    private final Map<UUID, InterfaceRequest> activeRequests = new LinkedHashMap<>();
    private int processTimer = 0;
    private int blockedRecheckTimer = 0;
    private int sourceValidationTimer = 0;

    public RequestManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    // === Publication ===

    /**
     * Publie une nouvelle demande. Si une demande identique (meme source, meme type,
     * meme item) existe deja, met a jour le count.
     */
    public void publishRequest(InterfaceRequest request) {
        // Chercher une demande existante du meme source + type + item pour fusion
        for (InterfaceRequest existing : activeRequests.values()) {
            if (existing.getSourcePos().equals(request.getSourcePos())
                    && existing.getType() == request.getType()
                    && ItemStack.isSameItemSameComponents(existing.getTemplate(), request.getTemplate())
                    && existing.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                // Mettre a jour le count pour PENDING, BLOCKED ou ASSIGNED
                if (existing.getStatus() == InterfaceRequest.RequestStatus.PENDING
                        || existing.getStatus() == InterfaceRequest.RequestStatus.BLOCKED
                        || existing.getStatus() == InterfaceRequest.RequestStatus.ASSIGNED) {
                    existing.setCount(request.getCount());
                    parent.setChanged();
                }
                return;
            }
        }

        activeRequests.put(request.getRequestId(), request);
        parent.setChanged();
    }

    /**
     * Annule une demande. Si une abeille est en vol, la tache est annulee aussi.
     * Pour les demandes preloaded non encore assignees, remet les items dans le reseau.
     */
    public void cancelRequest(UUID requestId) {
        InterfaceRequest request = activeRequests.get(requestId);
        if (request == null) return;

        request.setStatus(InterfaceRequest.RequestStatus.CANCELLED);

        if (request.getAssignedTaskId() != null) {
            parent.getDeliveryManager().cancelTask(request.getAssignedTaskId());
        } else if (request.isPreloaded()) {
            parent.getItemAggregator().depositItem(
                request.getTemplate().copyWithCount(request.getCount()));
        }

        activeRequests.remove(requestId);
        parent.setChanged();
    }

    /**
     * Annule toutes les demandes d'une source donnee (interface detruite, desactivee, etc.).
     */
    public void cancelRequestsFromSource(BlockPos sourcePos) {
        List<UUID> toCancel = new ArrayList<>();
        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getSourcePos().equals(sourcePos)) {
                toCancel.add(request.getRequestId());
            }
        }
        for (UUID id : toCancel) {
            cancelRequest(id);
        }
    }

    /**
     * Annule toutes les demandes emises par un requester donne (position de l'interface/terminal).
     * Utilise requesterPos au lieu de sourcePos car pour les exports, sourcePos = adjacent container.
     */
    public void cancelRequestsFromRequester(BlockPos requesterPos) {
        List<UUID> toCancel = new ArrayList<>();
        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getRequesterPos().equals(requesterPos)) {
                toCancel.add(request.getRequestId());
            }
        }
        for (UUID id : toCancel) {
            cancelRequest(id);
        }
    }

    /**
     * Met a jour le count d'une demande existante.
     * Si le nouveau count est 0 ou negatif, annule la demande.
     */
    public void updateRequestCount(UUID requestId, int newCount) {
        InterfaceRequest request = activeRequests.get(requestId);
        if (request == null) return;

        if (newCount <= 0) {
            cancelRequest(requestId);
            return;
        }

        request.setCount(newCount);
        parent.setChanged();
    }

    // === Callbacks ===

    /**
     * Appelee quand une tache liee a une demande est terminee.
     */
    public void onTaskCompleted(UUID taskId) {
        Iterator<Map.Entry<UUID, InterfaceRequest>> it = activeRequests.entrySet().iterator();
        while (it.hasNext()) {
            InterfaceRequest request = it.next().getValue();
            if (taskId.equals(request.getAssignedTaskId())) {
                it.remove();
                parent.setChanged();
                return;
            }
        }
    }

    /**
     * Appelee quand une tache liee a une demande echoue.
     * Remet la demande en PENDING pour re-tentative.
     */
    public void onTaskFailed(UUID taskId) {
        for (InterfaceRequest request : activeRequests.values()) {
            if (taskId.equals(request.getAssignedTaskId())) {
                request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                request.setAssignedTaskId(null);
                request.setBlockedReason("");
                parent.setChanged();
                return;
            }
        }
    }

    // === Processing ===

    /**
     * Convertit les demandes PENDING en DeliveryTasks.
     * Appele periodiquement par le tick du controller.
     */
    public void processRequests() {
        StorageDeliveryManager delivery = parent.getDeliveryManager();

        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.PENDING) continue;

            if (request.getType() == InterfaceRequest.RequestType.IMPORT) {
                processImportRequest(request, delivery);
            } else {
                processExportRequest(request, delivery);
            }
        }
    }

    /**
     * IMPORT: trouver un coffre source, creer une tache (coffre → interface/terminal).
     */
    private void processImportRequest(InterfaceRequest request, StorageDeliveryManager delivery) {
        // Scanner TOUS les coffres contenant l'item demande
        List<StorageDeliveryManager.ChestItemInfo> chests =
            delivery.findAllChestsWithItem(request.getTemplate());

        // Filtrer les coffres qui sont la destination (loop prevention)
        chests.removeIf(info -> info.pos().equals(request.getSourcePos()));

        if (chests.isEmpty()) {
            request.setStatus(InterfaceRequest.RequestStatus.BLOCKED);
            request.setBlockedReason("gui.beemancer.tasks.blocked.items_unavailable");
            return;
        }

        DeliveryTask.TaskOrigin taskOrigin = request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST
            ? DeliveryTask.TaskOrigin.REQUEST : DeliveryTask.TaskOrigin.AUTOMATION;

        // Creer une tache racine (premier coffre), puis des subtasks pour les coffres suivants.
        // Chaque tache a le count reel disponible dans le coffre (cap a la demande restante).
        // processDeliveryQueue decoupera ensuite par beeCapacity.
        int remaining = request.getCount();
        DeliveryTask rootTask = null;

        for (StorageDeliveryManager.ChestItemInfo chest : chests) {
            if (remaining <= 0) break;

            int toTake = Math.min(remaining, chest.count());
            UUID parentId = rootTask != null ? rootTask.getTaskId() : null;

            DeliveryTask task = new DeliveryTask(
                request.getTemplate(), toTake,
                chest.pos(), request.getSourcePos(),
                0, java.util.Collections.emptyList(),
                taskOrigin, false, request.getRequesterPos(), parentId
            );

            delivery.addDeliveryTask(task);
            remaining -= toTake;

            if (rootTask == null) {
                rootTask = task;
            }
        }

        request.setAssignedTaskId(rootTask.getTaskId());
        request.setStatus(InterfaceRequest.RequestStatus.ASSIGNED);
        request.setBlockedReason("");
        parent.setChanged();
    }

    /**
     * EXPORT: trouver un coffre destination, creer une tache (adjacent/terminal → coffre).
     * Pour les exports preloaded (terminal), les items sont pre-charges sur la bee.
     * Pour les exports non-preloaded (interface), la bee va a la source, extrait, puis depose.
     */
    private void processExportRequest(InterfaceRequest request, StorageDeliveryManager delivery) {
        // Utiliser findSlotForItem pour un placement intelligent (meme item > meme tag > meme mod > vide)
        BlockPos destChest = parent.getItemAggregator().findSlotForItem(request.getTemplate());
        if (destChest == null) {
            request.setStatus(InterfaceRequest.RequestStatus.BLOCKED);
            request.setBlockedReason("gui.beemancer.tasks.blocked.no_space");
            return;
        }

        // Ne pas exporter vers le bloc adjacent lui-meme (loop prevention)
        if (destChest.equals(request.getSourcePos())) return;

        DeliveryTask.TaskOrigin taskOrigin = request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST
            ? DeliveryTask.TaskOrigin.REQUEST : DeliveryTask.TaskOrigin.AUTOMATION;

        DeliveryTask task = new DeliveryTask(
            request.getTemplate(), request.getCount(),
            request.getSourcePos(), destChest,
            taskOrigin, request.isPreloaded(),
            request.getRequesterPos()
        );

        delivery.addDeliveryTask(task);
        request.setAssignedTaskId(task.getTaskId());
        request.setStatus(InterfaceRequest.RequestStatus.ASSIGNED);
        request.setBlockedReason("");
        parent.setChanged();
    }

    /**
     * Recheck les demandes BLOCKED: si la raison a disparu, repasser en PENDING.
     */
    private void recheckBlockedRequests() {
        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.BLOCKED) continue;

            StorageDeliveryManager delivery = parent.getDeliveryManager();

            if (request.getType() == InterfaceRequest.RequestType.IMPORT) {
                BlockPos chestPos = delivery.findChestWithItem(request.getTemplate(), 1);
                if (chestPos != null) {
                    request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                    request.setBlockedReason("");
                }
            } else {
                BlockPos destChest = parent.getItemAggregator().findSlotForItem(request.getTemplate());
                if (destChest != null) {
                    request.setStatus(InterfaceRequest.RequestStatus.PENDING);
                    request.setBlockedReason("");
                }
            }
        }
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
    public InterfaceRequest findRequestByTaskId(java.util.UUID taskId) {
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
        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getSourcePos().equals(sourcePos)
                    && request.getType() == type
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
     * Valide que les sources des demandes actives existent encore et sont operationnelles.
     * Annule les demandes dont l'interface/terminal source a ete detruite ou desactivee.
     */
    private void validateRequestSources() {
        if (parent.getLevel() == null) return;

        List<UUID> toCancel = new ArrayList<>();
        Set<BlockPos> checkedPositions = new HashSet<>();

        for (InterfaceRequest request : activeRequests.values()) {
            if (request.getStatus() == InterfaceRequest.RequestStatus.CANCELLED) continue;

            BlockPos reqPos = request.getRequesterPos();
            if (checkedPositions.contains(reqPos)) continue;
            checkedPositions.add(reqPos);

            if (!parent.getLevel().isLoaded(reqPos)) continue;

            BlockEntity be = parent.getLevel().getBlockEntity(reqPos);

            // Interface detruite
            if (be == null) {
                for (InterfaceRequest r : activeRequests.values()) {
                    if (r.getRequesterPos().equals(reqPos)
                            && r.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                        toCancel.add(r.getRequestId());
                    }
                }
                continue;
            }

            // Interface desactivee ou delinkee
            if (be instanceof NetworkInterfaceBlockEntity iface) {
                if (!iface.isActive() || !iface.isLinked()) {
                    for (InterfaceRequest r : activeRequests.values()) {
                        if (r.getRequesterPos().equals(reqPos)
                                && r.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                            toCancel.add(r.getRequestId());
                        }
                    }
                }
            }
        }

        for (UUID id : toCancel) {
            cancelRequest(id);
        }
    }

    // === Tick ===

    public void tick() {
        processTimer++;
        if (processTimer >= PROCESS_INTERVAL) {
            processTimer = 0;
            processRequests();
        }

        blockedRecheckTimer++;
        if (blockedRecheckTimer >= BLOCKED_RECHECK_INTERVAL) {
            blockedRecheckTimer = 0;
            recheckBlockedRequests();
        }

        sourceValidationTimer++;
        if (sourceValidationTimer >= SOURCE_VALIDATION_INTERVAL) {
            sourceValidationTimer = 0;
            validateRequestSources();
        }
    }

    // === NBT ===

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag requestsTag = new ListTag();
        for (InterfaceRequest request : activeRequests.values()) {
            // Ne pas sauvegarder les demandes CANCELLED
            if (request.getStatus() == InterfaceRequest.RequestStatus.CANCELLED) continue;
            // Sauvegarder les ASSIGNED comme PENDING (la bee sera perdue au reload)
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
        activeRequests.clear();
        if (tag.contains("Requests")) {
            ListTag requestsTag = tag.getList("Requests", Tag.TAG_COMPOUND);
            for (int i = 0; i < requestsTag.size(); i++) {
                InterfaceRequest request = InterfaceRequest.load(requestsTag.getCompound(i), registries);
                activeRequests.put(request.getRequestId(), request);
            }
        }
    }
}
