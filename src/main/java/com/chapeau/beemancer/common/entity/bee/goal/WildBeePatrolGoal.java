/**
 * ============================================================
 * [WildBeePatrolGoal.java]
 * Description: Goal de patrouille pour abeilles sauvages (nids naturels)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entite abeille       | Acces position, nid, mouvement |
 * | BeePathfinding      | Navigation Theta*    | Deplacement vers waypoints     |
 * | BeeBehaviorConfig   | Configuration        | Vitesse de vol                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java: Enregistre dans registerGoals()
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee.goal;

import com.chapeau.beemancer.common.entity.bee.BeePathfinding;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Patrouille sauvage pour abeilles issues de bee nests.
 * Cycle: sort du nid → visite 3 points (rayon 15) → retourne au nid → repos 10-20s → recommence.
 * A chaque point, attend 2-10 secondes avant de passer au suivant.
 */
public class WildBeePatrolGoal extends Goal {

    private static final int PATROL_RADIUS = 15;
    private static final int WAYPOINT_COUNT = 3;
    private static final int MIN_WAIT_TICKS = 40;   // 2 secondes
    private static final int MAX_WAIT_TICKS = 200;   // 10 secondes
    private static final int MIN_REST_TICKS = 200;   // 10 secondes
    private static final int MAX_REST_TICKS = 400;   // 20 secondes
    private static final double REACH_DISTANCE = 2.0;
    private static final double FLIGHT_SPEED_FACTOR = 0.15;
    private static final int MOVE_TIMEOUT = 600;     // 30 secondes max pour atteindre un point

    private enum PatrolState {
        RESTING_AT_NEST,
        MOVING_TO_WAYPOINT,
        WAITING_AT_WAYPOINT,
        RETURNING_TO_NEST
    }

    private final MagicBeeEntity bee;
    private BeePathfinding pathfinding;

    private PatrolState state = PatrolState.RESTING_AT_NEST;
    private final List<BlockPos> waypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private int timer = 0;

    public WildBeePatrolGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bee.hasAssignedHive()) return false;
        if (!bee.hasHomeNest()) return false;
        if (bee.shouldFlee()) return false;
        if (bee.isEnraged()) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (pathfinding == null) {
            pathfinding = new BeePathfinding(bee.level());
        }
        pathfinding.clearPath();

        state = PatrolState.RESTING_AT_NEST;
        timer = randomRange(MIN_REST_TICKS, MAX_REST_TICKS);
        waypoints.clear();
        currentWaypointIndex = 0;
    }

    @Override
    public void tick() {
        switch (state) {
            case RESTING_AT_NEST -> tickRestingAtNest();
            case MOVING_TO_WAYPOINT -> tickMovingToWaypoint();
            case WAITING_AT_WAYPOINT -> tickWaitingAtWaypoint();
            case RETURNING_TO_NEST -> tickReturningToNest();
        }
    }

    private void tickRestingAtNest() {
        // Rester stationnaire pres du nid
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos != null) {
            hoverNear(nestPos);
        }

        timer--;
        if (timer <= 0) {
            generateWaypoints();
            if (waypoints.isEmpty()) {
                timer = randomRange(MIN_REST_TICKS, MAX_REST_TICKS);
                return;
            }
            currentWaypointIndex = 0;
            state = PatrolState.MOVING_TO_WAYPOINT;
            timer = MOVE_TIMEOUT;
            pathfinding.clearPath();
            bee.setDebugDestination(waypoints.get(0));
        }
    }

    private void tickMovingToWaypoint() {
        if (currentWaypointIndex >= waypoints.size()) {
            startReturning();
            return;
        }

        BlockPos target = waypoints.get(currentWaypointIndex);
        double distance = bee.position().distanceTo(Vec3.atCenterOf(target));

        if (distance <= REACH_DISTANCE) {
            // Arrive au waypoint, commencer a attendre
            bee.setDeltaMovement(Vec3.ZERO);
            state = PatrolState.WAITING_AT_WAYPOINT;
            timer = randomRange(MIN_WAIT_TICKS, MAX_WAIT_TICKS);
            pathfinding.clearPath();
            return;
        }

        // Timeout: passer au waypoint suivant
        timer--;
        if (timer <= 0) {
            currentWaypointIndex++;
            if (currentWaypointIndex >= waypoints.size()) {
                startReturning();
            } else {
                timer = MOVE_TIMEOUT;
                pathfinding.clearPath();
                bee.setDebugDestination(waypoints.get(currentWaypointIndex));
            }
            return;
        }

        navigateWithPathfinding(target);
    }

    private void tickWaitingAtWaypoint() {
        // Rester stationnaire au waypoint
        if (currentWaypointIndex < waypoints.size()) {
            hoverNear(waypoints.get(currentWaypointIndex));
        }

        timer--;
        if (timer <= 0) {
            currentWaypointIndex++;
            if (currentWaypointIndex >= waypoints.size()) {
                startReturning();
            } else {
                state = PatrolState.MOVING_TO_WAYPOINT;
                timer = MOVE_TIMEOUT;
                pathfinding.clearPath();
                bee.setDebugDestination(waypoints.get(currentWaypointIndex));
            }
        }
    }

    private void tickReturningToNest() {
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos == null) return;

        double distance = bee.position().distanceTo(Vec3.atCenterOf(nestPos));

        if (distance <= REACH_DISTANCE) {
            bee.setDeltaMovement(Vec3.ZERO);
            bee.clearDebugDestination();
            bee.setDebugPath(null);
            state = PatrolState.RESTING_AT_NEST;
            timer = randomRange(MIN_REST_TICKS, MAX_REST_TICKS);
            pathfinding.clearPath();
            return;
        }

        // Timeout: teleporter pres du nid (fallback)
        timer--;
        if (timer <= 0) {
            bee.setPos(Vec3.atCenterOf(nestPos).add(0, 1, 0));
            state = PatrolState.RESTING_AT_NEST;
            timer = randomRange(MIN_REST_TICKS, MAX_REST_TICKS);
            pathfinding.clearPath();
            return;
        }

        navigateWithPathfinding(nestPos);
    }

    private void startReturning() {
        BlockPos nestPos = bee.getHomeNestPos();
        state = PatrolState.RETURNING_TO_NEST;
        timer = MOVE_TIMEOUT;
        pathfinding.clearPath();
        bee.setDebugDestination(nestPos);
    }

    /**
     * Genere 3 waypoints aleatoires dans un rayon de 15 blocs autour du nid.
     * Les waypoints sont en l'air (traversables par l'abeille).
     */
    private void generateWaypoints() {
        waypoints.clear();
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos == null) return;

        Level level = bee.level();

        for (int i = 0; i < WAYPOINT_COUNT; i++) {
            BlockPos wp = findValidWaypoint(nestPos, level);
            if (wp != null) {
                waypoints.add(wp);
            }
        }
    }

    /**
     * Trouve un waypoint valide (air) dans le rayon de patrouille.
     * Essaie plusieurs fois avant d'abandonner.
     */
    private BlockPos findValidWaypoint(BlockPos center, Level level) {
        for (int attempt = 0; attempt < 15; attempt++) {
            int dx = randomRange(-PATROL_RADIUS, PATROL_RADIUS);
            int dz = randomRange(-PATROL_RADIUS, PATROL_RADIUS);
            int dy = randomRange(-3, 5);

            BlockPos candidate = center.offset(dx, dy, dz);

            // Verifier que c'est de l'air et pas trop loin verticalement
            if (!level.getBlockState(candidate).isAir()) continue;
            if (!level.getBlockState(candidate.above()).isAir()) continue;

            // Verifier qu'on est au-dessus du sol (pas en l'air libre trop haut)
            boolean hasGround = false;
            for (int y = 0; y < 10; y++) {
                if (level.getBlockState(candidate.below(y + 1)).isSolid()) {
                    hasGround = true;
                    break;
                }
            }
            if (!hasGround) continue;

            return candidate;
        }
        return null;
    }

    /**
     * Navigation Theta* vers une destination.
     */
    private void navigateWithPathfinding(BlockPos destination) {
        BlockPos flightDest = destination.above(1);
        pathfinding.findPath(bee.blockPosition(), flightDest);

        List<BlockPos> path = pathfinding.getCurrentPath();
        bee.setDebugPath(path);

        BlockPos nextWaypoint = pathfinding.getNextWaypoint(bee.position(), REACH_DISTANCE);
        if (nextWaypoint == null) {
            nextWaypoint = flightDest;
        }

        navigateToVec3(Vec3.atCenterOf(nextWaypoint));
    }

    /**
     * Deplacement vers un Vec3 precis.
     */
    private void navigateToVec3(Vec3 targetVec) {
        Vec3 direction = targetVec.subtract(bee.position()).normalize();

        BeeBehaviorConfig config = bee.getBehaviorConfig();
        double speed = config.getFlyingSpeed() * FLIGHT_SPEED_FACTOR;

        bee.setDeltaMovement(direction.scale(speed));

        double dx = targetVec.x - bee.getX();
        double dz = targetVec.z - bee.getZ();
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        bee.setYRot(targetYaw);
        bee.yBodyRot = targetYaw;
    }

    /**
     * Reste stationnaire pres d'une position avec un leger mouvement de correction.
     */
    private void hoverNear(BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        Vec3 beePos = bee.position();
        double dx = target.x - beePos.x;
        double dy = target.y - beePos.y;
        double dz = target.z - beePos.z;
        bee.setDeltaMovement(new Vec3(dx * 0.05, dy * 0.05, dz * 0.05));
    }

    private int randomRange(int min, int max) {
        return min + bee.getRandom().nextInt(max - min + 1);
    }

    @Override
    public void stop() {
        bee.setDeltaMovement(Vec3.ZERO);
        bee.clearDebugDestination();
        bee.setDebugPath(null);
        waypoints.clear();
        state = PatrolState.RESTING_AT_NEST;
        if (pathfinding != null) {
            pathfinding.clearPath();
        }
    }
}
