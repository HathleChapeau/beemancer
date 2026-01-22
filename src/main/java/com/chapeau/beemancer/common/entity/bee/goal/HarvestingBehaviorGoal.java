/**
 * ============================================================
 * [HarvestingBehaviorGoal.java]
 * Description: Goal de récolte complet avec machine à états et logique par type de bloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Accès aux méthodes             |
 * | BeeInventory        | Inventaire interne   | Stockage des récoltes          |
 * | BeeBehaviorConfig   | Configuration        | Paramètres de récolte          |
 * | BeeBehaviorType     | Type comportement    | Vérification HARVESTER         |
 * | HarvestHelper       | Logique récolte      | Récolte par type de bloc       |
 * | MagicHiveBlockEntity| Ruche                | Liste des fleurs               |
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
import com.chapeau.beemancer.common.entity.bee.BeeInventory;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorType;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.util.FlowerSearchHelper;
import com.chapeau.beemancer.core.util.HarvestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal de priorité 3: Comportement de récolte complet.
 * Machine à états: IDLE → SEEKING_FLOWER → WORKING → (continue ou RETURNING)
 * 
 * Comportement:
 * - Récupère les fleurs disponibles depuis la ruche
 * - Se déplace vers une fleur
 * - Attend que la fleur soit mature (pour crops/nether wart)
 * - Récolte et stocke dans l'inventaire
 * - Continue vers d'autres fleurs jusqu'au seuil de retour
 * - Retourne à la ruche pour déposer
 */
public class HarvestingBehaviorGoal extends Goal {
    
    private static final double REACH_DISTANCE = 1.5;
    private static final double FLIGHT_SPEED_FACTOR = 0.1;
    private static final int WAIT_MATURE_CHECK_INTERVAL = 20; // Check every second
    
    private final MagicBeeEntity bee;
    
    private BeeActivityState state = BeeActivityState.IDLE;
    private BlockPos targetFlower = null;
    private int workTimer = 0;
    private int waitMatureTimer = 0;
    
    public HarvestingBehaviorGoal(MagicBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Vérifier les conditions de base
        if (!bee.hasAssignedHive()) return false;
        if (bee.shouldFlee()) return false;
        if (bee.isEnraged()) return false;
        
        // Vérifier que c'est une abeille récolteuse
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        if (config.getBehaviorType() != BeeBehaviorType.HARVESTER) return false;
        
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
        waitMatureTimer = 0;
    }
    
    @Override
    public void tick() {
        // Check if should return due to inventory threshold
        if (bee.shouldReturnDueToInventory()) {
            state = BeeActivityState.RETURNING;
                bee.setReturning(true);
        }
        
        // Check if inventory is full
        BeeInventory inventory = bee.getInventory();
        if (inventory != null && inventory.isFull()) {
            state = BeeActivityState.RETURNING;
                bee.setReturning(true);
        }
        
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
            // Return the flower to hive (it's invalid now)
            returnFlowerToHive(targetFlower);
            targetFlower = null;
            return;
        }
        
        // Se diriger vers la fleur
        double distance = bee.position().distanceTo(Vec3.atCenterOf(targetFlower));
        if (distance <= REACH_DISTANCE) {
            // Arrivé sur la fleur
            HarvestHelper.HarvestType harvestType = HarvestHelper.getHarvestType(bee.level(), targetFlower);
            
            // Check if trees (skip for now)
            if (harvestType == HarvestHelper.HarvestType.TREE) {
                targetFlower = null;
                return;
            }
            
            // Check if unsupported
            if (harvestType == HarvestHelper.HarvestType.UNSUPPORTED) {
                targetFlower = null;
                return;
            }
            
            // Check if ready to harvest
            if (HarvestHelper.isReadyToHarvest(bee.level(), targetFlower)) {
                state = BeeActivityState.WORKING;
                workTimer = bee.getBehaviorConfig().getHarvestingDuration();
            } else {
                // Wait for crop to mature
                waitMatureTimer = WAIT_MATURE_CHECK_INTERVAL;
            }
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
            // Perform harvest
            performHarvest();
            
            // Look for next flower or return
            targetFlower = null;
            state = BeeActivityState.SEEKING_FLOWER;
        }
    }
    
    private void performHarvest() {
        if (targetFlower == null) return;
        
        BeeInventory inventory = bee.getInventory();
        if (inventory == null) return;
        
        // Harvest the block
        List<ItemStack> drops = HarvestHelper.harvest(bee.level(), targetFlower);
        
        // Add drops to inventory
        for (ItemStack drop : drops) {
            ItemStack remainder = inventory.addItem(drop);
            // If inventory full, drops are lost (bee can't carry more)
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
        
        // Use effective speed (includes inventory weight penalty)
        double speed = bee.getEffectiveFlyingSpeed() * FLIGHT_SPEED_FACTOR;
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
            if (flower != null && HarvestHelper.getHarvestType(bee.level(), flower) != HarvestHelper.HarvestType.TREE) {
                return flower;
            }
        }
        
        // Fallback: search nearby (uses gene's flower tag)
        return findNearbyFlower();
    }
    
    private BlockPos findNearbyFlower() {
        Gene flowerGene = bee.getGeneData().getGene(GeneCategory.FLOWER);
        if (!(flowerGene instanceof FlowerGene flower)) {
            return null;
        }
        
        TagKey<Block> flowerTag = flower.getFlowerTag();
        if (flowerTag == null) return null;
        
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        int range = config.getAreaOfEffect();
        Level level = bee.level();
        
        // Search around hive
        BlockPos searchCenter = bee.getAssignedHivePos();
        if (searchCenter == null) {
            searchCenter = bee.blockPosition();
        }
        
        // Find closest valid flower (not tree)
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        
        List<BlockPos> flowers = FlowerSearchHelper.findAllFlowers(level, searchCenter, range, flowerTag);
        for (BlockPos pos : flowers) {
            HarvestHelper.HarvestType type = HarvestHelper.getHarvestType(level, pos);
            if (type == HarvestHelper.HarvestType.TREE || type == HarvestHelper.HarvestType.UNSUPPORTED) {
                continue;
            }
            
            double dist = pos.distSqr(bee.blockPosition());
            if (dist < closestDist) {
                closestDist = dist;
                closest = pos;
            }
        }
        
        return closest;
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
        waitMatureTimer = 0;
    }
    
    public BeeActivityState getState() {
        return state;
    }
}
