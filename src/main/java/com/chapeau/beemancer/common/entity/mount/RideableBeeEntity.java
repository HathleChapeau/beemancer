/**
 * ============================================================
 * [RideableBeeEntity.java]
 * Description: Entité chevauchable avec apparence d'abeille
 * ============================================================
 *
 * PATTERN: Basé sur Cobblemon PokemonEntity.kt
 * - Override tickRidden() pour tick quand monté
 * - Override travel() pour mouvement custom
 * - Override getRiddenInput() pour retourner vélocité
 * - Vélocité en espace local convertie en monde
 *
 * NOTE: Étend Mob (pas Bee) pour éviter comportements de vol.
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entité chevauchable avec apparence d'abeille.
 * - Mode WALK: mouvement libre selon caméra
 * - Mode RUN: accélération/décélération, A/D pour tourner
 */
public class RideableBeeEntity extends Mob implements PlayerRideable {

    // --- Entity Data (synced client-server) ---
    private static final EntityDataAccessor<Byte> DATA_RIDING_MODE = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_CURRENT_SPEED = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.FLOAT);

    // --- État de riding ---
    private Vec3 rideVelocity = Vec3.ZERO;
    private boolean sprinting = false;
    private int jumpCooldown = 0;

    // --- Settings ---
    private RidingSettings settings;

    public RideableBeeEntity(EntityType<? extends RideableBeeEntity> entityType, Level level) {
        super(entityType, level);
        this.settings = RidingSettingsLoader.getSettings();
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
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
            jumpCooldown = 0;
            setRidingMode(RidingMode.WALK);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player) {
            rideVelocity = Vec3.ZERO;
            sprinting = false;
            jumpCooldown = 0;
            setRidingMode(RidingMode.WALK);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() * 0.75, 0);
    }

    // --- Control (Pattern Cobblemon) ---

    /**
     * Indique que le client local contrôle cette entité quand montée.
     */
    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger instanceof Player player) {
            return player.isLocalPlayer();
        }
        return false;
    }

    // --- Riding Core (Pattern Cobblemon PokemonEntity.kt) ---

    /**
     * Appelé chaque tick quand un joueur est monté.
     * Pattern Cobblemon: tickRidden() lignes 2232-2283
     */
    @Override
    protected void tickRidden(Player driver, Vec3 movementInput) {
        super.tickRidden(driver, movementInput);

        // Extraire inputs
        float forward = (float) movementInput.z;
        float strafe = (float) movementInput.x;

        // Gestion du sprint
        updateSprinting(driver, forward);

        // Mise à jour du jump cooldown
        if (this.onGround() && jumpCooldown < 0) {
            jumpCooldown++;
        }

        // Gestion du mode et de la rotation
        if (getRidingMode() == RidingMode.WALK) {
            tickWalkMode(driver, forward, strafe);
        } else {
            tickRunMode(driver, forward, strafe);
        }

        // Sync les données
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();
    }

    /**
     * Calcule et applique le mouvement.
     * Pattern Cobblemon: travel() lignes 1815-1875
     */
    @Override
    public void travel(Vec3 movementInput) {
        if (this.getControllingPassenger() instanceof Player driver) {
            // Joueur monté: utiliser notre vélocité calculée

            // Convertir vélocité locale en monde si mode RUN
            Vec3 worldVelocity;
            if (getRidingMode() == RidingMode.WALK) {
                // En WALK, la vélocité est déjà en coordonnées monde
                worldVelocity = rideVelocity;
            } else {
                // En RUN, convertir de local à monde
                worldVelocity = RideableBeeMovement.localToWorldVelocity(rideVelocity, this.getYRot());
            }

            // Appliquer inertie (Cobblemon ligne 1843)
            Vec3 diff = worldVelocity.subtract(this.getDeltaMovement());
            double inertia = 0.5; // Valeur par défaut Cobblemon
            this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(inertia)));

            // Appliquer le mouvement
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Sync speed pour le client
            this.entityData.set(DATA_CURRENT_SPEED, (float) rideVelocity.z);

        } else {
            // Pas de passager: comportement par défaut avec gravité
            Vec3 motion = this.getDeltaMovement();
            if (!this.onGround()) {
                motion = new Vec3(motion.x,
                        Math.max(motion.y - RideableBeeMovement.GRAVITY, -RideableBeeMovement.TERMINAL_VELOCITY),
                        motion.z);
            }
            this.setDeltaMovement(motion.multiply(0.9, 0.98, 0.9));
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    /**
     * Retourne l'input de riding transformé.
     * Pattern Cobblemon: getRiddenInput() lignes 2397-2401
     */
    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 movementInput) {
        // Retourner la vélocité calculée
        if (getRidingMode() == RidingMode.WALK) {
            return rideVelocity;
        } else {
            return RideableBeeMovement.localToWorldVelocity(rideVelocity, this.getYRot());
        }
    }

    // --- Mode Handlers ---

    private void updateSprinting(Player driver, float forward) {
        boolean tryingToSprint = driver.isSprinting() && forward > 0;

        if (forward <= 0) {
            sprinting = false;
        }

        if (tryingToSprint && !sprinting) {
            sprinting = true;
        }
    }

    private void tickWalkMode(Player driver, float forward, float strafe) {
        boolean jumping = driver.jumping;

        // Transition vers RUN si sprint + avance
        if (sprinting && forward > 0.5f) {
            setRidingMode(RidingMode.RUN);
            rideVelocity = new Vec3(0, rideVelocity.y, settings.walkSpeed());
            return;
        }

        // Calculer vélocité (en coordonnées monde)
        rideVelocity = RideableBeeMovement.calculateWalkVelocity(
                driver, forward, strafe,
                rideVelocity.y, this.onGround(), jumping, jumpCooldown,
                settings
        );

        // Mettre à jour le jump cooldown
        if (jumping && this.onGround() && jumpCooldown >= 0) {
            jumpCooldown = -5;
        }

        // Tourner vers la direction de mouvement
        float targetYaw = RideableBeeMovement.calculateWalkTargetYaw(rideVelocity.x, rideVelocity.z);
        if (!Float.isNaN(targetYaw)) {
            this.setYRot(targetYaw);
        }
    }

    private void tickRunMode(Player driver, float forward, float strafe) {
        boolean jumping = driver.jumping;

        // Calculer vélocité (en espace local)
        rideVelocity = RideableBeeMovement.calculateRunVelocity(
                forward, rideVelocity.z,
                rideVelocity.y, this.onGround(), jumping, jumpCooldown,
                sprinting, settings
        );

        // Mettre à jour le jump cooldown
        if (jumping && this.onGround() && jumpCooldown >= 0) {
            jumpCooldown = -5;
        }

        // Rotation avec A/D (style Jet)
        float yawDelta = RideableBeeMovement.calculateRunYawDelta(strafe, 180.0);
        this.setYRot(this.getYRot() + yawDelta);

        // Transition vers WALK si plus de sprint et vitesse basse
        if (!sprinting && rideVelocity.z <= settings.runToWalkThreshold()) {
            setRidingMode(RidingMode.WALK);
        }
    }

    // --- AI ---

    @Override
    protected void registerGoals() {
        // Pas de goals - l'entité ne fait rien seule
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

    // --- Debug Getters (pour RideableBeeDebugHud) ---

    /**
     * Retourne la vélocité actuelle (Z = forward, X = strafe)
     */
    public Vec3 getRideVelocity() {
        return rideVelocity;
    }

    /**
     * Retourne le jump cooldown actuel
     */
    public int getJumpCooldown() {
        return jumpCooldown;
    }

    /**
     * Retourne si l'abeille est en train de sprinter
     */
    public boolean isSprinting() {
        return sprinting;
    }

    /**
     * Calcule l'état actuel de l'abeille pour le debug.
     * @return "WALK", "RUN", "AIRBORNE" ou "JUMPING"
     */
    public String getDebugState() {
        if (!this.onGround()) {
            if (jumpCooldown < 0) {
                return "JUMPING";
            }
            return "AIRBORNE";
        }
        return getRidingMode().name();
    }
}
