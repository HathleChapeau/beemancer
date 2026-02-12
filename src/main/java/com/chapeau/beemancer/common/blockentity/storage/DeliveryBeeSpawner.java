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
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere le spawn, le recall, la redirection et le kill des DeliveryBeeEntity.
 */
public class DeliveryBeeSpawner {
    private final StorageControllerBlockEntity parent;
    private final DeliveryNetworkPathfinder pathfinder;
    private final DeliveryContainerOps containerOps;
    static final int MAX_RANGE = 30;

    // [BD] Registre de bees par taskId: evite les scans AABB couteux
    private final Map<UUID, WeakReference<DeliveryBeeEntity>> beeRegistry = new HashMap<>();

    public DeliveryBeeSpawner(StorageControllerBlockEntity parent,
                              DeliveryNetworkPathfinder pathfinder,
                              DeliveryContainerOps containerOps) {
        this.parent = parent;
        this.pathfinder = pathfinder;
        this.containerOps = containerOps;
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

        if (!validateTaskTargets(task, serverLevel)) return false;

        // [BE] Determiner les items a transporter
        ItemStack carried = ItemStack.EMPTY;
        if (task.isPreloaded()) {
            carried = task.getTemplate().copyWithCount(task.getCount());
        } else if (task.getInterfaceTaskId() == null) {
            // [BE] Extraction atomique: extraire AVANT de spawner
            ItemStack extracted = containerOps.extractItemForDelivery(
                task.getTemplate(), task.getCount(), task.getSourcePos());
            if (extracted.isEmpty()) return false;
            task.setCount(extracted.getCount());
            carried = extracted;
        }
        // Else: interface import task — la bee vole a la source pour extraire

        BlockPos spawnPos = getSpawnPosBottom();
        BlockPos returnPos = getSpawnPosTop();
        if (spawnPos == null || returnPos == null) {
            // Restituer les items si extraction deja faite
            if (!carried.isEmpty() && !task.isPreloaded()) {
                containerOps.depositItemForDelivery(carried, task.getSourcePos());
            }
            return false;
        }

        DeliveryBeeEntity bee = BeemancerEntities.DELIVERY_BEE.get().create(serverLevel);
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

        // Calculer les waypoints via relays
        List<BlockPos> pathToSource = List.of();
        List<BlockPos> pathToDest = List.of();

        // [BE] Si items deja sur la bee, pas besoin de voler a la source
        if (carried.isEmpty()) {
            pathToSource = pathfinder.findPathToChest(task.getSourcePos(), task.getRequesterPos());
        }
        if (task.getDestPos() != null && !task.getDestPos().equals(parent.getBlockPos())) {
            pathToDest = pathfinder.findPathToChest(task.getDestPos(), task.getRequesterPos());
        }

        List<BlockPos> transitWaypoints = pathfinder.computeTransitWaypoints(pathToSource, pathToDest);
        List<BlockPos> homeWaypoints = new ArrayList<>(pathToDest);
        Collections.reverse(homeWaypoints);

        bee.setAllWaypoints(pathToSource, transitWaypoints, homeWaypoints);

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
     * Restitue les items transportes au reseau avant de discard.
     */
    public void killAllDeliveryBees() {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            bee.returnCarriedItemsToNetwork();
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
            savingChest = containerOps.findChestWithSpace(
                targetBee.getCarriedItems(), targetBee.getCarriedItems().getCount());
        }

        if (newTask != null) {
            List<BlockPos> pathToSource = List.of();
            List<BlockPos> pathToDest = List.of();

            if (!newTask.isPreloaded()) {
                pathToSource = pathfinder.findPathToChest(newTask.getSourcePos(), newTask.getRequesterPos());
            }
            if (newTask.getDestPos() != null && !newTask.getDestPos().equals(parent.getBlockPos())) {
                pathToDest = pathfinder.findPathToChest(newTask.getDestPos(), newTask.getRequesterPos());
            }

            List<BlockPos> outboundFromNode = pathfinder.trimPathFromNode(pathToSource, nearestNode);
            List<BlockPos> transitWaypoints = pathfinder.computeTransitWaypoints(pathToSource, pathToDest);
            List<BlockPos> homeWaypoints = new ArrayList<>(pathToDest);
            Collections.reverse(homeWaypoints);

            targetBee.cancelAndRedirect(
                nearestNode, savingChest,
                newTask.getTaskId(), newTask.getSourcePos(), newTask.getDestPos(),
                newTask.getRequesterPos(), newTask.getTemplate(),
                outboundFromNode, transitWaypoints, homeWaypoints
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
