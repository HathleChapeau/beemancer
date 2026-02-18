/**
 * ============================================================
 * [HoverbikeEntity.java]
 * Description: Entite hoverbike — moto flottante avec modes Hover et Run
 * ============================================================
 *
 * PATTERN: Inspire de Cobblemon PokemonEntity.kt
 * - travel() L1815-1875: Conversion velocite locale -> monde
 * - tickRidden() L2232-2283: Tick rotation et mode
 * - getDefaultGravity() L2494-2502: Desactive vanilla gravity quand monte
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeMode       | Enum mode            | Etat courant                   |
 * | HoverbikeSettings   | Constantes physiques | Reference                      |
 * | HoverbikePhysics    | Calculs              | velocity, rotation, transitions|
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

import java.util.Optional;
import java.util.UUID;

/**
 * Hoverbike — moto flottante avec physique velocite/acceleration.
 * Deux modes: HOVER (4 directions, basse vitesse) et RUN (avant uniquement, haute vitesse).
 * Gravite reduite, friction glace en permanence.
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

    // --- Part Variants (synched pour rendu cote client) ---
    private static final EntityDataAccessor<Integer> DATA_CHASSIS_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COEUR_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROPULSEUR_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RADIATEUR_VARIANT =
            SynchedEntityData.defineId(HoverbikeEntity.class, EntityDataSerializers.INT);

    // --- Settings ---
    private final HoverbikeSettings settings;

    // --- Etat interne ---
    private HoverbikeMode mode = HoverbikeMode.HOVER;
    private Vec3 rideVelocity = Vec3.ZERO;
    private boolean sprintPressed = false;
    private boolean jumpPressed = false;
    private float gaugeLevel = 1.0f;

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
        builder.define(DATA_CHASSIS_VARIANT, 0);
        builder.define(DATA_COEUR_VARIANT, 0);
        builder.define(DATA_PROPULSEUR_VARIANT, 0);
        builder.define(DATA_RADIATEUR_VARIANT, 0);
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.STEP_HEIGHT, 1.3);
    }

    // --- Gravity Override (Pattern Cobblemon PokemonEntity.kt L2494-2502) ---

    /**
     * Desactive la gravite vanilla quand monte.
     * Toute la gravite est geree manuellement dans HoverbikePhysics.
     * Evite le flickering onGround cause par la superposition de deux systemes de gravite.
     */
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
        // Shift+click : toggle edit mode
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

        // Click normal : monter (seulement si pas en edit mode et pas deja monte)
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
    }

    private void exitEditMode() {
        this.entityData.set(DATA_EDIT_MODE, false);
        this.entityData.set(DATA_EDITING_PLAYER, Optional.empty());
    }

    public boolean isEditMode() {
        return this.entityData.get(DATA_EDIT_MODE);
    }

    public Optional<UUID> getEditingPlayerUUID() {
        return this.entityData.get(DATA_EDITING_PLAYER);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return (passenger instanceof Player player) ? player : null;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        // Flag anti-push demontage : ne pas repousser le rider qui vient de descendre
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

    /**
     * Filtre d'exclusion pour les collisions.
     * Par defaut tout est inclus sauf : spectateurs, passager, projectiles, items, noPhysics.
     */
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

    // --- Control ---

    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger instanceof Player player) {
            return player.isLocalPlayer();
        }
        return super.isControlledByLocalInstance();
    }

    // --- Core Riding Logic (Pattern Cobblemon) ---

    /**
     * Appele chaque tick quand un joueur est monte.
     * Gere la rotation, la lecture du sprint key, et les transitions de mode.
     */
    @Override
    protected void tickRidden(Player driver, Vec3 movementInput) {
        super.tickRidden(driver, movementInput);

        // Sauvegarder yRot avant modification
        this.yRotO = this.getYRot();

        // Lire input
        float forward = Math.signum(driver.zza);

        // Lire sprint/jump key directement (pattern Cobblemon HorseBehaviour.kt L144)
        // driver.isSprinting() ne fonctionne pas quand le joueur monte une entite
        if (this.level().isClientSide) {
            this.sprintPressed = isSprintKeyDown();
            this.jumpPressed = isJumpKeyDown();
        }

        // Jauge d'envol : se remplit au sol, sans saut, et uniquement en HOVER
        if (!jumpPressed && this.onGround() && mode == HoverbikeMode.HOVER) {
            gaugeLevel = Math.min(1.0f, gaugeLevel + (float) settings.gaugeFillRate());
        }

        // Calculer et appliquer la rotation
        float yawDelta = HoverbikePhysics.calculateYawDelta(
                driver.getYRot(), this.getYRot(),
                rideVelocity.z, mode, settings
        );
        this.prevYawDelta = this.lastYawDelta;
        this.lastYawDelta = yawDelta;
        this.setRot(this.getYRot() + yawDelta, 0.0f);

        // Sync rotations
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();

        // Transitions de mode
        if (mode == HoverbikeMode.HOVER) {
            if (HoverbikePhysics.shouldTransitionToRun(forward, sprintPressed, rideVelocity.z, settings)) {
                mode = HoverbikeMode.RUN;
            }
        } else {
            if (HoverbikePhysics.shouldTransitionToHover(rideVelocity.z, settings)) {
                mode = HoverbikeMode.HOVER;
            }
        }

        // Syncher mode et sprint pour les autres clients
        this.entityData.set(DATA_MODE, (byte) mode.ordinal());
        this.entityData.set(DATA_SPRINT, sprintPressed);
    }

    /**
     * Lecture du sprint key cote client.
     * Isole dans une methode separee pour que la reference a Minecraft
     * ne soit resolue que cote client.
     */
    private static boolean isSprintKeyDown() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    /**
     * Lecture du jump key cote client.
     */
    private static boolean isJumpKeyDown() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    /**
     * Calcule et applique le mouvement.
     * Pattern Cobblemon: PokemonEntity.travel() L1815-1875
     */
    @Override
    public void travel(Vec3 movementInput) {
        Player driver = (this.getControllingPassenger() instanceof Player p) ? p : null;
        if (driver == null) {
            super.travel(movementInput);
            return;
        }

        // Lire input joueur
        float forward = Math.signum(driver.zza);
        float strafe = Math.signum(driver.xxa);

        // Calculer la velocite locale selon le mode
        if (mode == HoverbikeMode.RUN) {
            rideVelocity = HoverbikePhysics.calculateRunVelocity(
                    forward, rideVelocity, this.onGround(), settings
            );
        } else {
            rideVelocity = HoverbikePhysics.calculateHoverVelocity(
                    forward, strafe, rideVelocity, this.onGround(), settings
            );
        }

        // Envol : maintenir saut = montee douce qui consomme la jauge
        // En RUN: drain x1.3, jauge ne se remplit pas
        if (jumpPressed && gaugeLevel > 0) {
            rideVelocity = new Vec3(rideVelocity.x, settings.liftSpeed(), rideVelocity.z);
            float drain = (float) settings.gaugeDrainRate();
            if (mode == HoverbikeMode.RUN) {
                drain *= RUN_GAUGE_DRAIN_MULTIPLIER;
            }
            gaugeLevel = Math.max(0, gaugeLevel - drain);
        }

        // Convertir local -> world
        Vec3 worldVelocity = HoverbikePhysics.localToWorld(rideVelocity, this.getYRot());

        // Appliquer avec inertie (smooth glace)
        Vec3 diff = worldVelocity.subtract(this.getDeltaMovement());
        double inertia = 0.85;
        this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(inertia)));

        // Anti-bounce trick (Pattern Cobblemon PokemonEntity.kt L1785-1787)
        // Micro offset Y negatif quand au sol pour eviter le flickering onGround
        Vec3 movement = this.getDeltaMovement();
        if (this.onGround() && movement.y == 0.0) {
            movement = movement.subtract(0, 0.0001, 0);
            this.setDeltaMovement(movement);
        }

        // Raycasts predictifs : mesurer les distances aux blocs devant le nez (debug only)
        updatePredictiveRaycasts();

        // Appliquer le mouvement (vanilla move gere step-up et collision)
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Collision feedback (pattern Cobblemon HorseBehaviour L298-300) :
        // Rediriger la velocite dans la direction du mouvement reel au lieu de la reduire.
        // Le bike glisse le long des murs au lieu de s'arreter net.
        if (this.horizontalCollision) {
            Vec3 actualDelta = this.getDeltaMovement();
            double actualHorizSpeed = actualDelta.horizontalDistance();
            if (actualHorizSpeed > 0.001) {
                double rideSpeed = rideVelocity.horizontalDistance();
                Vec3 redirected = actualDelta.normalize().scale(rideSpeed);
                rideVelocity = new Vec3(redirected.x, rideVelocity.y, redirected.z);
            }
        }

        // Collision entites via probes (server-side uniquement)
        if (!this.level().isClientSide()) {
            AABB[] probes = HoverbikeCollisionGeometry.calculateWorldBoxes(this.position(), this.getYRot());
            collisionHandler.resolveEntityCollisions(this, probes, this.level(), settings);
        }
    }

    // =========================================================================
    // RAYCASTS PREDICTIFS — 6 rays depuis le nez du bike (informatifs)
    // =========================================================================

    /** Offset local du nez du bike (Z = avant). */
    private static final Vec3 NOSE_OFFSET = new Vec3(0, 0.4, 0.8);

    /** Ecart lateral des rays gauche/droite par rapport au centre. */
    private static final double RAY_LATERAL_SPREAD = 0.4;

    /** Offset Y vers le bas pour l'origine des 3 rays du dessous. */
    private static final double RAY_BOTTOM_Y_OFFSET = -0.3;

    /** Angle de plongee des 3 rays bas (radians, ~30 degres). */
    private static final double RAY_BOTTOM_PITCH = -0.52;

    /** Longueur minimale des rays (a vitesse quasi nulle). */
    private static final double RAY_MIN_LENGTH = 0.5;

    /** Longueur maximale des rays (a vitesse max). */
    private static final double RAY_MAX_LENGTH = 3.0;

    /** Seuil de vitesse en dessous duquel les raycasts ne s'activent pas. */
    private static final double RAY_SPEED_THRESHOLD = 0.03;

    /** Distances mesurees par les 6 raycasts (-1 = pas de hit). */
    private final double[] rayDistances = new double[6];

    /**
     * Lance 6 raycasts depuis le nez du bike (informatifs uniquement).
     * Ne modifie PAS la velocite — stocke les distances pour debug et usage futur.
     * 3 horizontaux (gauche, centre, droite) pointant devant,
     * 3 diagonaux bas (bas-gauche, bas-centre, bas-droite) pointant devant-bas.
     * La longueur des rays est proportionnelle a la vitesse.
     */
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

        // Longueur des rays proportionnelle a la vitesse
        double maxSpeed = (mode == HoverbikeMode.RUN) ? settings.maxRunSpeed() : settings.maxHoverSpeed();
        double speedRatio = Mth.clamp(speed / maxSpeed, 0.0, 1.0);
        double rayLength = Mth.lerp(speedRatio, RAY_MIN_LENGTH, RAY_MAX_LENGTH);

        // Position du nez en monde
        Vec3 noseWorld = this.position().add(
                NOSE_OFFSET.x * cosYaw - NOSE_OFFSET.z * sinYaw,
                NOSE_OFFSET.y,
                NOSE_OFFSET.z * cosYaw + NOSE_OFFSET.x * sinYaw
        );

        // Direction avant (horizontale)
        Vec3 forwardDir = new Vec3(-sinYaw, 0, cosYaw);

        // Direction laterale (droite)
        Vec3 rightDir = new Vec3(cosYaw, 0, sinYaw);

        // Direction avant-bas (diagonale plongee)
        double pitchCos = Math.cos(RAY_BOTTOM_PITCH);
        double pitchSin = Math.sin(RAY_BOTTOM_PITCH);
        Vec3 forwardDownDir = new Vec3(
                -sinYaw * pitchCos,
                pitchSin,
                cosYaw * pitchCos
        );

        // Origine des 3 rays bas (meme X/Z que le nez, decale en Y)
        Vec3 noseBottom = noseWorld.add(0, RAY_BOTTOM_Y_OFFSET, 0);

        // Origines et directions des 6 rays
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

    /**
     * L'entite brille en edit mode (contour visible).
     * Cote client uniquement pour eviter de modifier le synched glowing tag.
     */
    @Override
    public boolean isCurrentlyGlowing() {
        return isEditMode() || super.isCurrentlyGlowing();
    }

    // --- Tick (edit mode + shader management) ---

    @Override
    public void tick() {
        super.tick();

        // Server: tick collision handler, unstick, anti-fly kick
        if (!this.level().isClientSide()) {
            collisionHandler.tick();

            // Unstick server-side : si le bounding box du bike chevauche un bloc solide,
            // le pousser vers le haut jusqu'a ce qu'il soit libre.
            // Cote serveur = autoritaire, pas de "moved wrongly".
            serverUnstick();

            // Anti-fly kick : reset les compteurs vanilla quand le bike vole normalement
            if (this.isVehicle() && !this.onGround()) {
                customAboveGroundTicks++;
            } else {
                customAboveGroundTicks = 0;
            }
        }

        // Server: verifier que l'editeur est toujours a portee
        if (!this.level().isClientSide() && isEditMode() && getEditingPlayerUUID().isPresent()) {
            Player editor = this.level().getPlayerByUUID(getEditingPlayerUUID().get());
            if (editor == null || editor.distanceTo(this) > EDIT_MODE_MAX_DISTANCE) {
                exitEditMode();
            }
        }

        // Client: gerer le shader d'assombrissement
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
        // Nettoyer le shader si l'entite est retiree pendant l'edit mode
        if (this.level().isClientSide() && editShaderActive) {
            unloadEditShader();
            editShaderActive = false;
        }
    }

    /**
     * Verifie si le joueur local est l'editeur de cette moto.
     */
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

    /**
     * Indique si le shader d'edit mode est actif (utilise par HoverbikeEditModeEffect).
     */
    public static boolean isEditShaderActive() {
        return editShaderActive;
    }

    // --- Server-side Unstick ---

    /**
     * Verifie cote serveur si le bounding box du bike chevauche un bloc solide.
     * Si oui, pousse le bike vers le haut par petits increments jusqu'a une position libre.
     * Cote serveur = autoritaire, elimine les "moved wrongly" quand le bike est coince.
     */
    private void serverUnstick() {
        AABB bbox = this.getBoundingBox();
        if (this.level().noCollision(this, bbox)) {
            return;
        }

        // Le bike est coince dans un bloc solide — chercher une position libre au-dessus
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
        // Pas de goals — l'entite ne fait rien seule
    }

    // --- Save/Load ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChassisVariant", this.entityData.get(DATA_CHASSIS_VARIANT));
        tag.putInt("CoeurVariant", this.entityData.get(DATA_COEUR_VARIANT));
        tag.putInt("PropulseurVariant", this.entityData.get(DATA_PROPULSEUR_VARIANT));
        tag.putInt("RadiateurVariant", this.entityData.get(DATA_RADIATEUR_VARIANT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChassisVariant")) this.entityData.set(DATA_CHASSIS_VARIANT, tag.getInt("ChassisVariant"));
        if (tag.contains("CoeurVariant")) this.entityData.set(DATA_COEUR_VARIANT, tag.getInt("CoeurVariant"));
        if (tag.contains("PropulseurVariant")) this.entityData.set(DATA_PROPULSEUR_VARIANT, tag.getInt("PropulseurVariant"));
        if (tag.contains("RadiateurVariant")) this.entityData.set(DATA_RADIATEUR_VARIANT, tag.getInt("RadiateurVariant"));
    }

    /** Retourne l'index de variante pour une partie donnee. */
    public int getPartVariant(HoverbikePart part) {
        return switch (part) {
            case CHASSIS -> this.entityData.get(DATA_CHASSIS_VARIANT);
            case COEUR -> this.entityData.get(DATA_COEUR_VARIANT);
            case PROPULSEUR -> this.entityData.get(DATA_PROPULSEUR_VARIANT);
            case RADIATEUR -> this.entityData.get(DATA_RADIATEUR_VARIANT);
        };
    }

    /** Definit l'index de variante pour une partie donnee. */
    public void setPartVariant(HoverbikePart part, int variant) {
        switch (part) {
            case CHASSIS -> this.entityData.set(DATA_CHASSIS_VARIANT, variant);
            case COEUR -> this.entityData.set(DATA_COEUR_VARIANT, variant);
            case PROPULSEUR -> this.entityData.set(DATA_PROPULSEUR_VARIANT, variant);
            case RADIATEUR -> this.entityData.set(DATA_RADIATEUR_VARIANT, variant);
        }
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

    // --- Debug Getters ---

    public HoverbikeMode getMode() {
        return mode;
    }

    /**
     * Retourne le mode synched depuis EntityData (pour les clients distants).
     */
    public HoverbikeMode getSynchedMode() {
        byte ordinal = this.entityData.get(DATA_MODE);
        HoverbikeMode[] modes = HoverbikeMode.values();
        return (ordinal >= 0 && ordinal < modes.length) ? modes[ordinal] : HoverbikeMode.HOVER;
    }

    /**
     * Retourne le sprint synched depuis EntityData (pour les clients distants).
     */
    public boolean isSynchedSprinting() {
        return this.entityData.get(DATA_SPRINT);
    }

    public Vec3 getRideVelocity() {
        return rideVelocity;
    }

    public double getForwardSpeed() {
        return rideVelocity.z;
    }

    public boolean isSprintPressed() {
        return sprintPressed;
    }

    public HoverbikeSettings getSettings() {
        return settings;
    }

    public float getGaugeLevel() {
        return gaugeLevel;
    }

    public boolean isJumpPressed() {
        return jumpPressed;
    }

    public float getLastYawDelta() {
        return lastYawDelta;
    }

    public float getPrevYawDelta() {
        return prevYawDelta;
    }

    // --- Debug Raycasts Getters ---

    public boolean isDebugRaysActive() {
        return debugRaysActive;
    }

    public Vec3[] getDebugRayStarts() {
        return debugRayStarts;
    }

    public Vec3[] getDebugRayEnds() {
        return debugRayEnds;
    }

    public Vec3[] getDebugRayHits() {
        return debugRayHits;
    }

    public double[] getRayDistances() {
        return rayDistances;
    }
}
