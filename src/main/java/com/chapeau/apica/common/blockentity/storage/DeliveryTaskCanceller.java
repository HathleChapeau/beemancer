/**
 * ============================================================
 * [DeliveryTaskCanceller.java]
 * Description: Annulation de taches et gestion des dependances
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageDeliveryManager         | Orchestrateur        | Acces donnees partagees   |
 * | DeliveryTask                   | Tache de livraison   | Etat, dependances         |
 * | DeliveryBeeSpawner             | Gestion bees         | Recall, redirect          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.DeliveryTask;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Gere l'annulation des taches de livraison et la propagation aux taches dependantes.
 * [BT] Utilise une approche iterative (Deque) au lieu de la recursion.
 */
public class DeliveryTaskCanceller {
    private final StorageDeliveryManager manager;

    public DeliveryTaskCanceller(StorageDeliveryManager manager) {
        this.manager = manager;
    }

    /**
     * Annule une tache par son ID.
     * Si la tache est en queue, la retire. Si active, marque comme FAILED et rappelle la bee.
     * Annule aussi les taches dependantes via cancelDependentTasks.
     *
     * @return true si la tache a ete trouvee et annulee
     */
    public boolean cancelTask(UUID taskId) {
        PriorityQueue<DeliveryTask> queue = manager.getDeliveryQueue();
        List<DeliveryTask> activeTasks = manager.getActiveTasks();
        DeliveryBeeSpawner beeSpawner = manager.getBeeSpawner();

        // Verifier dans la queue
        Iterator<DeliveryTask> it = queue.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                it.remove();
                handleCancelledTask(task);
                cancelDependentTasks(taskId);
                manager.markTasksDirty();
                manager.getParent().setChanged();
                return true;
            }
        }

        // Verifier dans les taches actives
        for (DeliveryTask task : activeTasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setState(DeliveryTask.DeliveryState.FAILED);

                // Chercher une tache en queue pour rediriger la bee
                long tick = manager.getParent().getLevel() != null
                    ? manager.getParent().getLevel().getGameTime() : 0;
                DeliveryTask nextTask = manager.findEligibleTask(tick);
                if (nextTask != null) {
                    queue.remove(nextTask);
                    DeliveryTask activated = beeSpawner.redirectBee(task, nextTask);
                    if (activated != null) {
                        activeTasks.add(activated);
                    }
                } else {
                    beeSpawner.recallBeeForTask(task);
                }

                handleCancelledTask(task);
                cancelDependentTasks(taskId);
                manager.markTasksDirty();
                manager.getParent().setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * [BT] Annule iterativement toutes les taches qui dependent de la tache donnee,
     * ainsi que toutes les subtasks du meme groupe (meme rootTaskId).
     */
    void cancelDependentTasks(UUID cancelledTaskId) {
        PriorityQueue<DeliveryTask> queue = manager.getDeliveryQueue();
        List<DeliveryTask> activeTasks = manager.getActiveTasks();
        DeliveryBeeSpawner beeSpawner = manager.getBeeSpawner();

        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(cancelledTaskId);

        while (!stack.isEmpty()) {
            UUID currentId = stack.pop();

            List<DeliveryTask> toCancel = new ArrayList<>();
            for (DeliveryTask task : queue) {
                if (task.getDependencies().contains(currentId)
                        || currentId.equals(task.getParentTaskId())) {
                    toCancel.add(task);
                }
            }
            for (DeliveryTask task : toCancel) {
                queue.remove(task);
                handleCancelledTask(task);
                stack.push(task.getTaskId());
            }

            for (DeliveryTask task : new ArrayList<>(activeTasks)) {
                if (currentId.equals(task.getParentTaskId())) {
                    task.setState(DeliveryTask.DeliveryState.FAILED);
                    beeSpawner.recallBeeForTask(task);
                    handleCancelledTask(task);
                }
            }
        }
    }

    /**
     * Gere les consequences de l'annulation d'une tache.
     * Pour les taches preloaded: remet les items dans le reseau (drop au sol si plein).
     */
    void handleCancelledTask(DeliveryTask task) {
        if (task.isPreloaded()) {
            ItemStack toReturn = task.getTemplate().copyWithCount(task.getCount());
            ItemStack remaining = manager.getParent().getItemAggregator().depositItem(toReturn);
            if (!remaining.isEmpty() && manager.getParent().getLevel() != null) {
                Containers.dropItemStack(manager.getParent().getLevel(),
                    manager.getParent().getBlockPos().getX() + 0.5,
                    manager.getParent().getBlockPos().getY() + 1.0,
                    manager.getParent().getBlockPos().getZ() + 0.5, remaining);
            }
        }
    }
}
