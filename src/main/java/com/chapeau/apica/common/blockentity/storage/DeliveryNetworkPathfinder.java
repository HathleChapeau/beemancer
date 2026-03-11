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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DeliveryNetworkPathfinder.class);
    private final StorageControllerBlockEntity parent;

    public DeliveryNetworkPathfinder(StorageControllerBlockEntity parent) {
        this.parent = parent;
    }

    /**
     * [DEBUG] Affiche la topologie complete du reseau dans les logs.
     * Utile pour diagnostiquer les problemes de pathfinding.
     */
    public void dumpNetworkTopology() {
        if (parent.getLevel() == null) {
            LOGGER.debug("[Topology] Level is null");
            return;
        }

        LOGGER.debug("[Topology] === Network Topology for Controller {} ===", parent.getBlockPos());
        LOGGER.debug("[Topology] Controller neighbors: {}", parent.getConnectedNodes());

        Set<BlockPos> visited = new HashSet<>();
        visited.add(parent.getBlockPos());
        Queue<BlockPos> toVisit = new LinkedList<>(parent.getConnectedNodes());

        while (!toVisit.isEmpty()) {
            BlockPos nodePos = toVisit.poll();
            if (!visited.add(nodePos)) continue;
            if (!parent.getLevel().hasChunkAt(nodePos)) continue;

            BlockEntity be = parent.getLevel().getBlockEntity(nodePos);
            if (be instanceof INetworkNode node) {
                Set<BlockPos> neighbors = node.getConnectedNodes();
                boolean hasController = neighbors.contains(parent.getBlockPos());
                LOGGER.debug("[Topology] Relay {} → neighbors: {} (directToController: {})",
                    nodePos, neighbors, hasController);

                Set<BlockPos> ownedBlocks = parent.getNetworkRegistry().getBlocksByOwner(nodePos);
                if (!ownedBlocks.isEmpty()) {
                    LOGGER.debug("[Topology]   Owns: {}", ownedBlocks);
                }

                for (BlockPos neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        toVisit.add(neighbor);
                    }
                }
            }
        }

        LOGGER.debug("[Topology] === End Topology ===");
    }

    /**
     * [NEW] Trouve le chemin de relais vers une position.
     * Approche hybride:
     * 1. D'abord essayer l'ownership du registre (si le bloc est enregistre a un relay)
     * 2. Si l'owner est le controller ou null, utiliser la proximite physique comme fallback
     *
     * @param targetPos position cible (coffre, interface, terminal, etc.)
     * @return liste des relays a traverser depuis le controller (vide si target pres du controller)
     */
    public List<BlockPos> findPathToPosition(BlockPos targetPos) {
        if (targetPos == null) return List.of();
        if (parent.getLevel() == null) return List.of();

        // Etape 1: Verifier si le bloc est dans le registre avec un owner relay (pas controller)
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(targetPos);

        if (ownerNode != null && !ownerNode.equals(parent.getBlockPos())) {
            // Le bloc est enregistre a un relay - utiliser ce relay comme cible
            LOGGER.info("[PathToPosition] Target {} owned by relay {}, using ownership path", targetPos, ownerNode);
            List<BlockPos> path = findRelayPathToNode(ownerNode);
            LOGGER.info("[PathToPosition] Path via owner: {}", path);
            return path;
        }

        // Etape 2: Verifier les coffres directs du controller
        if (parent.getChestManager().getRegisteredChests().contains(targetPos)) {
            LOGGER.info("[PathToPosition] Target {} is direct controller chest, returning empty path", targetPos);
            return List.of();
        }

        // Etape 3: Fallback - utiliser le noeud le plus proche physiquement
        BlockPos nearestNode = findNearestNetworkNode(Vec3.atCenterOf(targetPos));
        LOGGER.info("[PathToPosition] Target {} has no relay owner, using nearest node: {}", targetPos, nearestNode);

        if (nearestNode.equals(parent.getBlockPos())) {
            LOGGER.info("[PathToPosition] Nearest is controller, returning empty path");
            return List.of();
        }

        List<BlockPos> path = findRelayPathToNode(nearestNode);
        LOGGER.info("[PathToPosition] Path to nearest node: {}", path);
        return path;
    }

    /**
     * BFS pour trouver le chemin de relays depuis le controller vers un noeud specifique.
     */
    private List<BlockPos> findRelayPathToNode(BlockPos targetNode) {
        if (targetNode == null) return List.of();
        if (targetNode.equals(parent.getBlockPos())) return List.of();
        if (parent.getLevel() == null) return List.of();

        Set<BlockPos> visited = new HashSet<>();
        visited.add(parent.getBlockPos());

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();
        for (BlockPos neighbor : parent.getConnectedNodes()) {
            if (neighbor.equals(targetNode)) {
                return List.of(neighbor);
            }
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
                if (neighbor.equals(targetNode)) {
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

        // Pas trouve - le noeud n'est peut-etre pas dans le graphe connecte
        LOGGER.warn("[PathToPosition] Could not find path to node {}", targetNode);
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
     * [NEW] Trouve le chemin de relais depuis une position vers le controller.
     * Approche hybride: ownership d'abord, proximite physique en fallback.
     * Le chemin est INVERSE par rapport a findPathToPosition car on va dans l'autre sens.
     *
     * @param fromPos position de depart (coffre, interface, etc.)
     * @return liste des relays a traverser vers le controller (du plus eloigne au plus proche du controller)
     */
    public List<BlockPos> findPathFromPosition(BlockPos fromPos) {
        if (fromPos == null) return List.of();
        if (parent.getLevel() == null) return List.of();

        // Etape 1: Verifier si le bloc est dans le registre avec un owner relay
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(fromPos);

        if (ownerNode != null && !ownerNode.equals(parent.getBlockPos())) {
            // Le bloc est enregistre a un relay - utiliser ce relay comme point de depart
            LOGGER.info("[PathFromPosition] From {} owned by relay {}", fromPos, ownerNode);
            List<BlockPos> pathToNode = findRelayPathToNode(ownerNode);
            // INVERSER le chemin car on va de la position vers le controller
            List<BlockPos> reversedPath = new ArrayList<>(pathToNode);
            java.util.Collections.reverse(reversedPath);
            LOGGER.info("[PathFromPosition] Path (reversed): {}", reversedPath);
            return reversedPath;
        }

        // Etape 2: Verifier les coffres directs du controller
        if (parent.getChestManager().getRegisteredChests().contains(fromPos)) {
            LOGGER.info("[PathFromPosition] From {} is direct controller chest, returning empty path", fromPos);
            return List.of();
        }

        // Etape 3: Fallback - utiliser le noeud le plus proche physiquement
        BlockPos nearestNode = findNearestNetworkNode(Vec3.atCenterOf(fromPos));
        LOGGER.info("[PathFromPosition] From {} has no relay owner, using nearest node: {}", fromPos, nearestNode);

        if (nearestNode.equals(parent.getBlockPos())) {
            LOGGER.info("[PathFromPosition] Nearest is controller, returning empty path");
            return List.of();
        }

        List<BlockPos> pathToNode = findRelayPathToNode(nearestNode);
        // INVERSER le chemin car on va de la position vers le controller
        List<BlockPos> reversedPath = new ArrayList<>(pathToNode);
        java.util.Collections.reverse(reversedPath);
        LOGGER.info("[PathFromPosition] Path (reversed): {}", reversedPath);
        return reversedPath;
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

        LOGGER.info("[PathToController] Finding path from {} to controller {}", fromPos, parent.getBlockPos());

        // Etape 1: trouver le noeud proprietaire de fromPos
        StorageNetworkRegistry registry = parent.getNetworkRegistry();
        BlockPos ownerNode = registry.getOwner(fromPos);

        // Si pas de proprietaire, verifier si c'est un coffre direct du controller
        if (ownerNode == null) {
            LOGGER.debug("[PathToController] No owner found for {}, checking controller chests", fromPos);
            if (parent.getChestManager().getRegisteredChests().contains(fromPos)) {
                LOGGER.debug("[PathToController] Position is direct controller chest, returning empty path");
                return List.of();
            }
            LOGGER.debug("[PathToController] Position not in registry, returning empty path");
            return List.of();
        }

        LOGGER.debug("[PathToController] Owner node: {}", ownerNode);

        // Si le proprietaire est le controller lui-meme, pas de relays a traverser
        if (ownerNode.equals(parent.getBlockPos())) {
            LOGGER.debug("[PathToController] Owner is controller, returning empty path");
            return List.of();
        }

        // Etape 2: BFS depuis ownerNode vers le controller
        if (parent.getLevel() == null) return List.of();

        // BFS depuis ownerNode pour trouver le chemin vers le controller
        Set<BlockPos> visited = new HashSet<>();
        visited.add(ownerNode);

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();

        // Initialiser avec les voisins du ownerNode
        BlockEntity ownerBe = parent.getLevel().getBlockEntity(ownerNode);
        if (!(ownerBe instanceof INetworkNode ownerNodeEntity)) {
            LOGGER.debug("[PathToController] Owner {} is not INetworkNode, returning [ownerNode]", ownerNode);
            return List.of(ownerNode);
        }

        Set<BlockPos> ownerNeighbors = ownerNodeEntity.getConnectedNodes();
        LOGGER.debug("[PathToController] Owner {} has {} connected nodes: {}", ownerNode, ownerNeighbors.size(), ownerNeighbors);

        for (BlockPos neighbor : ownerNeighbors) {
            if (neighbor.equals(parent.getBlockPos())) {
                // ownerNode est directement connecte au controller
                LOGGER.debug("[PathToController] Owner {} is directly connected to controller, returning [ownerNode]", ownerNode);
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

            Set<BlockPos> nodeNeighbors = node.getConnectedNodes();
            LOGGER.debug("[PathToController] BFS visiting {}, path so far: {}, neighbors: {}", nodePos, path, nodeNeighbors);

            for (BlockPos neighbor : nodeNeighbors) {
                if (neighbor.equals(parent.getBlockPos())) {
                    // Trouve! path contient ownerNode → ... → nodePos
                    LOGGER.debug("[PathToController] Found controller! Final path: {}", path);
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
        LOGGER.debug("[PathToController] No path found (disconnected?), returning [ownerNode]: {}", ownerNode);
        return List.of(ownerNode);
    }

    /**
     * [NEW] Calcule le chemin entre deux positions.
     * Approche hybride: ownership d'abord, proximite physique en fallback.
     *
     * @param fromPos position de depart
     * @param toPos position d'arrivee
     * @return liste des relays a traverser (peut etre vide si meme noeud ou adjacent)
     */
    public List<BlockPos> findPathBetweenPositions(BlockPos fromPos, BlockPos toPos) {
        if (fromPos == null || toPos == null) return List.of();
        if (fromPos.equals(toPos)) return List.of();
        if (parent.getLevel() == null) return List.of();

        StorageNetworkRegistry registry = parent.getNetworkRegistry();

        // Determiner le noeud pour fromPos (ownership d'abord, sinon nearest)
        BlockPos fromNode = registry.getOwner(fromPos);
        if (fromNode == null || fromNode.equals(parent.getBlockPos())) {
            if (parent.getChestManager().getRegisteredChests().contains(fromPos)) {
                fromNode = parent.getBlockPos();
            } else {
                fromNode = findNearestNetworkNode(Vec3.atCenterOf(fromPos));
            }
        }

        // Determiner le noeud pour toPos (ownership d'abord, sinon nearest)
        BlockPos toNode = registry.getOwner(toPos);
        if (toNode == null || toNode.equals(parent.getBlockPos())) {
            if (parent.getChestManager().getRegisteredChests().contains(toPos)) {
                toNode = parent.getBlockPos();
            } else {
                toNode = findNearestNetworkNode(Vec3.atCenterOf(toPos));
            }
        }

        LOGGER.info("[PathBetweenPositions] From {} (node: {}) to {} (node: {})",
            fromPos, fromNode, toPos, toNode);

        // Meme noeud: pas de relays a traverser
        if (fromNode.equals(toNode)) {
            LOGGER.info("[PathBetweenPositions] Same nearest node, returning empty path");
            return List.of();
        }

        // Si l'un est le controller
        if (fromNode.equals(parent.getBlockPos())) {
            // Depuis controller vers toNode
            return findRelayPathToNode(toNode);
        }
        if (toNode.equals(parent.getBlockPos())) {
            // Vers controller depuis fromNode
            return findRelayPathToNode(fromNode);
        }

        // BFS depuis fromNode vers toNode
        Set<BlockPos> visited = new HashSet<>();
        visited.add(fromNode);

        Queue<Map.Entry<BlockPos, List<BlockPos>>> queue = new LinkedList<>();

        BlockEntity fromBe = parent.getLevel().getBlockEntity(fromNode);
        if (!(fromBe instanceof INetworkNode fromNodeEntity)) {
            return List.of(fromNode);
        }

        for (BlockPos neighbor : fromNodeEntity.getConnectedNodes()) {
            if (neighbor.equals(toNode)) {
                return List.of(fromNode, toNode);
            }
            if (!visited.contains(neighbor)) {
                List<BlockPos> path = new ArrayList<>();
                path.add(fromNode);
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
                if (neighbor.equals(toNode)) {
                    List<BlockPos> result = new ArrayList<>(path);
                    result.add(toNode);
                    LOGGER.info("[PathBetweenPositions] Found path: {}", result);
                    return result;
                }
                if (!visited.contains(neighbor)) {
                    List<BlockPos> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(Map.entry(neighbor, newPath));
                }
            }
        }

        LOGGER.info("[PathBetweenPositions] No path found, returning [fromNode]: {}", fromNode);
        return List.of(fromNode);
    }

}
