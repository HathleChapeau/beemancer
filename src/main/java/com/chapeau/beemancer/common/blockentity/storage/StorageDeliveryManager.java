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
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.core.util.ContainerHelper;
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
import net.minecraft.world.phys.Vec3;
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

    private static final int DELIVERY_PROCESS_INTERVAL = 10;
    private static final int REEVALUATE_INTERVAL = 20;
    private static final int MAX_COMPLETED_IDS = 100;
    private static final int HONEY_CONSUME_INTERVAL = 20;
    private static final int MAX_RANGE = 30;

    private final Queue<DeliveryTask> deliveryQueue = new LinkedList<>();
    private final List<DeliveryTask> activeTasks = new ArrayList<>();
    private final Set<UUID> completedTaskIds = new LinkedHashSet<>();
    private int deliveryTimer = 0;
    private int reevaluateTimer = 0;
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
                if (ContainerHelper.countItem(container, template) >= minCount) {
                    return chestPos;
                }
            }
        }
        return null;
    }

    /**
     * Trouve le chemin de relais entre le controller et le noeud qui possede un coffre donne.
     * BFS a travers le graphe de noeuds connectes.
     * Supporte les coffres (via getRegisteredChests) et les blocs du NetworkRegistry
     * (interfaces, terminaux) via getOwner().
     *
     * @param chestPos position du coffre ou bloc cible
     * @param requesterPos position de l'interface/terminal emetteur (hint pour relay routing)
     * @return liste ordonnee des positions de relais (vide si le bloc est sur le controller)
     */
    private List<BlockPos> findPathToChest(BlockPos chestPos, @Nullable BlockPos requesterPos) {
        // Cas 1: coffre enregistre directement sur le controller
        if (parent.getChestManager().getRegisteredChests().contains(chestPos)) {
            return List.of();
        }

        // Cas 2: bloc enregistre dans le NetworkRegistry (interface, terminal)
        // Utiliser getOwner() pour trouver le noeud proprietaire et BFS vers ce noeud
        List<BlockPos> registryPath = findPathToOwnerNode(chestPos);
        if (!registryPath.isEmpty()) return registryPath;

        // Cas 2b: utiliser le requesterPos (position exacte de l'interface) pour trouver le relay
        // Cela resout le cas ou sourcePos est un container adjacent et non enregistre directement
        if (requesterPos != null) {
            List<BlockPos> requesterPath = findPathToOwnerNode(requesterPos);
            if (!requesterPath.isEmpty()) return requesterPath;
        }

        if (parent.getLevel() == null) return List.of();

        // Cas 3: BFS standard cherchant le noeud qui possede ce coffre dans ses chests
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
     * Trouve le chemin de relais vers le noeud proprietaire d'une position enregistree
     * dans le NetworkRegistry (interface, terminal). BFS dans le graphe de noeuds.
     *
     * @return liste ordonnee des relais, ou liste vide si le owner est le controller ou introuvable
     */
    private List<BlockPos> findPathToOwnerNode(BlockPos targetPos) {
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(targetPos);
        if (ownerNode == null) return List.of();

        // Si le owner est le controller lui-meme, pas de relais
        if (ownerNode.equals(parent.getBlockPos())) return List.of();

        if (parent.getLevel() == null) return List.of();

        // BFS pour trouver le chemin vers le ownerNode
        Set<BlockPos> visited = new HashSet<>();
        visited.add(parent.getBlockPos());

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();
        for (BlockPos neighbor : parent.getConnectedNodes()) {
            if (neighbor.equals(ownerNode)) return List.of(neighbor);
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

            for (BlockPos neighbor : node.getConnectedNodes()) {
                if (neighbor.equals(ownerNode)) {
                    List<BlockPos> result = new ArrayList<>(path);
                    result.add(neighbor);
                    return result;
                }
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
     * Calcule les waypoints de transit source → dest en passant par l'ancetre commun (LCA).
     * Evite le passage par le controller quand source et dest partagent des relais.
     *
     * Algorithme:
     * 1. Trouver le prefixe commun de pathToSource et pathToDest
     * 2. Transit = reversed(pathToSource suffixe unique) + pathToDest suffixe unique
     *
     * Exemple: pathToSource=[R1,R2,R3], pathToDest=[R1,R4,R5]
     *   Prefixe commun=[R1], transit=[R3,R2,R1,R4,R5]
     */
    private List<BlockPos> computeTransitWaypoints(List<BlockPos> pathToSource, List<BlockPos> pathToDest) {
        // Trouver la longueur du prefixe commun
        int commonLen = 0;
        int minLen = Math.min(pathToSource.size(), pathToDest.size());
        while (commonLen < minLen && pathToSource.get(commonLen).equals(pathToDest.get(commonLen))) {
            commonLen++;
        }

        // Suffixe unique de pathToSource (apres le prefixe commun), inverse
        List<BlockPos> transit = new ArrayList<>();
        for (int i = pathToSource.size() - 1; i >= commonLen; i--) {
            transit.add(pathToSource.get(i));
        }

        // Si les deux chemins divergent, ajouter le dernier noeud commun comme pivot
        if (commonLen > 0 && commonLen < pathToSource.size()) {
            transit.add(pathToSource.get(commonLen - 1));
        }

        // Suffixe unique de pathToDest (apres le prefixe commun)
        for (int i = commonLen; i < pathToDest.size(); i++) {
            transit.add(pathToDest.get(i));
        }

        return transit;
    }

    /**
     * Compte le nombre d'items d'un type donne dans un coffre specifique.
     */
    public int countItemInChest(ItemStack template, BlockPos chestPos) {
        if (parent.getLevel() == null || template.isEmpty()) return 0;
        if (!parent.getLevel().isLoaded(chestPos)) return 0;
        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);

        // Terminal: compter les deposit slots uniquement
        if (be instanceof StorageTerminalBlockEntity terminal) {
            return terminal.countInDeposit(template);
        }

        if (!(be instanceof Container container)) return 0;
        return ContainerHelper.countItem(container, template);
    }

    /**
     * Trouve un coffre du reseau qui a de la place pour l'item donne.
     * @return la position du coffre, ou null si aucun n'a de place
     */
    @Nullable
    public BlockPos findChestWithSpace(ItemStack template, int count) {
        if (parent.getLevel() == null || template.isEmpty()) return null;

        for (BlockPos chestPos : parent.getAllNetworkChests()) {
            if (!parent.getLevel().isLoaded(chestPos)) continue;
            BlockEntity be = parent.getLevel().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                if (ContainerHelper.availableSpace(container, template) >= count) {
                    return chestPos;
                }
            }
        }
        return null;
    }

    /**
     * Extrait un item d'un coffre ou terminal pour une livraison.
     * Si la source est un terminal, extrait uniquement des deposit slots.
     */
    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        if (parent.getLevel() == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;
        if (!parent.getLevel().isLoaded(chestPos)) return ItemStack.EMPTY;

        BlockEntity be = parent.getLevel().getBlockEntity(chestPos);

        // Terminal: extraire des deposit slots uniquement
        if (be instanceof StorageTerminalBlockEntity terminal) {
            ItemStack result = terminal.extractFromDeposit(template, count);
            parent.getItemAggregator().setNeedsSync(true);
            return result;
        }

        if (!(be instanceof Container container)) return ItemStack.EMPTY;

        ItemStack result = ContainerHelper.extractItem(container, template, count);
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

        ItemStack remaining = ContainerHelper.insertItem(container, stack);
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

            boolean spawned = spawnDeliveryBee(eligible);
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
        Iterator<DeliveryTask> queueIt = deliveryQueue.iterator();
        while (queueIt.hasNext()) {
            DeliveryTask task = queueIt.next();
            if (task.getState() != DeliveryTask.DeliveryState.QUEUED) continue;

            InterfaceRequest request = requestManager.findRequestByTaskId(task.getRootTaskId());
            if (request == null) continue;

            if (request.getCount() <= 0) {
                queueIt.remove();
                handleCancelledTask(task);
                parent.setChanged();
            }
        }

        // Reevaluer les taches actives: annuler si demande tombe a 0
        for (DeliveryTask task : new ArrayList<>(activeTasks)) {
            if (task.getState() != DeliveryTask.DeliveryState.FLYING) continue;

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

    /**
     * Verifie s'il existe deja une tache en queue associee a la meme request.
     */
    private boolean hasQueuedTaskForRequest(UUID requestId) {
        for (DeliveryTask task : deliveryQueue) {
            if (task.getState() == DeliveryTask.DeliveryState.QUEUED) {
                InterfaceRequest taskReq = parent.getRequestManager().findRequestByTaskId(task.getTaskId());
                if (taskReq != null && taskReq.getRequestId().equals(requestId)) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Valide que les blocs cibles d'une tache existent encore.
     * Si un bloc a ete casse, le retire du registre et refuse la tache.
     * Si une interface cible ne se considere plus liee, la retire du registre.
     */
    private boolean validateTaskTargets(DeliveryTask task, ServerLevel level) {
        StorageNetworkRegistry registry = parent.getNetworkRegistry();

        // Verifier la source (sauf si preloaded: pas de source a atteindre)
        if (!task.isPreloaded()) {
            BlockPos sourcePos = task.getSourcePos();
            if (sourcePos != null && level.isLoaded(sourcePos)) {
                BlockEntity sourceBe = level.getBlockEntity(sourcePos);
                if (sourceBe == null) {
                    registry.unregisterBlock(sourcePos);
                    parent.setChanged();
                    return false;
                }
                if (sourceBe instanceof NetworkInterfaceBlockEntity iface && iface.getControllerPos() == null) {
                    registry.unregisterBlock(sourcePos);
                    parent.setChanged();
                    return false;
                }
            }
        }

        // Verifier la destination
        BlockPos destPos = task.getDestPos();
        if (destPos != null && !destPos.equals(parent.getBlockPos()) && level.isLoaded(destPos)) {
            BlockEntity destBe = level.getBlockEntity(destPos);
            if (destBe == null) {
                registry.unregisterBlock(destPos);
                parent.setChanged();
                return false;
            }
            if (destBe instanceof NetworkInterfaceBlockEntity iface && iface.getControllerPos() == null) {
                registry.unregisterBlock(destPos);
                parent.setChanged();
                return false;
            }
        }

        return true;
    }

    private boolean spawnDeliveryBee(DeliveryTask task) {
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return false;

        // Valider que les blocs cibles existent encore
        if (!validateTaskTargets(task, serverLevel)) return false;

        // Pour les taches non-preloaded, re-valider la disponibilite des items a la source
        if (!task.isPreloaded()) {
            int available = countItemInChest(task.getTemplate(), task.getSourcePos());
            if (available <= 0) return false;
            if (available < task.getCount()) {
                task.setCount(available);
            }
        }

        BlockPos spawnPos = getSpawnPosBottom();
        BlockPos returnPos = getSpawnPosTop();
        if (spawnPos == null || returnPos == null) return false;

        DeliveryBeeEntity bee = BeemancerEntities.DELIVERY_BEE.get().create(serverLevel);
        if (bee == null) return false;

        bee.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);

        // Pre-charger les items si la tache est preloaded (terminal deposit)
        ItemStack carried = ItemStack.EMPTY;
        if (task.isPreloaded()) {
            carried = task.getTemplate().copyWithCount(task.getCount());
        }

        bee.initDeliveryTask(
            parent.getBlockPos(),
            task.getSourcePos(),
            returnPos,
            task.getDestPos(),
            task.getTemplate(),
            task.getCount(),
            carried,
            ControllerStats.getFlightSpeedMultiplier(parent.getEssenceSlots()),
            ControllerStats.getSearchSpeedMultiplier(parent.getEssenceSlots()),
            task.getTaskId()
        );

        // Calculer les waypoints: outbound (→source), transit (source→dest), home (dest→controller)
        List<BlockPos> pathToSource = List.of();
        List<BlockPos> pathToDest = List.of();

        if (!task.isPreloaded()) {
            pathToSource = findPathToChest(task.getSourcePos(), task.getRequesterPos());
        }
        if (task.getDestPos() != null && !task.getDestPos().equals(parent.getBlockPos())) {
            pathToDest = findPathToChest(task.getDestPos(), task.getRequesterPos());
        }

        List<BlockPos> transitWaypoints = computeTransitWaypoints(pathToSource, pathToDest);
        List<BlockPos> homeWaypoints = new ArrayList<>(pathToDest);
        Collections.reverse(homeWaypoints);

        bee.setAllWaypoints(pathToSource, transitWaypoints, homeWaypoints);

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
     * Tue toutes les DeliveryBeeEntity liees a ce controller.
     * Restitue les items transportes au reseau avant de discard.
     */
    public void killAllDeliveryBees() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            bee.returnCarriedItemsToNetwork();
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
                    if (!hasRemainingSubtasks(rootId)) {
                        parent.getRequestManager().onTaskCompleted(rootId);
                    }
                } else if (state == DeliveryTask.DeliveryState.FAILED) {
                    parent.getRequestManager().onTaskFailed(rootId);
                }

                parent.setChanged();
                parent.getItemAggregator().setNeedsSync(true);
                return;
            }
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
                    redirectBee(task, nextTask);
                } else {
                    recallBeeForTask(task);
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
                recallBeeForTask(task);
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

    /**
     * Rappelle la bee assignee a une tache active.
     * La bee volera directement vers le controller, restituera ses items, puis se discard.
     * Identifie la bee par taskId (fiable meme si plusieurs bees transportent le meme item).
     */
    private void recallBeeForTask(DeliveryTask task) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            if (task.getTaskId().equals(bee.getTaskId())) {
                bee.recall();
                break;
            }
        }
    }

    /**
     * Redirige une bee dont la tache est annulee vers une nouvelle tache.
     * La bee va au noeud reseau le plus proche, puis suit les waypoints vers la nouvelle source.
     * Si newTask est null, la bee rentre simplement au controller.
     *
     * @param cancelledTask la tache annulee
     * @param newTask la nouvelle tache a executer (nullable)
     */
    public void redirectBee(DeliveryTask cancelledTask, @Nullable DeliveryTask newTask) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        DeliveryBeeEntity targetBee = findBeeForTask(cancelledTask.getTaskId());
        if (targetBee == null) return;

        BlockPos nearestNode = findNearestNetworkNode(targetBee.position());

        // Trouver un coffre pour deposer les items si la bee en transporte
        BlockPos savingChest = null;
        if (!targetBee.getCarriedItems().isEmpty()) {
            savingChest = findChestWithSpace(
                targetBee.getCarriedItems(), targetBee.getCarriedItems().getCount());
        }

        if (newTask != null) {
            // Calculer les waypoints depuis le nearestNode vers la nouvelle tache
            List<BlockPos> pathToSource = List.of();
            List<BlockPos> pathToDest = List.of();

            if (!newTask.isPreloaded()) {
                pathToSource = findPathToChest(newTask.getSourcePos(), newTask.getRequesterPos());
            }
            if (newTask.getDestPos() != null && !newTask.getDestPos().equals(parent.getBlockPos())) {
                pathToDest = findPathToChest(newTask.getDestPos(), newTask.getRequesterPos());
            }

            // Outbound: du nearestNode vers la source (trimmer le prefixe commun)
            List<BlockPos> outboundFromNode = trimPathFromNode(pathToSource, nearestNode);
            List<BlockPos> transitWaypoints = computeTransitWaypoints(pathToSource, pathToDest);
            List<BlockPos> homeWaypoints = new ArrayList<>(pathToDest);
            Collections.reverse(homeWaypoints);

            targetBee.cancelAndRedirect(
                nearestNode, savingChest,
                newTask.getTaskId(), newTask.getSourcePos(), newTask.getDestPos(),
                newTask.getRequesterPos(), newTask.getTemplate(),
                outboundFromNode, transitWaypoints, homeWaypoints
            );

            newTask.setState(DeliveryTask.DeliveryState.FLYING);
            activeTasks.add(newTask);
        } else {
            targetBee.cancelAndRedirect(
                nearestNode, savingChest,
                null, null, null, null, ItemStack.EMPTY,
                List.of(), List.of(), List.of()
            );
        }

        parent.setChanged();
    }

    /**
     * Trouve la bee assignee a une tache par taskId.
     */
    @Nullable
    private DeliveryBeeEntity findBeeForTask(UUID taskId) {
        if (parent.getLevel() == null) return null;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            if (taskId.equals(bee.getTaskId())) {
                return bee;
            }
        }
        return null;
    }

    /**
     * Trouve le noeud du reseau (controller ou relay) le plus proche d'une position donnee.
     * Itere le controller + tous les noeuds connectes recursivement.
     */
    public BlockPos findNearestNetworkNode(Vec3 position) {
        BlockPos nearest = parent.getBlockPos();
        double nearestDistSq = position.distanceToSqr(
            nearest.getX() + 0.5, nearest.getY() + 0.5, nearest.getZ() + 0.5);

        Set<BlockPos> visited = new HashSet<>();
        visited.add(parent.getBlockPos());
        Queue<BlockPos> toVisit = new LinkedList<>(parent.getConnectedNodes());

        while (!toVisit.isEmpty()) {
            BlockPos nodePos = toVisit.poll();
            if (!visited.add(nodePos)) continue;
            if (parent.getLevel() == null || !parent.getLevel().isLoaded(nodePos)) continue;

            double distSq = position.distanceToSqr(
                nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = nodePos;
            }

            BlockEntity be = parent.getLevel().getBlockEntity(nodePos);
            if (be instanceof INetworkNode node) {
                for (BlockPos neighbor : node.getConnectedNodes()) {
                    if (!visited.contains(neighbor)) {
                        toVisit.add(neighbor);
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Trimme le prefixe d'un chemin de relais jusqu'au noeud donne.
     * Retourne le suffixe du chemin apres le noeud.
     */
    private List<BlockPos> trimPathFromNode(List<BlockPos> fullPath, BlockPos fromNode) {
        for (int i = 0; i < fullPath.size(); i++) {
            if (fullPath.get(i).equals(fromNode)) {
                return new ArrayList<>(fullPath.subList(i + 1, fullPath.size()));
            }
        }
        return new ArrayList<>(fullPath);
    }

    // === Task Display Data ===

    /**
     * Retourne les donnees d'affichage: requests comme parents, tasks comme enfants.
     * Chaque InterfaceRequest ASSIGNED genere une entree racine (avec le count original),
     * et toutes les DeliveryTasks associees deviennent des enfants de cette racine.
     * Les tasks sans request associee restent des racines independantes.
     */
    public List<TaskDisplayData> getTaskDisplayData() {
        List<TaskDisplayData> result = new ArrayList<>();
        RequestManager requestManager = parent.getRequestManager();

        // Map rootTaskId -> InterfaceRequest pour les requests qui ont des tasks assignees
        Map<UUID, InterfaceRequest> rootToRequest = new HashMap<>();
        for (InterfaceRequest request : requestManager.getAllRequests()) {
            if (request.getAssignedTaskId() != null
                    && request.getStatus() != InterfaceRequest.RequestStatus.CANCELLED) {
                rootToRequest.put(request.getAssignedTaskId(), request);
            }
        }

        // Generer les entrees racine depuis les requests
        for (InterfaceRequest request : rootToRequest.values()) {
            String aggregateState = computeRequestAggregateState(request.getAssignedTaskId());
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

        // Ajouter les tasks reelles comme enfants des requests
        for (DeliveryTask task : activeTasks) {
            InterfaceRequest request = rootToRequest.get(task.getRootTaskId());
            if (request != null) {
                result.add(taskToDisplayDataAsChild(task, request.getRequestId()));
            } else {
                result.add(taskToDisplayData(task));
            }
        }
        for (DeliveryTask task : deliveryQueue) {
            InterfaceRequest request = rootToRequest.get(task.getRootTaskId());
            if (request != null) {
                result.add(taskToDisplayDataAsChild(task, request.getRequestId()));
            } else {
                result.add(taskToDisplayData(task));
            }
        }

        return result;
    }

    /**
     * Calcule l'etat agrege d'une request en fonction de ses tasks.
     */
    private String computeRequestAggregateState(UUID rootTaskId) {
        boolean hasFlying = false;
        boolean hasQueued = false;
        for (DeliveryTask t : activeTasks) {
            if (t.getRootTaskId().equals(rootTaskId)
                    && t.getState() == DeliveryTask.DeliveryState.FLYING) {
                hasFlying = true;
            }
        }
        for (DeliveryTask t : deliveryQueue) {
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
    private TaskDisplayData taskToDisplayDataAsChild(DeliveryTask task, UUID displayParentId) {
        return new TaskDisplayData(
            task.getTaskId(),
            task.getTemplate(),
            task.getCount(),
            task.getState().name(),
            task.getDependencies(),
            task.getOrigin().name(),
            computeBlockedReason(task),
            task.getRequesterPos(),
            "",
            displayParentId
        );
    }

    /**
     * Cree un TaskDisplayData pour une task standalone (sans request associee).
     */
    private TaskDisplayData taskToDisplayData(DeliveryTask task) {
        BlockPos requesterPos = null;
        String requesterType = "";
        InterfaceRequest request = parent.getRequestManager().findRequestByTaskId(task.getRootTaskId());
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
            computeBlockedReason(task),
            requesterPos,
            requesterType,
            task.getParentTaskId()
        );
    }

    /**
     * Derive le type de bloc demandeur depuis les champs de la request.
     */
    private String deriveRequesterType(InterfaceRequest request) {
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
    private String computeBlockedReason(DeliveryTask task) {
        if (task.getState() != DeliveryTask.DeliveryState.QUEUED) return "";

        if (honeyDepleted) return "gui.beemancer.tasks.blocked.honey_depleted";

        if (activeTasks.size() >= parent.getMaxDeliveryBees()) return "gui.beemancer.tasks.blocked.no_bee_slot";

        if (!task.isReady(completedTaskIds)) return "gui.beemancer.tasks.blocked.dependency_pending";

        // Pour les taches non-preloaded, verifier la disponibilite a la source
        if (!task.isPreloaded()) {
            int available = countItemInChest(task.getTemplate(), task.getSourcePos());
            if (available <= 0) return "gui.beemancer.tasks.blocked.items_unavailable";
        }

        // Verifier que la source existe encore (sauf preloaded)
        if (!task.isPreloaded() && parent.getLevel() != null && task.getSourcePos() != null
                && parent.getLevel().isLoaded(task.getSourcePos())) {
            BlockEntity be = parent.getLevel().getBlockEntity(task.getSourcePos());
            if (be == null) return "gui.beemancer.tasks.blocked.target_missing";
        }

        return "";
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
