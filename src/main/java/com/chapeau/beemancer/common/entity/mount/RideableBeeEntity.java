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

    // --- Movement (pattern Cobblemon HorseBehaviour.kt) ---

    @Override
    public void travel(Vec3 travelVector) {
        if (this.getControllingPassenger() instanceof Player driver) {
            // === JOUEUR MONTE: contrôle du mouvement ===

            // Gestion du sprint (Cobblemon handleSprinting ligne 141-159)
            handleSprinting(driver);

            // Gestion des modes WALK/RUN
            if (getRidingMode() == RidingMode.WALK) {
                handleWalkMode(driver);
            } else {
                handleRunMode(driver);
            }

            // Convertir vélocité locale en vélocité monde
            // (Cobblemon fait ça dans le mixin, on le fait ici)
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
    private void handleSprinting(Player driver) {
        boolean tryingToSprint = driver.isSprinting() && driver.zza > 0;

        if (!driver.zza.equals(0f) || driver.zza <= 0) {
            // Si pas d'input forward, arrêter le sprint
            if (driver.zza <= 0) {
                sprinting = false;
            }
        }

        if (tryingToSprint && !sprinting) {
            sprinting = true;
        }
    }

    /**
     * Mode WALK: mouvement libre dans toutes les directions.
     * La rotation est basée sur la direction de mouvement.
     */
    private void handleWalkMode(Player driver) {
        float forward = driver.zza;
        float strafe = driver.xxa;
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
     * Mode RUN: vélocité avec inertie (copié de Cobblemon HorseBehaviour.kt)
     * calculateRideSpaceVel() lignes 264-380
     */
    private void handleRunMode(Player driver) {
        float forward = driver.zza;
        float strafe = driver.xxa;
        boolean jump = driver.jumping;

        // === Paramètres (Cobblemon utilise des expressions MoLang, on simplifie) ===
        double topSpeed = sprinting ? settings.maxRunSpeed() : settings.walkSpeed();
        double accel = topSpeed / (settings.acceleration() * 20.0); // Cobblemon ligne 276
        double handling = 180.0; // Degrés par seconde de rotation max
        float lookYawLimit = 90.0f; // Limite de rotation

        // === Copie de la vélocité précédente ===
        Vec3 newVelocity = rideVelocity;

        // === Collision horizontale (Cobblemon ligne 298-300) ===
        if (this.horizontalCollision) {
            newVelocity = newVelocity.normalize().scale(this.getDeltaMovement().length());
        }

        // === Input Forward (Cobblemon ligne 305-334) ===
        boolean activeInput = false;
        if (forward != 0.0f) {
            // Calcul du turn speed limit (Cobblemon ligne 309-313)
            float percOfMaxTurnSpeed = Math.abs(Mth.wrapDegrees(driver.getYRot() - this.getYRot()) / lookYawLimit) * 100.0f;
            float turnPercThresh = 0.0f;
            float s = Math.min((float) Math.pow((percOfMaxTurnSpeed - turnPercThresh) / (100.0f - turnPercThresh), 1), 1.0f);
            double effectiveTopSpeed = percOfMaxTurnSpeed > turnPercThresh ? topSpeed / Math.max(2.0f * s, 1.0f) : topSpeed;

            // Input (Cobblemon ligne 317-321)
            double forwardInput;
            if (forward > 0 && newVelocity.z > effectiveTopSpeed) {
                forwardInput = 0.0;
            } else if (forward < 0 && newVelocity.z < (-effectiveTopSpeed / 3.0)) {
                forwardInput = 0.0;
            } else {
                forwardInput = Math.signum(forward);
            }

            // Extra friction si on tourne (Cobblemon ligne 324-326)
            double turningSlowDown = s * 0.1;
            if (newVelocity.z > effectiveTopSpeed) {
                newVelocity = newVelocity.subtract(0, 0, Math.min(turningSlowDown * Math.signum(newVelocity.z), newVelocity.z));
            }

            // Appliquer accélération (Cobblemon ligne 328-331)
            newVelocity = new Vec3(newVelocity.x, newVelocity.y, newVelocity.z + (accel * forwardInput));
            activeInput = true;
        }

        // === Input Strafe pour la rotation ===
        if (Math.abs(strafe) > 0.1f) {
            // Strafe influence la rotation, pas la vélocité latérale directement
            // On garde X à 0 comme Cobblemon (ligne 376)
        }

        // === Gravité (Cobblemon ligne 339-345) ===
        if (!this.onGround() && jumpTicks <= 0) {
            newVelocity = new Vec3(newVelocity.x, Math.max(newVelocity.y - GRAVITY, -TERMINAL_VELOCITY), newVelocity.z);
        } else if (this.onGround()) {
            newVelocity = new Vec3(newVelocity.x, 0, newVelocity.z);
        }

        // === Friction au sol (Cobblemon ligne 351-353) ===
        if (newVelocity.horizontalDistance() > 0 && this.onGround() && !activeInput) {
            newVelocity = newVelocity.subtract(0, 0, Math.min(GROUND_FRICTION * Math.signum(newVelocity.z), newVelocity.z));
        }

        // === Saut/Leap (Cobblemon ligne 358-373) ===
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

        // === Zero lateral velocity (Cobblemon ligne 376) ===
        newVelocity = new Vec3(0, newVelocity.y, newVelocity.z);

        // === Rotation (Cobblemon calcRotAmount ligne 217-250) ===
        float rotAmount = calcRotAmount(driver, handling, lookYawLimit, topSpeed);
        float newYaw = this.getYRot() + rotAmount;
        this.setYRot(newYaw);
        this.yBodyRot = newYaw;
        this.yHeadRot = newYaw;

        // === Transition vers WALK si trop lent ===
        if (newVelocity.z < settings.runToWalkThreshold() && !activeInput) {
            setRidingMode(RidingMode.WALK);
            sprinting = false;
        }

        rideVelocity = newVelocity;
    }

    /**
     * Calcul de la rotation (copié de Cobblemon HorseBehaviour.kt ligne 217-250)
     */
    private float calcRotAmount(Player driver, double handling, float maxYawDiff, double topSpeed) {
        // Normalize the current rotation diff (ligne 229-230)
        float rotDiff = Mth.clamp(Mth.wrapDegrees(driver.getYRot() - this.getYRot()), -maxYawDiff, maxYawDiff);
        float rotDiffNorm = rotDiff / maxYawDiff;

        // Square root pour rotation plus douce (ligne 235-236)
        float minRotMod = 0.4f;
        float rotDiffMod = (float) ((Math.sqrt(Math.abs(rotDiffNorm)) * (1.0f - minRotMod)) + minRotMod) * Math.signum(rotDiffNorm);

        // Turn rate basé sur la vitesse (ligne 239-243)
        double walkSpeed = settings.walkSpeed();
        double w = Math.max(walkSpeed, this.getDeltaMovement().horizontalDistance());
        double invRelSpeed = (scaleToRange(w, walkSpeed, topSpeed) - 1.0) * -1.0;
        int walkHandlingBoost = 5;
        float turnRate = (float) ((handling / 20.0) * Math.max(walkHandlingBoost * invRelSpeed, 1.0));

        // Clamp (ligne 246-247)
        float turnSpeed = turnRate * rotDiffMod;
        return Mth.clamp(turnSpeed, -Math.abs(rotDiff), Math.abs(rotDiff));
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
