/**
 * ============================================================
 * [ForagingBehaviorGoal.java]
 * Description: Goal de butinage avec machine à états complète
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Accès aux méthodes             |
 * | BeeBehaviorConfig   | Configuration        | Durées et paramètres           |
 * | BeeActivityState    | États                | Machine à états                |
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
import com.chapeau.beemancer.common.entity.bee.BeeActivityState;
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

/**
 * Goal de priorité 3: Comportement de butinage complet.
 * Machine à états: IDLE → SEEKING_FLOWER → WORKING → RETURNING
 * 
 * L'abeille:
 * 1. Récupère une fleur depuis la ruche
 * 2. Se déplace vers la fleur
 * 3. Travaille pendant foragingDuration ticks
 * 4. Devient pollinisée et retourne à la ruche
 */
public class ForagingBehaviorGoal extends Goal {
    
    private static final double REACH_DISTANCE = 1.5;
    private static final double FLIGHT_SPEED_FACTOR = 0.1;
    
    private final MagicBeeEntity bee;
    
    private BeeActivityState state = BeeActivityState.IDLE;
    private BlockPos targetFlower = null;
    private int workTimer = 0;
    
    public ForagingBehaviorGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Vérifier les conditions de base
        if (!bee.hasAssignedHive()) return false;
        if (bee.shouldFlee()) return false;
        if (bee.isEnraged()) return false;
        
        // Vérifier que c'est une abeille butineuse
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
        state = BeeActivityState.SEEKING_FLOWER;
        targetFlower = null;
        workTimer = 0;
    }
    
    @Override
    public void tick() {
        switch (state) {
            case SEEKING_FLOWER -> tickSeekingFlower();
            case WORKING -> tickWorking();
            case RETURNING -> tickReturning();
            default -> {} // IDLE, LEAVING_HIVE, RESTING gérés par la ruche
        }
    }
    
    private void tickSeekingFlower() {
        // Si pas de fleur cible, en chercher une
        if (targetFlower == null) {
            targetFlower = findNextFlower();
            if (targetFlower == null) {
                // Pas de fleur trouvée, retourner à la ruche
                state = BeeActivityState.RETURNING;
                bee.setReturning(true);
                return;
            }
        }
        
        // Vérifier que la fleur existe toujours
        if (!isValidFlower(targetFlower)) {
            returnFlowerToHive(targetFlower);
            targetFlower = null;
            return;
        }
        
        // Se diriger vers la fleur
        double distance = bee.position().distanceTo(Vec3.atCenterOf(targetFlower));
        if (distance <= REACH_DISTANCE) {
            // Arrivé sur la fleur, commencer à travailler
            state = BeeActivityState.WORKING;
            workTimer = bee.getBehaviorConfig().getForagingDuration();
            return;
        }
        
        navigateTo(targetFlower);
    }
    
    private void tickWorking() {
        // Vérifier que la fleur existe toujours
        if (targetFlower == null || !isValidFlower(targetFlower)) {
            state = BeeActivityState.SEEKING_FLOWER;
            targetFlower = null;
            return;
        }
        
        // Rester sur place
        bee.setDeltaMovement(Vec3.ZERO);
        
        // Décrémenter le timer
        workTimer--;
        
        if (workTimer <= 0) {
            // Travail terminé, marquer comme pollinisée et retourner
            bee.setPollinated(true);
            state = BeeActivityState.RETURNING;
                bee.setReturning(true);
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
            // On reste juste à proximité
            bee.setDeltaMovement(Vec3.ZERO);
            return;
        }
        
        navigateTo(hivePos);
    }
    
    private void navigateTo(BlockPos pos) {
        Vec3 targetVec = Vec3.atCenterOf(pos);
        Vec3 direction = targetVec.subtract(bee.position()).normalize();
        
        double speed = bee.getBehaviorConfig().getFlyingSpeed() * FLIGHT_SPEED_FACTOR;
        if (bee.isEnraged()) {
            speed = bee.getBehaviorConfig().getEnragedFlyingSpeed() * FLIGHT_SPEED_FACTOR;
        }
        bee.setDeltaMovement(direction.scale(speed));
        
        // Rotation
        double dx = targetVec.x - bee.getX();
        double dz = targetVec.z - bee.getZ();
        float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        bee.setYRot(targetYaw);
        bee.yBodyRot = targetYaw;
    }
    
    private BlockPos findNextFlower() {
        // First try to get from hive
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos != null && bee.level().getBlockEntity(hivePos) instanceof MagicHiveBlockEntity hive) {
            int slot = bee.getAssignedSlot();
            BlockPos flower = hive.getAndAssignFlower(slot);
            if (flower != null) {
                return flower;
            }
        }
        
        // Fallback: search nearby
        return findNearbyFlower();
    }
    
    private BlockPos findNearbyFlower() {
        // Récupérer le gène de fleur
        Gene flowerGene = bee.getGeneData().getGene(GeneCategory.FLOWER);
        if (!(flowerGene instanceof FlowerGene flower)) {
            return null;
        }
        
        TagKey<Block> flowerTag = flower.getFlowerTag();
        if (flowerTag == null) return null;
        
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        int range = config.getAreaOfEffect();
        Level level = bee.level();
        
        // Chercher autour de la ruche (pas de l'abeille)
        BlockPos searchCenter = bee.getAssignedHivePos();
        if (searchCenter == null) {
            searchCenter = bee.blockPosition();
        }
        
        return FlowerSearchHelper.findClosestFlower(level, searchCenter, range, flowerTag);
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
        BlockPos hivePos = bee.getAssignedHivePos();
        if (hivePos != null && bee.level().getBlockEntity(hivePos) instanceof MagicHiveBlockEntity hive) {
            hive.returnFlower(bee.getAssignedSlot(), flower);
        }
    }
    
    @Override
    public void stop() {
        // Return current target flower to hive if we're stopping
        if (targetFlower != null) {
            returnFlowerToHive(targetFlower);
        }
        
        bee.setDeltaMovement(Vec3.ZERO);
        state = BeeActivityState.IDLE;
        targetFlower = null;
        workTimer = 0;
    }
    
    public BeeActivityState getState() {
        return state;
    }
}
