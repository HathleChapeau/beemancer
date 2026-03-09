/**
 * ============================================================
 * [HoverbikeEntity.java]
 * Description: Entite HoverBee — abeille geante montable avec modes Hover et Run
 * ============================================================
 *
 * PATTERN: Inspire de Cobblemon PokemonEntity.kt
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeMode       | Enum mode            | Etat courant                   |
 * | HoverbikeSettings   | Constantes physiques | Reference                      |
 * | HoverbikePhysics    | Calculs              | velocity, rotation, transitions|
 * | BeeSpeciesManager   | Especes abeille      | Model/couleurs par espece      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaEntities.java: Enregistrement
 * - HoverbikeRenderer.java: Rendu
 * - HoverbikeDebugHud.java: Affichage debug
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import com.chapeau.apica.core.entity.InteractionMarkerManager;
import com.chapeau.apica.core.registry.ApicaTags;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

/**
 * HoverBee — abeille geante montable avec physique velocite/acceleration.
 * Deux modes: HOVER (4 directions, basse vitesse) et RUN (avant uniquement, haute vitesse).
 * Gravite reduite, friction glace en permanence.
 * L'apparence depend de l'espece d'abeille selectionnee (species).
 */
public class HoverbikeEntity extends Mob implements PlayerRideable {

    // --- Synched Data ---
    private static final EntityDataAccessor<Boolean> DATA_EDIT_MODE =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_EDITING_PLAYER =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // --- Mode & Sprint (synched pour rendu et multi) ---
    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_SPRINT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.BOOLEAN);

    // --- Species (synched pour rendu cote client) ---
    private static final EntityDataAccessor<String> DATA_SPECIES =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.STRING);

    // --- Part Variants (synched pour rendu cote client) ---
    private static final EntityDataAccessor<Integer> DATA_SADDLE_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WING_PROTECTOR_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CONTROL_LEFT_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CONTROL_RIGHT_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);

    // --- Part Stacks (synched pour calcul de stats et persistence) ---
    private static final EntityDataAccessor<ItemStack> DATA_SADDLE_STACK =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_WING_PROTECTOR_STACK =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_CONTROL_LEFT_STACK =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_CONTROL_RIGHT_STACK =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.ITEM_STACK);

    // --- Ownership (synched pour affichage client et restriction d'acces) ---
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.STRING);

    // --- Settings ---
    private HoverbikeSettings settings;

    // --- Etat interne ---
    private HoverbikeMode mode = HoverbikeMode.HOVER;
    private Vec3 rideVelocity = Vec3.ZERO;
    private boolean sprintPressed = false;
    private boolean jumpPressed = false;
    private float gaugeLevel = 1.0f;
    private long feedCooldownUntil = -1;

    /** Facteur de surconsommation de la jauge en mode RUN. */
    private static final float RUN_GAUGE_DRAIN_MULTIPLIER = 1.5f;

    // --- Collision ---
    private final HoverbikeCollisionHandler collisionHandler = new HoverbikeCollisionHandler();

    // --- Anti-fly kick (double compteur NeoForge/vanilla) ---
    private int customAboveGroundTicks = 0;

    // --- Visual banking (client-side, expose pour renderer) ---
    private float prevYawDelta = 0;
    private float lastYawDelta = 0;

    // --- Raycasts predictifs: resultats stockes pour step-up + debug ---
    private Vec3[] debugRayStarts = new Vec3[6];
    private Vec3[] debugRayEnds = new Vec3[6];
    private Vec3[] debugRayHits = new Vec3[6];
    private BlockHitResult[] rayHitResults = new BlockHitResult[6];
    private boolean debugRaysActive = false;

    // --- Edit mode ---
    private static final double EDIT_MODE_MAX_DISTANCE = 5.0;
    private static boolean editShaderActive = false;

    // --- Constructor ---

    public HoverbikeEntity(EntityType<? extends HoverbikeEntity> entityType, Level level) {
        super(entityType, level);
        this.settings = HoverbikeConfigManager.getBaseStats();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EDIT_MODE, false);
        builder.define(DATA_EDITING_PLAYER, Optional.empty());
        builder.define(DATA_MODE, (byte) 0);
        builder.define(DATA_SPRINT, false);
        builder.define(DATA_SPECIES, "meadow");
        builder.define(DATA_SADDLE_VARIANT, 0);
        builder.define(DATA_WING_PROTECTOR_VARIANT, 0);
        builder.define(DATA_CONTROL_LEFT_VARIANT, 0);
        builder.define(DATA_CONTROL_RIGHT_VARIANT, 0);
        builder.define(DATA_SADDLE_STACK, ItemStack.EMPTY);
        builder.define(DATA_WING_PROTECTOR_STACK, ItemStack.EMPTY);
        builder.define(DATA_CONTROL_LEFT_STACK, ItemStack.EMPTY);
        builder.define(DATA_CONTROL_RIGHT_STACK, ItemStack.EMPTY);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_OWNER_NAME, "");
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.STEP_HEIGHT, 1.3);
    }

    // --- Species ---

    public String getSpeciesId() {
        return this.entityData.get(DATA_SPECIES);
    }

    public void setSpeciesId(String speciesId) {
        this.entityData.set(DATA_SPECIES, speciesId);
    }

    // --- Gravity Override (Pattern Cobblemon PokemonEntity.kt L2494-2502) ---

    @Override
    protected double getDefaultGravity() {
        if (this.isVehicle()) {
            return 0.0;
        }
        return super.getDefaultGravity();
    }

    // --- Mounting & Edit Mode ---

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            if (getOwnerUUID().isPresent() && !isOwner(player)) {
                return InteractionResult.PASS;
            }
        }

        ItemStack heldItem = player.getItemInHand(hand);
        boolean isFood = heldItem.is(ApicaTags.Items.BEE_FOOD);
        boolean isHated = !isFood && heldItem.is(ApicaTags.Items.BEE_HATED_FOOD);
        if (isFood || isHated) {
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            if (level().getGameTime() < feedCooldownUntil) return InteractionResult.PASS;

            if (level() instanceof ServerLevel serverLevel) {
                Vec3 beePos = position().add(0, 0.5, 0);
                if (isFood) {
                    ParticleHelper.burst(serverLevel, beePos, ParticleHelper.EffectType.HEAL, 5);
                } else {
                    ParticleHelper.burst(serverLevel, beePos, ParticleHelper.EffectType.FAILURE, 8);
                }
            }
            heldItem.shrink(1);
            feedCooldownUntil = level().getGameTime() + 40;
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                if (isEditMode() && getEditingPlayerUUID().isPresent()
                        && getEditingPlayerUUID().get().equals(player.getUUID())) {
                    exitEditMode();
                } else if (!isEditMode()) {
                    enterEditMode(player.getUUID());
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (!isEditMode() && !this.isVehicle()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    private void enterEditMode(UUID playerUUID) {
        this.entityData.set(DATA_EDIT_MODE, true);
        this.entityData.set(DATA_EDITING_PLAYER, Optional.of(playerUUID));
        spawnPartMarkers();
    }

    private void exitEditMode() {
        despawnPartMarkers();
        this.entityData.set(DATA_EDIT_MODE, false);
        this.entityData.set(DATA_EDITING_PLAYER, Optional.empty());
    }

    public boolean isEditMode() {
        return this.entityData.get(DATA_EDIT_MODE);
    }

    public Optional<UUID> getEditingPlayerUUID() {
        return this.entityData.get(DATA_EDITING_PLAYER);
    }

    // --- Ownership ---

    public void setOwner(Player player) {
        this.entityData.set(DATA_OWNER_UUID, Optional.of(player.getUUID()));
        this.entityData.set(DATA_OWNER_NAME, player.getGameProfile().getName());
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    public String getOwnerName() {
        return this.entityData.get(DATA_OWNER_NAME);
    }

    public boolean isOwner(Player player) {
        Optional<UUID> owner = getOwnerUUID();
        return owner.isEmpty() || owner.get().equals(player.getUUID());
    }

    // --- Part Interaction Markers ---

    private static final String MARKER_PREFIX = "hoverbee_part_";

    /** Offsets en espace modele pour chaque part en edit mode. */
    public static final Vec3[] PART_EDIT_OFFSETS = {
            new Vec3(0, 1, 1),    // SADDLE
            new Vec3(0, 1, -1),   // WING_PROTECTOR
            new Vec3(0, 0, 1),    // CONTROL_LEFT
            new Vec3(0, 0, -1)    // CONTROL_RIGHT
    };

    private static final double MODEL_ORIGIN_Y = 1.501;

    public Vec3 computePartWorldPos(int partOrdinal) {
        if (partOrdinal < 0 || partOrdinal >= PART_EDIT_OFFSETS.length) return position();
        Vec3 modelOffset = PART_EDIT_OFFSETS[partOrdinal];
        float yawRad = (float) Math.toRadians(180.0 - this.yBodyRot);
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);
        double wx = -modelOffset.x;
        double wy = -modelOffset.y;
        double wz = modelOffset.z;
        double rotX = wx * cosYaw - wz * sinYaw;
        double rotZ = wx * sinYaw + wz * cosYaw;
        return position().add(rotX, MODEL_ORIGIN_Y + wy, rotZ);
    }

    private void spawnPartMarkers() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        for (HoverbikePart part : HoverbikePart.values()) {
            String markerType = MARKER_PREFIX + part.name().toLowerCase();
            Vec3 worldPos = computePartWorldPos(part.ordinal());
            Vec3 offset = worldPos.subtract(position());
            InteractionMarkerManager.spawnForEntity(
                    serverLevel, this, markerType, part.ordinal(), offset);
        }
    }

    private void despawnPartMarkers() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        for (HoverbikePart part : HoverbikePart.values()) {
            String markerType = MARKER_PREFIX + part.name().toLowerCase();
            InteractionMarkerManager.despawnForEntity(serverLevel, this.getId(), markerType);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return (passenger instanceof Player player) ? player : null;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        collisionHandler.onPassengerRemoved(passenger.getUUID());
        super.removePassenger(passenger);
        if (this.getPassengers().isEmpty()) {
            mode = HoverbikeMode.HOVER;
            rideVelocity = Vec3.ZERO;
            sprintPressed = false;
            jumpPressed = false;
            gaugeLevel = 1.0f;
        }
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (other.isSpectator()) return false;
        if (this.getPassengers().contains(other)) return false;
        if (other instanceof Projectile) return false;
        if (other instanceof ItemEntity) return false;
        if (other.noPhysics) return false;
        return super.canCollideWith(other);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() + 0.1, 0);
    }

    // --- Movement Overrides ---

    @Override
    public void move(MoverType type, Vec3 movement) {
        if (this.getControllingPassenger() != null) {
            if (this.getDeltaMovement().y > -0.5 && this.fallDistance > 1.0f) {
                this.fallDistance = 1.0f;
            }
        }
        super.move(type, movement);
    }

    @Override
    public boolean isAffectedByFluids() {
        if (this.isVehicle()) {
            return false;
        }
        return super.isAffectedByFluids();
    }

    // --- Control ---

    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger instanceof Player player) {
            return player.isLocalPlayer();
        }
        return super.isControlledByLocalInstance();
    }

    // --- Core Riding Logic ---

    @Override
    protected void tickRidden(Player driver, Vec3 movementInput) {
        super.tickRidden(driver, movementInput);
        this.yRotO = this.getYRot();
        float forward = Math.signum(driver.zza);

        if (this.level().isClientSide) {
            this.sprintPressed = isSprintKeyDown();
            this.jumpPressed = isJumpKeyDown();
        }

        if (!jumpPressed && this.onGround() && mode == HoverbikeMode.HOVER) {
            gaugeLevel = Math.min(1.0f, gaugeLevel + (float) settings.gaugeFillRate());
        }

        float yawDelta = HoverbikePhysics.calculateYawDelta(
                driver.getYRot(), this.getYRot(),
                rideVelocity.z, mode, settings
        );
        this.prevYawDelta = this.lastYawDelta;
        this.lastYawDelta = yawDelta;
        this.setRot(this.getYRot() + yawDelta, 0.0f);
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();

        if (mode == HoverbikeMode.HOVER) {
            if (HoverbikePhysics.shouldTransitionToRun(forward, sprintPressed, rideVelocity.z, settings)) {
                mode = HoverbikeMode.RUN;
            }
        } else {
            if (HoverbikePhysics.shouldTransitionToHover(rideVelocity.z, settings)) {
                mode = HoverbikeMode.HOVER;
            }
        }

        this.entityData.set(DATA_MODE, (byte) mode.ordinal());
        this.entityData.set(DATA_SPRINT, sprintPressed);
    }

    private static boolean isSprintKeyDown() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    private static boolean isJumpKeyDown() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    @Override
    public void travel(Vec3 movementInput) {
        Player driver = (this.getControllingPassenger() instanceof Player p) ? p : null;
        if (driver == null) {
            super.travel(movementInput);
            return;
        }

        float forward = Math.signum(driver.zza);
        float strafe = Math.signum(driver.xxa);

        if (mode == HoverbikeMode.RUN) {
            rideVelocity = HoverbikePhysics.calculateRunVelocity(
                    forward, rideVelocity, this.onGround(), settings
            );
        } else {
            rideVelocity = HoverbikePhysics.calculateHoverVelocity(
                    forward, strafe, rideVelocity, this.onGround(), settings
            );
        }

        if (jumpPressed && gaugeLevel > 0) {
            rideVelocity = new Vec3(rideVelocity.x, settings.liftSpeed(), rideVelocity.z);
            float drain = (float) settings.gaugeDrainRate();
            if (mode == HoverbikeMode.RUN) {
                drain *= RUN_GAUGE_DRAIN_MULTIPLIER;
            }
            gaugeLevel = Math.max(0, gaugeLevel - drain);
        }

        Vec3 worldVelocity = HoverbikePhysics.localToWorld(rideVelocity, this.getYRot());
        Vec3 diff = worldVelocity.subtract(this.getDeltaMovement());
        double inertia = 0.85;
        this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(inertia)));

        Vec3 movement = this.getDeltaMovement();
        if (this.onGround() && movement.y == 0.0) {
            movement = movement.subtract(0, 0.0001, 0);
            this.setDeltaMovement(movement);
        }

        updatePredictiveRaycasts();
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.horizontalCollision) {
            double postCollisionSpeed = this.getDeltaMovement().length();
            rideVelocity = rideVelocity.normalize().scale(postCollisionSpeed);
        }

        if (!this.level().isClientSide()) {
            AABB[] probes = HoverbikeCollisionGeometry.calculateWorldBoxes(this.position(), this.getYRot());
            collisionHandler.resolveEntityCollisions(this, probes, this.level(), settings);
        }
    }

    // =========================================================================
    // RAYCASTS PREDICTIFS
    // =========================================================================

    private static final Vec3 NOSE_OFFSET = new Vec3(0, 0.4, 0.8);
    private static final double RAY_LATERAL_SPREAD = 0.4;
    private static final double RAY_BOTTOM_Y_OFFSET = -0.3;
    private static final double RAY_BOTTOM_PITCH = -0.52;
    private static final double RAY_MIN_LENGTH = 0.5;
    private static final double RAY_MAX_LENGTH = 3.0;
    private static final double RAY_SPEED_THRESHOLD = 0.03;
    private final double[] rayDistances = new double[6];

    private void updatePredictiveRaycasts() {
        double speed = rideVelocity.horizontalDistance();
        if (speed < RAY_SPEED_THRESHOLD) {
            debugRaysActive = false;
            for (int i = 0; i < 6; i++) rayDistances[i] = -1;
            return;
        }

        debugRaysActive = true;

        float yaw = this.getYRot();
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);

        double maxSpeed = (mode == HoverbikeMode.RUN) ? settings.maxRunSpeed() : settings.maxHoverSpeed();
        double speedRatio = Mth.clamp(speed / maxSpeed, 0.0, 1.0);
        double rayLength = Mth.lerp(speedRatio, RAY_MIN_LENGTH, RAY_MAX_LENGTH);

        Vec3 noseWorld = this.position().add(
                NOSE_OFFSET.x * cosYaw - NOSE_OFFSET.z * sinYaw,
                NOSE_OFFSET.y,
                NOSE_OFFSET.z * cosYaw + NOSE_OFFSET.x * sinYaw
        );

        Vec3 forwardDir = new Vec3(-sinYaw, 0, cosYaw);
        Vec3 rightDir = new Vec3(cosYaw, 0, sinYaw);

        double pitchCos = Math.cos(RAY_BOTTOM_PITCH);
        double pitchSin = Math.sin(RAY_BOTTOM_PITCH);
        Vec3 forwardDownDir = new Vec3(
                -sinYaw * pitchCos,
                pitchSin,
                cosYaw * pitchCos
        );

        Vec3 noseBottom = noseWorld.add(0, RAY_BOTTOM_Y_OFFSET, 0);

        Vec3[] origins = {
                noseWorld.add(rightDir.scale(-RAY_LATERAL_SPREAD)),
                noseWorld,
                noseWorld.add(rightDir.scale(RAY_LATERAL_SPREAD)),
                noseBottom.add(rightDir.scale(-RAY_LATERAL_SPREAD)),
                noseBottom,
                noseBottom.add(rightDir.scale(RAY_LATERAL_SPREAD))
        };
        Vec3[] directions = {
                forwardDir, forwardDir, forwardDir,
                forwardDownDir, forwardDownDir, forwardDownDir
        };

        for (int i = 0; i < 6; i++) {
            Vec3 end = origins[i].add(directions[i].scale(rayLength));
            debugRayStarts[i] = origins[i];
            debugRayEnds[i] = end;
            debugRayHits[i] = null;
            rayHitResults[i] = null;
            rayDistances[i] = -1;

            BlockHitResult result = this.level().clip(new ClipContext(
                    origins[i], end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.of(this)));

            if (result.getType() == HitResult.Type.BLOCK) {
                double hitDist = result.getLocation().distanceTo(origins[i]);
                debugRayHits[i] = result.getLocation();
                rayHitResults[i] = result;
                rayDistances[i] = hitDist;
            }
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 movementInput) {
        return rideVelocity;
    }

    @Override
    protected float getRiddenSpeed(Player driver) {
        return (float) rideVelocity.length();
    }

    // --- Glow en edit mode ---

    @Override
    public boolean isCurrentlyGlowing() {
        return isEditMode() || super.isCurrentlyGlowing();
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            collisionHandler.tick();
            serverUnstick();

            if (this.isVehicle() && !this.onGround()) {
                customAboveGroundTicks++;
            } else {
                customAboveGroundTicks = 0;
            }
        }

        if (!this.level().isClientSide() && isEditMode() && getEditingPlayerUUID().isPresent()) {
            Player editor = this.level().getPlayerByUUID(getEditingPlayerUUID().get());
            if (editor == null || editor.distanceTo(this) > EDIT_MODE_MAX_DISTANCE) {
                exitEditMode();
            }
        }

        if (this.level().isClientSide()) {
            boolean shouldBeActive = isEditMode() && isLocalPlayerEditor();
            if (shouldBeActive && !editShaderActive) {
                loadEditShader();
                editShaderActive = true;
            } else if (!shouldBeActive && editShaderActive) {
                unloadEditShader();
                editShaderActive = false;
            }
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (this.level().isClientSide() && editShaderActive) {
            unloadEditShader();
            editShaderActive = false;
        }
    }

    private boolean isLocalPlayerEditor() {
        Optional<UUID> editorUUID = getEditingPlayerUUID();
        if (editorUUID.isEmpty()) return false;
        return editorUUID.get().equals(getLocalPlayerUUID());
    }

    private static UUID getLocalPlayerUUID() {
        return Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
    }

    private static void loadEditShader() {
        Minecraft.getInstance().gameRenderer.loadEffect(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("apica", "shaders/post/edit_mode.json")
        );
    }

    private static void unloadEditShader() {
        Minecraft.getInstance().gameRenderer.shutdownEffect();
    }

    public static boolean isEditShaderActive() {
        return editShaderActive;
    }

    // --- Server-side Unstick ---

    private void serverUnstick() {
        AABB bbox = this.getBoundingBox();
        if (this.level().noCollision(this, bbox)) {
            return;
        }
        double originalY = this.getY();
        for (int i = 1; i <= 5; i++) {
            double testY = originalY + i * 0.25;
            AABB testBox = bbox.move(0, testY - originalY, 0);
            if (this.level().noCollision(this, testBox)) {
                this.setPos(this.getX(), testY, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
                return;
            }
        }
    }

    // --- AI ---

    @Override
    protected void registerGoals() {
        // Pas de goals
    }

    // --- Save/Load ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Species", getSpeciesId());
        tag.putInt("SaddleVariant", this.entityData.get(DATA_SADDLE_VARIANT));
        tag.putInt("WingProtectorVariant", this.entityData.get(DATA_WING_PROTECTOR_VARIANT));
        tag.putInt("ControlLeftVariant", this.entityData.get(DATA_CONTROL_LEFT_VARIANT));
        tag.putInt("ControlRightVariant", this.entityData.get(DATA_CONTROL_RIGHT_VARIANT));
        savePartStack(tag, "SaddleStack", DATA_SADDLE_STACK);
        savePartStack(tag, "WingProtectorStack", DATA_WING_PROTECTOR_STACK);
        savePartStack(tag, "ControlLeftStack", DATA_CONTROL_LEFT_STACK);
        savePartStack(tag, "ControlRightStack", DATA_CONTROL_RIGHT_STACK);
        Optional<UUID> ownerUuid = getOwnerUUID();
        if (ownerUuid.isPresent()) {
            tag.putUUID("OwnerUUID", ownerUuid.get());
            tag.putString("OwnerName", getOwnerName());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Species")) setSpeciesId(tag.getString("Species"));
        if (tag.contains("SaddleVariant")) this.entityData.set(DATA_SADDLE_VARIANT, tag.getInt("SaddleVariant"));
        if (tag.contains("WingProtectorVariant")) this.entityData.set(DATA_WING_PROTECTOR_VARIANT, tag.getInt("WingProtectorVariant"));
        if (tag.contains("ControlLeftVariant")) this.entityData.set(DATA_CONTROL_LEFT_VARIANT, tag.getInt("ControlLeftVariant"));
        if (tag.contains("ControlRightVariant")) this.entityData.set(DATA_CONTROL_RIGHT_VARIANT, tag.getInt("ControlRightVariant"));
        loadPartStack(tag, "SaddleStack", DATA_SADDLE_STACK);
        loadPartStack(tag, "WingProtectorStack", DATA_WING_PROTECTOR_STACK);
        loadPartStack(tag, "ControlLeftStack", DATA_CONTROL_LEFT_STACK);
        loadPartStack(tag, "ControlRightStack", DATA_CONTROL_RIGHT_STACK);
        recomputeSettings();
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
            this.entityData.set(DATA_OWNER_NAME, tag.getString("OwnerName"));
        }
    }

    private void savePartStack(CompoundTag tag, String key, EntityDataAccessor<ItemStack> accessor) {
        ItemStack stack = this.entityData.get(accessor);
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(this.registryAccess(), new CompoundTag()));
        }
    }

    private void loadPartStack(CompoundTag tag, String key, EntityDataAccessor<ItemStack> accessor) {
        if (tag.contains(key)) {
            ItemStack stack = ItemStack.parse(this.registryAccess(), tag.getCompound(key))
                    .orElse(ItemStack.EMPTY);
            this.entityData.set(accessor, stack);
        }
    }

    // --- Death drops ---

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (HoverbikePart part : HoverbikePart.values()) {
            ItemStack stack = getPartStack(part);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy(), 0.0f);
            }
        }
    }

    /** Retourne l'index de variante pour une partie donnee. */
    public int getPartVariant(HoverbikePart part) {
        return switch (part) {
            case SADDLE -> this.entityData.get(DATA_SADDLE_VARIANT);
            case WING_PROTECTOR -> this.entityData.get(DATA_WING_PROTECTOR_VARIANT);
            case CONTROL_LEFT -> this.entityData.get(DATA_CONTROL_LEFT_VARIANT);
            case CONTROL_RIGHT -> this.entityData.get(DATA_CONTROL_RIGHT_VARIANT);
        };
    }

    /** Definit l'index de variante pour une partie donnee. */
    public void setPartVariant(HoverbikePart part, int variant) {
        switch (part) {
            case SADDLE -> this.entityData.set(DATA_SADDLE_VARIANT, variant);
            case WING_PROTECTOR -> this.entityData.set(DATA_WING_PROTECTOR_VARIANT, variant);
            case CONTROL_LEFT -> this.entityData.set(DATA_CONTROL_LEFT_VARIANT, variant);
            case CONTROL_RIGHT -> this.entityData.set(DATA_CONTROL_RIGHT_VARIANT, variant);
        }
    }

    // --- Part Stacks ---

    public ItemStack getPartStack(HoverbikePart part) {
        return switch (part) {
            case SADDLE -> this.entityData.get(DATA_SADDLE_STACK);
            case WING_PROTECTOR -> this.entityData.get(DATA_WING_PROTECTOR_STACK);
            case CONTROL_LEFT -> this.entityData.get(DATA_CONTROL_LEFT_STACK);
            case CONTROL_RIGHT -> this.entityData.get(DATA_CONTROL_RIGHT_STACK);
        };
    }

    public void setPartStack(HoverbikePart part, ItemStack stack) {
        switch (part) {
            case SADDLE -> this.entityData.set(DATA_SADDLE_STACK, stack.copy());
            case WING_PROTECTOR -> this.entityData.set(DATA_WING_PROTECTOR_STACK, stack.copy());
            case CONTROL_LEFT -> this.entityData.set(DATA_CONTROL_LEFT_STACK, stack.copy());
            case CONTROL_RIGHT -> this.entityData.set(DATA_CONTROL_RIGHT_STACK, stack.copy());
        }
        if (!stack.isEmpty() && stack.getItem() instanceof com.chapeau.apica.common.item.mount.HoverbikePartItem partItem) {
            setPartVariant(part, partItem.getVariantIndex());
        }
        recomputeSettings();
    }

    public ItemStack[] getAllPartStacks() {
        return new ItemStack[]{
                getPartStack(HoverbikePart.SADDLE),
                getPartStack(HoverbikePart.WING_PROTECTOR),
                getPartStack(HoverbikePart.CONTROL_LEFT),
                getPartStack(HoverbikePart.CONTROL_RIGHT)
        };
    }

    public void recomputeSettings() {
        this.settings = HoverbikeSettingsComputer.compute(getAllPartStacks());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (this.level().isClientSide()) {
            if (accessor.equals(DATA_SADDLE_STACK) || accessor.equals(DATA_WING_PROTECTOR_STACK)
                    || accessor.equals(DATA_CONTROL_LEFT_STACK) || accessor.equals(DATA_CONTROL_RIGHT_STACK)) {
                recomputeSettings();
            }
        }
    }

    // --- Misc ---

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean dismountsUnderwater() { return false; }

    // --- Debug Getters ---

    public HoverbikeMode getMode() { return mode; }

    public HoverbikeMode getSynchedMode() {
        byte ordinal = this.entityData.get(DATA_MODE);
        HoverbikeMode[] modes = HoverbikeMode.values();
        return (ordinal >= 0 && ordinal < modes.length) ? modes[ordinal] : HoverbikeMode.HOVER;
    }

    public boolean isSynchedSprinting() { return this.entityData.get(DATA_SPRINT); }
    public Vec3 getRideVelocity() { return rideVelocity; }
    public double getForwardSpeed() { return rideVelocity.z; }
    public boolean isSprintPressed() { return sprintPressed; }
    public HoverbikeSettings getSettings() { return settings; }
    public float getGaugeLevel() { return gaugeLevel; }
    public boolean isJumpPressed() { return jumpPressed; }
    public float getLastYawDelta() { return lastYawDelta; }
    public float getPrevYawDelta() { return prevYawDelta; }

    public boolean isDebugRaysActive() { return debugRaysActive; }
    public Vec3[] getDebugRayStarts() { return debugRayStarts; }
    public Vec3[] getDebugRayEnds() { return debugRayEnds; }
    public Vec3[] getDebugRayHits() { return debugRayHits; }
    public double[] getRayDistances() { return rayDistances; }
}
