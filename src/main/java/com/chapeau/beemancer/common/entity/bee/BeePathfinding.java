/**
 * ============================================================
 * [BeePathfinding.java]
 * Description: Pathfinding A* simplifié pour abeilles volantes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Level               | Monde Minecraft      | Vérification collisions        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ForagingBehaviorGoal.java: Navigation vers fleurs/ruche
 * - MagicBeeEntity.java: Navigation manuelle
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Pathfinding A* simplifié pour les abeilles volantes.
 * Optimisé pour les déplacements aériens en 3D.
 */
public class BeePathfinding {

    private static final int MAX_ITERATIONS = 200;
    private static final int MAX_PATH_LENGTH = 64;

    // Directions 3D: 6 cardinales + 20 diagonales = 26 directions
    private static final int[][] DIRECTIONS = {
            // Cardinales
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            // Diagonales horizontales
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            // Diagonales verticales
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            // Diagonales 3D
            {1, 1, 1}, {1, 1, -1}, {-1, 1, 1}, {-1, 1, -1},
            {1, -1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };

    private final Level level;

    // Cache du chemin actuel
    @Nullable
    private List<BlockPos> currentPath = null;
    private int currentPathIndex = 0;
    @Nullable
    private BlockPos lastDestination = null;

    public BeePathfinding(Level level) {
        this.level = level;
    }

    /**
     * Calcule un chemin de start vers end.
     * @return Liste de positions formant le chemin, ou null si impossible
     */
    @Nullable
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Si même destination et chemin valide, réutiliser
        if (end.equals(lastDestination) && currentPath != null && currentPathIndex < currentPath.size()) {
            return currentPath;
        }

        // Reset
        lastDestination = end;
        currentPath = computePath(start, end);
        currentPathIndex = 0;

        return currentPath;
    }

    /**
     * Retourne le prochain waypoint du chemin.
     */
    @Nullable
    public BlockPos getNextWaypoint(Vec3 currentPos, double reachDistance) {
        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }

        // Avancer dans le chemin si on a atteint le waypoint actuel
        while (currentPathIndex < currentPath.size()) {
            BlockPos waypoint = currentPath.get(currentPathIndex);
            double dist = currentPos.distanceTo(Vec3.atCenterOf(waypoint));

            if (dist <= reachDistance) {
                currentPathIndex++;
            } else {
                return waypoint;
            }
        }

        // Chemin terminé
        return null;
    }

    /**
     * Vérifie si le chemin est terminé.
     */
    public boolean isPathComplete() {
        return currentPath == null || currentPathIndex >= currentPath.size();
    }

    /**
     * Invalide le chemin actuel.
     */
    public void clearPath() {
        currentPath = null;
        currentPathIndex = 0;
        lastDestination = null;
    }

    /**
     * Algorithme A* simplifié.
     */
    @Nullable
    private List<BlockPos> computePath(BlockPos start, BlockPos end) {
        // Si ligne directe possible, l'utiliser
        if (hasLineOfSight(start, end)) {
            return List.of(end);
        }

        // A* classique
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            Node current = openSet.poll();
            if (current == null) break;

            // Arrivé à destination (ou très proche)
            if (current.pos.closerThan(end, 1.5)) {
                return reconstructPath(current, end);
            }

            closedSet.add(current.pos);

            // Explorer les voisins
            for (int[] dir : DIRECTIONS) {
                BlockPos neighborPos = current.pos.offset(dir[0], dir[1], dir[2]);

                if (closedSet.contains(neighborPos)) continue;
                if (!isPassable(neighborPos)) continue;

                // Coût diagonal légèrement supérieur
                double moveCost = (dir[0] != 0 && dir[1] != 0) || (dir[0] != 0 && dir[2] != 0)
                        || (dir[1] != 0 && dir[2] != 0) ? 1.414 : 1.0;

                double tentativeG = current.gScore + moveCost;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null) {
                    neighbor = new Node(neighborPos, current, tentativeG, heuristic(neighborPos, end));
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeG < neighbor.gScore) {
                    // Meilleur chemin trouvé
                    openSet.remove(neighbor);
                    neighbor.parent = current;
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + heuristic(neighborPos, end);
                    openSet.add(neighbor);
                }
            }
        }

        // Pas de chemin trouvé, retourner chemin direct
        return List.of(end);
    }

    /**
     * Reconstruit le chemin depuis le noeud final.
     */
    private List<BlockPos> reconstructPath(Node endNode, BlockPos finalDest) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(path);

        // Ajouter la destination finale si différente
        if (!path.isEmpty() && !path.get(path.size() - 1).equals(finalDest)) {
            path.add(finalDest);
        }

        // Simplifier le chemin (supprimer les points alignés)
        return simplifyPath(path);
    }

    /**
     * Simplifie le chemin en supprimant les waypoints inutiles.
     */
    private List<BlockPos> simplifyPath(List<BlockPos> path) {
        if (path.size() <= 2) return path;

        List<BlockPos> simplified = new ArrayList<>();
        simplified.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos prev = simplified.get(simplified.size() - 1);
            BlockPos next = path.get(i + 1);

            // Si pas de ligne de vue directe, garder ce point
            if (!hasLineOfSight(prev, next)) {
                simplified.add(path.get(i));
            }
        }

        simplified.add(path.get(path.size() - 1));
        return simplified;
    }

    /**
     * Vérifie si une position est traversable par une abeille.
     */
    private boolean isPassable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isSolid();
    }

    /**
     * Vérifie s'il y a une ligne de vue directe entre deux positions.
     */
    private boolean hasLineOfSight(BlockPos start, BlockPos end) {
        // Bresenham 3D simplifié
        int dx = Math.abs(end.getX() - start.getX());
        int dy = Math.abs(end.getY() - start.getY());
        int dz = Math.abs(end.getZ() - start.getZ());

        int sx = start.getX() < end.getX() ? 1 : -1;
        int sy = start.getY() < end.getY() ? 1 : -1;
        int sz = start.getZ() < end.getZ() ? 1 : -1;

        int x = start.getX();
        int y = start.getY();
        int z = start.getZ();

        // Nombre de steps = max des distances
        int steps = Math.max(dx, Math.max(dy, dz));
        if (steps == 0) return true;

        double xInc = (double) (end.getX() - start.getX()) / steps;
        double yInc = (double) (end.getY() - start.getY()) / steps;
        double zInc = (double) (end.getZ() - start.getZ()) / steps;

        double cx = start.getX();
        double cy = start.getY();
        double cz = start.getZ();

        for (int i = 0; i <= steps; i++) {
            BlockPos check = new BlockPos((int) Math.round(cx), (int) Math.round(cy), (int) Math.round(cz));
            if (!isPassable(check) && !check.equals(start) && !check.equals(end)) {
                return false;
            }
            cx += xInc;
            cy += yInc;
            cz += zInc;
        }

        return true;
    }

    /**
     * Heuristique A*: distance euclidienne.
     */
    private double heuristic(BlockPos from, BlockPos to) {
        return Math.sqrt(from.distSqr(to));
    }

    /**
     * Noeud pour l'algorithme A*.
     */
    private static class Node {
        final BlockPos pos;
        Node parent;
        double gScore; // Coût depuis le départ
        double fScore; // gScore + heuristique

        Node(BlockPos pos, Node parent, double gScore, double heuristic) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = gScore + heuristic;
        }
    }
}
