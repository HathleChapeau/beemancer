/**
 * ============================================================
 * [RideableBeeController.java]
 * Description: Orchestrateur du système de déplacement de l'abeille chevauchable
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | RidingMode          | Transitions modes    | WALK/RUN                       |
 * | RidingSettings      | Paramètres           | Seuils de transition           |
 * | RidingState         | État runtime         | Mise à jour vitesse/yaw        |
 * | RideableBeeMovement | Calculs              | Vélocité, rotation             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RideableBeeEntity.java: Appelé dans tick()
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

import net.minecraft.world.phys.Vec3;

/**
 * Contrôleur de déplacement pour RideableBeeEntity.
 * Gère les transitions de mode, le saut/leap, et l'application du mouvement.
 */
public class RideableBeeController {

    private static final float TURN_RATE = 3.0f; // Degrés par tick

    private final RidingSettings settings;
    private final RidingState state;

    public RideableBeeController(RidingSettings settings, RidingState state) {
        this.settings = settings;
        this.state = state;
    }

    /**
     * Tick principal appelé chaque tick serveur quand l'entité est montée.
     *
     * @param entity L'entité contrôlée
     * @param cameraYaw Yaw de la caméra du joueur (pour mode WALK)
     * @param onGround True si l'entité touche le sol
     * @return Le vecteur de mouvement à appliquer
     */
    public Vec3 tick(RideableBeeEntity entity, float cameraYaw, boolean onGround) {
        // Récupérer l'input
        float forward = state.getInputForward();
        float strafe = state.getInputStrafe();
        boolean jump = state.isInputJump();
        boolean sprint = state.isInputSprint();

        // Gérer l'atterrissage après un leap
        if (state.isLeaping()) {
            if (onGround && !state.wasOnGround()) {
                // On vient d'atterrir
                state.setLeaping(false);
            }
        }
        state.setWasOnGround(onGround);

        // Si en leap, pas de contrôle
        if (state.isLeaping()) {
            return Vec3.ZERO; // Laisser la physique gérer
        }

        // Gérer les transitions de mode
        handleModeTransition(forward, sprint);

        // Calculer le mouvement selon le mode
        Vec3 movement;
        if (state.getCurrentMode() == RidingMode.WALK) {
            movement = handleWalkMode(forward, strafe, cameraYaw, jump, onGround, entity);
        } else {
            movement = handleRunMode(forward, strafe, jump, onGround, entity);
        }

        return movement;
    }

    /**
     * Gère les transitions entre modes WALK et RUN.
     *
     * @param forward Input avant (-1 à 1) pour vérifier si le joueur avance
     * @param sprint True si sprint pressé (Ctrl ou double-tap W)
     */
    private void handleModeTransition(float forward, boolean sprint) {
        RidingMode currentMode = state.getCurrentMode();

        if (currentMode == RidingMode.WALK) {
            // Vérifier si on peut passer en RUN (avance + sprint)
            if (RideableBeeMovement.canEnterRunMode(forward, sprint, settings)) {
                state.setCurrentMode(RidingMode.RUN);
                // Initialiser la vitesse en RUN à la vitesse de marche
                state.setCurrentSpeed(settings.walkSpeed());
            }
        } else {
            // Vérifier si on doit repasser en WALK (vitesse trop basse)
            if (RideableBeeMovement.shouldExitRunMode(state.getCurrentSpeed(), settings)) {
                state.setCurrentMode(RidingMode.WALK);
                state.setCurrentSpeed(0f);
            }
        }
    }

    /**
     * Gère le mouvement en mode WALK.
     */
    private Vec3 handleWalkMode(float forward, float strafe, float cameraYaw,
                                 boolean jump, boolean onGround, RideableBeeEntity entity) {
        // Calculer la vélocité
        Vec3 velocity = RideableBeeMovement.calculateWalkVelocity(forward, strafe, cameraYaw, settings);

        // Mettre à jour la vitesse pour les transitions
        state.setCurrentSpeed((float) velocity.horizontalDistance());

        // Gérer le saut
        if (jump && onGround) {
            Vec3 jumpVec = RideableBeeMovement.calculateWalkJump(settings.walkJumpStrength());
            velocity = velocity.add(jumpVec);
        }

        // Mettre à jour le yaw de l'entité vers la direction de mouvement
        if (velocity.horizontalDistanceSqr() > 0.001) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            entity.setYRot(targetYaw);
            state.setCurrentYaw(targetYaw);
            state.setTargetYaw(targetYaw);
        }

        return velocity;
    }

    /**
     * Gère le mouvement en mode RUN.
     */
    private Vec3 handleRunMode(float forward, float strafe, boolean jump,
                                boolean onGround, RideableBeeEntity entity) {
        // Mettre à jour la vitesse
        float newSpeed = RideableBeeMovement.updateRunSpeed(forward, state.getCurrentSpeed(), settings);
        state.setCurrentSpeed(newSpeed);

        // Mettre à jour la rotation avec inertie
        float newYaw = RideableBeeMovement.updateYawWithInertia(
            strafe,
            state.getCurrentYaw(),
            state.getTargetYaw(),
            settings.turnInertia(),
            TURN_RATE
        );
        state.setCurrentYaw(newYaw);
        state.setTargetYaw(newYaw);
        entity.setYRot(newYaw);

        // Calculer la vélocité
        Vec3 velocity = RideableBeeMovement.calculateRunVelocity(forward, settings, state);

        // Gérer le leap (bond)
        if (jump && onGround && newSpeed > settings.walkSpeed()) {
            Vec3 leapVec = RideableBeeMovement.calculateLeapVelocity(newYaw, settings.runLeapForce());
            state.setLeaping(true);
            return leapVec;
        }

        return velocity;
    }

    /**
     * Réinitialise le contrôleur.
     * Appelé quand le joueur descend.
     */
    public void reset() {
        state.reset();
    }

    // --- Getters ---

    public RidingSettings getSettings() {
        return settings;
    }

    public RidingState getState() {
        return state;
    }
}
