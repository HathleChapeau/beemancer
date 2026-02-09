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
 * | DeliveryTask                   | Tache de livraison   | Queue et etat             |
 * | DeliveryBeeSpawner             | Spawn/recall bees    | spawnDeliveryBee, recall  |
 * | DeliveryContainerOps           | Operations coffres   | Delegation                |
 * | DeliveryNetworkPathfinder      | Pathfinding reseau   | Delegation                |
 * | DeliveryTaskDisplayBuilder     | Affichage Tasks tab  | buildDisplayData          |
 * | ControllerStats                | Stats essences       | Capacite, consommation    |
 * | HoneyReservoirBlockEntity      | Drain miel           | consumeHoney              |
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
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrateur du systeme de livraison: queue de taches, ticks, cancel, NBT.
 * Delegue les operations specifiques a DeliveryBeeSpawner, DeliveryContainerOps,
 * DeliveryNetworkPathfinder et DeliveryTaskDisplayBuilder.
 */
public class StorageDeliveryManager {
    private final StorageControllerBlockEntity parent;

    private static final int DELIVERY_PROCESS_INTERVAL = 10;
    private static final int REEVALUATE_INTERVAL = 20;
    private static final int MAX_COMPLETED_IDS = 100;
    private static final int HONEY_CONSUME_INTERVAL = 20;

    private final Queue<DeliveryTask> deliveryQueue = new LinkedList<>();
    private final List<DeliveryTask> activeTasks = new ArrayList<>();
    private final Set<UUID> completedTaskIds = new LinkedHashSet<>();
    private int deliveryTimer = 0;
    private int reevaluateTimer = 0;
    private int honeyConsumeTimer = 0;
    private boolean honeyDepleted = false;

    private final DeliveryNetworkPathfinder pathfinder;
    private final DeliveryContainerOps containerOps;
    private final DeliveryBeeSpawner beeSpawner;

    public StorageDeliveryManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
        this.pathfinder = new DeliveryNetworkPathfinder(parent);
        this.containerOps = new DeliveryContainerOps(parent);
        this.beeSpawner = new DeliveryBeeSpawner(parent, pathfinder, containerOps);
    }

    public DeliveryNetworkPathfinder getPathfinder() {
        return pathfinder;
    }

    public DeliveryContainerOps getContainerOps() {
        return containerOps;
    }

    public DeliveryBeeSpawner getBeeSpawner() {
        return beeSpawner;
    }

    // === Task Management ===

    /**
     * Ajoute une tâche de livraison à la queue.
     */
    public void addDeliveryTask(DeliveryTask task) {
        deliveryQueue.add(task);
        parent.setChanged();
    }

    // === Honey Consumption ===

    /**
     * Consomme du miel depuis les 2 HoneyReservoirs du multibloc.
     */
    private void consumeHoney() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        int consumptionPerSecond = ControllerStats.getHoneyConsumption(
            parent.getEssenceSlots(), parent.getChestManager().getRegisteredChestCount(),
            parent.getHiveMultiplier());
        int remaining = consumptionPerSecond;

        int rotation = parent.getMultiblockManager().getRotation();
        for (int xOff : new int[]{-1, 1}) {
            if (remaining <= 0) break;
            Vec3i rotatedOffset = MultiblockPattern.rotateY(new Vec3i(xOff, 0, 0), rotation);
            BlockPos reservoirPos = parent.getBlockPos().offset(rotatedOffset);
            if (!parent.getLevel().isLoaded(reservoirPos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(reservoirPos);
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack drained = reservoir.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    remaining -= drained.getAmount();
                }
            }
        }

        boolean wasDepleted = honeyDepleted;
        honeyDepleted = remaining > 0;
        if (wasDepleted != honeyDepleted) {
            parent.syncToClient();
        }
    }

    public boolean isHoneyDepleted() {
        return honeyDepleted;
    }

    // === Queue Processing ===

    /**
     * Traite la queue de livraison: spawne des bees pour les tâches en attente.
     */
    private void processDeliveryQueue() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()
            || !parent.getMultiblockManager().isFormed() || honeyDepleted) return;

        activeTasks.removeIf(task -> {
            if (task.getState() == DeliveryTask.DeliveryState.COMPLETED) {
                completedTaskIds.add(task.getTaskId());
                return true;
            }
            if (task.getState() == DeliveryTask.DeliveryState.FAILED) {
                return true;
            }
            return false;
        });

        if (completedTaskIds.size() > MAX_COMPLETED_IDS) {
            Iterator<UUID> it = completedTaskIds.iterator();
            while (completedTaskIds.size() > MAX_COMPLETED_IDS / 2 && it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        List<DeliveryTask> sortedQueue = new ArrayList<>(deliveryQueue);
        sortedQueue.sort(Comparator.comparingInt(DeliveryTask::getPriority));
        deliveryQueue.clear();
        deliveryQueue.addAll(sortedQueue);

        while (activeTasks.size() < parent.getMaxDeliveryBees()) {
            DeliveryTask eligible = findEligibleTask();
            if (eligible == null) break;

            deliveryQueue.remove(eligible);

            // Interface tasks sont deja split par l'interface, ne pas re-split
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
                activeTasks.add(eligible);
            } else {
                eligible.setState(DeliveryTask.DeliveryState.FAILED);
            }
        }
    }

    /**
     * Reevalue les taches en fonction de la demande actuelle.
     * - QUEUED: met a jour le count, split si necessaire, cancel si demande nulle
     * - FLYING: cree des sous-taches si demande augmente, cancel si demande tombe a 0
     */
    private void reevaluateTasks() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()
            || !parent.getMultiblockManager().isFormed()) return;

        RequestManager requestManager = parent.getRequestManager();

        // Reevaluer les taches en queue: annuler si demande tombe a 0
        // (seulement les tasks terminal, les tasks interface sont gerees par l'interface)
        Iterator<DeliveryTask> queueIt = deliveryQueue.iterator();
        while (queueIt.hasNext()) {
            DeliveryTask task = queueIt.next();
            if (task.getState() != DeliveryTask.DeliveryState.QUEUED) continue;
            if (task.getInterfaceTaskId() != null) continue; // Geree par l'interface

            InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
            if (request == null) continue;

            if (request.getCount() <= 0) {
                queueIt.remove();
                handleCancelledTask(task);
                parent.setChanged();
            }
        }

        // Reevaluer les taches actives: annuler si demande tombe a 0
        // (seulement les tasks terminal)
        for (DeliveryTask task : new ArrayList<>(activeTasks)) {
            if (task.getState() != DeliveryTask.DeliveryState.FLYING) continue;
            if (task.getInterfaceTaskId() != null) continue; // Geree par l'interface

            InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
            if (request == null) continue;

            if (request.getCount() <= 0) {
                cancelTask(task.getTaskId());
            }
        }

        // Nettoyer les requests orphelines: ASSIGNED mais plus aucune subtask en cours
        // (arrive quand des subtasks echouent au spawn, ex: deposit vide)
        for (InterfaceRequest request : new ArrayList<>(requestManager.getAllRequests())) {
            if (request.getStatus() != InterfaceRequest.RequestStatus.ASSIGNED) continue;
            UUID assignedId = request.getAssignedTaskId();
            if (assignedId == null) continue;
            if (!hasRemainingSubtasks(assignedId)) {
                requestManager.onTaskCompleted(assignedId);
            }
        }
    }

    private DeliveryTask findEligibleTask() {
        for (DeliveryTask task : deliveryQueue) {
            if (task.getState() == DeliveryTask.DeliveryState.QUEUED
                && task.isReady(completedTaskIds)) {
                return task;
            }
        }
        return null;
    }

    // === Bee Management ===

    /**
     * Tue toutes les DeliveryBeeEntity liees a ce controller.
     */
    public void killAllDeliveryBees() {
        beeSpawner.killAllDeliveryBees();
    }

    /**
     * Vide toute la queue et les tâches actives.
     */
    public void clearAllTasks() {
        deliveryQueue.clear();
        activeTasks.clear();
        completedTaskIds.clear();
    }

    // === Task Completion ===

    /**
     * Marque une tâche active comme complétée par son UUID.
     * Appelé par la DeliveryBeeEntity quand elle termine sa livraison.
     */
    public void completeTask(UUID taskId) {
        finishTask(taskId, DeliveryTask.DeliveryState.COMPLETED);
    }

    /**
     * Marque une tâche active comme échouée par son UUID.
     * Appelé par la DeliveryBeeEntity en cas de timeout.
     */
    public void failTask(UUID taskId) {
        finishTask(taskId, DeliveryTask.DeliveryState.FAILED);
    }

    /**
     * Termine une tache active: met a jour l'etat, retire de la liste, marque pour sync.
     * Pour les taches COMPLETED, ne notifie le RequestManager que quand toutes les
     * subtasks du meme groupe (meme rootTaskId) sont terminees.
     */
    private void finishTask(UUID taskId, DeliveryTask.DeliveryState state) {
        Iterator<DeliveryTask> it = activeTasks.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                task.setState(state);
                UUID rootId = task.getRootTaskId();
                it.remove();

                if (state == DeliveryTask.DeliveryState.COMPLETED) {
                    completedTaskIds.add(taskId);
                    if (task.getInterfaceTaskId() == null) {
                        // Terminal task: utiliser RequestManager
                        if (!hasRemainingSubtasks(rootId)) {
                            parent.getRequestManager().onTaskCompleted(rootId);
                        }
                    }
                    // Interface tasks: notification faite par la bee dans notifyTaskCompleted()
                } else if (state == DeliveryTask.DeliveryState.FAILED) {
                    if (task.getInterfaceTaskId() == null) {
                        parent.getRequestManager().onTaskFailed(rootId);
                    } else {
                        // Interface task echouee: remettre en NEEDED pour re-tentative
                        notifyInterfaceTaskFailed(task);
                    }
                }

                parent.setChanged();
                parent.getItemAggregator().setNeedsSync(true);
                return;
            }
        }
    }

    /**
     * Notifie l'interface qu'une task a echoue: remet la task en NEEDED pour re-tentative.
     */
    private void notifyInterfaceTaskFailed(DeliveryTask task) {
        if (task.getInterfacePos() == null || task.getInterfaceTaskId() == null) return;
        if (parent.getLevel() == null) return;
        if (!parent.getLevel().isLoaded(task.getInterfacePos())) return;
        BlockEntity be = parent.getLevel().getBlockEntity(task.getInterfacePos());
        if (be instanceof NetworkInterfaceBlockEntity iface) {
            iface.unlockTask(task.getInterfaceTaskId());
        }
    }

    /**
     * Verifie s'il reste des subtasks actives ou en queue pour un meme rootTaskId.
     */
    private boolean hasRemainingSubtasks(UUID rootId) {
        for (DeliveryTask t : activeTasks) {
            if (t.getRootTaskId().equals(rootId)
                    && (t.getState() == DeliveryTask.DeliveryState.FLYING
                        || t.getState() == DeliveryTask.DeliveryState.QUEUED)) {
                return true;
            }
        }
        for (DeliveryTask t : deliveryQueue) {
            if (t.getRootTaskId().equals(rootId)
                    && t.getState() == DeliveryTask.DeliveryState.QUEUED) {
                return true;
            }
        }
        return false;
    }

    // === Task Cancellation ===

    /**
     * Annule une tâche par son ID.
     * Si la tâche est en queue, la retire. Si active, marque comme FAILED et tue la bee.
     * Annule aussi récursivement les tâches dépendantes.
     *
     * @return true si la tâche a été trouvée et annulée
     */
    public boolean cancelTask(UUID taskId) {
        // Verifier dans la queue
        Iterator<DeliveryTask> it = deliveryQueue.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                it.remove();
                handleCancelledTask(task);
                cancelDependentTasks(taskId);
                parent.setChanged();
                return true;
            }
        }

        // Verifier dans les taches actives
        for (DeliveryTask task : activeTasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setState(DeliveryTask.DeliveryState.FAILED);

                // Chercher une tache en queue pour rediriger la bee
                DeliveryTask nextTask = findEligibleTask();
                if (nextTask != null) {
                    deliveryQueue.remove(nextTask);
                    DeliveryTask activated = beeSpawner.redirectBee(task, nextTask);
                    if (activated != null) {
                        activeTasks.add(activated);
                    }
                } else {
                    beeSpawner.recallBeeForTask(task);
                }

                handleCancelledTask(task);
                cancelDependentTasks(taskId);
                parent.setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Annule recursivement toutes les taches qui dependent de la tache donnee,
     * ainsi que toutes les subtasks du meme groupe (meme rootTaskId).
     */
    private void cancelDependentTasks(UUID cancelledTaskId) {
        List<DeliveryTask> toCancel = new ArrayList<>();
        for (DeliveryTask task : deliveryQueue) {
            if (task.getDependencies().contains(cancelledTaskId)) {
                toCancel.add(task);
            }
        }
        // Annuler les subtasks du meme groupe
        for (DeliveryTask task : deliveryQueue) {
            if (cancelledTaskId.equals(task.getParentTaskId()) && !toCancel.contains(task)) {
                toCancel.add(task);
            }
        }
        for (DeliveryTask task : toCancel) {
            deliveryQueue.remove(task);
            handleCancelledTask(task);
            cancelDependentTasks(task.getTaskId());
        }
        // Annuler les subtasks actives (bees en vol)
        for (DeliveryTask task : new ArrayList<>(activeTasks)) {
            if (cancelledTaskId.equals(task.getParentTaskId())) {
                task.setState(DeliveryTask.DeliveryState.FAILED);
                beeSpawner.recallBeeForTask(task);
                handleCancelledTask(task);
            }
        }
    }

    /**
     * Gere les consequences de l'annulation d'une tache.
     * Pour les taches preloaded: remet les items dans le reseau.
     */
    private void handleCancelledTask(DeliveryTask task) {
        if (task.isPreloaded()) {
            parent.getItemAggregator().depositItem(
                task.getTemplate().copyWithCount(task.getCount()));
        }
    }

    // === Task Display Data ===

    /**
     * Retourne les donnees d'affichage pour l'onglet Tasks du terminal.
     */
    public List<TaskDisplayData> getTaskDisplayData() {
        return DeliveryTaskDisplayBuilder.buildDisplayData(
            activeTasks, deliveryQueue, honeyDepleted,
            parent.getMaxDeliveryBees(), completedTaskIds,
            parent.getRequestManager(), containerOps, parent.getLevel());
    }

    /**
     * Verifie si une tache est encore en attente (queue ou active).
     */
    public boolean isTaskPending(UUID taskId) {
        for (DeliveryTask t : activeTasks) {
            if (t.getTaskId().equals(taskId)) return true;
        }
        for (DeliveryTask t : deliveryQueue) {
            if (t.getTaskId().equals(taskId)) return true;
        }
        return false;
    }

    /**
     * Retourne le nombre de tâches actives (bees en vol).
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * Retourne le nombre de tâches en queue.
     */
    public int getQueuedTaskCount() {
        return deliveryQueue.size();
    }

    // === Tick ===

    /**
     * Tick du timer de livraison et reevaluation.
     */
    public void tickDelivery() {
        deliveryTimer++;
        if (deliveryTimer >= DELIVERY_PROCESS_INTERVAL) {
            deliveryTimer = 0;
            processDeliveryQueue();
        }

        reevaluateTimer++;
        if (reevaluateTimer >= REEVALUATE_INTERVAL) {
            reevaluateTimer = 0;
            reevaluateTasks();
        }
    }

    /**
     * Tick de la consommation de miel.
     */
    public void tickHoneyConsumption() {
        honeyConsumeTimer++;
        if (honeyConsumeTimer >= HONEY_CONSUME_INTERVAL) {
            honeyConsumeTimer = 0;
            consumeHoney();
        }
    }

    // === NBT ===

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("HoneyDepleted", honeyDepleted);

        ListTag queueTag = new ListTag();
        for (DeliveryTask task : deliveryQueue) {
            queueTag.add(task.save(registries));
        }
        // Sauvegarder les taches actives comme QUEUED SANS muter l'original
        // (getUpdateTag appelle aussi saveAdditional, muter ici corromprait l'etat)
        for (DeliveryTask task : activeTasks) {
            CompoundTag taskTag = task.save(registries);
            taskTag.putString("State", DeliveryTask.DeliveryState.QUEUED.name());
            queueTag.add(taskTag);
        }
        tag.put("DeliveryQueue", queueTag);

        ListTag completedTag = new ListTag();
        for (UUID id : completedTaskIds) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("Id", id);
            completedTag.add(idTag);
        }
        tag.put("CompletedTaskIds", completedTag);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        honeyDepleted = tag.getBoolean("HoneyDepleted");

        deliveryQueue.clear();
        activeTasks.clear();
        if (tag.contains("DeliveryQueue")) {
            ListTag queueTag = tag.getList("DeliveryQueue", Tag.TAG_COMPOUND);
            for (int i = 0; i < queueTag.size(); i++) {
                DeliveryTask task = DeliveryTask.load(queueTag.getCompound(i), registries);
                deliveryQueue.add(task);
            }
        }

        completedTaskIds.clear();
        if (tag.contains("CompletedTaskIds")) {
            ListTag completedTag = tag.getList("CompletedTaskIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < completedTag.size(); i++) {
                completedTaskIds.add(completedTag.getCompound(i).getUUID("Id"));
            }
        }
    }
}
