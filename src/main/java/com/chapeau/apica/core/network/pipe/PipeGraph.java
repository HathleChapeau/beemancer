/**
 * ============================================================
 * [PipeGraph.java]
 * Description: Structure de données graphe pour le réseau de pipes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BlockPos            | Position monde       | Noeuds du graphe               |
 * | AbstractPipeBlock   | Détection connexions | buildFromWorld                 |
 * | ItemPipeBlock       | Type de pipe         | Vérification isSamePipeType    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PipeNetwork.java (graphe interne du réseau)
 * - ItemPipeNetworkManager.java (détection composantes connexes)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import com.chapeau.apica.common.block.alchemy.AbstractPipeBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Graphe non-orienté de positions de pipes.
 * Chaque noeud est un BlockPos, chaque arête une connexion entre deux pipes adjacentes.
 * Fournit BFS, détection de composantes connexes, et reconstruction depuis le monde.
 */
public class PipeGraph {
    private final Map<BlockPos, Set<BlockPos>> adjacency = new HashMap<>();

    public void addNode(BlockPos pos) {
        adjacency.computeIfAbsent(pos, k -> new HashSet<>());
    }

    public void removeNode(BlockPos pos) {
        Set<BlockPos> neighbors = adjacency.remove(pos);
        if (neighbors != null) {
            for (BlockPos neighbor : neighbors) {
                Set<BlockPos> neighborEdges = adjacency.get(neighbor);
                if (neighborEdges != null) {
                    neighborEdges.remove(pos);
                }
            }
        }
    }

    public void addEdge(BlockPos a, BlockPos b) {
        adjacency.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        adjacency.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    public void removeEdge(BlockPos a, BlockPos b) {
        Set<BlockPos> aNeighbors = adjacency.get(a);
        if (aNeighbors != null) aNeighbors.remove(b);
        Set<BlockPos> bNeighbors = adjacency.get(b);
        if (bNeighbors != null) bNeighbors.remove(a);
    }

    public Set<BlockPos> getNeighbors(BlockPos pos) {
        return adjacency.getOrDefault(pos, Collections.emptySet());
    }

    public Set<BlockPos> getAllNodes() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public boolean contains(BlockPos pos) {
        return adjacency.containsKey(pos);
    }

    public int size() {
        return adjacency.size();
    }

    /**
     * BFS pour trouver le chemin le plus court entre deux positions.
     * Retourne la liste des BlockPos du chemin (incluant from et to), ou null si aucun chemin.
     */
    @Nullable
    public List<BlockPos> bfsPath(BlockPos from, BlockPos to) {
        if (from.equals(to)) return List.of(from);
        if (!adjacency.containsKey(from) || !adjacency.containsKey(to)) return null;

        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, BlockPos> parentMap = new HashMap<>();
        Queue<BlockPos> queue = new LinkedList<>();

        visited.add(from);
        queue.add(from);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.add(neighbor)) {
                    parentMap.put(neighbor, current);
                    if (neighbor.equals(to)) {
                        return reconstructPath(parentMap, from, to);
                    }
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> parentMap, BlockPos from, BlockPos to) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = to;
        while (current != null && !current.equals(from)) {
            path.add(current);
            current = parentMap.get(current);
        }
        path.add(from);
        Collections.reverse(path);
        return path;
    }

    /**
     * Flood fill depuis un point de départ. Retourne toutes les positions atteignables.
     */
    public Set<BlockPos> floodFill(BlockPos start) {
        Set<BlockPos> component = new HashSet<>();
        if (!adjacency.containsKey(start)) return component;

        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        component.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : getNeighbors(current)) {
                if (component.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return component;
    }

    /**
     * Détecte toutes les composantes connexes du graphe.
     * Utilisé après un removeNode pour vérifier si le réseau s'est splitté.
     */
    public List<Set<BlockPos>> findConnectedComponents() {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos node : adjacency.keySet()) {
            if (visited.add(node)) {
                Set<BlockPos> component = floodFill(node);
                visited.addAll(component);
                components.add(component);
            }
        }
        return components;
    }

    /**
     * Reconstruit le graphe depuis le monde en scannant les connexions réelles des pipes.
     * Utilisé au chargement depuis SavedData.
     */
    public static PipeGraph buildFromWorld(Set<BlockPos> pipePositions, ServerLevel level) {
        PipeGraph graph = new PipeGraph();
        for (BlockPos pos : pipePositions) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ItemPipeBlock)) continue;

            graph.addNode(pos);
            for (Direction dir : Direction.values()) {
                if (!AbstractPipeBlock.isConnected(state, dir)) continue;
                BlockPos neighbor = pos.relative(dir);
                if (pipePositions.contains(neighbor)) {
                    graph.addEdge(pos, neighbor);
                }
            }
        }
        return graph;
    }
}
