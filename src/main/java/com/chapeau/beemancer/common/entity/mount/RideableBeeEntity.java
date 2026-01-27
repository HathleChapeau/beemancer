/**
 * ============================================================
 * [RideableBeeEntity.java]
 * Description: Entité abeille chevauchable avec modes WALK/RUN
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RidingMode              | Mode actuel          | Synchronisation client         |
 * | RidingSettings          | Paramètres           | Config depuis JSON             |
 * | RidingState             | État runtime         | Vitesse, yaw, leap             |
 * | RidingController        | Logique déplacement  | Tick mouvement                 |
 * | RidingSettingsLoader    | Chargement JSON      | Récupération settings          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerEntities.java: Enregistrement de l'entité
 * - RideableBeeRenderer.java: Rendu
 * - RidingInputHandler.java: Envoi des inputs
 * - RidingInputPacket.java: Réception des inputs
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entité abeille géante chevauchable.
 * Deux modes de déplacement: WALK (libre) et RUN (inertie).
 * Étend Bee pour réutiliser le BeeModel vanilla.
 */
public class RideableBeeEntity extends Bee implements PlayerRideable {

    // --- Entity Data Accessors (synchronisés client-serveur) ---
    private static final EntityDataAccessor<Byte> DATA_RIDING_MODE = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_CURRENT_SPEED = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_LEAPING = SynchedEntityData.defineId(
            RideableBeeEntity.class, EntityDataSerializers.BOOLEAN);

    // --- Runtime state ---
    private final RidingState state = new RidingState();
    private RideableBeeController controller;

    // --- Camera yaw (reçu du client) ---
    private float clientCameraYaw = 0f;

    public RideableBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.controller = new RideableBeeController(RidingSettingsLoader.getSettings(), state);
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
        builder.define(DATA_IS_LEAPING, false);
    }

    // --- Riding Mode Sync ---

    public RidingMode getRidingMode() {
        return RidingMode.fromByte(this.entityData.get(DATA_RIDING_MODE));
    }

    public void setRidingMode(RidingMode mode) {
        this.entityData.set(DATA_RIDING_MODE, mode.toByte());
    }

    public float getSyncedSpeed() {
        return this.entityData.get(DATA_CURRENT_SPEED);
    }

    public void setSyncedSpeed(float speed) {
        this.entityData.set(DATA_CURRENT_SPEED, speed);
    }

    public boolean isSyncedLeaping() {
        return this.entityData.get(DATA_IS_LEAPING);
    }

    public void setSyncedLeaping(boolean leaping) {
        this.entityData.set(DATA_IS_LEAPING, leaping);
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
            // Initialiser l'état au montage
            state.reset();
            state.setCurrentYaw(this.getYRot());
            state.setTargetYaw(this.getYRot());
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player) {
            // Reset à la descente
            controller.reset();
            setRidingMode(RidingMode.WALK);
            setSyncedSpeed(0f);
            setSyncedLeaping(false);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        // Position du joueur sur l'abeille
        return new Vec3(0, dimensions.height() * 0.85, 0);
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        // Seulement côté serveur et si monté
        if (!this.level().isClientSide() && this.getControllingPassenger() instanceof Player) {
            // Appeler le controller
            Vec3 movement = controller.tick(this, clientCameraYaw, this.onGround());

            // Appliquer le mouvement
            if (movement.lengthSqr() > 0.0001) {
                this.setDeltaMovement(this.getDeltaMovement().add(movement));
            }

            // Synchroniser l'état
            setRidingMode(state.getCurrentMode());
            setSyncedSpeed(state.getCurrentSpeed());
            setSyncedLeaping(state.isLeaping());
        }
        // Note: La gravité est gérée automatiquement par Minecraft via LivingEntity.travel()
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.getControllingPassenger() instanceof Player) {
            // Le mouvement est géré par tick() via le controller
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9)); // Friction
        } else {
            super.travel(travelVector);
        }
    }

    // --- Input from Client ---

    /**
     * Reçoit l'input du client via packet.
     */
    public void receiveInput(float forward, float strafe, boolean jump, boolean sprint, float cameraYaw) {
        this.state.updateInput(forward, strafe, jump, sprint);
        this.clientCameraYaw = cameraYaw;
    }

    // --- Save/Load ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Pas besoin de sauvegarder l'état de montage (temporaire)
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    // --- Misc ---

    @Override
    protected void registerGoals() {
        // Pas de goals AI - l'abeille ne fait rien quand pas montée
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // --- Getters ---

    public RidingState getState() {
        return state;
    }

    public RideableBeeController getController() {
        return controller;
    }
}
