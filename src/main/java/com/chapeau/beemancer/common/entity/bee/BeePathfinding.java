/**
 * ============================================================
 * [BeePathfinding.java]
 * Description: Pathfinding Theta* optimise pour abeilles volantes
 * ============================================================
 *
 * ALGORITHME: Theta* (Any-Angle Pathfinding)
 * - Extension de A* qui permet des chemins en ligne droite
 * - Utilise DDA 3D pour la verification de ligne de vue
 * - Optimise pour les entites volantes en 3D
 * - Penalite de proximite aux murs (steering behavior)
 * - Preference d'altitude (voler au-dessus des obstacles)
 * - Clearance entity (verification 1x2 pour le hitbox)
 *
 * Reference: Nash et al. 2007 "Theta*: Any-Angle Path Planning on Grids"
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Level               | Monde Minecraft      | Verification collisions        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ForagingBehaviorGoal.java: Navigation vers fleurs/ruche
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * plutot que de suivre la grille.
 *
 * Ameliorations par rapport a A* standard:
 * - Vol direct si ligne de vue degagee (O(1) dans le meilleur cas)
 * - DDA 3D precise pour la detection de ligne de vue
 * - Penalite murs: evite de raser les obstacles
 * - Preference altitude: prefere voler au-dessus des obstacles
 * - Clearance entity: verifie 1x2 blocs pour le hitbox
 * - Fallback intelligent: se rapproche au maximum si echec
 * - Replanning: recalcule si l'abeille est poussee hors du chemin
 */
public class BeePathfinding {

    // Configuration
    private static final int MAX_ITERATIONS = 1000;
    private static final int MAX_PATH_LENGTH = 100;
    private static final double DIAGONAL_COST = 1.414;
    private static final double STRAIGHT_COST = 1.0;
    private static final double WALL_PENALTY = 0.5;
    private static final double ALTITUDE_BONUS = 0.3;
    private static final double OFF_PATH_THRESHOLD = 5.0;

    // Directions 3D pour l'expansion des noeuds (26 directions)
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
    private boolean debugMode = false;
    private long lastParticleTime = 0;
    private static final long PARTICLE_INTERVAL = 500; // ms

    public BeePathfinding(Level level) {
        this.level = level;
    }

    /**
     * Calcule un chemin optimise de start vers end.
     * Utilise le vol direct si possible, sinon Theta*.
     */
    @Nullable
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        // Cache hit - reutiliser le chemin existant
        if (end.equals(cachedEnd) && cachedStart != null && start.closerThan(cachedStart, 3) && currentPath != null) {
            return currentPath;
        }

        cachedStart = start;
        cachedEnd = end;

        // Etape 1: Essayer le vol direct (ligne de vue)
        if (hasLineOfSight(start, end)) {
            currentPath = List.of(end);
            currentPathIndex = 0;
            return currentPath;
        }

        // Etape 2: Theta* pour contourner les obstacles
        currentPath = computeThetaStar(start, end);
        currentPathIndex = 0;

        return currentPath;
    }

    /**
     * Retourne le prochain waypoint du chemin.
     * Suit le chemin compute fidelement (pas de skip de waypoints).
     * Le lissage est deja fait dans smoothPath() lors du calcul.
     * Inclut replanning si l'abeille est trop loin du chemin.
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

            // Replanning: si trop loin du prochain waypoint, invalider le cache
            if (dist > OFF_PATH_THRESHOLD) {
                clearPath();
                return null;
            }

            if (dist <= reachDistance) {
                currentPathIndex++;
            } else {
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
     * Inclut penalites murs, preference altitude, et fallback intelligent.
     */
    @Nullable
    private List<BlockPos> computeThetaStar(BlockPos start, BlockPos end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        // Tracker le noeud le plus proche de la destination (pour fallback)
        Node closestToGoal = startNode;
        double closestDist = heuristic(start, end);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            Node current = openSet.poll();
            if (current == null) break;

            // Mettre a jour le noeud le plus proche
            double distToGoal = heuristic(current.pos, end);
            if (distToGoal < closestDist) {
                closestDist = distToGoal;
                closestToGoal = current;
            }

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

                // Calculer le cout de deplacement avec penalites
                double moveCost = computeMoveCost(dir);
                double penalty = computeWallPenalty(neighborPos);

                // === THETA* KEY DIFFERENCE ===
                // Au lieu de toujours utiliser current comme parent,
                // on verifie si le parent de current a une ligne de vue vers neighbor
                Node parent = current;
                double tentativeG;

                if (current.parent != null && hasLineOfSight(current.parent.pos, neighborPos)) {
                    // Ligne de vue directe depuis le grand-parent
                    parent = current.parent;
                    tentativeG = parent.gScore + euclideanDistance(parent.pos, neighborPos) + penalty;
                } else {
                    // Pas de ligne de vue, utiliser le chemin standard
                    tentativeG = current.gScore + moveCost + penalty;
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

        // Echec: retourner le chemin vers le noeud le plus proche atteint
        return reconstructAndSmoothPath(closestToGoal, closestToGoal.pos);
    }

    /**
     * Calcule le cout de deplacement pour une direction, avec preference d'altitude.
     * Monter coute moins cher, descendre coute plus cher.
     */
    private double computeMoveCost(int[] dir) {
        double baseCost = isDiagonal(dir) ? DIAGONAL_COST : STRAIGHT_COST;

        // Preference d'altitude: bonus pour monter, malus pour descendre
        if (dir[1] > 0) {
            baseCost -= ALTITUDE_BONUS;
        } else if (dir[1] < 0) {
            baseCost += ALTITUDE_BONUS;
        }

        return baseCost;
    }

    /**
     * Calcule la penalite de proximite aux murs.
     * Plus il y a de blocs solides adjacents, plus le cout est eleve.
     */
    private double computeWallPenalty(BlockPos pos) {
        int solidCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            if (neighborState.isSolid()) {
                solidCount++;
            }
        }
        return solidCount * WALL_PENALTY;
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

        // Ajouter destination si necessaire
        if (!path.isEmpty() && !path.get(path.size() - 1).equals(finalDest)) {
            path.add(finalDest);
        }

        // Lissage final avec DDA
        return smoothPath(path);
    }

    /**
     * Lisse le chemin en supprimant les waypoints intermediaires inutiles.
     * Utilise la technique du "string pulling" avec verification de ligne de vue.
     */
    private List<BlockPos> smoothPath(List<BlockPos> path) {
        if (path.size() <= 2) return path;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int current = 0;
        while (current < path.size() - 1) {
            // Chercher le point le plus eloigne visible depuis current
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
     * Verifie la ligne de vue avec l'algorithme DDA 3D.
     * Avance pas-a-pas le long de l'axe dominant, interpolant les deux autres axes.
     * Utilise l'arithmetique entiere pour eviter les erreurs de floating point.
     */
    public boolean hasLineOfSight(BlockPos start, BlockPos end) {
        int x0 = start.getX(), y0 = start.getY(), z0 = start.getZ();
        int x1 = end.getX(), y1 = end.getY(), z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);

        int steps = Math.max(dx, Math.max(dy, dz));
        if (steps == 0) return true;

        // DDA 3D avec arithmetique entiere
        // On multiplie par steps pour eviter les divisions flottantes
        // position = start * steps + i * delta
        int deltaX = x1 - x0;
        int deltaY = y1 - y0;
        int deltaZ = z1 - z0;

        for (int i = 1; i < steps; i++) {
            // Interpolation entiere: pos = start + (delta * i) / steps
            // Arrondi au plus proche: (delta * i + steps/2) / steps
            int x = x0 + (deltaX * i + (deltaX > 0 ? steps / 2 : -steps / 2)) / steps;
            int y = y0 + (deltaY * i + (deltaY > 0 ? steps / 2 : -steps / 2)) / steps;
            int z = z0 + (deltaZ * i + (deltaZ > 0 ? steps / 2 : -steps / 2)) / steps;

            if (!isPassable(new BlockPos(x, y, z))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifie si une position est traversable par l'abeille.
     * Verifie le bloc et le bloc au-dessus (clearance 1x2 pour le hitbox).
     */
    private boolean isPassable(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isSolid() || state.liquid()) {
            return false;
        }
        // Clearance: verifier le bloc au-dessus pour le hitbox de l'abeille
        BlockState above = level.getBlockState(pos.above());
        return !above.isSolid() && !above.liquid();
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
     * Verifie si une direction est diagonale.
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
     * Noeud pour Theta*.
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
