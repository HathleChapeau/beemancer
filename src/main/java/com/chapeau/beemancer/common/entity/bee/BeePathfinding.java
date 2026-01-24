/**
 * ============================================================
 * [BeePathfinding.java]
 * Description: Pathfinding Theta* optimisé pour abeilles volantes
 * ============================================================
 *
 * ALGORITHME: Theta* (Any-Angle Pathfinding)
 * - Extension de A* qui permet des chemins en ligne droite
 * - Utilise Bresenham 3D pour la vérification de ligne de vue
 * - Optimisé pour les entités volantes en 3D
 *
 * Référence: Nash et al. 2007 "Theta*: Any-Angle Path Planning on Grids"
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
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Pathfinding Theta* pour abeilles volantes.
 *
 * Theta* est un algorithme "any-angle" qui produit des chemins plus courts
 * et plus naturels que A* standard en permettant des mouvements en ligne droite
 * plutôt que de suivre la grille.
 *
 * Caractéristiques:
 * - Vol direct si ligne de vue dégagée (O(1) dans le meilleur cas)
 * - Theta* avec Bresenham 3D pour les obstacles
 * - Lissage automatique du chemin
 * - Visualisation debug par particules
 */
public class BeePathfinding {

    // Configuration
    private static final int MAX_ITERATIONS = 500;
    private static final int MAX_PATH_LENGTH = 100;
    private static final double DIAGONAL_COST = 1.414;
    private static final double STRAIGHT_COST = 1.0;

    // Directions 3D pour l'expansion des nœuds (26 directions)
    private static final int[][] DIRECTIONS = {
            // Cardinales (6)
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            // Diagonales face (4)
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            // Diagonales verticales (8)
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            // Diagonales 3D (8)
            {1, 1, 1}, {1, 1, -1}, {-1, 1, 1}, {-1, 1, -1},
            {1, -1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };

    private final Level level;

    // Cache du chemin
    @Nullable private List<BlockPos> currentPath = null;
    private int currentPathIndex = 0;
    @Nullable private BlockPos cachedStart = null;
    @Nullable private BlockPos cachedEnd = null;

    // Debug
    private boolean debugMode = true;
    private long lastParticleTime = 0;
    private static final long PARTICLE_INTERVAL = 500; // ms

    public BeePathfinding(Level level) {
        this.level = level;
    }

    /**
     * Calcule un chemin optimisé de start vers end.
     * Utilise le vol direct si possible, sinon Theta*.
     */
    @Nullable
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Cache hit - réutiliser le chemin existant
        if (end.equals(cachedEnd) && start.closerThan(cachedStart, 3) && currentPath != null) {
            return currentPath;
        }

        cachedStart = start;
        cachedEnd = end;

        // Étape 1: Essayer le vol direct (ligne de vue)
        if (hasLineOfSight(start, end)) {
            currentPath = List.of(end);
            currentPathIndex = 0;
            return currentPath;
        }

        // Étape 2: Theta* pour contourner les obstacles
        currentPath = computeThetaStar(start, end);
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

        // Avancer dans le chemin
        while (currentPathIndex < currentPath.size()) {
            BlockPos waypoint = currentPath.get(currentPathIndex);
            double dist = currentPos.distanceTo(Vec3.atCenterOf(waypoint));

            if (dist <= reachDistance) {
                currentPathIndex++;
            } else {
                // Optimisation: vérifier si on peut sauter des waypoints
                // en ayant une ligne de vue directe vers un waypoint plus loin
                for (int i = currentPath.size() - 1; i > currentPathIndex; i--) {
                    BlockPos futureWaypoint = currentPath.get(i);
                    if (hasLineOfSight(BlockPos.containing(currentPos), futureWaypoint)) {
                        currentPathIndex = i;
                        return futureWaypoint;
                    }
                }
                return waypoint;
            }
        }

        return null;
    }

    /**
     * Affiche le chemin avec des particules (debug).
     */
    public void showPathParticles(Vec3 entityPos) {
        if (!debugMode || currentPath == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastParticleTime < PARTICLE_INTERVAL) {
            return;
        }
        lastParticleTime = now;

        // Dessiner des particules le long du chemin
        Vec3 lastPos = entityPos;
        for (int i = currentPathIndex; i < currentPath.size(); i++) {
            Vec3 waypointPos = Vec3.atCenterOf(currentPath.get(i));

            // Interpoler des particules entre les points
            double distance = lastPos.distanceTo(waypointPos);
            int particleCount = (int) Math.ceil(distance / 1.5);

            for (int j = 0; j <= particleCount; j++) {
                double t = (double) j / particleCount;
                double x = lastPos.x + (waypointPos.x - lastPos.x) * t;
                double y = lastPos.y + (waypointPos.y - lastPos.y) * t;
                double z = lastPos.z + (waypointPos.z - lastPos.z) * t;

                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        1, 0, 0, 0, 0
                );
            }

            lastPos = waypointPos;
        }
    }

    /**
     * Algorithme Theta* - Any-Angle Pathfinding.
     * Produit des chemins plus courts que A* en utilisant des lignes de vue.
     */
    @Nullable
    private List<BlockPos> computeThetaStar(BlockPos start, BlockPos end) {
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

            // Destination atteinte
            if (current.pos.closerThan(end, 1.5)) {
                return reconstructAndSmoothPath(current, end);
            }

            closedSet.add(current.pos);

            // Explorer les voisins
            for (int[] dir : DIRECTIONS) {
                BlockPos neighborPos = current.pos.offset(dir[0], dir[1], dir[2]);

                if (closedSet.contains(neighborPos)) continue;
                if (!isPassable(neighborPos)) continue;

                // === THETA* KEY DIFFERENCE ===
                // Au lieu de toujours utiliser current comme parent,
                // on vérifie si le parent de current a une ligne de vue vers neighbor
                Node parent = current;
                double tentativeG;

                if (current.parent != null && hasLineOfSight(current.parent.pos, neighborPos)) {
                    // Ligne de vue directe depuis le grand-parent
                    parent = current.parent;
                    tentativeG = parent.gScore + euclideanDistance(parent.pos, neighborPos);
                } else {
                    // Pas de ligne de vue, utiliser le chemin standard
                    double moveCost = isDiagonal(dir) ? DIAGONAL_COST : STRAIGHT_COST;
                    tentativeG = current.gScore + moveCost;
                }

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null) {
                    neighbor = new Node(neighborPos, parent, tentativeG, heuristic(neighborPos, end));
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeG < neighbor.gScore) {
                    openSet.remove(neighbor);
                    neighbor.parent = parent;
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + heuristic(neighborPos, end);
                    openSet.add(neighbor);
                }
            }
        }

        // Échec: retourner chemin direct (l'abeille essaiera de contourner)
        return List.of(end);
    }

    /**
     * Reconstruit et lisse le chemin.
     */
    private List<BlockPos> reconstructAndSmoothPath(Node endNode, BlockPos finalDest) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(path);

        // Ajouter destination si nécessaire
        if (!path.isEmpty() && !path.get(path.size() - 1).equals(finalDest)) {
            path.add(finalDest);
        }

        // Lissage final avec Bresenham
        return smoothPath(path);
    }

    /**
     * Lisse le chemin en supprimant les waypoints intermédiaires inutiles.
     * Utilise la technique du "string pulling" avec vérification de ligne de vue.
     */
    private List<BlockPos> smoothPath(List<BlockPos> path) {
        if (path.size() <= 2) return path;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int current = 0;
        while (current < path.size() - 1) {
            // Chercher le point le plus éloigné visible depuis current
            int farthestVisible = current + 1;

            for (int i = path.size() - 1; i > current + 1; i--) {
                if (hasLineOfSight(path.get(current), path.get(i))) {
                    farthestVisible = i;
                    break;
                }
            }

            smoothed.add(path.get(farthestVisible));
            current = farthestVisible;
        }

        return smoothed;
    }

    /**
     * Vérifie la ligne de vue avec l'algorithme de Bresenham 3D.
     * Optimisé pour éviter les allocations et utiliser uniquement des entiers.
     */
    public boolean hasLineOfSight(BlockPos start, BlockPos end) {
        int x0 = start.getX(), y0 = start.getY(), z0 = start.getZ();
        int x1 = end.getX(), y1 = end.getY(), z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        // Déterminer l'axe dominant
        int maxDelta = Math.max(dx, Math.max(dy, dz));
        if (maxDelta == 0) return true;

        // Bresenham 3D
        int x = x0, y = y0, z = z0;
        int errXY = dx - dy;
        int errXZ = dx - dz;
        int errYZ = dy - dz;

        for (int i = 0; i <= maxDelta; i++) {
            // Vérifier si le bloc est passable (ignorer start et end)
            if (i > 0 && i < maxDelta) {
                BlockPos check = new BlockPos(x, y, z);
                if (!isPassable(check)) {
                    return false;
                }
            }

            // Avancer selon Bresenham
            int errXY2 = errXY * 2;
            int errXZ2 = errXZ * 2;
            int errYZ2 = errYZ * 2;

            if (errXY2 > -dy) {
                errXY -= dy;
                x += sx;
            }
            if (errXY2 < dx) {
                errXY += dx;
                y += sy;
            }
            if (errXZ2 > -dz) {
                errXZ -= dz;
                // x already updated
            }
            if (errXZ2 < dx) {
                errXZ += dx;
                z += sz;
            }
        }

        return true;
    }

    /**
     * Vérifie si une position est traversable.
     */
    private boolean isPassable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Pour les abeilles, on considère les blocs non-solides comme passables
        return !state.isSolid() && !state.liquid();
    }

    /**
     * Heuristique: distance euclidienne (admissible pour Theta*).
     */
    private double heuristic(BlockPos from, BlockPos to) {
        return euclideanDistance(from, to);
    }

    /**
     * Distance euclidienne entre deux positions.
     */
    private double euclideanDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Vérifie si une direction est diagonale.
     */
    private boolean isDiagonal(int[] dir) {
        int nonZero = 0;
        if (dir[0] != 0) nonZero++;
        if (dir[1] != 0) nonZero++;
        if (dir[2] != 0) nonZero++;
        return nonZero > 1;
    }

    // === Utilitaires publics ===

    public void clearPath() {
        currentPath = null;
        currentPathIndex = 0;
        cachedStart = null;
        cachedEnd = null;
    }

    public boolean isPathComplete() {
        return currentPath == null || currentPathIndex >= currentPath.size();
    }

    public boolean hasPath() {
        return currentPath != null && !currentPath.isEmpty();
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    @Nullable
    public List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    /**
     * Nœud pour Theta*.
     */
    private static class Node {
        final BlockPos pos;
        Node parent;
        double gScore;
        double fScore;

        Node(BlockPos pos, Node parent, double gScore, double heuristic) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = gScore + heuristic;
        }
    }
}
