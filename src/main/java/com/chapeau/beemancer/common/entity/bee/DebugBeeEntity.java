/**
 * ============================================================
 * [DebugBeeEntity.java]
 * Description: Entité abeille de debug avec stats HP/Speed/Strength
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | Bee                 | Classe parente       | Modèle et animations|
 * | BeemancerEntities   | Type d'entité        | Enregistrement      |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - BeeDebugItem.java (spawn et pickup)
 * - BeeWandItem.java (sélection et navigation)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

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

public class DebugBeeEntity extends Bee {
    // Stats synchronisées client/serveur
    private static final EntityDataAccessor<Integer> DATA_HP = SynchedEntityData.defineId(
            DebugBeeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPEED = SynchedEntityData.defineId(
            DebugBeeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRENGTH = SynchedEntityData.defineId(
            DebugBeeEntity.class, EntityDataSerializers.INT);

    // Position cible pour la navigation
    @Nullable
    private BlockPos targetPos = null;

    public DebugBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HP, 1);
        builder.define(DATA_SPEED, 1);
        builder.define(DATA_STRENGTH, 1);
    }

    @Override
    protected void registerGoals() {
        // On ne garde que notre goal de navigation personnalisé
        this.goalSelector.addGoal(1, new MoveToTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    // --- Stats Getters/Setters ---

    public int getBeeHp() {
        return this.entityData.get(DATA_HP);
    }

    public void setBeeHp(int hp) {
        this.entityData.set(DATA_HP, hp);
    }

    public int getBeeSpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    public void setBeeSpeed(int speed) {
        this.entityData.set(DATA_SPEED, speed);
    }

    public int getBeeStrength() {
        return this.entityData.get(DATA_STRENGTH);
    }

    public void setBeeStrength(int strength) {
        this.entityData.set(DATA_STRENGTH, strength);
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

    // --- NBT Serialization ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BeeHp", getBeeHp());
        tag.putInt("BeeSpeed", getBeeSpeed());
        tag.putInt("BeeStrength", getBeeStrength());
        
        if (targetPos != null) {
            tag.putInt("TargetX", targetPos.getX());
            tag.putInt("TargetY", targetPos.getY());
            tag.putInt("TargetZ", targetPos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BeeHp")) setBeeHp(tag.getInt("BeeHp"));
        if (tag.contains("BeeSpeed")) setBeeSpeed(tag.getInt("BeeSpeed"));
        if (tag.contains("BeeStrength")) setBeeStrength(tag.getInt("BeeStrength"));
        
        if (tag.contains("TargetX")) {
            targetPos = new BlockPos(
                    tag.getInt("TargetX"),
                    tag.getInt("TargetY"),
                    tag.getInt("TargetZ"));
        }
    }

    // --- Goal de navigation personnalisé ---

    private static class MoveToTargetGoal extends Goal {
        private final DebugBeeEntity bee;
        private static final double REACH_DISTANCE = 1.5;

        public MoveToTargetGoal(DebugBeeEntity bee) {
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
            
            // Arrêter si on est assez proche
            double distance = bee.position().distanceTo(Vec3.atCenterOf(target));
            return distance > REACH_DISTANCE;
        }

        @Override
        public void tick() {
            BlockPos target = bee.getTargetPos();
            if (target == null) return;

            Vec3 targetVec = Vec3.atCenterOf(target);
            Vec3 direction = targetVec.subtract(bee.position()).normalize();
            
            // Vitesse basée sur le stat speed
            double speed = 0.05 * bee.getBeeSpeed();
            
            Vec3 movement = direction.scale(speed);
            bee.setDeltaMovement(movement);
            
            // Orienter l'abeille vers la cible
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
