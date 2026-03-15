/**
 * ============================================================
 * [BeePathfinding.java]
 * Description: Facade de pathfinding pour abeilles — delegue a ThetaStarSolver
 * ============================================================
 *
 * Facade gardant la meme API publique que l'ancien BeePathfinding monolithique,
 * mais deleguant le calcul de chemin a ThetaStarSolver (Lazy Theta*).
 *
 * Responsabilites conservees dans la facade:
 * - Cache TTL (20 ticks min entre recalculs pour la meme destination)
 * - Stall detection (force recalcul si aucune progression en 60 ticks)
 * - Gestion du waypoint courant et progression le long du chemin
 * - Particules de debug
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance           | Raison                | Utilisation                    |
 * |----------------------|----------------------|--------------------------------|
 * | ThetaStarSolver      | Algorithme Lazy Theta*| Calcul de chemin               |
 * | PathCollisionHelper  | Collision            | Vol direct (ligne de vue)      |
 * | ParticleHelper       | Particules           | Debug path rendering           |
 * | Level                | Monde Minecraft      | gameTime, particules           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ForagingBehaviorGoal.java: Navigation vers fleurs/ruche
 * - WildBeePatrolGoal.java: Navigation patrouille sauvage
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.pathfinding;

import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Facade de pathfinding pour abeilles volantes.
 * Delegue le calcul a ThetaStarSolver (Lazy Theta*) et gere
 * le cache TTL, la stall detection, et les particules de debug.
 */
public class BeePathfinding {

    private static final double OFF_PATH_THRESHOLD = 5.0;
    private static final double WAYPOINT_REACH_DISTANCE = 0.7;
    private static final int RECOMPUTE_INTERVAL = 20;
    private static final int STALL_THRESHOLD = 40;
    private static final int MAX_CONSECUTIVE_STALLS = 3;

    private final Level level;

    @Nullable private List<BlockPos> currentPath = null;
    private int currentPathIndex = 0;
    @Nullable private BlockPos cachedEnd = null;
    @Nullable private BlockPos recoveryWaypoint = null;

    private long lastComputeGameTime = 0;
    private int progressStallCounter = 0;
    private int lastTrackedPathIndex = 0;
    private int consecutiveStalls = 0;

    private boolean debugMode = false;
    private long lastParticleTime = 0;
    private static final long PARTICLE_INTERVAL = 500;

    public BeePathfinding(Level level) {
        this.level = level;
    }

    /**
     * Calcule un chemin de start vers end.
     * Vol direct si ligne de vue, sinon Lazy Theta*.
     * Cache TTL: meme destination +   20 ticks → reutilise le chemin existant.
     */
    @Nullable
    public List<BlockPos> findPath(BlockPos start, BlockPos end) {
        if (recoveryWaypoint != null) {
            return null;
        }

        long gameTime = level.getGameTime();

        if (end.equals(cachedEnd) && currentPath != null
                && (gameTime - lastComputeGameTime) < RECOMPUTE_INTERVAL) {
            return currentPath;
        }

        cachedEnd = end;
        lastComputeGameTime = gameTime;
        progressStallCounter = 0;
        lastTrackedPathIndex = 0;

        if (PathCollisionHelper.hasLineOfSight(level, start, end)) {
            currentPath = List.of(end);
            currentPathIndex = 0;
            return currentPath;
        }

        currentPath = ThetaStarSolver.solve(level, start, end);
        currentPathIndex = 0;
        return currentPath;
    }

    /**
     * Retourne le prochain waypoint du chemin.
     * Distance serree (0.7) pour les intermediaires, reachDistance pour le dernier.
     * Inclut replanning si l'abeille est trop loin du chemin.
     */
    @Nullable
    public BlockPos getNextWaypoint(Vec3 currentPos, double reachDistance) {
        // Recovery waypoint takes priority: guide bee out of stuck position
        if (recoveryWaypoint != null) {
            double dist = currentPos.distanceTo(Vec3.atCenterOf(recoveryWaypoint));
            if (dist <= reachDistance) {
                recoveryWaypoint = null;
                return null;
            }
            return recoveryWaypoint;
        }

        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }

        if (currentPathIndex == lastTrackedPathIndex) {
            progressStallCounter++;
            if (progressStallCounter >= STALL_THRESHOLD) {
                consecutiveStalls++;
                clearPath();
                if (consecutiveStalls >= MAX_CONSECUTIVE_STALLS) {
                    // Fly upward + random horizontal jitter to escape stuck position
                    recoveryWaypoint = BlockPos.containing(
                            currentPos.x + (Math.random() * 4 - 2),
                            currentPos.y + 3,
                            currentPos.z + (Math.random() * 4 - 2)
                    );
                    consecutiveStalls = 0;
                }
                return null;
            }
        } else {
            lastTrackedPathIndex = currentPathIndex;
            progressStallCounter = 0;
            consecutiveStalls = 0;
        }

        while (currentPathIndex < currentPath.size()) {
            BlockPos waypoint = currentPath.get(currentPathIndex);
            double dist = currentPos.distanceTo(Vec3.atCenterOf(waypoint));

            if (dist > OFF_PATH_THRESHOLD) {
                clearPath();
                return null;
            }

            boolean isLastWaypoint = (currentPathIndex == currentPath.size() - 1);
            double threshold = isLastWaypoint ? reachDistance : WAYPOINT_REACH_DISTANCE;

            if (dist <= threshold) {
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

        Vec3 lastPos = entityPos;
        for (int i = currentPathIndex; i < currentPath.size(); i++) {
            Vec3 waypointPos = Vec3.atCenterOf(currentPath.get(i));
            double distance = lastPos.distanceTo(waypointPos);
            int particleCount = (int) Math.ceil(distance / 1.5);

            for (int j = 0; j <= particleCount; j++) {
                double t = (double) j / particleCount;
                double x = lastPos.x + (waypointPos.x - lastPos.x) * t;
                double y = lastPos.y + (waypointPos.y - lastPos.y) * t;
                double z = lastPos.z + (waypointPos.z - lastPos.z) * t;
                ParticleHelper.spawnParticles(serverLevel, ParticleTypes.END_ROD, new Vec3(x, y, z), 1, 0, 0);
            }
            lastPos = waypointPos;
        }
    }

    public void clearPath() {
        currentPath = null;
        currentPathIndex = 0;
        cachedEnd = null;
        progressStallCounter = 0;
        lastTrackedPathIndex = 0;
        recoveryWaypoint = null;
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
}
