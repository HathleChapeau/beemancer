/**
 * ============================================================
 * [HorseSettings.java]
 * Description: Settings statiques pour le comportement Horse (terrestre)
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon HorseBehaviour.kt L547-612 (HorseSettings class)
 * - Settings constants chargés au démarrage
 * - Pas de MoLang expressions (valeurs directes)
 *
 * SIMPLIFICATION Beemancer:
 * - Valeurs directes au lieu de MoLang Expression
 * - Pas de stamina (infiniteStamina = true implicite)
 *
 * UTILISÉ PAR:
 * - HorseBehaviour.java: Accès aux paramètres
 * - RidingController.java: Stocké dans contexte
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour.types.land;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.mount.behaviour.RidingBehaviourSettings;
import net.minecraft.resources.ResourceLocation;

/**
 * Settings pour le comportement Horse (terrestre).
 * Pattern Cobblemon: HorseSettings
 */
public class HorseSettings implements RidingBehaviourSettings {

    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "land/horse");

    // --- Paramètres de vitesse ---

    /**
     * Vitesse maximale en sprint (blocs/tick).
     * Cobblemon: speedExpr, typiquement 0.3-0.5
     */
    private float topSpeed = 0.35f;

    /**
     * Temps en secondes pour atteindre topSpeed.
     * Cobblemon: accelerationExpr
     */
    private float accelerationTime = 1.5f;

    /**
     * Facteur de maniabilité (degrés/tick à vitesse de marche).
     * Cobblemon: handlingExpr
     */
    private float handling = 180.0f;

    /**
     * Limite d'angle de vue pour le turning (degrés).
     * Plus haut = peut tourner plus brusquement.
     * Cobblemon: lookYawLimit
     */
    private float lookYawLimit = 45.0f;

    // --- Paramètres de saut ---

    /**
     * Peut-il sauter?
     * Cobblemon: canJump
     */
    private boolean canJump = true;

    /**
     * Force du saut (multiplicateur).
     * Cobblemon: jumpExpr, typiquement 0.5-1.5
     */
    private float jumpForce = 0.8f;

    // --- Paramètres de sprint ---

    /**
     * Peut-il sprinter?
     * Cobblemon: canSprint
     */
    private boolean canSprint = true;

    // --- Constructors ---

    public HorseSettings() {
    }

    /**
     * Crée des settings personnalisés.
     */
    public HorseSettings(
            float topSpeed,
            float accelerationTime,
            float handling,
            float lookYawLimit,
            boolean canJump,
            float jumpForce,
            boolean canSprint
    ) {
        this.topSpeed = topSpeed;
        this.accelerationTime = accelerationTime;
        this.handling = handling;
        this.lookYawLimit = lookYawLimit;
        this.canJump = canJump;
        this.jumpForce = jumpForce;
        this.canSprint = canSprint;
    }

    // --- RidingBehaviourSettings ---

    @Override
    public ResourceLocation getKey() {
        return KEY;
    }

    // --- Getters ---

    public float getTopSpeed() {
        return topSpeed;
    }

    public float getAccelerationTime() {
        return accelerationTime;
    }

    public float getHandling() {
        return handling;
    }

    public float getLookYawLimit() {
        return lookYawLimit;
    }

    public boolean canJump() {
        return canJump;
    }

    public float getJumpForce() {
        return jumpForce;
    }

    public boolean canSprint() {
        return canSprint;
    }

    // --- Computed values ---

    /**
     * Calcule l'accélération par tick.
     * acceleration = topSpeed / (accelerationTime * 20 ticks)
     */
    public float getAcceleration() {
        return topSpeed / (accelerationTime * 20.0f);
    }

    // --- Builder pattern pour configuration facile ---

    public HorseSettings withTopSpeed(float topSpeed) {
        this.topSpeed = topSpeed;
        return this;
    }

    public HorseSettings withAccelerationTime(float accelerationTime) {
        this.accelerationTime = accelerationTime;
        return this;
    }

    public HorseSettings withHandling(float handling) {
        this.handling = handling;
        return this;
    }

    public HorseSettings withLookYawLimit(float lookYawLimit) {
        this.lookYawLimit = lookYawLimit;
        return this;
    }

    public HorseSettings withCanJump(boolean canJump) {
        this.canJump = canJump;
        return this;
    }

    public HorseSettings withJumpForce(float jumpForce) {
        this.jumpForce = jumpForce;
        return this;
    }

    public HorseSettings withCanSprint(boolean canSprint) {
        this.canSprint = canSprint;
        return this;
    }

    // --- Defaults ---

    /**
     * Settings par défaut pour l'abeille.
     */
    public static HorseSettings createBeeDefaults() {
        return new HorseSettings()
                .withTopSpeed(0.4f)
                .withAccelerationTime(1.0f)
                .withHandling(200.0f)
                .withLookYawLimit(60.0f)
                .withCanJump(true)
                .withJumpForce(0.6f)
                .withCanSprint(true);
    }
}
