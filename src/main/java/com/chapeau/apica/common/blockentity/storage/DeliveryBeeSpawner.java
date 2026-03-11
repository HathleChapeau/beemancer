/**
 * ============================================================
 * [DeliveryBeeSpawner.java]
 * Description: Spawn, recall, redirect et kill des abeilles de livraison
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Spawn pos, essences       |
 * | DeliveryNetworkPathfinder      | Pathfinding reseau   | Waypoints bee             |
 * | DeliveryContainerOps           | Operations coffres   | countItem, findSpace      |
 * | DeliveryBeeEntity              | Abeille livraison    | Spawn, recall, redirect   |
 * | ControllerStats                | Stats essences       | Vitesse vol/recherche     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.ControllerStats;
import com.chapeau.apica.common.block.storage.DeliveryTask;
import com.chapeau.apica.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.registry.ApicaEntities;
import com.chapeau.apica.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le spawn, le recall, la redirection et le kill des DeliveryBeeEntity.
 */
public class DeliveryBeeSpawner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryBeeSpawner.class);
    private final StorageControllerBlockEntity parent;
    private final DeliveryNetworkPathfinder pathfinder;
    private final DeliveryContainerOps containerOps;
    static final int MAX_RANGE = 30;

    // [BD] Registre de bees par taskId: evite les scans AABB couteux
    private final Map<UUID, WeakReference<DeliveryBeeEntity>> beeRegistry = new HashMap<>();

    /**
     * Encapsule les trois chemins de waypoints d'une tâche de livraison.
     * Utilisé par spawnDeliveryBee et redirectBee pour une logique unifiée.
     */
    public record WaypointPaths(
        List<BlockPos> outbound,   // Controller → Source
        List<BlockPos> transit,    // Source → Dest (via LCA)
        List<BlockPos> home        // Dest → Controller (reverse de pathToDest)
    ) {}

    public DeliveryBeeSpawner(StorageControllerBlockEntity parent,
                              DeliveryNetworkPathfinder pathfinder,
                              DeliveryContainerOps containerOps) {
        this.parent = parent;
        this.pathfinder = pathfinder;
        this.containerOps = containerOps;
    }

    // =========================================================================
    // WAYPOINT CALCULATION - UNIFIED FOR IMPORT/EXPORT
    // =========================================================================

    /**
     * Calcule les waypoints pour une tâche de livraison.
     * Unifie la logique IMPORT et EXPORT en utilisant toujours des positions
     * enregistrées dans le réseau pour le pathfinding.
     *
     * @param sourcePos position source réelle (coffre réseau ou coffre adjacent)
     * @param destPos position destination réelle
     * @param interfacePos position de l'interface (pour tâches interface, sinon null)
     * @return les trois chemins: outbound, transit, home
     */
    public WaypointPaths calculateTaskWaypoints(@Nullable BlockPos sourcePos,
                                                 @Nullable BlockPos destPos,
                                                 @Nullable BlockPos interfacePos) {
        // Résoudre les positions réseau pour le pathfinding
        // Pour EXPORT: sourcePos = adjacent (hors réseau), on utilise interfacePos à la place
        // Pour IMPORT: sourcePos = coffre réseau, pas de changement
        BlockPos pathfindingSource = resolveNetworkPosition(sourcePos, interfacePos);
        BlockPos pathfindingDest = resolveNetworkPosition(destPos, null);

        LOGGER.info("[Waypoints] Calculating: source={} (pathfinding={}), dest={} (pathfinding={})",
            sourcePos, pathfindingSource, destPos, pathfindingDest);

        // Chemin controller → source
        List<BlockPos> pathToSource = List.of();
        if (pathfindingSource != null) {
            pathToSource = pathfinder.findPathToPosition(pathfindingSource);
            LOGGER.info("[Waypoints] pathToSource: {}", pathToSource);
        }

        // Chemin controller → destination
        List<BlockPos> pathToDest = List.of();
        if (pathfindingDest != null && !pathfindingDest.equals(parent.getBlockPos())) {
            pathToDest = pathfinder.findPathToPosition(pathfindingDest);
            LOGGER.info("[Waypoints] pathToDest: {}", pathToDest);
        }

        // Transit: chemin source → destination via l'ancêtre commun (LCA)
        List<BlockPos> transit = pathfinder.computeTransitWaypoints(pathToSource, pathToDest);
        LOGGER.info("[Waypoints] transit (via LCA): {}", transit);

        // Home: reverse de pathToDest (garantit symétrie parfaite)
        List<BlockPos> home = new ArrayList<>(pathToDest);
        java.util.Collections.reverse(home);
        LOGGER.info("[Waypoints] home (reverse pathToDest): {}", home);

        return new WaypointPaths(pathToSource, transit, home);
    }

    /**
     * Résout la position réseau pour le pathfinding.
     * Si la position est hors réseau (ex: coffre adjacent d'une interface EXPORT),
     * utilise l'interface comme proxy car elle EST enregistrée dans le réseau.
     *
     * @param actualPos position réelle (peut être hors réseau)
     * @param interfacePos position de l'interface (proxy si actualPos hors réseau)
     * @return position à utiliser pour le pathfinding (toujours dans le réseau)
     */
    private BlockPos resolveNetworkPosition(@Nullable BlockPos actualPos, @Nullable BlockPos interfacePos) {
        if (actualPos == null) return null;

        // Vérifier si la position est enregistrée dans le réseau
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        boolean isInNetwork = registry.getOwner(actualPos) != null
                           || parent.getChestManager().getRegisteredChests().contains(actualPos);

        if (isInNetwork) {
            // Position dans le réseau → utiliser directement
            return actualPos;
        }

        // Position hors réseau (ex: coffre adjacent EXPORT)
        // → Utiliser l'interface comme proxy si disponible
        if (interfacePos != null) {
            LOGGER.debug("[Waypoints] {} is outside network, using interface {} as proxy",
                actualPos, interfacePos);
            return interfacePos;
        }

        // Pas de proxy disponible, utiliser la position telle quelle
        // (le pathfinder utilisera le fallback nearest node)
        return actualPos;
    }

    /**
     * Valide que les blocs cibles d'une tache existent encore.
     * Si un bloc a ete casse, le retire du registre et refuse la tache.
     */
    boolean validateTaskTargets(DeliveryTask task, ServerLevel level) {
        StorageNetworkRegistry registry = parent.getNetworkRegistry();

        if (!task.isPreloaded()) {
            BlockPos sourcePos = task.getSourcePos();
            if (sourcePos != null && level.hasChunkAt(sourcePos)) {
                BlockEntity sourceBe = level.getBlockEntity(sourcePos);
                if (sourceBe == null) {
                    registry.unregisterBlock(sourcePos);
                    parent.setChanged();
                    return false;
                }
                if (sourceBe instanceof NetworkInterfaceBlockEntity iface
                        && iface.getControllerPos() == null) {
                    registry.unregisterBlock(sourcePos);
                    parent.setChanged();
                    return false;
                }
            }
        }

        BlockPos destPos = task.getDestPos();
        if (destPos != null && !destPos.equals(parent.getBlockPos()) && level.hasChunkAt(destPos)) {
            BlockEntity destBe = level.getBlockEntity(destPos);
            if (destBe == null) {
                registry.unregisterBlock(destPos);
                parent.setChanged();
                return false;
            }
            if (destBe instanceof NetworkInterfaceBlockEntity iface
                    && iface.getControllerPos() == null) {
                registry.unregisterBlock(destPos);
                parent.setChanged();
                return false;
            }
            // [BH] Verifier que la destination peut recevoir des items (IItemHandler)
            if (!(destBe instanceof NetworkInterfaceBlockEntity)
                    && !(destBe instanceof StorageTerminalBlockEntity)) {
                IItemHandler handler = StorageHelper.getItemHandler(level, destPos, null);
                if (handler == null) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Spawne une abeille de livraison pour une tache donnee.
     * [BE] Extraction atomique: pour les taches non-preloaded et non-interface,
     * les items sont extraits du coffre AVANT le spawn pour eviter les TOCTOU.
     */
    boolean spawnDeliveryBee(DeliveryTask task) {
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return false;

        // [DEBUG] Log task details for pathfinding diagnosis - INFO level to ensure visibility
        LOGGER.info("[Spawner] === Spawning Task {} ===", task.getTaskId());
        LOGGER.info("[Spawner] Task type: {}, sourcePos: {}, destPos: {}",
            task.getInterfaceTaskId() != null ? "INTERFACE" : (task.isPreloaded() ? "PRELOADED" : "DIRECT"),
            task.getSourcePos(), task.getDestPos());

        // [DEBUG] Get ownership info for source and dest
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos sourceOwner = task.getSourcePos() != null ? registry.getOwner(task.getSourcePos()) : null;
        BlockPos destOwner = task.getDestPos() != null ? registry.getOwner(task.getDestPos()) : null;
        LOGGER.info("[Spawner] Source {} owned by: {}", task.getSourcePos(), sourceOwner);
        LOGGER.info("[Spawner] Dest {} owned by: {}", task.getDestPos(), destOwner);

        if (!validateTaskTargets(task, serverLevel)) return false;

        // [BE] Determiner les items a transporter
        ItemStack carried = ItemStack.EMPTY;
        if (task.isPreloaded()) {
            carried = task.getTemplate().copyWithCount(task.getCount());
            LOGGER.debug("[Spawner] Task {} preloaded: {}x{}", task.getTaskId(),
                task.getCount(), task.getTemplate().getItem());
        } else if (task.getInterfaceTaskId() == null) {
            // [BE] Extraction atomique: extraire AVANT de spawner
            LOGGER.debug("[Spawner] Task {} G3 pre-extract: {}x{} from {}",
                task.getTaskId(), task.getCount(), task.getTemplate().getItem(), task.getSourcePos());
            ItemStack extracted = containerOps.extractItemForDelivery(
                task.getTemplate(), task.getCount(), task.getSourcePos());
            if (extracted.isEmpty()) {
                LOGGER.debug("[Spawner] Task {} G3 extraction FAILED (empty)", task.getTaskId());
                return false;
            }
            task.setCount(extracted.getCount());
            carried = extracted;
            LOGGER.debug("[Spawner] Task {} G3 extracted: {}x{}", task.getTaskId(),
                extracted.getCount(), extracted.getItem());
        } else {
            // [FIX] Interface import task: vérifier que le coffre source a toujours l'item
            // Avant ce fix, la bee était spawnée même si le coffre était vide,
            // ce qui causait des cycles d'échec/retry inutiles.
            int available = containerOps.countItemInChest(task.getTemplate(), task.getSourcePos());
            if (available <= 0) {
                LOGGER.debug("[Spawner] Task {} interface import: source {} has no items, skipping spawn",
                    task.getTaskId(), task.getSourcePos());
                return false;
            }
            LOGGER.debug("[Spawner] Task {} interface import: bee will extract at source {} (available: {})",
                task.getTaskId(), task.getSourcePos(), available);
        }

        BlockPos spawnPos = getSpawnPosBottom();
        BlockPos returnPos = getSpawnPosTop();
        if (spawnPos == null || returnPos == null) {
            // Restituer les items si extraction deja faite
            if (!carried.isEmpty() && !task.isPreloaded()) {
                containerOps.depositItemForDelivery(carried, task.getSourcePos());
            }
            return false;
        }

        DeliveryBeeEntity bee = ApicaEntities.DELIVERY_BEE.get().create(serverLevel);
        if (bee == null) {
            if (!carried.isEmpty() && !task.isPreloaded()) {
                containerOps.depositItemForDelivery(carried, task.getSourcePos());
            }
            return false;
        }

        bee.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5);

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
            task.getTaskId(),
            task.getInterfaceTaskId(),
            task.getInterfacePos()
        );
        bee.setPreloaded(task.isPreloaded());

        // Calcul unifié des waypoints (IMPORT et EXPORT utilisent la même logique)
        WaypointPaths paths = calculateTaskWaypoints(
            task.getSourcePos(),
            task.getDestPos(),
            task.getInterfacePos()
        );
        LOGGER.info("[Spawner] Task {} waypoints: outbound={}, transit={}, home={}",
            task.getTaskId(), paths.outbound(), paths.transit(), paths.home());

        bee.setAllWaypoints(paths.outbound(), paths.transit(), paths.home());

        if (!serverLevel.addFreshEntity(bee)) {
            // [BE] Spawn echoue: restituer les items extraits
            if (!carried.isEmpty() && !task.isPreloaded()) {
                containerOps.depositItemForDelivery(carried, task.getSourcePos());
            }
            return false;
        }

        // [BD] Enregistrer la bee dans le registre
        beeRegistry.put(task.getTaskId(), new WeakReference<>(bee));
        return true;
    }

    @Nullable
    BlockPos getSpawnPosBottom() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, -1, 0),
            parent.getMultiblockManager().getRotation());
        return parent.getBlockPos().offset(offset);
    }

    @Nullable
    BlockPos getSpawnPosTop() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, 1, 0),
            parent.getMultiblockManager().getRotation());
        return parent.getBlockPos().offset(offset);
    }

    /**
     * Tue toutes les DeliveryBeeEntity liees a ce controller.
     * [FIX] Ne PAS appeler returnCarriedItemsToNetwork() pendant le world unload.
     * Deposer des items dans les coffres appelle setChanged() sur les chests,
     * ce qui re-dirtie des chunks deja sauvegardes → boucle infinie dans saveAllChunks.
     * Les items transportes sont perdus, mais le monde ne hang plus.
     * En gameplay normal (controller casse hors shutdown), les items sont restitues.
     */
    public void killAllDeliveryBees() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        boolean isShutdown = com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown();

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        if (isShutdown && !bees.isEmpty()) {
            LOGGER.info("[Apica] Shutdown: discarding {} delivery bees (skipping item return to avoid setChanged)",
                bees.size());
        }
        for (DeliveryBeeEntity bee : bees) {
            if (!isShutdown) {
                bee.returnCarriedItemsToNetwork();
            }
            bee.discard();
        }
        // [BD] Vider le registre
        beeRegistry.clear();
    }

    /**
     * [BD] Rappelle la bee assignee a une tache active via le registre.
     */
    void recallBeeForTask(DeliveryTask task) {
        DeliveryBeeEntity bee = findBeeForTask(task.getTaskId());
        if (bee != null) {
            bee.recall();
        }
    }

    /**
     * Redirige une bee vers une nouvelle tache. La bee va au noeud reseau le plus proche,
     * depose ses items si necessaire, puis suit les waypoints vers la nouvelle source.
     *
     * @return la newTask activee (a ajouter dans activeTasks par le caller), ou null si pas de newTask
     */
    @Nullable
    public DeliveryTask redirectBee(DeliveryTask cancelledTask, @Nullable DeliveryTask newTask) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return null;

        DeliveryBeeEntity targetBee = findBeeForTask(cancelledTask.getTaskId());
        if (targetBee == null) return null;

        BlockPos nearestNode = pathfinder.findNearestNetworkNode(targetBee.position());

        BlockPos savingChest = null;
        if (!targetBee.getCarriedItems().isEmpty()) {
            // Utiliser findSlotForItem pour respecter les priorites de depot
            savingChest = parent.findSlotForItem(targetBee.getCarriedItems());
        }

        if (newTask != null) {
            // Calcul unifié des waypoints (même logique que spawnDeliveryBee)
            WaypointPaths paths = calculateTaskWaypoints(
                newTask.isPreloaded() ? null : newTask.getSourcePos(),
                newTask.getDestPos(),
                newTask.getInterfacePos()
            );

            // Pour redirect: trimmer le chemin outbound depuis la position actuelle
            List<BlockPos> outboundFromNode = pathfinder.trimPathFromNode(paths.outbound(), nearestNode);

            targetBee.cancelAndRedirect(
                nearestNode, savingChest,
                newTask.getTaskId(), newTask.getSourcePos(), newTask.getDestPos(),
                newTask.getRequesterPos(), newTask.getTemplate(),
                outboundFromNode, paths.transit(), paths.home()
            );

            newTask.setState(DeliveryTask.DeliveryState.FLYING);
            parent.setChanged();
            return newTask;
        } else {
            targetBee.cancelAndRedirect(
                nearestNode, savingChest,
                null, null, null, null, ItemStack.EMPTY,
                List.of(), List.of(), List.of()
            );
            parent.setChanged();
            return null;
        }
    }

    /**
     * [BD] Trouve la bee assignee a une tache via le registre O(1).
     * Fallback: cleanup si la WeakReference est morte.
     */
    @Nullable
    DeliveryBeeEntity findBeeForTask(UUID taskId) {
        WeakReference<DeliveryBeeEntity> ref = beeRegistry.get(taskId);
        if (ref != null) {
            DeliveryBeeEntity bee = ref.get();
            if (bee != null && bee.isAlive()) {
                return bee;
            }
            beeRegistry.remove(taskId);
        }
        return null;
    }
}
