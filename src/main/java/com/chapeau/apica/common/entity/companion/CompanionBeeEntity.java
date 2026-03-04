/**
 * ============================================================
 * [CompanionBeeEntity.java]
 * Description: Abeille compagnon qui hover pres du joueur et ramasse les items au sol
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Bee                 | Entite parent        | Modele, texture, ailes         |
 * | IAccessory          | Systeme accessoire   | Spawn/despawn via equip        |
 * | AccessoryPlayerData | Donnees joueur       | Slot accessoire                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeMagnetItem.java (spawn/despawn)
 * - CompanionBeeRenderer.java (rendu)
 * - Apica.java (lifecycle login/respawn)
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.companion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Abeille compagnon attachee a un joueur via l'accessoire BeeMagnet.
 * Hover pres de l'epaule du joueur et vole chercher les items droppes au sol.
 *
 * Machine a etats: IDLE → FLYING_TO_ITEM → PICKING_UP → RETURNING → IDLE
 *
 * Entite invulnerable, sans gravite, sans collision, non-persistante.
 */
public class CompanionBeeEntity extends Bee {

    /** Rayon de detection des items au sol (en blocs). */
    private static final double MAGNET_RANGE = 8.0;
    /** Frequence de scan des items (en ticks). */
    private static final int SCAN_INTERVAL = 5;
    /** Duree de l'animation de ramassage (en ticks). */
    private static final int PICKUP_DURATION = 10;
    /** Distance pour considerer l'arrivee a un item. */
    private static final double ITEM_ARRIVAL_DISTANCE = 0.8;
    /** Distance pour considerer le retour au joueur. */
    private static final double RETURN_ARRIVAL_DISTANCE = 1.5;
    /** Vitesse de croisiere (blocs/tick). */
    private static final double FLY_SPEED = 0.2;
    /** Facteur de lerp pour le lissage du mouvement. */
    private static final double SMOOTHING = 0.25;
    /** Rayon de deceleration (en blocs). */
    private static final double DECEL_RADIUS = 2.5;
    /** Distance max avant teleport de rattrapage. */
    private static final double TELEPORT_DISTANCE = 16.0;
    /** Cooldown avant de ramasser un item jete par le owner (en ticks). */
    private static final int THROW_COOLDOWN_TICKS = 60;
    /** Offset Y au-dessus de l'epaule du joueur. */
    private static final double SHOULDER_Y = 1.8;
    /** Offset lateral pour l'epaule. */
    private static final double SHOULDER_X = 0.6;
    /** Offset vers l'arriere du joueur. */
    private static final double SHOULDER_Z = -0.3;

    // --- Synched data ---
    private static final EntityDataAccessor<String> DATA_STATE =
        SynchedEntityData.defineId(CompanionBeeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED_ITEM =
        SynchedEntityData.defineId(CompanionBeeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(CompanionBeeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_ACCESSORY_SLOT =
        SynchedEntityData.defineId(CompanionBeeEntity.class, EntityDataSerializers.INT);

    /** Etat actuel de l'abeille. */
    public enum State {
        IDLE,
        FLYING_TO_ITEM,
        PICKING_UP,
        RETURNING
    }

    /** Items en cours de ciblage par toutes les companion bees (evite les doublons). */
    private static final Set<Integer> CLAIMED_ITEM_IDS = new HashSet<>();

    private int pickupTimer = 0;
    private int targetItemId = -1;
    private long ownerLastThrowTime = -1000;

    public CompanionBeeEntity(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, State.IDLE.name());
        builder.define(DATA_CARRIED_ITEM, ItemStack.EMPTY);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_ACCESSORY_SLOT, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
    }

    // =========================================================================
    // TICK
    // =========================================================================

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();

        if (level().isClientSide()) return;

        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        State state = getCompanionState();
        switch (state) {
            case IDLE -> tickIdle(owner);
            case FLYING_TO_ITEM -> tickFlyingToItem(owner);
            case PICKING_UP -> tickPickingUp(owner);
            case RETURNING -> tickReturning(owner);
        }
    }

    // =========================================================================
    // STATE: IDLE
    // =========================================================================

    private void tickIdle(Player owner) {
        Vec3 target = computeShoulderPosition(owner);
        flyTowards(target);

        if (distanceTo(owner) > TELEPORT_DISTANCE) {
            teleportTo(target.x, target.y, target.z);
        }

        if (tickCount % SCAN_INTERVAL == 0) {
            scanForItems(owner);
        }
    }

    private void scanForItems(Player owner) {
        AABB area = owner.getBoundingBox().inflate(MAGNET_RANGE);
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, area,
            item -> canMagnetize(item, owner));

        items.sort(Comparator.comparingDouble(item -> item.distanceToSqr(this)));

        for (ItemEntity item : items) {
            if (!CLAIMED_ITEM_IDS.contains(item.getId())) {
                targetItemId = item.getId();
                CLAIMED_ITEM_IDS.add(targetItemId);
                setCompanionState(State.FLYING_TO_ITEM);
                return;
            }
        }
    }

    private boolean canMagnetize(ItemEntity item, Player owner) {
        if (!item.isAlive() || item.getItem().isEmpty()) return false;
        if (item.hasPickUpDelay()) return false;
        if (item.getTarget() != null) return false;

        CompoundTag persistentData = item.getPersistentData();
        if (persistentData.contains("PreventRemoteMovement")) return false;

        if (owner.getUUID().equals(item.getOwner()) &&
            (level().getGameTime() - ownerLastThrowTime) < THROW_COOLDOWN_TICKS) {
            return false;
        }

        return true;
    }

    // =========================================================================
    // STATE: FLYING_TO_ITEM
    // =========================================================================

    private void tickFlyingToItem(Player owner) {
        Entity targetEntity = level().getEntity(targetItemId);
        if (!(targetEntity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty()) {
            releaseTarget();
            setCompanionState(State.RETURNING);
            return;
        }

        Vec3 itemPos = item.position().add(0, 0.5, 0);
        flyTowards(itemPos);

        double dist = position().distanceTo(itemPos);
        if (dist < ITEM_ARRIVAL_DISTANCE) {
            pickupTimer = 0;
            setCompanionState(State.PICKING_UP);
        }

        if (distanceTo(owner) > TELEPORT_DISTANCE * 2) {
            releaseTarget();
            setCompanionState(State.IDLE);
        }
    }

    // =========================================================================
    // STATE: PICKING_UP
    // =========================================================================

    private void tickPickingUp(Player owner) {
        Entity targetEntity = level().getEntity(targetItemId);
        if (!(targetEntity instanceof ItemEntity item) || !item.isAlive()) {
            releaseTarget();
            setCompanionState(State.RETURNING);
            return;
        }

        Vec3 hoverPos = item.position().add(0, 0.8, 0);
        flyTowards(hoverPos);

        pickupTimer++;
        if (pickupTimer >= PICKUP_DURATION) {
            item.setNoPickUpDelay();
            item.playerTouch(owner);

            if (item.isAlive() && !item.getItem().isEmpty()) {
                releaseTarget();
                setCompanionState(State.RETURNING);
            } else {
                setCarriedItem(item.getItem().copy());
                releaseTarget();
                setCompanionState(State.RETURNING);
            }
        }
    }

    // =========================================================================
    // STATE: RETURNING
    // =========================================================================

    private void tickReturning(Player owner) {
        Vec3 target = computeShoulderPosition(owner);
        flyTowards(target);

        double dist = position().distanceTo(target);
        if (dist < RETURN_ARRIVAL_DISTANCE) {
            setCarriedItem(ItemStack.EMPTY);
            setCompanionState(State.IDLE);
        }

        if (distanceTo(owner) > TELEPORT_DISTANCE) {
            setCarriedItem(ItemStack.EMPTY);
            teleportTo(target.x, target.y, target.z);
            setCompanionState(State.IDLE);
        }
    }

    // =========================================================================
    // MOVEMENT
    // =========================================================================

    private void flyTowards(Vec3 target) {
        Vec3 toTarget = target.subtract(position());
        double dist = toTarget.length();

        if (dist < 0.1) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        double speed = FLY_SPEED;
        if (dist < DECEL_RADIUS) {
            double factor = dist / DECEL_RADIUS;
            speed *= Math.max(0.05, factor * factor);
        }

        Vec3 desired = toTarget.normalize().scale(speed);
        Vec3 current = getDeltaMovement();
        Vec3 smoothed = new Vec3(
            Mth.lerp(SMOOTHING, current.x, desired.x),
            Mth.lerp(SMOOTHING, current.y, desired.y),
            Mth.lerp(SMOOTHING, current.z, desired.z)
        );
        setDeltaMovement(smoothed);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
        }
    }

    private Vec3 computeShoulderPosition(Player owner) {
        double angle = Math.toRadians(owner.yBodyRot);
        double offsetX = (getAccessorySlot() == 0) ? -SHOULDER_X : SHOULDER_X;
        double offsetZ = SHOULDER_Z;

        double worldX = owner.getX() + offsetX * Math.cos(angle) - offsetZ * Math.sin(angle);
        double worldZ = owner.getZ() + offsetX * Math.sin(angle) + offsetZ * Math.cos(angle);
        double bob = Math.sin(tickCount * 0.15) * 0.1;
        double worldY = owner.getY() + SHOULDER_Y + bob;

        return new Vec3(worldX, worldY, worldZ);
    }

    // =========================================================================
    // TARGET MANAGEMENT
    // =========================================================================

    private void releaseTarget() {
        if (targetItemId != -1) {
            CLAIMED_ITEM_IDS.remove(targetItemId);
            targetItemId = -1;
        }
    }

    /** Notifie que le owner vient de jeter un item (cooldown anti-magnet). */
    public void notifyOwnerThrow() {
        ownerLastThrowTime = level().getGameTime();
    }

    // =========================================================================
    // SYNCHED DATA GETTERS/SETTERS
    // =========================================================================

    public State getCompanionState() {
        try {
            return State.valueOf(entityData.get(DATA_STATE));
        } catch (IllegalArgumentException e) {
            return State.IDLE;
        }
    }

    public void setCompanionState(State state) {
        entityData.set(DATA_STATE, state.name());
    }

    public ItemStack getCarriedItem() {
        return entityData.get(DATA_CARRIED_ITEM);
    }

    public void setCarriedItem(ItemStack stack) {
        entityData.set(DATA_CARRIED_ITEM, stack);
    }

    @Nullable
    public Player getOwnerPlayer() {
        Optional<UUID> ownerUuid = entityData.get(DATA_OWNER_UUID);
        return ownerUuid.map(uuid -> level().getPlayerByUUID(uuid)).orElse(null);
    }

    public void setOwnerUuid(UUID uuid) {
        entityData.set(DATA_OWNER_UUID, Optional.of(uuid));
    }

    public int getAccessorySlot() {
        return entityData.get(DATA_ACCESSORY_SLOT);
    }

    public void setAccessorySlot(int slot) {
        entityData.set(DATA_ACCESSORY_SLOT, slot);
    }

    // =========================================================================
    // INVULNERABILITY + NEUTRALIZATION
    // =========================================================================

    @Override public boolean isInvulnerable() { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean fireImmune() { return true; }
    @Override protected void dropExperience(Entity killer) { }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected void pushEntities() { }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { }
    @Override
    public void playAmbientSound() { }
    @Override
    @Nullable
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override
    @Nullable
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) { return null; }
    @Override
    @Nullable
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }

    // =========================================================================
    // PERSISTENCE — Non-persistante (respawn au login)
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // Ne pas sauvegarder — l'abeille est respawnee au login
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        // Ne pas charger — l'abeille est respawnee au login
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseTarget();
        super.remove(reason);
    }
}
