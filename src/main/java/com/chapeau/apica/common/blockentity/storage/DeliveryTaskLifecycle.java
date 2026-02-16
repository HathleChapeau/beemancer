/**
 * ============================================================
 * [DeliveryTaskLifecycle.java]
 * Description: Gestion du cycle de vie des taches (completion, echec, retry)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageDeliveryManager         | Orchestrateur        | Acces donnees partagees   |
 * | DeliveryTask                   | Tache de livraison   | Etat, retry               |
 * | NetworkInterfaceBlockEntity    | Interface reseau     | Notification echec        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.DeliveryTask;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Gere la completion et l'echec des taches de livraison.
 * Delegue au RequestManager pour notifier les requests associees.
 * [AV] Implemente le retry avec backoff exponentiel.
 */
public class DeliveryTaskLifecycle {
    private final StorageDeliveryManager manager;

    public DeliveryTaskLifecycle(StorageDeliveryManager manager) {
        this.manager = manager;
    }

    /**
     * Marque une tache active comme completee par son UUID.
     */
    public void completeTask(UUID taskId) {
        finishTask(taskId, DeliveryTask.DeliveryState.COMPLETED);
    }

    /**
     * Marque une tache active comme echouee par son UUID.
     */
    public void failTask(UUID taskId) {
        finishTask(taskId, DeliveryTask.DeliveryState.FAILED);
    }

    /**
     * Termine une tache active: met a jour l'etat, retire de la liste, marque pour sync.
     * Pour les taches COMPLETED, ne notifie le RequestManager que quand toutes les
     * subtasks du meme groupe (meme rootTaskId) sont terminees.
     * [AV] Pour les taches FAILED: retry avec backoff exponentiel si retries restantes.
     */
    void finishTask(UUID taskId, DeliveryTask.DeliveryState state) {
        long gameTick = manager.getParent().getLevel() != null
            ? manager.getParent().getLevel().getGameTime() : 0;
        List<DeliveryTask> activeTasks = manager.getActiveTasks();
        Map<UUID, Long> completedTaskIds = manager.getCompletedTaskIds();
        PriorityQueue<DeliveryTask> queue = manager.getDeliveryQueue();

        Iterator<DeliveryTask> it = activeTasks.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                task.setState(state);
                UUID rootId = task.getRootTaskId();
                it.remove();

                if (state == DeliveryTask.DeliveryState.COMPLETED) {
                    completedTaskIds.put(taskId, gameTick);
                    if (task.getInterfaceTaskId() == null) {
                        if (!hasRemainingSubtasks(rootId)) {
                            manager.getParent().getRequestManager().onTaskCompleted(rootId);
                        }
                    }
                } else if (state == DeliveryTask.DeliveryState.FAILED) {
                    if (task.canRetry()) {
                        task.incrementRetry(gameTick);
                        task.setState(DeliveryTask.DeliveryState.QUEUED);
                        task.setFlyingStartTick(-1);
                        queue.add(task);
                    } else {
                        if (task.getInterfaceTaskId() == null) {
                            manager.getParent().getRequestManager().onTaskFailed(rootId);
                        } else {
                            notifyInterfaceTaskFailed(task);
                        }
                    }
                }

                manager.markTasksDirty();
                manager.getParent().setChanged();
                manager.getParent().getItemAggregator().setNeedsSync(true);
                return;
            }
        }
    }

    /**
     * Notifie l'interface qu'une task a echoue: remet la task en NEEDED pour re-tentative.
     */
    private void notifyInterfaceTaskFailed(DeliveryTask task) {
        if (task.getInterfacePos() == null || task.getInterfaceTaskId() == null) return;
        if (manager.getParent().getLevel() == null) return;
        if (!manager.getParent().getLevel().hasChunkAt(task.getInterfacePos())) return;
        BlockEntity be = manager.getParent().getLevel().getBlockEntity(task.getInterfacePos());
        if (be instanceof NetworkInterfaceBlockEntity iface) {
            iface.unlockTask(task.getInterfaceTaskId());
        }
    }

    /**
     * Verifie s'il reste des subtasks actives ou en queue pour un meme rootTaskId.
     */
    boolean hasRemainingSubtasks(UUID rootId) {
        for (DeliveryTask t : manager.getActiveTasks()) {
            if (t.getRootTaskId().equals(rootId)
                    && (t.getState() == DeliveryTask.DeliveryState.FLYING
                        || t.getState() == DeliveryTask.DeliveryState.QUEUED)) {
                return true;
            }
        }
        for (DeliveryTask t : manager.getDeliveryQueue()) {
            if (t.getRootTaskId().equals(rootId)
                    && t.getState() == DeliveryTask.DeliveryState.QUEUED) {
                return true;
            }
        }
        return false;
    }
}
