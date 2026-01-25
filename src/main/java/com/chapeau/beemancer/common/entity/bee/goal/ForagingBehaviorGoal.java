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
 * - Approche par le haut (comme une vraie abeille)
 * - Inclinaison vers l'avant pendant le butinage
 * - Descente douce vers le sol si fleur en hauteur
 */
public class ForagingBehaviorGoal extends Goal {

    private static final double REACH_DISTANCE = 1.5;
    private static final double REACH_DISTANCE_VERTICAL = 0.8;
    private static final double FLIGHT_SPEED_FACTOR = 0.2;
    private static final double FLIGHT_ALTITUDE = 1.0; // Altitude de vol au-dessus des destinations
    private static final double HOVER_HEIGHT = 0.4; // Hauteur au-dessus de la fleur pour l'approche finale
    private static final double APPROACH_OFFSET = 0.2; // Décalage d'approche depuis la ruche
    private static final double FALL_SPEED = 0.02; // Vitesse de descente pendant le butinage
    private static final float FORAGING_PITCH = 40.0f; // Inclinaison vers l'avant (degrés)
    private static final Random RANDOM = new Random();

    private final MagicBeeEntity bee;
    private final BeeAIStateMachine stateMachine;
    private BeePathfinding pathfinding;

    // État d'approche
    private boolean isApproachingFromAbove = false;
    private float originalPitch = 0;

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

        // Reset état d'approche
        isApproachingFromAbove = false;
        resetPitch();
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
                bee.setReturning(true);
                bee.setDebugDestination(bee.getAssignedHivePos());
                stateMachine.setState(BeeActivityState.RETURNING);
            }
            case RETURNING -> {
                // Bloqué en retour, téléporter près de la ruche (fallback)
                BlockPos hivePos = bee.getAssignedHivePos();
                if (hivePos != null) {
                    bee.setPos(Vec3.atCenterOf(hivePos).add(0, 1, 0));
                    bee.setReturning(true);
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
                bee.setReturning(true);
                bee.setDebugDestination(bee.getAssignedHivePos());
                stateMachine.setState(BeeActivityState.RETURNING);
                return;
            }
            stateMachine.setTargetPos(targetFlower);
            bee.setDebugDestination(targetFlower);
            pathfinding.clearPath();
            isApproachingFromAbove = true;
        }

        // Vérifier que la fleur existe toujours
        if (!isValidFlower(targetFlower)) {
            returnFlowerToHive(targetFlower);
            stateMachine.clearTarget();
            pathfinding.clearPath();
            isApproachingFromAbove = false;
            return;
        }

        Vec3 flowerCenter = Vec3.atCenterOf(targetFlower);
        Vec3 beePos = bee.position();

        // Calculer l'offset d'approche (vecteur ruche->fleur, Y=0, inversé, magnitude 0.3)
        Vec3 approachOffset = calculateApproachOffset(targetFlower);

        // Phase 1: Approche par le haut
        if (isApproachingFromAbove) {
            Vec3 hoverPoint = flowerCenter.add(0, HOVER_HEIGHT, 0).add(approachOffset);
            double distToHover = beePos.distanceTo(hoverPoint);

            if (distToHover <= REACH_DISTANCE) {
                // Arrivé au point de survol, passer à la descente
                isApproachingFromAbove = false;
            } else {
                // Naviguer vers le point de survol
                navigateWithPathfinding(BlockPos.containing(hoverPoint));
                return;
            }
        }

        // Phase 2: Descente vers la fleur
        double horizontalDist = Math.sqrt(
                Math.pow(beePos.x - flowerCenter.x, 2) +
                Math.pow(beePos.z - flowerCenter.z, 2)
        );
        double verticalDist = Math.abs(beePos.y - flowerCenter.y);

        if (horizontalDist <= REACH_DISTANCE && verticalDist <= REACH_DISTANCE_VERTICAL) {
            // Arrivé sur la fleur, commencer à travailler
            originalPitch = bee.getXRot();
            stateMachine.setWorkTimer(bee.getBehaviorConfig().getForagingDuration());
            stateMachine.setState(BeeActivityState.WORKING);
            return;
        }

        // Descendre doucement vers la fleur (pas d'altitude additionnelle)
        navigateToExact(targetFlower);
    }

    private void tickWorking() {
        BlockPos targetFlower = stateMachine.getTargetPos();

        // Vérifier que la fleur existe toujours
        if (targetFlower == null || !isValidFlower(targetFlower)) {
            resetPitch();
            stateMachine.clearTarget();
            stateMachine.setState(BeeActivityState.SEEKING_FLOWER);
            return;
        }

        // Incliner l'abeille vers l'avant pendant le butinage
        bee.setXRot(FORAGING_PITCH);

        // Rester stationnaire au-dessus de la fleur avec l'offset d'approche
        Vec3 flowerCenter = Vec3.atCenterOf(targetFlower);
        Vec3 approachOffset = calculateApproachOffset(targetFlower);
        Vec3 hoverPos = flowerCenter.add(0, HOVER_HEIGHT, 0).add(approachOffset);
        Vec3 beePos = bee.position();

        // Maintenir la position au-dessus de la fleur avec un léger mouvement
        double dx = hoverPos.x - beePos.x;
        double dy = hoverPos.y - beePos.y;
        double dz = hoverPos.z - beePos.z;

        // Mouvement de correction doux pour rester en place
        bee.setDeltaMovement(new Vec3(dx * 0.1, dy * 0.1, dz * 0.1));

        // Vérifier si le travail est terminé
        if (stateMachine.isWorkComplete()) {
            bee.setPollinated(true);
            bee.setReturning(true); // Signal pour la ruche
            bee.setDebugDestination(bee.getAssignedHivePos());
            resetPitch();
            stateMachine.setState(BeeActivityState.RETURNING);
        }
    }

    /**
     * Remet le pitch de l'abeille à la normale.
     */
    private void resetPitch() {
        bee.setXRot(0);
    }

    private void tickReturning() {
        // S'assurer que le pitch est normal pendant le retour
        resetPitch();

        // S'assurer que le flag returning est activé
        if (!bee.isReturning()) {
            bee.setReturning(true);
        }

        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos == null) {
            return;
        }

        Vec3 hiveVec = Vec3.atCenterOf(hivePos);
        double distance = bee.position().distanceTo(hiveVec);

        if (distance <= REACH_DISTANCE) {
            // La ruche va gérer l'entrée dans serverTick
            bee.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // Quand proche de la ruche, descendre directement vers elle
        if (distance <= REACH_DISTANCE * 3) {
            navigateToExact(hivePos);
        } else {
            // Sinon voler en altitude
            navigateWithPathfinding(hivePos);
        }
    }

    /**
     * Navigation avec pathfinding Theta*.
     * L'altitude est gérée par le pathfinding en 3D.
     */
    private void navigateWithPathfinding(BlockPos destination) {
        // Ajouter une altitude de vol à la destination pour voler au-dessus
        BlockPos flightDestination = destination.above((int) FLIGHT_ALTITUDE);

        // Calculer ou récupérer le chemin avec Theta*
        pathfinding.findPath(bee.blockPosition(), flightDestination);

        // Afficher le chemin avec des particules (debug)
        pathfinding.showPathParticles(bee.position());

        // Obtenir le prochain waypoint
        BlockPos nextWaypoint = pathfinding.getNextWaypoint(bee.position(), REACH_DISTANCE);

        if (nextWaypoint == null) {
            // Chemin terminé ou pas de chemin, aller directement vers la destination
            nextWaypoint = flightDestination;
        }

        // Navigation directe vers le waypoint (le pathfinding gère l'altitude)
        navigateToVec3(Vec3.atCenterOf(nextWaypoint));
    }

    /**
     * Déplacement direct vers une position de bloc.
     */
    private void navigateTo(BlockPos pos) {
        navigateToVec3(Vec3.atCenterOf(pos));
    }

    /**
     * Déplacement direct vers une position exacte.
     */
    private void navigateToExact(BlockPos pos) {
        navigateToVec3(Vec3.atCenterOf(pos));
    }

    /**
     * Déplacement vers un Vec3 précis.
     */
    private void navigateToVec3(Vec3 targetVec) {
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

    /**
     * Calcule l'offset d'approche pour que l'abeille arrive du côté de la ruche.
     * Vecteur ruche->fleur, Y=0, normalisé, magnitude APPROACH_OFFSET, inversé.
     */
    private Vec3 calculateApproachOffset(BlockPos flowerPos) {
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos == null) {
            return Vec3.ZERO;
        }

        // Vecteur ruche -> fleur
        double dx = flowerPos.getX() - hivePos.getX();
        double dz = flowerPos.getZ() - hivePos.getZ();

        // Magnitude horizontale
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.01) {
            return Vec3.ZERO;
        }

        // Normaliser, appliquer magnitude, inverser (pour approcher depuis la ruche)
        double factor = -APPROACH_OFFSET / length;
        return new Vec3(dx * factor, 0, dz * factor);
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
        bee.clearDebugDestination();
        resetPitch();
        isApproachingFromAbove = false;
        stateMachine.reset();

        if (pathfinding != null) {
            pathfinding.clearPath();
        }
    }

    public BeeActivityState getState() {
        return stateMachine.getState();
    }
}
