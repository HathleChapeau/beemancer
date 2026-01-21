/**
 * ============================================================
 * [MagicBeeEntity.java]
 * Description: Entité abeille magique avec système de gènes
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class MagicBeeEntity extends Bee {
    private static final EntityDataAccessor<String> DATA_SPECIES = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ENVIRONMENT = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FLOWER = SynchedEntityData.defineId(
            MagicBeeEntity.class, EntityDataSerializers.STRING);

    private final BeeGeneData geneData = new BeeGeneData();
    @Nullable
    private BlockPos targetPos = null;

    public MagicBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIES, "common");
        builder.define(DATA_ENVIRONMENT, "normal");
        builder.define(DATA_FLOWER, "flowers");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MoveToTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
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
        }
    }

    private void loadGenesFromData() {
        Gene species = GeneRegistry.getGene(GeneCategory.SPECIES, entityData.get(DATA_SPECIES));
        Gene environment = GeneRegistry.getGene(GeneCategory.ENVIRONMENT, entityData.get(DATA_ENVIRONMENT));
        Gene flower = GeneRegistry.getGene(GeneCategory.FLOWER, entityData.get(DATA_FLOWER));
        
        if (species != null) geneData.setGene(species);
        if (environment != null) geneData.setGene(environment);
        if (flower != null) geneData.setGene(flower);
    }

    @Override
    public void tick() {
        super.tick();
        // Apply gene behaviors
        for (Gene gene : geneData.getAllGenes()) {
            gene.applyBehavior(this);
        }
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GeneData")) {
            geneData.load(tag.getCompound("GeneData"));
            // Sync to entity data
            for (Gene gene : geneData.getAllGenes()) {
                syncGeneToData(gene);
            }
        }
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == DATA_SPECIES || key == DATA_ENVIRONMENT || key == DATA_FLOWER) {
            loadGenesFromData();
        }
    }

    // --- Movement Goal ---

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
