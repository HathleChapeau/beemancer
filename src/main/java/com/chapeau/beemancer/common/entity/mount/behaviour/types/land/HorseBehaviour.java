/**
 * ============================================================
 * [HorseBehaviour.java]
 * Description: Comportement terrestre style cheval - logique complète de mouvement
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon HorseBehaviour.kt L55-545
 * - Mouvement terrestre avec inertie
 * - Rotation influencée par la direction du regard
 * - Sprint avec gestion de vitesse
 * - Saut avec force variable
 *
 * CALCULS CLÉS (Cobblemon lignes):
 * - Gravité: L340 (9.8/20)*0.2*0.6
 * - Friction: L352 min(0.03 * sign, velocity)
 * - Rotation: L217-250 calcRotAmount()
 * - Vélocité: L264-380 calculateRideSpaceVel()
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HorseSettings       | Settings constants   | Paramètres de vitesse/saut     |
 * | HorseState          | État mutable         | sprinting, jumpTicks, etc.     |
 * | RidingBehaviour     | Interface parent     | Contrat à implémenter          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingBehaviours.java: Enregistré au démarrage
 * - RidingController.java: Appelé via getBehaviour()
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour.types.land;

import com.chapeau.beemancer.common.entity.mount.behaviour.RidingBehaviour;
import com.chapeau.beemancer.common.entity.mount.behaviour.RidingStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Comportement terrestre style cheval.
 * STATELESS - une seule instance partagée.
 *
 * Pattern Cobblemon: HorseBehaviour
 */
public class HorseBehaviour implements RidingBehaviour<HorseSettings, HorseState> {

    public static final ResourceLocation KEY = HorseSettings.KEY;

    // --- Constantes physiques (Cobblemon L340-341) ---
    private static final double GRAVITY = (9.8 / 20.0) * 0.2 * 0.6;
    private static final double TERMINAL_VELOCITY = 2.0;
    private static final double GROUND_FRICTION = 0.03;

    // --- RidingBehaviour Interface ---

    @Override
    public ResourceLocation getKey() {
        return KEY;
    }

    @Override
    public RidingStyle getRidingStyle(HorseSettings settings, HorseState state) {
        return RidingStyle.LAND;
    }

    @Override
    public boolean isActive(HorseSettings settings, HorseState state, LivingEntity vehicle) {
        // Actif si au sol et pas dans l'eau
        if (vehicle.isInWater() || vehicle.isUnderWater()) {
            return false;
        }
        return vehicle.onGround();
    }

    @Override
    public void tick(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver, Vec3 input) {
        if (vehicle.level().isClientSide) {
            handleSprinting(settings, state, driver);
            inAirCheck(state, vehicle);
            state.setWalking(state.getRideVelocity().horizontalDistance() > 0.01);
        }
    }

    @Override
    public float speed(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver) {
        return (float) state.getRideVelocity().length();
    }

    @Override
    public Vec2 rotation(HorseSettings settings, HorseState state, LivingEntity vehicle, LivingEntity driver) {
        float turnAmount = calcRotAmount(settings, state, vehicle, driver);
        return new Vec2(vehicle.getXRot(), vehicle.getYRot() + turnAmount);
    }

    @Override
    public Vec3 velocity(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver, Vec3 input) {
        Vec3 newVelocity = calculateRideSpaceVel(settings, state, vehicle, driver);
        state.setRideVelocity(newVelocity);
        return newVelocity;
    }

    @Override
    public boolean canJump(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver) {
        // Le saut est géré directement dans calculateRideSpaceVel
        return false;
    }

    @Override
    public Vec3 jumpForce(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver, int jumpStrength) {
        // Le saut est géré directement dans calculateRideSpaceVel
        return Vec3.ZERO;
    }

    @Override
    public double gravity(HorseSettings settings, HorseState state, LivingEntity vehicle, double regularGravity) {
        // La gravité est gérée dans calculateRideSpaceVel
        return 0.0;
    }

    @Override
    public double inertia(HorseSettings settings, HorseState state, LivingEntity vehicle) {
        return 1.0;
    }

    @Override
    public HorseState createDefaultState(HorseSettings settings) {
        return new HorseState();
    }

    // --- Méthodes internes (Pattern Cobblemon) ---

    /**
     * Gère l'état de sprint.
     * Pattern Cobblemon: handleSprinting() L141-159
     */
    private void handleSprinting(HorseSettings settings, HorseState state, Player driver) {
        if (!settings.canSprint()) {
            state.setSprinting(false);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean tryingToSprint = mc.options.keySprint.isDown() && mc.options.keyUp.isDown();

        // Stamina toujours à 1.0 dans Beemancer, donc pas de vérification stamina
        if (!mc.options.keyUp.isDown()) {
            // Si pas de forward, arrêter le sprint
            state.setSprinting(false);
        } else if (!state.isSprinting() && !state.isSprintToggleable()) {
            // Permettre le toggle si pas en sprint
            state.setSprintToggleable(true);
        } else if (tryingToSprint && state.isSprintToggleable()) {
            // Activer le sprint
            state.setSprinting(true);
        }
    }

    /**
     * Vérifie si l'entité est en l'air.
     * Pattern Cobblemon: inAirCheck() L124-139
     *
     * LOGIQUE:
     * - deltaMovement.y == 0.0 → au sol (vélocité Y nulle)
     * - standingOnSolid → bloc solide sous les pieds
     * - inAir = NOT (yVelZero OR standingOnSolid)
     */
    private void inAirCheck(HorseState state, LivingEntity vehicle) {
        var posBelow = vehicle.blockPosition().below();
        var blockStateBelow = vehicle.level().getBlockState(posBelow);
        boolean isAirOrLiquid = blockStateBelow.isAir() || !blockStateBelow.getFluidState().isEmpty();

        boolean canSupportEntity = blockStateBelow.isFaceSturdy(vehicle.level(), posBelow, Direction.UP);
        boolean standingOnSolid = canSupportEntity && !isAirOrLiquid;

        // Cobblemon L137: inAir si PAS (vélocité Y nulle OU sur bloc solide)
        boolean yVelocityZero = Math.abs(vehicle.getDeltaMovement().y) < 0.001; // Tolérance pour float
        boolean inAir = !(yVelocityZero || standingOnSolid);
        state.setInAir(inAir);
    }

    /**
     * Calcule la rotation à appliquer basée sur la différence de yaw.
     * Pattern Cobblemon: calcRotAmount() L217-250
     */
    private float calcRotAmount(HorseSettings settings, HorseState state, LivingEntity vehicle, LivingEntity driver) {
        float topSpeed = settings.getTopSpeed();
        float handling = settings.getHandling();
        float walkHandlingBoost = 5.0f;
        float maxYawDiff = settings.getLookYawLimit();

        // Normaliser la différence de rotation
        float rotDiff = Mth.wrapDegrees(driver.getYRot() - vehicle.getYRot());
        rotDiff = Mth.clamp(rotDiff, -maxYawDiff, maxYawDiff);
        float rotDiffNorm = rotDiff / maxYawDiff;

        // Racine carrée pour niveler plus vite à faible différence
        float minRotMod = 0.4f;
        float rotDiffMod = ((float) Math.sqrt(Math.abs(rotDiffNorm)) * (1.0f - minRotMod) + minRotMod) * Math.signum(rotDiffNorm);

        // Taux de rotation plus rapide en marche, plus lent en sprint
        float walkSpeed = getWalkSpeed(vehicle);
        double w = Math.max(walkSpeed, vehicle.getDeltaMovement().horizontalDistance());
        double invRelSpeed = (RidingBehaviour.scaleToRange(w, walkSpeed, topSpeed) - 1.0) * -1.0;
        float turnRate = (handling / 20.0f) * (float) Math.max(walkHandlingBoost * invRelSpeed, 1.0);

        // Limiter la rotation à la différence d'angle
        float turnSpeed = turnRate * rotDiffMod;
        return Mth.clamp(turnSpeed, -Math.abs(rotDiff), Math.abs(rotDiff));
    }

    /**
     * Calcule la vélocité en espace local.
     * Pattern Cobblemon: calculateRideSpaceVel() L264-380
     *
     * C'est le cœur de la logique de mouvement.
     */
    private Vec3 calculateRideSpaceVel(HorseSettings settings, HorseState state, LivingEntity vehicle, Player driver) {
        boolean canSprint = settings.canSprint();
        boolean canJump = settings.canJump();
        float jumpForce = settings.getJumpForce() * 0.75f;
        float rideTopSpeed = settings.getTopSpeed();
        float walkSpeed = getWalkSpeed(vehicle);
        float topSpeed = (canSprint && state.isSprinting()) ? rideTopSpeed : walkSpeed;
        float accel = settings.getAcceleration();

        // activeInput = true si le joueur appuie sur une touche de direction (WASD)
        boolean activeInput = (driver.zza != 0.0f) || (driver.xxa != 0.0f);

        // Récupérer la vélocité précédente
        Vec3 newVelocity = state.getRideVelocity();

        // Gestion des collisions horizontales
        if (vehicle.horizontalCollision) {
            newVelocity = newVelocity.normalize().scale(vehicle.getDeltaMovement().length());
        }

        // --- Accélération/Décélération basée sur input ---
        if (driver.zza != 0.0f) {
            float lookYawLimit = settings.getLookYawLimit();
            float percOfMaxTurnSpeed = Math.abs(Mth.wrapDegrees(driver.getYRot() - vehicle.getYRot()) / lookYawLimit) * 100.0f;
            float turnPercThresh = 0.0f;
            float s = Math.min((float) Math.pow((percOfMaxTurnSpeed - turnPercThresh) / (100.0f - turnPercThresh), 1), 1.0f);
            float effectiveTopSpeed = (percOfMaxTurnSpeed > turnPercThresh) ? topSpeed / Math.max(2.0f * s, 1.0f) : topSpeed;
            double turningSlowDown = s * 0.1;

            // Déterminer l'input forward
            double forwardInput;
            if (driver.zza > 0 && newVelocity.z > effectiveTopSpeed) {
                forwardInput = 0.0;
            } else if (driver.zza < 0 && newVelocity.z < (-effectiveTopSpeed / 3.0)) {
                forwardInput = 0.0;
            } else {
                forwardInput = Math.signum(driver.zza);
            }

            // Friction supplémentaire si ralentissement pour tourner
            if (newVelocity.z > effectiveTopSpeed) {
                double slowDown = Math.min(turningSlowDown * Math.signum(newVelocity.z), newVelocity.z);
                newVelocity = newVelocity.subtract(0.0, 0.0, slowDown);
            }

            // Appliquer l'accélération
            newVelocity = new Vec3(
                    newVelocity.x,
                    newVelocity.y,
                    newVelocity.z + (accel * forwardInput)
            );
        }

        // --- Gravité ---
        if (!vehicle.onGround() && state.getJumpTicks() <= 0) {
            newVelocity = new Vec3(
                    newVelocity.x,
                    Math.max(newVelocity.y - GRAVITY, -TERMINAL_VELOCITY),
                    newVelocity.z
            );
        } else if (vehicle.onGround()) {
            newVelocity = new Vec3(newVelocity.x, 0.0, newVelocity.z);
        }

        // --- 1. Clamp vélocité à la vitesse max ---
        double clampedZ = Mth.clamp(newVelocity.z, -topSpeed / 3.0, topSpeed);
        newVelocity = new Vec3(newVelocity.x, newVelocity.y, clampedZ);

        // --- 2. Friction au sol (seulement si aucune touche pressée) ---
        if (!activeInput && vehicle.onGround() && newVelocity.z != 0.0) {
            double friction = Math.min(GROUND_FRICTION, Math.abs(newVelocity.z)) * Math.signum(newVelocity.z);
            newVelocity = newVelocity.subtract(0.0, 0.0, friction);
        }

        // --- Logique de saut ---
        if (canJump) {
            int jumpTicks = state.getJumpTicks();

            if (jumpTicks > 0 || (jumpTicks >= 0 && driver.jumping && vehicle.onGround() && driver.getDeltaMovement().y <= 0.1)) {
                // Répartir la force de saut sur plusieurs ticks
                int jumpInputTicks = 6;
                if (driver.jumping && jumpTicks >= 0 && jumpTicks < jumpInputTicks) {
                    double appliedJumpForce = (jumpForce * 1.5) / jumpInputTicks;
                    newVelocity = new Vec3(newVelocity.x, newVelocity.y + appliedJumpForce, newVelocity.z);
                    state.setJumpTicks(jumpTicks + 1);
                } else {
                    // Délai avant prochain saut
                    int tickJumpDelay = 3;
                    state.setJumpTicks(-tickJumpDelay);
                }
            } else if (vehicle.onGround() && jumpTicks < 0) {
                // Décompter le délai
                state.setJumpTicks(jumpTicks + 1);
            }
        }

        // Zéro sur la vélocité latérale (pas de strafe en mode horse)
        newVelocity = new Vec3(0.0, newVelocity.y, newVelocity.z);

        return newVelocity;
    }

    /**
     * Calcule la vitesse de marche de l'entité.
     * Pattern Cobblemon: getWalkSpeed() L382-387
     */
    private float getWalkSpeed(LivingEntity vehicle) {
        // Vitesse de base * attribut movement speed * modificateur
        double movementSpeed = vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double speedModifier = 1.2 * 0.35;
        return (float) (0.3 * movementSpeed * speedModifier);
    }
}
