/**
 * ============================================================
 * [DeliveryTaskDisplayBuilder.java]
 * Description: Construction des donnees d'affichage pour l'onglet Tasks du terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | TaskDisplayData                | DTO affichage        | Construction resultat     |
 * | DeliveryTask                   | Taches de livraison  | Lecture etat/template     |
 * | InterfaceRequest               | Requests reseau      | Groupement parent/enfant  |
 * | DeliveryContainerOps           | Operations coffres   | computeBlockedReason      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Construit les donnees d'affichage (TaskDisplayData) pour l'onglet Tasks du terminal.
 * Stateless: toutes les donnees necessaires sont passees en parametres.
 */
public final class DeliveryTaskDisplayBuilder {

    private DeliveryTaskDisplayBuilder() {}

    /**
     * Construit la liste d'affichage: requests comme parents, tasks comme enfants.
     */
    public static List<TaskDisplayData> buildDisplayData(
            Collection<DeliveryTask> activeTasks,
            Collection<DeliveryTask> queuedTasks,
            boolean honeyDepleted,
            int maxBees,
            Set<UUID> completedTaskIds,
            RequestManager requestManager,
            DeliveryContainerOps containerOps,
            @Nullable Level level) {

        List<TaskDisplayData> result = new ArrayList<>();

        Map<UUID, InterfaceRequest> rootToRequest = new HashMap<>();
        for (InterfaceRequest request : requestManager.getAllRequests()) {
            if (request.getAssignedTaskId() != null
                    && request.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                rootToRequest.put(request.getAssignedTaskId(), request);
            }
        }

        for (InterfaceRequest request : rootToRequest.values()) {
            String aggregateState = computeAggregateState(
                request.getAssignedTaskId(), activeTasks, queuedTasks);
            String originStr = request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST
                ? "REQUEST" : "AUTOMATION";
            result.add(new TaskDisplayData(
                request.getRequestId(),
                request.getTemplate(),
                request.getOriginalCount(),
                aggregateState,
                List.of(),
                originStr,
                "",
                request.getRequesterPos(),
                deriveRequesterType(request),
                null
            ));
        }

        for (DeliveryTask task : activeTasks) {
            InterfaceRequest request = rootToRequest.get(task.getRootTaskId());
            if (request != null) {
                result.add(taskAsChild(task, request.getRequestId(),
                    honeyDepleted, activeTasks.size(), maxBees, completedTaskIds, containerOps, level));
            } else {
                result.add(taskAsRoot(task, requestManager,
                    honeyDepleted, activeTasks.size(), maxBees, completedTaskIds, containerOps, level));
            }
        }
        for (DeliveryTask task : queuedTasks) {
            InterfaceRequest request = rootToRequest.get(task.getRootTaskId());
            if (request != null) {
                result.add(taskAsChild(task, request.getRequestId(),
                    honeyDepleted, activeTasks.size(), maxBees, completedTaskIds, containerOps, level));
            } else {
                result.add(taskAsRoot(task, requestManager,
                    honeyDepleted, activeTasks.size(), maxBees, completedTaskIds, containerOps, level));
            }
        }

        return result;
    }

    /**
     * Calcule l'etat agrege d'une request en fonction de ses tasks.
     */
    static String computeAggregateState(UUID rootTaskId,
            Collection<DeliveryTask> activeTasks, Collection<DeliveryTask> queuedTasks) {
        boolean hasFlying = false;
        boolean hasQueued = false;
        for (DeliveryTask t : activeTasks) {
            if (t.getRootTaskId().equals(rootTaskId)
                    && t.getState() == DeliveryTask.DeliveryState.FLYING) {
                hasFlying = true;
            }
        }
        for (DeliveryTask t : queuedTasks) {
            if (t.getRootTaskId().equals(rootTaskId)
                    && t.getState() == DeliveryTask.DeliveryState.QUEUED) {
                hasQueued = true;
            }
        }
        if (hasFlying) return "FLYING";
        if (hasQueued) return "QUEUED";
        return "COMPLETED";
    }

    /**
     * Cree un TaskDisplayData pour une task enfant d'une request.
     */
    private static TaskDisplayData taskAsChild(DeliveryTask task, UUID displayParentId,
            boolean honeyDepleted, int activeCount, int maxBees,
            Set<UUID> completedTaskIds, DeliveryContainerOps containerOps,
            @Nullable Level level) {
        return new TaskDisplayData(
            task.getTaskId(),
            task.getTemplate(),
            task.getCount(),
            task.getState().name(),
            task.getDependencies(),
            task.getOrigin().name(),
            computeBlockedReason(task, honeyDepleted, activeCount, maxBees,
                completedTaskIds, containerOps, level),
            task.getRequesterPos(),
            "",
            displayParentId
        );
    }

    /**
     * Cree un TaskDisplayData pour une task standalone (sans request associee).
     */
    private static TaskDisplayData taskAsRoot(DeliveryTask task, RequestManager requestManager,
            boolean honeyDepleted, int activeCount, int maxBees,
            Set<UUID> completedTaskIds, DeliveryContainerOps containerOps,
            @Nullable Level level) {
        BlockPos requesterPos = null;
        String requesterType = "";
        InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
        if (request != null) {
            requesterPos = request.getRequesterPos();
            requesterType = deriveRequesterType(request);
        }

        return new TaskDisplayData(
            task.getTaskId(),
            task.getTemplate(),
            task.getCount(),
            task.getState().name(),
            task.getDependencies(),
            task.getOrigin().name(),
            computeBlockedReason(task, honeyDepleted, activeCount, maxBees,
                completedTaskIds, containerOps, level),
            requesterPos,
            requesterType,
            task.getParentTaskId()
        );
    }

    /**
     * Derive le type de bloc demandeur depuis les champs de la request.
     */
    static String deriveRequesterType(InterfaceRequest request) {
        if (request.getOrigin() == InterfaceRequest.TaskOrigin.REQUEST) {
            return "Terminal";
        }
        return switch (request.getType()) {
            case IMPORT -> "Import";
            case EXPORT -> "Export";
        };
    }

    /**
     * Calcule la raison de blocage d'une tache QUEUED.
     * Retourne une cle de langue ou une chaine vide si non bloquee.
     */
    static String computeBlockedReason(DeliveryTask task,
            boolean honeyDepleted, int activeCount, int maxBees,
            Set<UUID> completedTaskIds, DeliveryContainerOps containerOps,
            @Nullable Level level) {
        if (task.getState() != DeliveryTask.DeliveryState.QUEUED) return "";

        if (honeyDepleted) return "gui.beemancer.tasks.blocked.honey_depleted";
        if (activeCount >= maxBees) return "gui.beemancer.tasks.blocked.no_bee_slot";
        if (!task.isReady(completedTaskIds)) return "gui.beemancer.tasks.blocked.dependency_pending";

        if (!task.isPreloaded()) {
            int available = containerOps.countItemInChest(task.getTemplate(), task.getSourcePos());
            if (available <= 0) return "gui.beemancer.tasks.blocked.items_unavailable";
        }

        if (!task.isPreloaded() && level != null && task.getSourcePos() != null
                && level.isLoaded(task.getSourcePos())) {
            BlockEntity be = level.getBlockEntity(task.getSourcePos());
            if (be == null) return "gui.beemancer.tasks.blocked.target_missing";
        }

        return "";
    }
}
