/**
 * ============================================================
 * [RideableBeeEntity.java]
 * Description: Entité abeille chevauchable - Refonte complète basée sur Cobblemon PokemonEntity
 * ============================================================
 *
 * PATTERN: Basé sur Cobblemon PokemonEntity.kt
 * - travel() L1815-1875: Conversion vélocité locale → monde
 * - tickRidden() L2232-2283: Tick du behaviour actif
 * - getRiddenInput() L2397-2400: Retourne vélocité du behaviour
 *
 * ARCHITECTURE:
 * - RidingController: Orchestre les behaviours
 * - RidingBehaviour: Logique stateless (HorseBehaviour)
 * - RidingBehaviourState: État mutable (HorseState)
 * - RidingBehaviourSettings: Config constante (HorseSettings)
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | RidingController        | Orchestration        | Gestion du behaviour actif     |
 * | HorseBehaviour          | Logique mouvement    | Calculs vélocité/rotation      |
 * | HorseSettings           | Configuration        | Paramètres vitesse/saut        |
 * | HorseState              | État runtime         | sprinting, jumpTicks, etc.     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerEntities.java: Enregistrement
 * - RideableBeeRenderer.java: Rendu
 * - RideableBeeDebugHud.java: Affichage debug
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import com.chapeau.beemancer.common.entity.mount.behaviour.*;
import com.chapeau.beemancer.common.entity.mount.behaviour.types.land.HorseBehaviour;
import com.chapeau.beemancer.common.entity.mount.behaviour.types.land.HorseSettings;
import com.chapeau.beemancer.common.entity.mount.behaviour.types.land.HorseState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Entité abeille chevauchable.
 * Architecture Cobblemon avec simplifications Beemancer.
 */
public class RideableBeeEntity extends Mob implements PlayerRideable {

    // --- Riding System ---
    @Nullable
    private RidingController ridingController;

    @Nullable
    private RidingBehaviourState previousRidingState;

    // --- Settings ---
    private final HorseSettings horseSettings;

    // --- Constructor ---

    public RideableBeeEntity(EntityType<? extends RideableBeeEntity> entityType, Level level) {
        super(entityType, level);
        this.horseSettings = HorseSettings.createBeeDefaults();
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
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
        if (passenger instanceof Player player) {
            return player;
        }

        // Pas de passager - reset le controller
        if (ridingController != null) {
            ridingController.reset();
        }
        return null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player) {
            // Initialiser le riding controller
            if (ridingController == null) {
                ridingController = new RidingController(this, horseSettings);
            }
            ridingController.initContext();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (this.getPassengers().isEmpty()) {
            // Reset quand plus de passagers
            if (ridingController != null) {
                ridingController.reset();
            }
            previousRidingState = null;
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() * 0.75, 0);
    }

    // --- Control ---

    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger instanceof Player player) {
            return player.isLocalPlayer();
        }
        return false;
    }

    // --- Riding Core (Pattern Cobblemon) ---

    @Override
    public void tick() {
        super.tick();

        // Tick le controller
        if (ridingController != null) {
            ridingController.tick();
        }

        // Sauvegarder l'état précédent pour sync
        if (ridingController != null && ridingController.getContext() != null) {
            previousRidingState = ridingController.getContext().getState().copy();
        }
    }

    /**
     * Appelé chaque tick quand un joueur est monté.
     * Pattern Cobblemon: PokemonEntity.tickRidden() L2232-2283
     */
    @Override
    protected void tickRidden(Player driver, Vec3 movementInput) {
        super.tickRidden(driver, movementInput);

        if (ridingController == null || !ridingController.isRidingAvailable()) {
            return;
        }

        ridingController.ifRidingAvailable((behaviour, settings, state) -> {
            // Tick le behaviour
            behaviour.tick(settings, state, this, driver, movementInput);

            // Stamina toujours à 1.0 dans Beemancer
            state.setStamina(1.0f, true);

            // Sauvegarder yRot avant modification
            this.yRotO = this.getYRot();

            // Calculer et appliquer la rotation
            Vec2 rotation = behaviour.rotation(settings, state, this, driver);
            this.setRot(rotation.y, 0.0f); // xRot = 0 pour entité terrestre

            // Sync rotations
            this.yHeadRot = this.getYRot();
            this.yBodyRot = this.getYRot();
        });
    }

    /**
     * Calcule et applique le mouvement.
     * Pattern Cobblemon: PokemonEntity.travel() L1815-1875
     */
    @Override
    public void travel(Vec3 movementInput) {
        // Pas de passager contrôleur - comportement par défaut
        if (this.getControllingPassenger() == null || !(this.getControllingPassenger() instanceof Player)) {
            super.travel(movementInput);
            return;
        }

        Player driver = (Player) this.getControllingPassenger();

        if (ridingController == null || !ridingController.isRidingAvailable()) {
            super.travel(movementInput);
            return;
        }

        // Récupérer la vélocité du behaviour (en espace local)
        Vec3 localVelocity = ridingController.ifRidingAvailableSupply(Vec3.ZERO, (behaviour, settings, state) ->
                behaviour.velocity(settings, state, this, driver, this.getDeltaMovement())
        );

        // Convertir vélocité locale en espace monde (rotation par yRot)
        // Pattern Cobblemon L1829-1835
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);

        Vec3 worldVelocity = new Vec3(
                localVelocity.x * cosYaw - localVelocity.z * sinYaw,
                localVelocity.y,
                localVelocity.z * cosYaw + localVelocity.x * sinYaw
        );

        // Appliquer inertie (lissage)
        // Pattern Cobblemon L1837-1843
        Vec3 diff = worldVelocity.subtract(this.getDeltaMovement());
        double inertia = ridingController.ifRidingAvailableSupply(0.5, (behaviour, settings, state) ->
                behaviour.inertia(settings, state, this)
        );
        this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(inertia)));

        // Appliquer le mouvement
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    /**
     * Retourne la vélocité calculée par le behaviour.
     * Pattern Cobblemon: PokemonEntity.getRiddenInput() L2397-2400
     */
    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 movementInput) {
        if (ridingController == null || !ridingController.isRidingAvailable()) {
            return Vec3.ZERO;
        }

        return ridingController.ifRidingAvailableSupply(Vec3.ZERO, (behaviour, settings, state) ->
                behaviour.velocity(settings, state, this, driver, movementInput)
        );
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

    // --- Debug Getters (pour RideableBeeDebugHud) ---

    /**
     * Retourne la vélocité actuelle en espace local.
     */
    public Vec3 getRideVelocity() {
        if (ridingController != null && ridingController.getContext() != null) {
            return ridingController.getContext().getState().getRideVelocity();
        }
        return Vec3.ZERO;
    }

    /**
     * Retourne le jump cooldown actuel.
     */
    public int getJumpCooldown() {
        if (ridingController != null && ridingController.getContext() != null) {
            RidingBehaviourState state = ridingController.getContext().getState();
            if (state instanceof HorseState horseState) {
                return horseState.getJumpTicks();
            }
        }
        return 0;
    }

    /**
     * Retourne si l'abeille sprinte.
     */
    public boolean isSprinting() {
        if (ridingController != null && ridingController.getContext() != null) {
            RidingBehaviourState state = ridingController.getContext().getState();
            if (state instanceof HorseState horseState) {
                return horseState.isSprinting();
            }
        }
        return false;
    }

    /**
     * Retourne le riding mode actuel.
     */
    public RidingMode getRidingMode() {
        if (ridingController != null && ridingController.getContext() != null) {
            RidingBehaviourState state = ridingController.getContext().getState();
            if (state instanceof HorseState horseState) {
                return horseState.isSprinting() ? RidingMode.RUN : RidingMode.WALK;
            }
        }
        return RidingMode.WALK;
    }

    /**
     * Calcule l'état actuel pour le debug.
     */
    public String getDebugState() {
        if (!this.onGround()) {
            if (getJumpCooldown() > 0) {
                return "JUMPING";
            }
            return "AIRBORNE";
        }
        return isSprinting() ? "RUN" : "WALK";
    }

    /**
     * Retourne les settings actuels.
     */
    public HorseSettings getSettings() {
        return horseSettings;
    }

    /**
     * Pour compatibilité avec ancien code - retourne la vitesse forward.
     */
    public float getVelocityForward() {
        return (float) getRideVelocity().z;
    }

    /**
     * Pour compatibilité - synced speed.
     */
    public float getSyncedSpeed() {
        return getVelocityForward();
    }
}
