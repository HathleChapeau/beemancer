/**
 * ============================================================
 * [MagicBeeEntity.java]
 * Description: Entité abeille magique avec système de gènes, lifetime et comportements
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeGeneData         | Stockage gènes       | Gestion des gènes de l'abeille |
 * | GeneRegistry        | Registre gènes       | Lookup des gènes               |
 * | BeeBehaviorConfig   | Configuration        | Paramètres de comportement     |
 * | BeeBehaviorManager  | Gestionnaire config  | Récupération config par espèce |
 * | MagicHiveBlockEntity| Ruche                | Notification et interaction    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicBeeItem.java: Création/capture d'abeilles
 * - MagicHiveBlockEntity.java: Gestion des abeilles
 * - Goals: AI de l'abeille
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.entity.bee.goal.BeeRevengeGoal;
import com.chapeau.beemancer.common.entity.bee.goal.ForagingBehaviorGoal;
import com.chapeau.beemancer.common.entity.bee.goal.HarvestingBehaviorGoal;
import com.chapeau.beemancer.common.entity.bee.goal.ReturnToHiveWhenLowHealthGoal;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MagicBeeEntity extends Bee {
    
    // --- Entity Data Accessors ---
    private static final EntityDataAccessor<String> DATA_SPECIES = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ENVIRONMENT = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FLOWER = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_LIFETIME_GENE = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_LIFETIME = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.INT);
    
    // Nouveaux EntityData pour le comportement
    private static final EntityDataAccessor<Boolean> DATA_POLLINATED = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.BOOLEAN);
    
    // --- Gene Data ---
    private final BeeGeneData geneData = new BeeGeneData();
    
    // --- Navigation ---
    @Nullable
    private BlockPos targetPos = null;
    
    // --- Hive Assignment ---
    @Nullable
    private BlockPos assignedHivePos = null;
    private int assignedSlot = -1;
    
    // --- State Flags ---
    private boolean notifiedHiveOfRemoval = false;
    
    // --- Health tracking for items ---
    private float storedHealth = -1; // -1 = use max health

    public MagicBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIES, "common");
        builder.define(DATA_ENVIRONMENT, "normal");
        builder.define(DATA_FLOWER, "flowers");
        builder.define(DATA_LIFETIME_GENE, "normal");
        builder.define(DATA_REMAINING_LIFETIME, 24000);
        builder.define(DATA_POLLINATED, false);
        builder.define(DATA_ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        // Priorité 1: Fuite si vie < 30%
        this.goalSelector.addGoal(1, new ReturnToHiveWhenLowHealthGoal(this));
        
        // Priorité 2: Vengeance si attaqué
        this.targetSelector.addGoal(2, new BeeRevengeGoal(this));
        
        // Priorité 3: Comportement de travail (butinage ou récolte)
        this.goalSelector.addGoal(3, new ForagingBehaviorGoal(this));
        this.goalSelector.addGoal(3, new HarvestingBehaviorGoal(this));
        
        // Priorité 4: Navigation manuelle (BeeWand)
        this.goalSelector.addGoal(4, new MoveToTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    // --- Gene System ---

    public BeeGeneData getGeneData() {
        return geneData;
    }

    public void setGene(Gene gene) {
        if (geneData.setGene(gene)) {
            syncGeneToData(gene);
        }
    }

    private void syncGeneToData(Gene gene) {
        GeneCategory cat = gene.getCategory();
        if (cat == GeneCategory.SPECIES) {
            this.entityData.set(DATA_SPECIES, gene.getId());
        } else if (cat == GeneCategory.ENVIRONMENT) {
            this.entityData.set(DATA_ENVIRONMENT, gene.getId());
        } else if (cat == GeneCategory.FLOWER) {
            this.entityData.set(DATA_FLOWER, gene.getId());
        } else if (cat == GeneCategory.LIFETIME) {
            this.entityData.set(DATA_LIFETIME_GENE, gene.getId());
        }
    }

    private void loadGenesFromData() {
        Gene species = GeneRegistry.getGene(GeneCategory.SPECIES, entityData.get(DATA_SPECIES));
        Gene environment = GeneRegistry.getGene(GeneCategory.ENVIRONMENT, entityData.get(DATA_ENVIRONMENT));
        Gene flower = GeneRegistry.getGene(GeneCategory.FLOWER, entityData.get(DATA_FLOWER));
        Gene lifetime = GeneRegistry.getGene(GeneCategory.LIFETIME, entityData.get(DATA_LIFETIME_GENE));
        
        if (species != null) geneData.setGene(species);
        if (environment != null) geneData.setGene(environment);
        if (flower != null) geneData.setGene(flower);
        if (lifetime != null) geneData.setGene(lifetime);
    }
    
    /**
     * Récupère l'ID de l'espèce actuelle.
     */
    public String getSpeciesId() {
        return entityData.get(DATA_SPECIES);
    }

    // --- Behavior Config ---
    
    /**
     * Récupère la configuration de comportement pour cette espèce.
     */
    public BeeBehaviorConfig getBehaviorConfig() {
        return BeeBehaviorManager.getConfig(getSpeciesId());
    }

    // --- Pollination State ---
    
    public boolean isPollinated() {
        return entityData.get(DATA_POLLINATED);
    }
    
    public void setPollinated(boolean pollinated) {
        entityData.set(DATA_POLLINATED, pollinated);
    }
    
    // --- Combat State ---
    
    public boolean isEnraged() {
        return entityData.get(DATA_ENRAGED);
    }
    
    public void setEnraged(boolean enraged) {
        entityData.set(DATA_ENRAGED, enraged);
    }
    
    /**
     * Pourcentage de vie actuel (0.0 à 1.0).
     */
    public float getHealthPercentage() {
        return getHealth() / getMaxHealth();
    }
    
    /**
     * Vérifie si l'abeille doit fuir (vie < 30%).
     */
    public boolean shouldFlee() {
        return getHealthPercentage() < 0.3f;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        
        if (hurt && !level().isClientSide()) {
            // Devenir enragée si attaquée par une entité vivante
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity) {
                setEnraged(true);
            }
        }
        
        return hurt;
    }

    // --- Lifetime System ---

    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide() && tickCount % 20 == 0) {
            // Décrémente le lifetime chaque seconde
            boolean alive = geneData.decrementLifetime(20);
            entityData.set(DATA_REMAINING_LIFETIME, geneData.getRemainingLifetime());
            
            if (!alive) {
                this.discard();
                return;
            }
        }
        
        // Apply gene behaviors
        for (Gene gene : geneData.getAllGenes()) {
            gene.applyBehavior(this);
        }
    }

    public int getRemainingLifetime() {
        return entityData.get(DATA_REMAINING_LIFETIME);
    }

    public int getMaxLifetime() {
        return geneData.getMaxLifetime();
    }

    public float getLifetimeRatio() {
        int max = getMaxLifetime();
        if (max <= 0) return 1.0f;
        return (float) getRemainingLifetime() / max;
    }
    
    // --- Stored Health (for item capture) ---
    
    public float getStoredHealth() {
        return storedHealth >= 0 ? storedHealth : getHealth();
    }
    
    public void setStoredHealth(float health) {
        this.storedHealth = health;
        if (health > 0 && health <= getMaxHealth()) {
            setHealth(health);
        }
    }

    // --- Removal / Death ---

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide() && hasAssignedHive() && !notifiedHiveOfRemoval) {
            notifiedHiveOfRemoval = true;
            notifyHiveOfDeath();
        }
        super.remove(reason);
    }

    private void notifyHiveOfDeath() {
        if (assignedHivePos == null) return;
        
        if (level().getBlockEntity(assignedHivePos) instanceof MagicHiveBlockEntity hive) {
            hive.onBeeKilled(getUUID());
        }
    }

    public void markAsEnteredHive() {
        notifiedHiveOfRemoval = true;
    }

    // --- Hive Assignment ---

    public boolean hasAssignedHive() {
        return assignedHivePos != null && assignedSlot >= 0;
    }

    @Nullable
    public BlockPos getAssignedHivePos() {
        return assignedHivePos;
    }

    public int getAssignedSlot() {
        return assignedSlot;
    }

    public void setAssignedHive(@Nullable BlockPos hivePos, int slot) {
        this.assignedHivePos = hivePos;
        this.assignedSlot = slot;
    }

    public void clearAssignedHive() {
        this.assignedHivePos = null;
        this.assignedSlot = -1;
    }

    // --- Navigation ---

    @Nullable
    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos pos) {
        this.targetPos = pos;
    }

    public boolean hasTarget() {
        return targetPos != null;
    }

    public void clearTarget() {
        this.targetPos = null;
    }

    // --- NBT ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("GeneData", geneData.save());
        
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
        
        if (assignedHivePos != null) {
            tag.putInt("HiveX", assignedHivePos.getX());
            tag.putInt("HiveY", assignedHivePos.getY());
            tag.putInt("HiveZ", assignedHivePos.getZ());
            tag.putInt("HiveSlot", assignedSlot);
        }
        
        // Sauvegarder l'état de comportement
        tag.putBoolean("Pollinated", isPollinated());
        tag.putBoolean("Enraged", isEnraged());
        tag.putFloat("StoredHealth", getHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("GeneData")) {
            geneData.load(tag.getCompound("GeneData"));
            for (Gene gene : geneData.getAllGenes()) {
                syncGeneToData(gene);
            }
            entityData.set(DATA_REMAINING_LIFETIME, geneData.getRemainingLifetime());
        }
        
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
        
        if (tag.contains("HiveX")) {
            assignedHivePos = new BlockPos(tag.getInt("HiveX"), tag.getInt("HiveY"), tag.getInt("HiveZ"));
            assignedSlot = tag.getInt("HiveSlot");
        }
        
        if (tag.contains("Pollinated")) {
            setPollinated(tag.getBoolean("Pollinated"));
        }
        if (tag.contains("Enraged")) {
            setEnraged(tag.getBoolean("Enraged"));
        }
        if (tag.contains("StoredHealth")) {
            storedHealth = tag.getFloat("StoredHealth");
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == DATA_SPECIES || key == DATA_ENVIRONMENT || key == DATA_FLOWER || key == DATA_LIFETIME_GENE) {
            loadGenesFromData();
        }
    }

    // --- Movement Goal (manual navigation via BeeWand) ---

    private static class MoveToTargetGoal extends Goal {
        private final MagicBeeEntity bee;
        private static final double REACH_DISTANCE = 1.5;

        public MoveToTargetGoal(MagicBeeEntity bee) {
            this.bee = bee;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bee.hasTarget();
        }

        @Override
        public boolean canContinueToUse() {
            if (!bee.hasTarget()) return false;
            BlockPos target = bee.getTargetPos();
            if (target == null) return false;
            double distance = bee.position().distanceTo(Vec3.atCenterOf(target));
            return distance > REACH_DISTANCE;
        }

        @Override
        public void tick() {
            BlockPos target = bee.getTargetPos();
            if (target == null) return;

            Vec3 targetVec = Vec3.atCenterOf(target);
            Vec3 direction = targetVec.subtract(bee.position()).normalize();
            double speed = 0.08;
            
            Vec3 movement = direction.scale(speed);
            bee.setDeltaMovement(movement);
            
            double dx = targetVec.x - bee.getX();
            double dz = targetVec.z - bee.getZ();
            float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            bee.setYRot(targetYaw);
            bee.yBodyRot = targetYaw;
        }

        @Override
        public void stop() {
            bee.setDeltaMovement(Vec3.ZERO);
        }
    }
}
