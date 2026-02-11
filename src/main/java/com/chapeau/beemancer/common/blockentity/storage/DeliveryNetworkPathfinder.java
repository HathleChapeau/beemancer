/**
 * ============================================================
 * [DeliveryNetworkPathfinder.java]
 * Description: BFS pathfinding dans le graphe reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | StorageControllerBlockEntity   | Parent controller    | Graphe reseau             |
 * | INetworkNode                   | Noeuds reseau        | getConnectedNodes()       |
 * | StorageNetworkRegistry         | Registry blocs       | getOwner()                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (delegation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Pathfinding BFS dans le graphe de noeuds du reseau de stockage.
 * Calcule les chemins de relais entre controller, relays et coffres.
 */
public class DeliveryNetworkPathfinder {
    private final StorageControllerBlockEntity parent;

    public DeliveryNetworkPathfinder(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    /**
     * Trouve le chemin de relais entre le controller et le noeud qui possede un coffre donne.
     * BFS a travers le graphe de noeuds connectes.
     */
    public List<BlockPos> findPathToChest(BlockPos chestPos, BlockPos requesterPos) {
        if (parent.getChestManager().getRegisteredChests().contains(chestPos)) {
            return List.of();
        }

        List<BlockPos> registryPath = findPathToOwnerNode(chestPos);
        if (!registryPath.isEmpty()) return registryPath;

        if (requesterPos != null) {
            List<BlockPos> requesterPath = findPathToOwnerNode(requesterPos);
            if (!requesterPath.isEmpty()) return requesterPath;
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
            if (!parent.getLevel().hasChunkAt(nodePos)) continue;

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
     * dans le NetworkRegistry. BFS dans le graphe de noeuds.
     */
    List<BlockPos> findPathToOwnerNode(BlockPos targetPos) {
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(targetPos);
        if (ownerNode == null) return List.of();
        if (ownerNode.equals(parent.getBlockPos())) return List.of();
        if (parent.getLevel() == null) return List.of();

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
            if (!parent.getLevel().hasChunkAt(nodePos)) continue;

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
     * Calcule les waypoints de transit source -> dest en passant par l'ancetre commun (LCA).
     */
    public List<BlockPos> computeTransitWaypoints(List<BlockPos> pathToSource, List<BlockPos> pathToDest) {
        int commonLen = 0;
        int minLen = Math.min(pathToSource.size(), pathToDest.size());
        while (commonLen < minLen && pathToSource.get(commonLen).equals(pathToDest.get(commonLen))) {
            commonLen++;
        }

        List<BlockPos> transit = new ArrayList<>();
        for (int i = pathToSource.size() - 1; i >= commonLen; i--) {
            transit.add(pathToSource.get(i));
        }

        if (commonLen > 0 && commonLen < pathToSource.size()) {
            transit.add(pathToSource.get(commonLen - 1));
        }

        for (int i = commonLen; i < pathToDest.size(); i++) {
            transit.add(pathToDest.get(i));
        }

        return transit;
    }

    /**
     * Trouve le noeud du reseau le plus proche d'une position donnee.
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
            if (parent.getLevel() == null || !parent.getLevel().hasChunkAt(nodePos)) continue;

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
     */
    public List<BlockPos> trimPathFromNode(List<BlockPos> fullPath, BlockPos fromNode) {
        for (int i = 0; i < fullPath.size(); i++) {
            if (fullPath.get(i).equals(fromNode)) {
                return new ArrayList<>(fullPath.subList(i + 1, fullPath.size()));
            }
        }
        return new ArrayList<>(fullPath);
    }
}
