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
package com.chapeau.apica.common.blockentity.storage;

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

    /**
     * Calcule le chemin de retour depuis une position vers le controller.
     * Le point de depart doit etre une position du reseau (chest, interface, terminal).
     * Retourne la liste des relays a traverser dans l'ordre, du plus proche au plus loin du controller.
     *
     * @param fromPos position de depart (doit etre enregistree dans le reseau)
     * @return liste des relays a traverser pour revenir au controller (peut etre vide si direct)
     */
    public List<BlockPos> findPathToController(BlockPos fromPos) {
        if (fromPos == null) return List.of();
        if (fromPos.equals(parent.getBlockPos())) return List.of();

        // Etape 1: trouver le noeud proprietaire de fromPos
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(fromPos);

        // Si pas de proprietaire, verifier si c'est un coffre direct du controller
        if (ownerNode == null) {
            if (parent.getChestManager().getRegisteredChests().contains(fromPos)) {
                return List.of();
            }
            return List.of();
        }

        // Si le proprietaire est le controller lui-meme, pas de relays a traverser
        if (ownerNode.equals(parent.getBlockPos())) {
            return List.of();
        }

        // Etape 2: BFS depuis ownerNode vers le controller
        // On construit le chemin dans l'ordre ownerNode → ... → controller
        // puis on inverse pour avoir controller → ... → ownerNode
        // et on re-inverse pour le chemin de retour
        if (parent.getLevel() == null) return List.of();

        // BFS depuis ownerNode pour trouver le chemin vers le controller
        Set<BlockPos> visited = new HashSet<>();
        visited.add(ownerNode);

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();

        // Initialiser avec les voisins du ownerNode
        BlockEntity ownerBe = parent.getLevel().getBlockEntity(ownerNode);
        if (!(ownerBe instanceof INetworkNode ownerNodeEntity)) {
            return List.of(ownerNode);
        }

        for (BlockPos neighbor : ownerNodeEntity.getConnectedNodes()) {
            if (neighbor.equals(parent.getBlockPos())) {
                // ownerNode est directement connecte au controller
                return List.of(ownerNode);
            }
            if (!visited.contains(neighbor)) {
                List<BlockPos> path = new ArrayList<>();
                path.add(ownerNode);
                path.add(neighbor);
                queue.add(Map.entry(neighbor, path));
            }
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
                if (neighbor.equals(parent.getBlockPos())) {
                    // Trouve! path contient ownerNode → ... → nodePos
                    // Le chemin de retour EST ce path (du owner vers controller)
                    return path;
                }
                if (!visited.contains(neighbor)) {
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(Map.entry(neighbor, newPath));
                }
            }
        }

        // Pas de chemin trouve (reseau deconnecte?)
        return List.of(ownerNode);
    }

    /**
     * Calcule le chemin entre deux positions du reseau.
     * Les deux positions doivent etre enregistrees dans le reseau.
     *
     * @param fromPos position de depart
     * @param toPos position d'arrivee
     * @return liste des relays a traverser (peut etre vide si meme proprietaire ou direct)
     */
    public List<BlockPos> findPathBetween(BlockPos fromPos, BlockPos toPos) {
        if (fromPos == null || toPos == null) return List.of();
        if (fromPos.equals(toPos)) return List.of();

        StorageNetworkRegistry registry = parent.getNetworkRegistry();

        // Trouver les noeuds proprietaires
        BlockPos fromOwner = registry.getOwner(fromPos);
        BlockPos toOwner = registry.getOwner(toPos);

        // Fallback pour les coffres du controller
        if (fromOwner == null && parent.getChestManager().getRegisteredChests().contains(fromPos)) {
            fromOwner = parent.getBlockPos();
        }
        if (toOwner == null && parent.getChestManager().getRegisteredChests().contains(toPos)) {
            toOwner = parent.getBlockPos();
        }

        // Si pas de proprietaire, pas de chemin
        if (fromOwner == null || toOwner == null) return List.of();

        // Meme proprietaire: pas de relays a traverser
        if (fromOwner.equals(toOwner)) return List.of();

        // Cas special: un des deux est le controller
        if (fromOwner.equals(parent.getBlockPos())) {
            // Depuis le controller vers toOwner
            return findPathToOwnerNode(toPos);
        }
        if (toOwner.equals(parent.getBlockPos())) {
            // Vers le controller
            return findPathToController(fromPos);
        }

        // BFS depuis fromOwner vers toOwner a travers le graphe de noeuds
        if (parent.getLevel() == null) return List.of();

        Set<BlockPos> visited = new HashSet<>();
        visited.add(fromOwner);

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();

        BlockEntity fromBe = parent.getLevel().getBlockEntity(fromOwner);
        if (!(fromBe instanceof INetworkNode fromNode)) {
            return List.of(fromOwner);
        }

        for (BlockPos neighbor : fromNode.getConnectedNodes()) {
            if (neighbor.equals(toOwner)) {
                return List.of(fromOwner, toOwner);
            }
            if (!visited.contains(neighbor)) {
                List<BlockPos> path = new ArrayList<>();
                path.add(fromOwner);
                path.add(neighbor);
                queue.add(Map.entry(neighbor, path));
            }
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
                if (neighbor.equals(toOwner)) {
                    List<BlockPos> result = new ArrayList<>(path);
                    result.add(toOwner);
                    return result;
                }
                if (!visited.contains(neighbor)) {
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(Map.entry(neighbor, newPath));
                }
            }
        }

        return List.of(fromOwner);
    }
}
