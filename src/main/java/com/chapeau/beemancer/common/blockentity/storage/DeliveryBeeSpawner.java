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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Gere le spawn, le recall, la redirection et le kill des DeliveryBeeEntity.
 */
public class DeliveryBeeSpawner {
    private final StorageControllerBlockEntity parent;
    private final DeliveryNetworkPathfinder pathfinder;
    private final DeliveryContainerOps containerOps;
    static final int MAX_RANGE = 30;

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
            if (sourcePos != null && level.isLoaded(sourcePos)) {
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
        if (destPos != null && !destPos.equals(parent.getBlockPos()) && level.isLoaded(destPos)) {
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
        }

        return true;
    }

    /**
     * Spawne une abeille de livraison pour une tache donnee.
     */
    boolean spawnDeliveryBee(DeliveryTask task) {
        if (!(parent.getLevel() instanceof ServerLevel serverLevel)) return false;

        if (!validateTaskTargets(task, serverLevel)) return false;

        if (!task.isPreloaded()) {
            int available = containerOps.countItemInChest(task.getTemplate(), task.getSourcePos());
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
            task.getTaskId(),
            task.getInterfaceTaskId(),
            task.getInterfacePos(),
            task.getCraftTaskId(),
            task.isCraftReturn()
        );

        List<BlockPos> pathToSource = List.of();
        List<BlockPos> pathToDest = List.of();

        if (!task.isPreloaded()) {
            pathToSource = pathfinder.findPathToChest(task.getSourcePos(), task.getRequesterPos());
        }
        if (task.getDestPos() != null && !task.getDestPos().equals(parent.getBlockPos())) {
            pathToDest = pathfinder.findPathToChest(task.getDestPos(), task.getRequesterPos());
        }

        List<BlockPos> transitWaypoints = pathfinder.computeTransitWaypoints(pathToSource, pathToDest);
        List<BlockPos> homeWaypoints = new ArrayList<>(pathToDest);
        Collections.reverse(homeWaypoints);

        bee.setAllWaypoints(pathToSource, transitWaypoints, homeWaypoints);

        serverLevel.addFreshEntity(bee);
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
    }

    /**
     * Rappelle la bee assignee a une tache active.
     */
    void recallBeeForTask(DeliveryTask task) {
        if (parent.getLevel() == null || parent.getLevel().isClientSide()) return;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new AABB(parent.getBlockPos()).inflate(MAX_RANGE),
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
     * Trouve la bee assignee a une tache par taskId.
     */
    @Nullable
    DeliveryBeeEntity findBeeForTask(UUID taskId) {
        if (parent.getLevel() == null) return null;

        List<DeliveryBeeEntity> bees = parent.getLevel().getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new AABB(parent.getBlockPos()).inflate(MAX_RANGE),
            bee -> parent.getBlockPos().equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            if (taskId.equals(bee.getTaskId())) {
                return bee;
            }
        }
        return null;
    }
}
