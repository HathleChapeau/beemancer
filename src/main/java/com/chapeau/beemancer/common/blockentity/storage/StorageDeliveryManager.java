/**
 * ============================================================
 * [StorageDeliveryManager.java]
 * Description: Système de livraison par abeilles du réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Parent BlockEntity   | Back-reference                 |
 * | DeliveryTask                  | Tâche de livraison   | Queue et état                  |
 * | DeliveryBeeEntity             | Abeille de livraison | Spawn et gestion               |
 * | ControllerStats               | Stats essences       | Vitesse, consommation          |
 * | HoneyReservoirBlockEntity     | Drain miel           | consumeHoney                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (délégation)
 * - StorageMultiblockManager.java (clearAllTasks, killAllDeliveryBees)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gère la queue de livraison, le spawn des abeilles, la consommation de miel,
 * et les opérations d'extraction/dépôt liées aux livraisons.
 */
public class StorageDeliveryManager {
    private final StorageControllerBlockEntity parent;

    private static final int MAX_ACTIVE_BEES = 2;
    private static final int DELIVERY_PROCESS_INTERVAL = 10;
    private static final int MAX_COMPLETED_IDS = 100;
    private static final int HONEY_CONSUME_INTERVAL = 20;
    private static final int MAX_RANGE = 30;

    private final Queue<DeliveryTask> deliveryQueue = new LinkedList<>();
    private final List<DeliveryTask> activeTasks = new ArrayList<>();
    private final Set<UUID> completedTaskIds = new LinkedHashSet<>();
    private int deliveryTimer = 0;
    private int honeyConsumeTimer = 0;
    private boolean honeyDepleted = false;

    public StorageDeliveryManager(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    // === Task Management ===

    /**
     * Ajoute une tâche de livraison à la queue.
     */
    public void addDeliveryTask(DeliveryTask task) {
        deliveryQueue.add(task);
        parent.setChanged();
    }

    /**
     * Trouve un coffre contenant l'item demandé dans tout le reseau (controller + relays).
     * @return la position du coffre, ou null si introuvable
     */
    @Nullable
    public BlockPos findChestWithItem(ItemStack template, int minCount) {
        if (parent.getLevel() == null || template.isEmpty()) return null;

        for (BlockPos chestPos : parent.getAllNetworkChests()) {
            if (!parent.getLevel().isLoaded(chestPos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                int found = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, template)) {
                        found += existing.getCount();
                        if (found >= minCount) return chestPos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Trouve le chemin de relais entre le controller et le noeud qui possede un coffre donne.
     * BFS a travers le graphe de noeuds connectes.
     *
     * @return liste ordonnee des positions de relais (vide si le coffre est sur le controller)
     */
    private List<BlockPos> findPathToChest(BlockPos chestPos) {
        if (parent.getChestManager().getRegisteredChests().contains(chestPos)) {
            return List.of();
        }
        if (parent.getLevel() == null) return List.of();

        Set<BlockPos> visited = new HashSet<>();
        visited.add(parent.getBlockPos());

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();
        for (BlockPos neighbor : parent.getConnectedNodes()) {
            queue.add(Map.entry(neighbor, new ArrayList<>(List.of(neighbor))));
        }

        while (!queue.isEmpty()) {
            var entry = queue.poll();
            BlockPos nodePos = entry.getKey();
            List<BlockPos> path = entry.getValue();

            if (!visited.add(nodePos)) continue;
            if (!parent.getLevel().isLoaded(nodePos)) continue;

            BlockEntity be = parent.getLevel().getBlockEntity(nodePos);
            if (!(be instanceof INetworkNode node)) continue;

            if (node.getRegisteredChests().contains(chestPos)) {
                return path;
            }

            for (BlockPos neighbor : node.getConnectedNodes()) {
                if (!visited.contains(neighbor)) {
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(Map.entry(neighbor, newPath));
                }
            }
        }

        return List.of();
    }

    /**
     * Extrait un item d'un coffre spécifique pour une livraison.
     */
    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;
        if (!parent.getLevel().isLoaded(chestPos)) return ItemStack.EMPTY;

        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
        if (!(be instanceof Container container)) return ItemStack.EMPTY;

        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                int toTake = Math.min(needed, existing.getCount());
                existing.shrink(toTake);
                result.grow(toTake);
                needed -= toTake;
                container.setChanged();
                if (existing.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
            }
            if (needed <= 0) break;
        }

        parent.getItemAggregator().setNeedsSync(true);
        return result;
    }

    /**
     * Dépose un item dans un coffre spécifique pour une livraison.
     * Si chestPos est null, dépose dans n'importe quel coffre du réseau.
     *
     * @return le reste non déposé
     */
    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        if (chestPos != null) {
            return depositIntoChest(stack, chestPos);
        }
        return parent.getItemAggregator().depositItem(stack);
    }

    private ItemStack depositIntoChest(ItemStack stack, BlockPos chestPos) {
        if (parent.getLevel() == null || stack.isEmpty()) return stack;
        if (!parent.getLevel().isLoaded(chestPos)) return stack;

        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
        if (!(be instanceof Container container)) return stack;

        ItemStack remaining = stack.copy();

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toTransfer = Math.min(space, remaining.getCount());
                if (toTransfer > 0) {
                    existing.grow(toTransfer);
                    remaining.shrink(toTransfer);
                    container.setChanged();
                }
            }
        }

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                ItemStack toPlace = remaining.copy();
                toPlace.setCount(toTransfer);
                container.setItem(i, toPlace);
                remaining.shrink(toTransfer);
                container.setChanged();
            }
        }

        parent.getItemAggregator().setNeedsSync(true);
        return remaining;
    }

    // === Honey Consumption ===

    /**
     * Consomme du miel depuis les 2 HoneyReservoirs du multibloc.
     */
    private void consumeHoney() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        int consumptionPerSecond = ControllerStats.getHoneyConsumption(
            parent.getEssenceSlots(), parent.getChestManager().getRegisteredChestCount());
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

        while (activeTasks.size() < MAX_ACTIVE_BEES) {
            DeliveryTask eligible = findEligibleTask();
            if (eligible == null) break;

            deliveryQueue.remove(eligible);

            int beeCapacity = eligible.getTemplate().getMaxStackSize();
            if (eligible.getCount() > beeCapacity) {
                DeliveryTask rest = eligible.splitRemaining(beeCapacity);
                if (rest != null) {
                    deliveryQueue.add(rest);
                }
            }

            boolean spawned = spawnDeliveryBee(eligible);
            if (spawned) {
                eligible.setState(DeliveryTask.DeliveryState.FLYING);
                eligible.setAssignedBeeId(null);
                activeTasks.add(eligible);
            } else {
                eligible.setState(DeliveryTask.DeliveryState.FAILED);
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

    private boolean spawnDeliveryBee(DeliveryTask task) {
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return false;

        BlockPos spawnPos;
        BlockPos returnPos;

        if (task.getType() == DeliveryTask.DeliveryType.EXTRACT) {
            spawnPos = getSpawnPosBottom();
            returnPos = getSpawnPosTop();
        } else {
            spawnPos = getSpawnPosTop();
            returnPos = getSpawnPosBottom();
        }

        if (spawnPos == null || returnPos == null) return false;

        DeliveryBeeEntity bee = BeemancerEntities.DELIVERY_BEE.get().create(serverLevel);
        if (bee == null) return false;

        bee.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);

        ItemStack carried = ItemStack.EMPTY;
        if (task.getType() == DeliveryTask.DeliveryType.DEPOSIT) {
            carried = task.getTemplate().copyWithCount(task.getCount());
        }

        bee.initDeliveryTask(
            parent.getBlockPos(),
            task.getTargetChest(),
            returnPos,
            task.getTerminalPos(),
            task.getTemplate(),
            task.getCount(),
            task.getType(),
            carried,
            ControllerStats.getFlightSpeedMultiplier(parent.getEssenceSlots()),
            ControllerStats.getSearchSpeedMultiplier(parent.getEssenceSlots()),
            task.getTaskId()
        );

        List<BlockPos> relayPath = findPathToChest(task.getTargetChest());
        if (!relayPath.isEmpty()) {
            List<BlockPos> returnPath = new ArrayList<>(relayPath);
            Collections.reverse(returnPath);
            bee.setWaypoints(relayPath, returnPath);
        }

        serverLevel.addFreshEntity(bee);
        return true;
    }

    @Nullable
    private BlockPos getSpawnPosBottom() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, -1, 0),
            parent.getMultiblockManager().getRotation());
        return parent.getBlockPos().offset(offset);
    }

    @Nullable
    private BlockPos getSpawnPosTop() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, 1, 0),
            parent.getMultiblockManager().getRotation());
        return parent.getBlockPos().offset(offset);
    }

    // === Bee Cleanup ===

    /**
     * Tue toutes les DeliveryBeeEntity liées à ce controller.
     */
    public void killAllDeliveryBees() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            bee.discard();
        }
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
        Iterator<DeliveryTask> it = activeTasks.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                task.setState(DeliveryTask.DeliveryState.COMPLETED);
                completedTaskIds.add(taskId);
                it.remove();
                parent.setChanged();
                parent.getItemAggregator().setNeedsSync(true);
                return;
            }
        }
    }

    /**
     * Marque une tâche active comme échouée par son UUID.
     * Appelé par la DeliveryBeeEntity en cas de timeout.
     */
    public void failTask(UUID taskId) {
        Iterator<DeliveryTask> it = activeTasks.iterator();
        while (it.hasNext()) {
            DeliveryTask task = it.next();
            if (task.getTaskId().equals(taskId)) {
                task.setState(DeliveryTask.DeliveryState.FAILED);
                it.remove();
                parent.setChanged();
                parent.getItemAggregator().setNeedsSync(true);
                return;
            }
        }
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
        // Vérifier dans la queue
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

        // Vérifier dans les tâches actives
        for (DeliveryTask task : activeTasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setState(DeliveryTask.DeliveryState.FAILED);
                killBeeForTask(task);
                handleCancelledTask(task);
                cancelDependentTasks(taskId);
                parent.setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Annule récursivement toutes les tâches qui dépendent de la tâche donnée.
     */
    private void cancelDependentTasks(UUID cancelledTaskId) {
        List<DeliveryTask> toCancel = new ArrayList<>();
        for (DeliveryTask task : deliveryQueue) {
            if (task.getDependencies().contains(cancelledTaskId)) {
                toCancel.add(task);
            }
        }
        for (DeliveryTask task : toCancel) {
            deliveryQueue.remove(task);
            handleCancelledTask(task);
            cancelDependentTasks(task.getTaskId());
        }
    }

    /**
     * Gère les conséquences de l'annulation d'une tâche.
     * Pour DEPOSIT: remet les items dans le réseau.
     */
    private void handleCancelledTask(DeliveryTask task) {
        if (task.getType() == DeliveryTask.DeliveryType.DEPOSIT) {
            parent.getItemAggregator().depositItem(
                task.getTemplate().copyWithCount(task.getCount()));
        }
    }

    /**
     * Tue la bee assignée à une tâche active.
     */
    private void killBeeForTask(DeliveryTask task) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            if (bee.getTemplate() != null &&
                ItemStack.isSameItemSameComponents(bee.getTemplate(), task.getTemplate()) &&
                bee.getRequestCount() == task.getCount()) {
                bee.discard();
                break;
            }
        }
    }

    // === Task Display Data ===

    /**
     * Retourne les données d'affichage de toutes les tâches (actives + en queue).
     */
    public List<TaskDisplayData> getTaskDisplayData() {
        List<TaskDisplayData> result = new ArrayList<>();
        for (DeliveryTask task : activeTasks) {
            result.add(taskToDisplayData(task));
        }
        for (DeliveryTask task : deliveryQueue) {
            result.add(taskToDisplayData(task));
        }
        return result;
    }

    private TaskDisplayData taskToDisplayData(DeliveryTask task) {
        return new TaskDisplayData(
            task.getTaskId(),
            task.getTemplate(),
            task.getCount(),
            task.getState().name(),
            task.getType().name(),
            task.getDependencies()
        );
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
     * Tick du timer de livraison.
     */
    public void tickDelivery() {
        deliveryTimer++;
        if (deliveryTimer >= DELIVERY_PROCESS_INTERVAL) {
            deliveryTimer = 0;
            processDeliveryQueue();
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
            taskTag.remove("AssignedBeeId");
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
