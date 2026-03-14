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
 * | BeeNestBlockEntity  | Nid d'origine        | Notification retour au nid     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java: Enregistre dans registerGoals()
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.bee.goal;

import com.chapeau.apica.common.block.hive.BeeNestBlockEntity;
import com.chapeau.apica.common.entity.bee.pathfinding.BeeFlightHelper;
import com.chapeau.apica.common.entity.bee.pathfinding.BeePathfinding;
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.core.behavior.BeeBehaviorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Patrouille sauvage pour abeilles issues de bee nests.
 * Cycle: sort du nid → visite 3 points (rayon 15) → retourne au nid → bee supprimee → respawn par le nid.
 * A chaque point, attend 2-10 secondes avant de passer au suivant.
 */
public class WildBeePatrolGoal extends Goal {

    private static final int PATROL_RADIUS = 15;
    private static final int WAYPOINT_COUNT = 3;
    private static final int MIN_WAIT_TICKS = 40;   // 2 secondes
    private static final int MAX_WAIT_TICKS = 200;   // 10 secondes
    private static final double REACH_DISTANCE = 2.0;
    private static final int MOVE_TIMEOUT = 600;     // 30 secondes max pour atteindre un point
    private static final int GRACE_PERIOD_TICKS = 12;

    private enum PatrolState {
        MOVING_TO_WAYPOINT,
        WAITING_AT_WAYPOINT,
        RETURNING_TO_NEST
    }

    private final MagicBeeEntity bee;
    private BeePathfinding pathfinding;

    // Vanilla pathfinding (FlyingPathNavigation)
    private FlyingPathNavigation vanillaNavigation;
    private Path currentVanillaPath;
    private int vanillaPathIndex = 0;

    private PatrolState state = PatrolState.MOVING_TO_WAYPOINT;
    private final List<BlockPos> waypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private int timer = 0;
    private int ticksSinceStart = 0;

    public WildBeePatrolGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bee.hasAssignedHive()) return false;
        if (!bee.hasHomeNest()) return false;
        if (bee.shouldFlee()) return false;
        if (bee.isEnraged() && bee.getTarget() != null) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        if (pathfinding == null) {
            pathfinding = new BeePathfinding(bee.level());
        }
        pathfinding.clearPath();

        // Init vanilla navigation
        if (vanillaNavigation == null) {
            vanillaNavigation = new FlyingPathNavigation(bee, bee.level());
        }
        currentVanillaPath = null;
        vanillaPathIndex = 0;

        waypoints.clear();
        currentWaypointIndex = 0;
        ticksSinceStart = 0;
        startNewPatrol();
    }

    @Override
    public void tick() {
        ticksSinceStart++;
        timer--;

        // Throttle N=2, skip pendant la grace period
        if (ticksSinceStart > GRACE_PERIOD_TICKS) {
            if (bee.tickCount % 2 != (int)(bee.getUUID().getLeastSignificantBits() & 0x1)) return;
        }

        switch (state) {
            case MOVING_TO_WAYPOINT -> tickMovingToWaypoint();
            case WAITING_AT_WAYPOINT -> tickWaitingAtWaypoint();
            case RETURNING_TO_NEST -> tickReturningToNest();
            default -> {}
        }
    }

    /**
     * Genere les waypoints et demarre une nouvelle patrouille.
     * Si la generation echoue, retourne directement au nid.
     */
    private void startNewPatrol() {
        generateWaypoints();
        if (waypoints.isEmpty()) {
            startReturning();
            return;
        }
        currentWaypointIndex = 0;
        state = PatrolState.MOVING_TO_WAYPOINT;
        timer = MOVE_TIMEOUT;
        pathfinding.clearPath();
        bee.setDebugDestination(waypoints.get(0));
        updateDebugPatrolPath();
    }

    private void tickMovingToWaypoint() {
        if (currentWaypointIndex >= waypoints.size()) {
            startReturning();
            return;
        }

        BlockPos target = waypoints.get(currentWaypointIndex);
        double distance = bee.position().distanceTo(Vec3.atCenterOf(target));

        if (distance <= REACH_DISTANCE) {
            bee.setDeltaMovement(Vec3.ZERO);
            state = PatrolState.WAITING_AT_WAYPOINT;
            timer = randomRange(MIN_WAIT_TICKS, MAX_WAIT_TICKS);
            pathfinding.clearPath();
            bee.setDebugDestination(target);
            updateDebugPatrolPath();
            return;
        }

        if (timer <= 0) {
            advanceToNextWaypoint();
            return;
        }

        navigateWithPathfinding(target);
    }

    private void tickWaitingAtWaypoint() {
        if (currentWaypointIndex < waypoints.size()) {
            hoverNear(waypoints.get(currentWaypointIndex));
        }

        if (timer <= 0) {
            advanceToNextWaypoint();
        }
    }

    /**
     * Passe au waypoint suivant, ou retourne au nid si c'etait le dernier.
     */
    private void advanceToNextWaypoint() {
        currentWaypointIndex++;
        if (currentWaypointIndex >= waypoints.size()) {
            startReturning();
        } else {
            state = PatrolState.MOVING_TO_WAYPOINT;
            timer = MOVE_TIMEOUT;
            pathfinding.clearPath();
            bee.setDebugDestination(waypoints.get(currentWaypointIndex));
            updateDebugPatrolPath();
        }
    }

    private void tickReturningToNest() {
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos == null) return;

        // Si le chunk du nid n'est pas charge, rester sur place et attendre
        if (!bee.level().isLoaded(nestPos)) {
            hoverNear(bee.blockPosition());
            return;
        }

        double distance = bee.position().distanceTo(Vec3.atCenterOf(nestPos));

        if (distance <= REACH_DISTANCE) {
            enterNest(nestPos);
            return;
        }

        if (timer <= 0) {
            // Timeout: teleporter au nid puis entrer
            bee.setPos(Vec3.atCenterOf(nestPos).add(0, 1, 0));
            enterNest(nestPos);
            return;
        }

        navigateWithPathfinding(nestPos);
    }

    /**
     * L'abeille entre dans le nid: notifie le BlockEntity qui la supprime et programme un respawn.
     */
    private void enterNest(BlockPos nestPos) {
        bee.setDeltaMovement(Vec3.ZERO);
        bee.clearDebugDestination();
        bee.setDebugPath(null);
        if (pathfinding != null) {
            pathfinding.clearPath();
        }

        if (bee.level().getBlockEntity(nestPos) instanceof BeeNestBlockEntity nest) {
            nest.onBeeReturned(bee);
        } else {
            // Le nid n'existe plus, l'abeille disparait
            bee.discard();
        }
    }

    private void startReturning() {
        BlockPos nestPos = bee.getHomeNestPos();
        state = PatrolState.RETURNING_TO_NEST;
        timer = MOVE_TIMEOUT;
        pathfinding.clearPath();
        bee.setDebugDestination(nestPos);
        updateDebugPatrolPath();
    }

    /**
     * Met a jour le chemin de debug pour afficher la route de patrouille restante.
     * Inclut les waypoints non visites + retour au nid.
     */
    private void updateDebugPatrolPath() {
        List<BlockPos> debugPath = new ArrayList<>();
        for (int i = currentWaypointIndex; i < waypoints.size(); i++) {
            debugPath.add(waypoints.get(i));
        }
        BlockPos nestPos = bee.getHomeNestPos();
        if (nestPos != null) {
            debugPath.add(nestPos);
        }
        bee.setDebugPath(debugPath);
    }

    /**
     * Genere 3 waypoints aleatoires dans un rayon de 15 blocs autour du nid.
     * Les waypoints sont dans des blocs traversables (non-solides).
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
     * Trouve un waypoint valide (non-solide) dans le rayon de patrouille.
     * Cherche au-dessus du sol, dans des blocs traversables.
     */
    private BlockPos findValidWaypoint(BlockPos center, Level level) {
        for (int attempt = 0; attempt < 30; attempt++) {
            int dx = randomRange(-PATROL_RADIUS, PATROL_RADIUS);
            int dz = randomRange(-PATROL_RADIUS, PATROL_RADIUS);

            BlockPos groundSearch = center.offset(dx, 5, dz);

            // Chercher le sol depuis le haut
            BlockPos groundPos = null;
            for (int y = 0; y < 15; y++) {
                BlockPos check = groundSearch.below(y);
                if (level.getBlockState(check).isSolid()) {
                    groundPos = check;
                    break;
                }
            }
            if (groundPos == null) continue;

            // Le waypoint est 2-4 blocs au-dessus du sol
            int flyHeight = randomRange(2, 4);
            BlockPos candidate = groundPos.above(flyHeight);

            // Verifier que le bloc et le bloc au-dessus sont traversables
            if (level.getBlockState(candidate).isSolid()) continue;
            if (level.getBlockState(candidate.above()).isSolid()) continue;

            return candidate;
        }
        return null;
    }

    /**
     * Navigation Theta* vers une destination.
     */
    private void navigateWithPathfinding(BlockPos destination) {
        BlockPos flightDest = destination.above(1);

        // --- THETA* (commenté) ---
        // pathfinding.findPath(bee.blockPosition(), flightDest);
        // BlockPos nextWaypoint = pathfinding.getNextWaypoint(bee.position(), REACH_DISTANCE);

        // --- VANILLA FLYING PATHFINDING ---
        // Recalculer le chemin si nécessaire
        if (currentVanillaPath == null || currentVanillaPath.isDone() ||
            !flightDest.equals(currentVanillaPath.getTarget())) {
            currentVanillaPath = vanillaNavigation.createPath(flightDest, 0);
            vanillaPathIndex = 0;
        }

        BlockPos nextWaypoint = null;

        if (currentVanillaPath != null && !currentVanillaPath.isDone()) {
            // Avancer dans le chemin si on a atteint le waypoint actuel
            while (vanillaPathIndex < currentVanillaPath.getNodeCount()) {
                Node node = currentVanillaPath.getNode(vanillaPathIndex);
                BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
                double dist = bee.position().distanceTo(Vec3.atCenterOf(nodePos));
                if (dist <= REACH_DISTANCE) {
                    vanillaPathIndex++;
                } else {
                    nextWaypoint = nodePos;
                    break;
                }
            }
        }

        if (nextWaypoint == null) {
            nextWaypoint = flightDest;
        }

        navigateToVec3(Vec3.atCenterOf(nextWaypoint));
    }

    /**
     * Deplacement vers un Vec3 precis.
     */
    private void navigateToVec3(Vec3 targetVec) {
        Vec3 beePos = bee.position();
        Vec3 diff = targetVec.subtract(beePos);
        double length = diff.length();
        if (length < 0.01) return;

        Vec3 direction = diff.normalize();
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        double speed = config.getPatrolSpeed();

        Vec3 movement = direction.scale(speed);

        // Separation boids: evite la superposition avec les abeilles voisines
        Vec3 separation = BeeFlightHelper.computeSeparation(bee);
        movement = movement.add(separation);

        bee.setDeltaMovement(movement);

        double dx = targetVec.x - beePos.x;
        double dz = targetVec.z - beePos.z;
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
        if (pathfinding != null) {
            pathfinding.clearPath();
        }
    }
}
