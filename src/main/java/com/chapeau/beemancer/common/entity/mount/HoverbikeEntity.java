/**
 * ============================================================
 * [HoverbikeEntity.java]
 * Description: Entite hoverbike — moto flottante avec modes Hover et Run
 * ============================================================
 *
 * PATTERN: Inspire de Cobblemon PokemonEntity.kt
 * - travel() L1815-1875: Conversion velocite locale -> monde
 * - tickRidden() L2232-2283: Tick rotation et mode
 * - getRiddenInput() L2397-2400: Retourne velocite du behaviour
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
 * - BeemancerEntities.java: Enregistrement
 * - HoverbikeRenderer.java: Rendu
 * - HoverbikeDebugHud.java: Affichage debug
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.nbt.CompoundTag;
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
 * Hoverbike — moto flottante avec physique velocite/acceleration.
 * Deux modes: HOVER (4 directions, basse vitesse) et RUN (avant uniquement, haute vitesse).
 * Gravite reduite, friction glace en permanence.
 */
public class HoverbikeEntity extends Mob implements PlayerRideable {

    // --- Settings ---
    private final HoverbikeSettings settings;

    // --- Etat interne ---
    private HoverbikeMode mode = HoverbikeMode.HOVER;
    private Vec3 rideVelocity = Vec3.ZERO;

    // --- Constructor ---

    public HoverbikeEntity(EntityType<? extends HoverbikeEntity> entityType, Level level) {
        super(entityType, level);
        this.settings = HoverbikeSettings.createDefaults();
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
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
        return (passenger instanceof Player player) ? player : null;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (this.getPassengers().isEmpty()) {
            mode = HoverbikeMode.HOVER;
            rideVelocity = Vec3.ZERO;
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        // Joueur assis sur le cube arriere, un peu au-dessus
        return new Vec3(0, dimensions.height() + 0.1, 0);
    }

    // --- Control ---

    @Override
    public boolean isControlledByLocalInstance() {
        LivingEntity passenger = this.getControllingPassenger();
        return (passenger instanceof Player player) && player.isLocalPlayer();
    }

    // --- Core Riding Logic (Pattern Cobblemon) ---

    /**
     * Appele chaque tick quand un joueur est monte.
     * Gere la rotation et les transitions de mode.
     */
    @Override
    protected void tickRidden(Player driver, Vec3 movementInput) {
        super.tickRidden(driver, movementInput);

        // Sauvegarder yRot avant modification
        this.yRotO = this.getYRot();

        // Lire input
        float forward = Math.signum(driver.zza);
        boolean sprinting = driver.isSprinting();

        // Calculer et appliquer la rotation
        float yawDelta = HoverbikePhysics.calculateYawDelta(
                driver.getYRot(), this.getYRot(),
                rideVelocity.z, mode, settings
        );
        this.setRot(this.getYRot() + yawDelta, 0.0f);

        // Sync rotations
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();

        // Transitions de mode
        if (mode == HoverbikeMode.HOVER) {
            if (HoverbikePhysics.shouldTransitionToRun(forward, sprinting, rideVelocity.z, settings)) {
                mode = HoverbikeMode.RUN;
            }
        } else {
            if (HoverbikePhysics.shouldTransitionToHover(rideVelocity.z, settings)) {
                mode = HoverbikeMode.HOVER;
            }
        }
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

        // Convertir local -> world
        Vec3 worldVelocity = HoverbikePhysics.localToWorld(rideVelocity, this.getYRot());

        // Appliquer avec inertie (smooth glace)
        Vec3 diff = worldVelocity.subtract(this.getDeltaMovement());
        double inertia = 0.85;
        this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(inertia)));

        // Appliquer le mouvement
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    protected Vec3 getRiddenInput(Player driver, Vec3 movementInput) {
        return rideVelocity;
    }

    @Override
    protected float getRiddenSpeed(Player driver) {
        return (float) rideVelocity.length();
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

    // --- Debug Getters ---

    public HoverbikeMode getMode() {
        return mode;
    }

    public Vec3 getRideVelocity() {
        return rideVelocity;
    }

    public double getForwardSpeed() {
        return rideVelocity.z;
    }

    public HoverbikeSettings getSettings() {
        return settings;
    }
}
