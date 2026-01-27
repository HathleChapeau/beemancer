/**
 * ============================================================
 * [RideableBeeEntity.java]
 * Description: Entité abeille chevauchable avec modes WALK/RUN
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon 1.7 HorseBehaviour.kt
 * - Vélocité en espace local (Z = avant)
 * - Input via driver.zza/xxa
 * - Rotation basée sur la différence de yaw player/entity
 * - Gravité: (9.8 / 20.0) * 0.2 * 0.6
 * - Friction au sol: 0.03 par tick
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Abeille géante chevauchable.
 * - Mode WALK: mouvement libre, rotation basée sur caméra joueur
 * - Mode RUN: vélocité avec inertie, rotation progressive (style Cobblemon)
 * Étend Bee uniquement pour réutiliser BeeModel.
 */
public class RideableBeeEntity extends Bee implements PlayerRideable {

    // Copié de Cobblemon HorseBehaviour.kt ligne 340
    private static final double GRAVITY = (9.8 / 20.0) * 0.2 * 0.6;
    private static final double TERMINAL_VELOCITY = 2.0;
    // Copié de Cobblemon HorseBehaviour.kt ligne 352
    private static final double GROUND_FRICTION = 0.03;

    // --- Entity Data (synced client-server) ---
    private static final EntityDataAccessor<Byte> DATA_RIDING_MODE = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_CURRENT_SPEED = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.FLOAT);

    // --- Vélocité en espace local (pattern Cobblemon) ---
    // Z = avant, X = côté, Y = vertical
    private Vec3 rideVelocity = Vec3.ZERO;

    // --- État sprint (pattern Cobblemon HorseState) ---
    private boolean sprinting = false;
    private int jumpTicks = 0;

    // --- Settings ---
    private RidingSettings settings;

    public RideableBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.settings = RidingSettingsLoader.getSettings();
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, RidingSettings.DEFAULT.health())
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    // --- Entity Data ---

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RIDING_MODE, RidingMode.WALK.toByte());
        builder.define(DATA_CURRENT_SPEED, 0f);
    }

    public RidingMode getRidingMode() {
        return RidingMode.fromByte(this.entityData.get(DATA_RIDING_MODE));
    }

    public void setRidingMode(RidingMode mode) {
        this.entityData.set(DATA_RIDING_MODE, mode.toByte());
    }

    public float getSyncedSpeed() {
        return this.entityData.get(DATA_CURRENT_SPEED);
    }

    // --- Mounting ---

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof Player ? (LivingEntity) passenger : null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player) {
            rideVelocity = Vec3.ZERO;
            sprinting = false;
            jumpTicks = 0;
            setRidingMode(RidingMode.WALK);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player) {
            rideVelocity = Vec3.ZERO;
            sprinting = false;
            jumpTicks = 0;
            setRidingMode(RidingMode.WALK);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() * 0.75, 0);
    }

    // --- Control ---

    /**
     * Indique que le client local contrôle cette entité quand montée.
     * Cela permet à Minecraft de synchroniser les inputs correctement.
     */
    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger instanceof Player player) {
            return player.isLocalPlayer();
        }
        return false;
    }

    // --- Movement (pattern Cobblemon HorseBehaviour.kt) ---

    /**
     * travelVector contient les inputs synchronisés:
     * - x = strafe (A/D)
     * - z = forward (W/S)
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.getControllingPassenger() instanceof Player driver) {
            // === JOUEUR MONTE: contrôle du mouvement ===

            // Extraire les inputs depuis travelVector (synchronisé par Minecraft)
            float forward = (float) travelVector.z;
            float strafe = (float) travelVector.x;

            // Gestion du sprint
            handleSprinting(driver, forward);

            // Gestion des modes WALK/RUN
            if (getRidingMode() == RidingMode.WALK) {
                handleWalkMode(driver, forward, strafe);
            } else {
                handleRunMode(driver, forward, strafe);
            }

            // Convertir vélocité locale en vélocité monde
            Vec3 worldVelocity = localToWorldVelocity(rideVelocity);
            this.setDeltaMovement(worldVelocity);

            // Appliquer le mouvement
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Sync speed pour le client (FOV effect)
            this.entityData.set(DATA_CURRENT_SPEED, (float) rideVelocity.z);

        } else {
            // === PAS DE PASSAGER: juste gravité ===
            Vec3 motion = this.getDeltaMovement();
            if (!this.onGround()) {
                motion = new Vec3(motion.x, Math.max(motion.y - GRAVITY, -TERMINAL_VELOCITY), motion.z);
            }
            this.setDeltaMovement(motion.multiply(0.9, 0.98, 0.9));
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    /**
     * Gestion du sprint (copié de Cobblemon HorseBehaviour.kt ligne 141-159)
     */
    private void handleSprinting(Player driver, float forward) {
        boolean tryingToSprint = driver.isSprinting() && forward > 0;

        // Si pas d'input forward, arrêter le sprint
        if (forward <= 0) {
            sprinting = false;
        }

        if (tryingToSprint && !sprinting) {
            sprinting = true;
        }
    }

    /**
     * Mode WALK: mouvement libre dans toutes les directions.
     * La rotation est basée sur la direction de mouvement.
     */
    private void handleWalkMode(Player driver, float forward, float strafe) {
        boolean jump = driver.jumping;

        // Transition vers RUN si sprint + avance
        if (sprinting && forward > 0.5f) {
            setRidingMode(RidingMode.RUN);
            rideVelocity = new Vec3(0, rideVelocity.y, settings.walkSpeed());
            return;
        }

        // Calculer vélocité en espace monde basé sur la caméra du joueur
        float cameraYaw = driver.getYRot();
        float yawRad = (float) Math.toRadians(-cameraYaw);

        double moveX = (strafe * Math.cos(yawRad) - forward * Math.sin(yawRad)) * settings.walkSpeed();
        double moveZ = (strafe * Math.sin(yawRad) + forward * Math.cos(yawRad)) * settings.walkSpeed();

        // Gravité (Cobblemon ligne 339-345)
        double velY = rideVelocity.y;
        if (!this.onGround() && jumpTicks <= 0) {
            velY = Math.max(velY - GRAVITY, -TERMINAL_VELOCITY);
        } else if (this.onGround()) {
            velY = 0;
        }

        // Saut
        if (jump && this.onGround() && jumpTicks >= 0) {
            velY = settings.walkJumpStrength();
            jumpTicks = -5; // Cooldown
        } else if (this.onGround() && jumpTicks < 0) {
            jumpTicks++;
        }

        // En mode WALK, on utilise directement les coordonnées monde
        // car on veut que le mouvement soit relatif à la caméra
        rideVelocity = new Vec3(moveX, velY, moveZ);

        // Tourner l'abeille vers la direction de mouvement
        if (Math.abs(moveX) > 0.01 || Math.abs(moveZ) > 0.01) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
            this.setYRot(targetYaw);
            this.yBodyRot = targetYaw;
            this.yHeadRot = targetYaw;
        }
    }

    /**
     * Mode RUN: style Jet de Cobblemon mais au sol.
     * - A/D (strafe) contrôle directement le yaw (JetBehaviour ligne 290)
     * - W accélère, S décélère (JetBehaviour lignes 232-255)
     * - Vitesse minimum maintenue (pas d'arrêt complet en RUN)
     * - Gravité et friction au sol
     */
    private void handleRunMode(Player driver, float forward, float strafe) {
        boolean jump = driver.jumping;

        // === Paramètres ===
        double topSpeed = settings.maxRunSpeed();
        double minSpeed = settings.walkSpeed() * 0.5; // Vitesse minimum en RUN
        double accel = topSpeed / (settings.acceleration() * 20.0);
        double deccel = accel * 0.5; // Décélération = moitié de l'accélération (JetBehaviour ligne 243)
        // Handling: degrés par seconde pour la rotation (JetBehaviour ligne 281)
        double handlingYaw = 180.0;

        // === Copie de la vélocité précédente ===
        Vec3 newVelocity = rideVelocity;
        double speed = newVelocity.z;

        // === Collision horizontale ===
        if (this.horizontalCollision) {
            newVelocity = newVelocity.normalize().scale(this.getDeltaMovement().length());
            speed = newVelocity.z;
        }

        // === Vitesse (JetBehaviour lignes 232-255) ===
        // W accélère, S décélère, vitesse minimum maintenue
        if (speed < minSpeed) {
            // Toujours accélérer vers minSpeed (JetBehaviour ligne 232)
            double accelMod = Math.max(1.0 - scaleToRange(speed, minSpeed, topSpeed), 0.0);
            speed = Math.min(speed + (accel * accelMod), topSpeed);
        } else if (forward > 0 && speed < topSpeed) {
            // W: accélérer vers topSpeed (JetBehaviour lignes 233-241)
            double accelMod = Math.max(1.0 - scaleToRange(speed, minSpeed, topSpeed), 0.0);
            speed = Math.min(speed + (accel * accelMod), topSpeed);
        } else if (forward < 0 && speed > minSpeed) {
            // S: décélérer vers minSpeed (JetBehaviour lignes 242-250)
            speed = Math.max(speed - deccel, minSpeed);
        } else if (speed > topSpeed) {
            // Au-dessus de topSpeed: friction (JetBehaviour lignes 251-255)
            speed *= 0.98;
        }

        newVelocity = new Vec3(0, newVelocity.y, speed);

        // === Rotation avec A/D (JetBehaviour angRollVel lignes 269-293) ===
        // val yawForce = driver.xxa * handlingYaw * -1 (ligne 290)
        float yawForce = (float) (strafe * (handlingYaw / 20.0) * -1.0);
        float newYaw = this.getYRot() + yawForce;
        this.setYRot(newYaw);
        this.yBodyRot = newYaw;
        this.yHeadRot = newYaw;

        // === Gravité ===
        if (!this.onGround() && jumpTicks <= 0) {
            newVelocity = new Vec3(newVelocity.x, Math.max(newVelocity.y - GRAVITY, -TERMINAL_VELOCITY), newVelocity.z);
        } else if (this.onGround()) {
            newVelocity = new Vec3(newVelocity.x, 0, newVelocity.z);
        }

        // === Saut/Leap ===
        if (jumpTicks > 0 || (jumpTicks >= 0 && jump && this.onGround() && this.getDeltaMovement().y <= 0.1)) {
            int jumpInputTicks = 6;
            if (jump && jumpTicks >= 0 && jumpTicks < jumpInputTicks) {
                double appliedJumpForce = (settings.runLeapForce() * 1.5) / jumpInputTicks;
                newVelocity = new Vec3(newVelocity.x, newVelocity.y + appliedJumpForce, newVelocity.z);
                jumpTicks++;
            } else {
                jumpTicks = -3;
            }
        } else if (this.onGround() && jumpTicks < 0) {
            jumpTicks++;
        }

        // === Transition vers WALK si relâche sprint et vitesse basse ===
        if (!sprinting && speed <= settings.runToWalkThreshold()) {
            setRidingMode(RidingMode.WALK);
        }

        rideVelocity = newVelocity;
    }

    /**
     * Utilitaire (copié de Cobblemon RidingBehaviour.kt ligne 140-141)
     */
    private double scaleToRange(double x, double min, double max) {
        if ((max - min) < 0.01) return 0.0;
        return Mth.clamp((x - min) / (max - min), 0.0, 1.0);
    }

    /**
     * Convertit la vélocité locale (Z = avant) en vélocité monde.
     */
    private Vec3 localToWorldVelocity(Vec3 localVel) {
        if (getRidingMode() == RidingMode.WALK) {
            // En mode WALK, la vélocité est déjà en coordonnées monde
            return localVel;
        }
        // En mode RUN, convertir de local à monde
        float yawRad = (float) Math.toRadians(-this.getYRot());
        double worldX = localVel.z * Math.sin(yawRad);
        double worldZ = localVel.z * Math.cos(yawRad);
        return new Vec3(worldX, localVel.y, worldZ);
    }

    // --- Override Bee behavior ---

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    public void tick() {
        if (this.getControllingPassenger() != null) {
            this.baseTick();
            this.aiStep();
        } else {
            super.tick();
        }
    }

    @Override
    public void aiStep() {
        if (this.getControllingPassenger() == null) {
            super.aiStep();
        } else {
            this.updateSwingTime();
        }
    }

    // --- Save/Load ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    // --- Misc ---

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // --- Getters ---

    public float getVelocityForward() {
        return (float) rideVelocity.z;
    }

    public RidingSettings getSettings() {
        return settings;
    }
}
