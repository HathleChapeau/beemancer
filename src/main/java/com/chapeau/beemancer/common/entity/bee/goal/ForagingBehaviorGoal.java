/**
 * ============================================================
 * [ForagingBehaviorGoal.java]
 * Description: Goal de butinage avec machine à états et pathfinding A*
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Accès aux méthodes             |
 * | BeeAIStateMachine   | Machine à états      | Gestion états centralisée      |
 * | BeePathfinding      | Navigation A*        | Pathfinding intelligent        |
 * | BeeBehaviorConfig   | Configuration        | Durées et paramètres           |
 * | BeeActivityState    | États                | Enum des états                 |
 * | FlowerGene          | Gène fleur           | Tag des fleurs cibles          |
 * | MagicHiveBlockEntity| Ruche                | Liste des fleurs               |
 * | FlowerSearchHelper  | Recherche fleurs     | Recherche de secours           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeEntity.java: Enregistré dans registerGoals()
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee.goal;

import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.entity.bee.BeeAIStateMachine;
import com.chapeau.beemancer.common.entity.bee.BeeActivityState;
import com.chapeau.beemancer.common.entity.bee.BeePathfinding;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorType;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.util.FlowerSearchHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Goal de priorité 3: Comportement de butinage complet.
 * Machine à états: IDLE → SEEKING_FLOWER → WORKING → RETURNING
 *
 * Améliorations:
 * - Machine à états centralisée (BeeAIStateMachine)
 * - Timeout de 40 secondes sur SEEKING_FLOWER
 * - Sélection aléatoire des fleurs (pas par distance)
 * - Pathfinding A* pour éviter les obstacles
 */
public class ForagingBehaviorGoal extends Goal {

    private static final double REACH_DISTANCE = 1.5;
    private static final double FLIGHT_SPEED_FACTOR = 0.1;
    private static final Random RANDOM = new Random();

    private final MagicBeeEntity bee;
    private final BeeAIStateMachine stateMachine;
    private BeePathfinding pathfinding;

    public ForagingBehaviorGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.stateMachine = new BeeAIStateMachine(bee);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Configurer le callback de timeout
        stateMachine.setOnTimeout(this::onStateTimeout);
    }

    @Override
    public boolean canUse() {
        if (!bee.hasAssignedHive()) return false;
        if (bee.shouldFlee()) return false;
        if (bee.isEnraged()) return false;

        BeeBehaviorConfig config = bee.getBehaviorConfig();
        if (config.getBehaviorType() != BeeBehaviorType.FORAGER) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        stateMachine.reset();
        stateMachine.setState(BeeActivityState.SEEKING_FLOWER);

        // Initialiser le pathfinding (lazy init)
        if (pathfinding == null) {
            pathfinding = new BeePathfinding(bee.level());
        }
        pathfinding.clearPath();
    }

    @Override
    public void tick() {
        // Tick la machine à états (gère les timeouts)
        boolean timedOut = stateMachine.tick();

        // Si timeout géré, ne pas continuer le tick normal
        if (timedOut) return;

        switch (stateMachine.getState()) {
            case SEEKING_FLOWER -> tickSeekingFlower();
            case WORKING -> tickWorking();
            case RETURNING -> tickReturning();
            default -> {} // IDLE, LEAVING_HIVE, RESTING gérés par la ruche
        }
    }

    /**
     * Appelé quand un état a timeout.
     */
    private void onStateTimeout() {
        BeeActivityState currentState = stateMachine.getState();

        switch (currentState) {
            case SEEKING_FLOWER -> {
                // Pas de fleur trouvée après 40 secondes, retourner à la ruche
                if (stateMachine.hasTarget()) {
                    returnFlowerToHive(stateMachine.getTargetPos());
                }
                stateMachine.setState(BeeActivityState.RETURNING);
            }
            case RETURNING -> {
                // Bloqué en retour, téléporter près de la ruche (fallback)
                BlockPos hivePos = bee.getAssignedHivePos();
                if (hivePos != null) {
                    bee.setPos(Vec3.atCenterOf(hivePos).add(0, 1, 0));
                }
            }
            default -> {}
        }
    }

    private void tickSeekingFlower() {
        BlockPos targetFlower = stateMachine.getTargetPos();

        // Si pas de fleur cible, en chercher une
        if (targetFlower == null) {
            targetFlower = findNextFlower();
            if (targetFlower == null) {
                // Pas de fleur trouvée, retourner à la ruche
                stateMachine.setState(BeeActivityState.RETURNING);
                return;
            }
            stateMachine.setTargetPos(targetFlower);
            pathfinding.clearPath();
        }

        // Vérifier que la fleur existe toujours
        if (!isValidFlower(targetFlower)) {
            returnFlowerToHive(targetFlower);
            stateMachine.clearTarget();
            pathfinding.clearPath();
            return;
        }

        // Naviguer vers la fleur
        double distance = bee.position().distanceTo(Vec3.atCenterOf(targetFlower));
        if (distance <= REACH_DISTANCE) {
            // Arrivé sur la fleur, commencer à travailler
            stateMachine.setWorkTimer(bee.getBehaviorConfig().getForagingDuration());
            stateMachine.setState(BeeActivityState.WORKING);
            return;
        }

        navigateWithPathfinding(targetFlower);
    }

    private void tickWorking() {
        BlockPos targetFlower = stateMachine.getTargetPos();

        // Vérifier que la fleur existe toujours
        if (targetFlower == null || !isValidFlower(targetFlower)) {
            stateMachine.clearTarget();
            stateMachine.setState(BeeActivityState.SEEKING_FLOWER);
            return;
        }

        // Rester sur place
        bee.setDeltaMovement(Vec3.ZERO);

        // Vérifier si le travail est terminé
        if (stateMachine.isWorkComplete()) {
            bee.setPollinated(true);
            stateMachine.setState(BeeActivityState.RETURNING);
        }
    }

    private void tickReturning() {
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos == null) {
            return;
        }

        double distance = bee.position().distanceTo(Vec3.atCenterOf(hivePos));
        if (distance <= REACH_DISTANCE) {
            // La ruche va gérer l'entrée dans serverTick
            bee.setDeltaMovement(Vec3.ZERO);
            return;
        }

        navigateWithPathfinding(hivePos);
    }

    /**
     * Navigation avec pathfinding A*.
     */
    private void navigateWithPathfinding(BlockPos destination) {
        // Calculer ou récupérer le chemin
        pathfinding.findPath(bee.blockPosition(), destination);

        // Obtenir le prochain waypoint
        BlockPos nextWaypoint = pathfinding.getNextWaypoint(bee.position(), REACH_DISTANCE);

        if (nextWaypoint == null) {
            // Chemin terminé ou pas de chemin, aller directement vers la destination
            nextWaypoint = destination;
        }

        navigateTo(nextWaypoint);
    }

    /**
     * Déplacement direct vers une position.
     */
    private void navigateTo(BlockPos pos) {
        Vec3 targetVec = Vec3.atCenterOf(pos);
        Vec3 direction = targetVec.subtract(bee.position()).normalize();

        BeeBehaviorConfig config = bee.getBehaviorConfig();
        double speed = config.getFlyingSpeed() * FLIGHT_SPEED_FACTOR;
        if (bee.isEnraged()) {
            speed = config.getEnragedFlyingSpeed() * FLIGHT_SPEED_FACTOR;
        }

        bee.setDeltaMovement(direction.scale(speed));

        // Rotation
        double dx = targetVec.x - bee.getX();
        double dz = targetVec.z - bee.getZ();
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        bee.setYRot(targetYaw);
        bee.yBodyRot = targetYaw;
    }

    /**
     * Trouve la prochaine fleur (sélection aléatoire parmi les disponibles).
     */
    private BlockPos findNextFlower() {
        // D'abord essayer depuis la ruche
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos != null && bee.level().getBlockEntity(hivePos) instanceof MagicHiveBlockEntity hive) {
            int slot = bee.getAssignedSlot();
            BlockPos flower = hive.getAndAssignFlower(slot);
            if (flower != null) {
                return flower;
            }
        }

        // Fallback: recherche dans les environs avec sélection aléatoire
        return findRandomNearbyFlower();
    }

    /**
     * Trouve une fleur aléatoire dans les environs.
     */
    private BlockPos findRandomNearbyFlower() {
        Gene flowerGene = bee.getGeneData().getGene(GeneCategory.FLOWER);
        if (!(flowerGene instanceof FlowerGene flower)) {
            return null;
        }

        TagKey<Block> flowerTag = flower.getFlowerTag();
        if (flowerTag == null) return null;

        BeeBehaviorConfig config = bee.getBehaviorConfig();
        int range = config.getAreaOfEffect();
        Level level = bee.level();

        // Chercher autour de la ruche
        BlockPos searchCenter = bee.getAssignedHivePos();
        if (searchCenter == null) {
            searchCenter = bee.blockPosition();
        }

        // Récupérer toutes les fleurs
        List<BlockPos> allFlowers = FlowerSearchHelper.findAllFlowers(level, searchCenter, range, flowerTag);

        if (allFlowers.isEmpty()) {
            return null;
        }

        // Sélection aléatoire
        return allFlowers.get(RANDOM.nextInt(allFlowers.size()));
    }

    private boolean isValidFlower(BlockPos pos) {
        Gene flowerGene = bee.getGeneData().getGene(GeneCategory.FLOWER);
        if (!(flowerGene instanceof FlowerGene flower)) {
            return false;
        }

        TagKey<Block> flowerTag = flower.getFlowerTag();
        if (flowerTag == null) return false;

        return FlowerSearchHelper.isValidFlower(bee.level(), pos, flowerTag);
    }

    private void returnFlowerToHive(BlockPos flower) {
        if (flower == null) return;

        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos != null && bee.level().getBlockEntity(hivePos) instanceof MagicHiveBlockEntity hive) {
            hive.returnFlower(bee.getAssignedSlot(), flower);
        }
    }

    @Override
    public void stop() {
        // Retourner la fleur actuelle à la ruche
        BlockPos targetFlower = stateMachine.getTargetPos();
        if (targetFlower != null) {
            returnFlowerToHive(targetFlower);
        }

        bee.setDeltaMovement(Vec3.ZERO);
        stateMachine.reset();

        if (pathfinding != null) {
            pathfinding.clearPath();
        }
    }

    public BeeActivityState getState() {
        return stateMachine.getState();
    }
}
