/**
 * ============================================================
 * [ThetaStarSolver.java]
 * Description: Algorithme Lazy Theta* pour pathfinding 3D des abeilles
 * ============================================================
 *
 * ALGORITHME: Lazy Theta* (Nash et al., AAAI 2010)
 * - Extension de Theta* qui reporte les checks de ligne de vue au moment du poll
 * - Reduit de 60-80% le nombre de LOS checks par rapport a Theta* standard
 * - Produit des chemins any-angle quasi-optimaux en espace 3D
 * - Cache de passabilite local par calcul (invalide a chaque nouveau calcul)
 *
 * Reference: Nash et al. "Lazy Theta*: Any-Angle Path Planning" (AAAI 2010)
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | PathCollisionHelper | Collision            | isPassable, hasLineOfSight     |
 * | Level               | Monde Minecraft      | Passe au collision helper      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeePathfinding.java: Delegation du calcul de chemin
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ThetaStarSolver {

    private static final int MAX_ITERATIONS = 1000;
    private static final double DIAGONAL_COST = 1.414;
    private static final double STRAIGHT_COST = 1.0;
    private static final double ALTITUDE_BONUS = 0.3;

    // 26 directions 3D pour l'expansion des noeuds
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            {1, 1, 1}, {1, 1, -1}, {-1, 1, 1}, {-1, 1, -1},
            {1, -1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };

    /**
     * Calcule un chemin Lazy Theta* de start vers end.
     * Le cache de passabilite est local a cet appel: cree au debut, GC'd a la fin.
     * Aucun risque de donnees stale entre ticks.
     *
     * @return chemin lisse (list de BlockPos), ou chemin vers le noeud le plus proche si echec
     */
    @Nullable
    public static List<BlockPos> solve(Level level, BlockPos start, BlockPos end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Map<BlockPos, Node> closedSet = new HashMap<>();
        // Cache local de passabilite — invalide a chaque nouveau solve()
        Map<BlockPos, Boolean> passableCache = new HashMap<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        startNode.parent = startNode; // Lazy Theta*: le start est son propre parent
        openSet.add(startNode);
        allNodes.put(start, startNode);

        Node closestToGoal = startNode;
        double closestDist = heuristic(start, end);
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node current = openSet.poll();
            if (current == null) break;

            // === LAZY THETA*: verification de LOS differee au moment du poll ===
            if (current.parent != null && current.parent != current && current.parent.parent != null) {
                if (!PathCollisionHelper.hasLineOfSight(level, current.parent.pos, current.pos)) {
                    // LOS echouee: chercher le meilleur parent parmi les voisins fermes
                    double bestG = Double.MAX_VALUE;
                    Node bestParent = current;
                    for (int[] dir : DIRECTIONS) {
                        BlockPos nPos = current.pos.offset(dir[0], dir[1], dir[2]);
                        Node closedNeighbor = closedSet.get(nPos);
                        if (closedNeighbor != null) {
                            double cost = isDiagonal(dir) ? DIAGONAL_COST : STRAIGHT_COST;
                            double g = closedNeighbor.gScore + cost;
                            if (g < bestG) {
                                bestG = g;
                                bestParent = closedNeighbor;
                            }
                        }
                    }
                    current.parent = bestParent;
                    current.gScore = bestG;
                    current.fScore = bestG + heuristic(current.pos, end);
                }
            }

            double distToGoal = heuristic(current.pos, end);
            if (distToGoal < closestDist) {
                closestDist = distToGoal;
                closestToGoal = current;
            }

            if (current.pos.closerThan(end, 1.5)) {
                return reconstructAndSmooth(level, current, end);
            }

            closedSet.put(current.pos, current);

            for (int[] dir : DIRECTIONS) {
                BlockPos neighborPos = current.pos.offset(dir[0], dir[1], dir[2]);
                if (closedSet.containsKey(neighborPos)) continue;
                if (!cachedIsPassable(level, neighborPos, passableCache)) continue;

                double penalty = computeWallPenaltyCached(level, neighborPos, passableCache);

                // === LAZY THETA*: attribution optimiste du parent (sans check LOS) ===
                Node effectiveParent = (current.parent != null) ? current.parent : current;
                double tentativeG = effectiveParent.gScore
                        + heuristic(effectiveParent.pos, neighborPos) + penalty;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null) {
                    neighbor = new Node(neighborPos, effectiveParent, tentativeG, heuristic(neighborPos, end));
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeG < neighbor.gScore) {
                    openSet.remove(neighbor);
                    neighbor.parent = effectiveParent;
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + heuristic(neighborPos, end);
                    openSet.add(neighbor);
                }
            }
        }

        return reconstructAndSmooth(level, closestToGoal, closestToGoal.pos);
    }

    // --- Cache de passabilite local ---

    private static boolean cachedIsPassable(Level level, BlockPos pos, Map<BlockPos, Boolean> cache) {
        return cache.computeIfAbsent(pos, p -> PathCollisionHelper.isPassable(level, p));
    }

    private static double computeWallPenaltyCached(Level level, BlockPos pos, Map<BlockPos, Boolean> cache) {
        int solidCount = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos rel = pos.relative(dir);
            boolean passable = cache.computeIfAbsent(rel, p -> PathCollisionHelper.isPassable(level, p));
            if (!passable) {
                solidCount++;
            }
        }
        return solidCount * 0.5;
    }

    // --- Reconstruction et lissage ---

    private static List<BlockPos> reconstructAndSmooth(Level level, Node endNode, BlockPos finalDest) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        while (current != null && current.parent != current) {
            path.add(current.pos);
            current = current.parent;
        }
        if (current != null) path.add(current.pos);
        Collections.reverse(path);

        if (!path.isEmpty() && !path.get(path.size() - 1).equals(finalDest)) {
            path.add(finalDest);
        }
        return smoothPath(level, path);
    }

    /**
     * Lissage par string pulling: supprime les waypoints intermediaires inutiles
     * en verifiant la ligne de vue entre points non-consecutifs.
     */
    private static List<BlockPos> smoothPath(Level level, List<BlockPos> path) {
        if (path.size() <= 2) return path;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        int current = 0;
        while (current < path.size() - 1) {
            int farthestVisible = current + 1;
            for (int i = path.size() - 1; i > current + 1; i--) {
                if (PathCollisionHelper.hasLineOfSight(level, path.get(current), path.get(i))) {
                    farthestVisible = i;
                    break;
                }
            }
            smoothed.add(path.get(farthestVisible));
            current = farthestVisible;
        }
        return smoothed;
    }

    // --- Utilitaires ---

    private static double heuristic(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isDiagonal(int[] dir) {
        int nonZero = 0;
        if (dir[0] != 0) nonZero++;
        if (dir[1] != 0) nonZero++;
        if (dir[2] != 0) nonZero++;
        return nonZero > 1;
    }

    /**
     * Noeud pour Lazy Theta*.
     */
    static class Node {
        final BlockPos pos;
        Node parent;
        double gScore;
        double fScore;

        Node(BlockPos pos, Node parent, double gScore, double h) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = gScore + h;
        }
    }
}
