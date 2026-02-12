/**
 * ============================================================
 * [StorageDeliveryManager.java]
 * Description: Orchestrateur du systeme de livraison par abeilles
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Back-reference            |
 * | HoneyEnergyManager             | Consommation miel    | Delegation                |
 * | DeliveryTaskLifecycle          | Completion/echec     | Delegation                |
 * | DeliveryTaskCanceller          | Annulation           | Delegation                |
 * | DeliveryBeeSpawner             | Spawn/recall bees    | spawnDeliveryBee, recall  |
 * | DeliveryContainerOps           | Operations coffres   | Delegation                |
 * | DeliveryNetworkPathfinder      | Pathfinding reseau   | Delegation                |
 * | DeliveryTaskDisplayBuilder     | Affichage Tasks tab  | buildDisplayData          |
 * | ControllerStats                | Stats essences       | Capacite bee              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (delegation)
 * - StorageMultiblockManager.java (clearAllTasks, killAllDeliveryBees)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Orchestrateur du systeme de livraison: queue de taches, ticks, NBT.
 * Possede les donnees partagees (queue, activeTasks, completedTaskIds)
 * et delegue la logique aux managers specialises:
 * - HoneyEnergyManager: consommation de miel
 * - DeliveryTaskLifecycle: completion, echec, retry
 * - DeliveryTaskCanceller: annulation, dependances
 * - DeliveryBeeSpawner: spawn, recall, redirect des bees
 * - DeliveryContainerOps: operations sur les coffres du reseau
 * - DeliveryNetworkPathfinder: pathfinding via relays
 */
public class StorageDeliveryManager {
    private final StorageControllerBlockEntity parent;

    private static final int DELIVERY_PROCESS_INTERVAL = 10;
    private static final int REEVALUATE_INTERVAL = 20;
    // [AW] Timeout pour les taches FLYING: 1 minute (1200 ticks)
    private static final int FLYING_TIMEOUT_TICKS = 1200;
    // [AT] TTL pour les completedTaskIds: 30 secondes (600 ticks)
    private static final int COMPLETED_TTL_TICKS = 600;
    // [BB] Sleep mode: seuil d'inactivite avant passage en sleep (30 sec = 600 ticks)
    private static final int IDLE_SLEEP_THRESHOLD = 600;
    // [BB] Intervalle de tick en mode sleep (2 sec = 40 ticks)
    private static final int SLEEP_TICK_INTERVAL = 40;
    // [BC] Backpressure: taille max de la queue par bee (interfaces seulement)
    private static final int QUEUE_SIZE_PER_BEE = 50;

    // === Donnees partagees (accessibles via package-private getters par les sub-managers) ===
    private final PriorityQueue<DeliveryTask> deliveryQueue =
        new PriorityQueue<>(Comparator.comparingInt(DeliveryTask::getPriority));
    private final List<DeliveryTask> activeTasks = new ArrayList<>();
    private final Map<UUID, Long> completedTaskIds = new HashMap<>();
    private boolean tasksDirty = true;
    private int idleTicks = 0;
    private boolean sleeping = false;

    // Items a retourner au reseau apres le load (differe au premier tick)
    private final List<ItemStack> deferredReturns = new ArrayList<>();

    // === Managers delegues ===
    private final HoneyEnergyManager honeyManager;
    private final DeliveryTaskLifecycle lifecycle;
    private final DeliveryTaskCanceller canceller;
    private final DeliveryNetworkPathfinder pathfinder;
    private final DeliveryContainerOps containerOps;
    private final DeliveryBeeSpawner beeSpawner;

    public StorageDeliveryManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
        this.honeyManager = new HoneyEnergyManager(parent);
        this.lifecycle = new DeliveryTaskLifecycle(this);
        this.canceller = new DeliveryTaskCanceller(this);
        this.pathfinder = new DeliveryNetworkPathfinder(parent);
        this.containerOps = new DeliveryContainerOps(parent);
        this.beeSpawner = new DeliveryBeeSpawner(parent, pathfinder, containerOps);
    }

    // === Package-private accessors pour les sub-managers ===

    StorageControllerBlockEntity getParent() { return parent; }
    PriorityQueue<DeliveryTask> getDeliveryQueue() { return deliveryQueue; }
    List<DeliveryTask> getActiveTasks() { return activeTasks; }
    Map<UUID, Long> getCompletedTaskIds() { return completedTaskIds; }
    void markTasksDirty() { tasksDirty = true; }

    // === Public accessors ===

    public DeliveryNetworkPathfinder getPathfinder() { return pathfinder; }
    public DeliveryContainerOps getContainerOps() { return containerOps; }
    public DeliveryBeeSpawner getBeeSpawner() { return beeSpawner; }
    public boolean isHoneyDepleted() { return honeyManager.isHoneyDepleted(); }

    // === Task Management ===

    public void addDeliveryTask(DeliveryTask task) {
        deliveryQueue.add(task);
        idleTicks = 0;
        sleeping = false;
        tasksDirty = true;
        parent.setChanged();
    }

    // === Delegation: Completion/Failure (DeliveryTaskLifecycle) ===

    public void completeTask(UUID taskId) { lifecycle.completeTask(taskId); }
    public void failTask(UUID taskId) { lifecycle.failTask(taskId); }

    // === Delegation: Cancellation (DeliveryTaskCanceller) ===

    public boolean cancelTask(UUID taskId) { return canceller.cancelTask(taskId); }

    // === Queue Processing ===

    private void processDeliveryQueue(long gameTick) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()
            || !parent.getMultiblockManager().isFormed() || honeyManager.isHoneyDepleted()) return;

        activeTasks.removeIf(task -> {
            if (task.getState() == DeliveryTask.DeliveryState.COMPLETED) {
                completedTaskIds.put(task.getTaskId(), gameTick);
                return true;
            }
            return task.getState() == DeliveryTask.DeliveryState.FAILED;
        });

        completedTaskIds.entrySet().removeIf(
            entry -> gameTick - entry.getValue() > COMPLETED_TTL_TICKS);

        while (activeTasks.size() < parent.getMaxDeliveryBees()) {
            DeliveryTask eligible = findEligibleTask(gameTick);
            if (eligible == null) break;

            deliveryQueue.remove(eligible);

            if (eligible.getInterfaceTaskId() == null) {
                int beeCapacity = Math.min(
                    ControllerStats.getQuantity(parent.getEssenceSlots()),
                    eligible.getTemplate().getMaxStackSize()
                );
                if (eligible.getCount() > beeCapacity) {
                    DeliveryTask rest = eligible.splitRemaining(beeCapacity);
                    if (rest != null) {
                        deliveryQueue.add(rest);
                    }
                }
            }

            boolean spawned = beeSpawner.spawnDeliveryBee(eligible);
            if (spawned) {
                eligible.setState(DeliveryTask.DeliveryState.FLYING);
                eligible.setFlyingStartTick(gameTick);
                activeTasks.add(eligible);
            } else {
                eligible.setState(DeliveryTask.DeliveryState.FAILED);
            }
        }
    }

    /**
     * Reevalue les taches en fonction de la demande actuelle.
     * [AW] Timeout FLYING, annulation si demande tombe a 0, nettoyage requests orphelines.
     */
    private void reevaluateTasks(long gameTick) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()
            || !parent.getMultiblockManager().isFormed()) return;

        // [AW] Timeout FLYING
        for (DeliveryTask task : new ArrayList<>(activeTasks)) {
            if (task.getState() == DeliveryTask.DeliveryState.FLYING
                    && task.getFlyingStartTick() >= 0
                    && (gameTick - task.getFlyingStartTick()) > FLYING_TIMEOUT_TICKS) {
                lifecycle.failTask(task.getTaskId());
            }
        }

        RequestManager requestManager = parent.getRequestManager();

        // Reevaluer les taches en queue: annuler si demande tombe a 0
        Iterator<DeliveryTask> queueIt = deliveryQueue.iterator();
        while (queueIt.hasNext()) {
            DeliveryTask task = queueIt.next();
            if (task.getState() != DeliveryTask.DeliveryState.QUEUED) continue;
            if (task.getInterfaceTaskId() != null) continue;

            InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
            if (request == null) continue;

            if (request.getCount() <= 0) {
                queueIt.remove();
                canceller.handleCancelledTask(task);
                tasksDirty = true;
                parent.setChanged();
            }
        }

        // Reevaluer les taches actives: annuler si demande tombe a 0
        for (DeliveryTask task : new ArrayList<>(activeTasks)) {
            if (task.getState() != DeliveryTask.DeliveryState.FLYING) continue;
            if (task.getInterfaceTaskId() != null) continue;

            InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
            if (request == null) continue;

            if (request.getCount() <= 0) {
                canceller.cancelTask(task.getTaskId());
            }
        }

        // Nettoyer les requests orphelines
        for (InterfaceRequest request : new ArrayList<>(requestManager.getAllRequests())) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.ASSIGNED) continue;
            UUID assignedId = request.getAssignedTaskId();
            if (assignedId == null) continue;
            if (!lifecycle.hasRemainingSubtasks(assignedId)) {
                requestManager.onTaskCompleted(assignedId);
            }
        }
    }

    /**
     * Trouve la tache eligible avec la plus haute priorite.
     * Package-private pour acces par DeliveryTaskCanceller (redirect bee).
     */
    DeliveryTask findEligibleTask(long gameTick) {
        DeliveryTask best = null;
        for (DeliveryTask task : deliveryQueue) {
            if (task.getState() == DeliveryTask.DeliveryState.QUEUED
                && task.isReady(completedTaskIds.keySet())
                && task.isRetryReady(gameTick)) {
                if (best == null || task.getPriority() < best.getPriority()) {
                    best = task;
                }
            }
        }
        return best;
    }

    // === Bee Management ===

    public void killAllDeliveryBees() { beeSpawner.killAllDeliveryBees(); }

    public void clearAllTasks() {
        deliveryQueue.clear();
        activeTasks.clear();
        completedTaskIds.clear();
    }

    // === Query ===

    public List<TaskDisplayData> getTaskDisplayData() {
        return DeliveryTaskDisplayBuilder.buildDisplayData(
            activeTasks, deliveryQueue, honeyManager.isHoneyDepleted(),
            parent.getMaxDeliveryBees(), completedTaskIds.keySet(),
            parent.getRequestManager(), containerOps, parent.getLevel());
    }

    public boolean isTaskPending(UUID taskId) {
        for (DeliveryTask t : activeTasks) {
            if (t.getTaskId().equals(taskId)) return true;
        }
        for (DeliveryTask t : deliveryQueue) {
            if (t.getTaskId().equals(taskId)) return true;
        }
        return false;
    }

    public int getActiveTaskCount() { return activeTasks.size(); }
    public int getQueuedTaskCount() { return deliveryQueue.size(); }
    public boolean isTasksDirty() { return tasksDirty; }
    public void resetTasksDirty() { tasksDirty = false; }

    public boolean isTooBusyFor(DeliveryTask.TaskOrigin origin) {
        if (origin == DeliveryTask.TaskOrigin.REQUEST) return false;
        int maxQueueSize = QUEUE_SIZE_PER_BEE * parent.getMaxDeliveryBees();
        return (deliveryQueue.size() + activeTasks.size()) >= maxQueueSize;
    }

    // === Tick ===

    public void tickDelivery(long gameTick) {
        processDeferredReturns();

        if (deliveryQueue.isEmpty() && activeTasks.isEmpty()) {
            idleTicks++;
            if (idleTicks >= IDLE_SLEEP_THRESHOLD) {
                sleeping = true;
            }
        } else {
            idleTicks = 0;
            sleeping = false;
        }

        if (sleeping && gameTick % SLEEP_TICK_INTERVAL != 0) {
            return;
        }

        long offset = parent.getBlockPos().hashCode();
        if ((gameTick + offset) % DELIVERY_PROCESS_INTERVAL == 0) {
            processDeliveryQueue(gameTick);
        }
        if ((gameTick + offset) % REEVALUATE_INTERVAL == 0) {
            reevaluateTasks(gameTick);
        }
    }

    public void tickHoneyConsumption(long gameTick) {
        honeyManager.tickHoneyConsumption(gameTick);
    }

    // === NBT ===

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("HoneyDepleted", honeyManager.isHoneyDepleted());

        ListTag queueTag = new ListTag();
        for (DeliveryTask task : deliveryQueue) {
            queueTag.add(task.save(registries));
        }
        for (DeliveryTask task : activeTasks) {
            queueTag.add(task.save(registries));
        }
        tag.put("DeliveryQueue", queueTag);

        ListTag completedTag = new ListTag();
        for (Map.Entry<UUID, Long> entry : completedTaskIds.entrySet()) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("Id", entry.getKey());
            idTag.putLong("Tick", entry.getValue());
            completedTag.add(idTag);
        }
        tag.put("CompletedTaskIds", completedTag);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        honeyManager.setHoneyDepleted(tag.getBoolean("HoneyDepleted"));

        deliveryQueue.clear();
        activeTasks.clear();
        deferredReturns.clear();
        if (tag.contains("DeliveryQueue")) {
            ListTag queueTag = tag.getList("DeliveryQueue", Tag.TAG_COMPOUND);
            for (int i = 0; i < queueTag.size(); i++) {
                try {
                    DeliveryTask task = DeliveryTask.load(queueTag.getCompound(i), registries);
                    if (task.getState() == DeliveryTask.DeliveryState.FLYING) {
                        if (task.getInterfaceTaskId() != null) {
                            // Interface task: l'interface gere ses propres items
                        } else {
                            // Differer au premier tick (ne pas acceder au monde pendant le load)
                            deferredReturns.add(task.getTemplate().copyWithCount(task.getCount()));
                        }
                    } else {
                        deliveryQueue.add(task);
                    }
                } catch (Exception e) {
                    com.chapeau.beemancer.Beemancer.LOGGER.warn("Skipping corrupted delivery task at index {}", i, e);
                }
            }
        }

        completedTaskIds.clear();
        if (tag.contains("CompletedTaskIds")) {
            ListTag completedTag = tag.getList("CompletedTaskIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < completedTag.size(); i++) {
                CompoundTag entry = completedTag.getCompound(i);
                UUID id = entry.getUUID("Id");
                long tick = entry.contains("Tick") ? entry.getLong("Tick") : 0L;
                completedTaskIds.put(id, tick);
            }
        }

        if (!deliveryQueue.isEmpty()) {
            tasksDirty = true;
        }
    }

    /**
     * Traite les items differes du load: depose dans les coffres ou drop au sol.
     * Appele au premier tickDelivery apres le chargement du monde.
     */
    private void processDeferredReturns() {
        if (deferredReturns.isEmpty()) return;
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        for (ItemStack toReturn : deferredReturns) {
            ItemStack remainder = parent.depositItem(toReturn);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(parent.getLevel(),
                    parent.getBlockPos().getX() + 0.5,
                    parent.getBlockPos().getY() + 1.0,
                    parent.getBlockPos().getZ() + 0.5,
                    remainder);
            }
        }
        deferredReturns.clear();
    }
}
