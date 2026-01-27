/**
 * ============================================================
 * [RidingState.java]
 * Description: État runtime mutable pour le système de déplacement
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RidingMode          | Mode actuel          | Stockage mode WALK/RUN         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RideableBeeEntity.java: Instance par entité
 * - RideableBeeController.java: Modification de l'état
 * - RideableBeeMovement.java: Lecture de l'état pour calculs
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * État runtime du système de déplacement.
 * Mutable - mis à jour chaque tick par le controller.
 */
public class RidingState {

    private RidingMode currentMode = RidingMode.WALK;
    private float currentSpeed = 0f;
    private float currentYaw = 0f;
    private float targetYaw = 0f;
    private boolean isLeaping = false;
    private boolean wasOnGround = true;

    // Input courant (reçu du client via packet)
    private float inputForward = 0f;
    private float inputStrafe = 0f;
    private boolean inputJump = false;
    private boolean inputSprint = false;

    // --- Mode ---

    public RidingMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(RidingMode mode) {
        this.currentMode = mode;
    }

    // --- Vitesse ---

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(float speed) {
        this.currentSpeed = speed;
    }

    // --- Rotation ---

    public float getCurrentYaw() {
        return currentYaw;
    }

    public void setCurrentYaw(float yaw) {
        this.currentYaw = yaw;
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public void setTargetYaw(float yaw) {
        this.targetYaw = yaw;
    }

    // --- Leap (bond) ---

    public boolean isLeaping() {
        return isLeaping;
    }

    public void setLeaping(boolean leaping) {
        this.isLeaping = leaping;
    }

    public boolean wasOnGround() {
        return wasOnGround;
    }

    public void setWasOnGround(boolean onGround) {
        this.wasOnGround = onGround;
    }

    // --- Input ---

    public float getInputForward() {
        return inputForward;
    }

    public float getInputStrafe() {
        return inputStrafe;
    }

    public boolean isInputJump() {
        return inputJump;
    }

    public boolean isInputSprint() {
        return inputSprint;
    }

    /**
     * Met à jour l'input reçu du client.
     */
    public void updateInput(float forward, float strafe, boolean jump, boolean sprint) {
        this.inputForward = forward;
        this.inputStrafe = strafe;
        this.inputJump = jump;
        this.inputSprint = sprint;
    }

    /**
     * Réinitialise l'état à ses valeurs par défaut.
     * Appelé quand le joueur descend de l'abeille.
     */
    public void reset() {
        this.currentMode = RidingMode.WALK;
        this.currentSpeed = 0f;
        this.isLeaping = false;
        this.wasOnGround = true;
        this.inputForward = 0f;
        this.inputStrafe = 0f;
        this.inputJump = false;
        this.inputSprint = false;
    }
}
