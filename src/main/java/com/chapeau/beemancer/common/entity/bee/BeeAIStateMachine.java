/**
 * ============================================================
 * [BeeAIStateMachine.java]
 * Description: Machine à états centralisée pour l'IA des abeilles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeActivityState    | États possibles      | Enum des états                 |
 * | MagicBeeEntity      | Entité abeille       | Contexte et callbacks          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ForagingBehaviorGoal.java: Gestion états de butinage
 * - MagicBeeEntity.java: État courant de l'abeille
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Machine à états centralisée pour gérer le comportement des abeilles.
 * Supporte les callbacks enter/exit, les timeouts et les données d'état.
 */
public class BeeAIStateMachine {

    // Timeout par défaut: 40 secondes (800 ticks)
    public static final int DEFAULT_SEEK_TIMEOUT = 800;

    private final MagicBeeEntity bee;

    private BeeActivityState currentState = BeeActivityState.IDLE;
    private int stateTimer = 0;
    private int stateTimeout = 0;

    // Données d'état
    @Nullable
    private BlockPos targetPos = null;
    private int workTimer = 0;

    // Callbacks
    @Nullable
    private Consumer<BeeActivityState> onStateChange;
    @Nullable
    private Runnable onTimeout;

    public BeeAIStateMachine(MagicBeeEntity bee) {
        this.bee = bee;
    }

    /**
     * Change l'état de la machine.
     * Appelle les callbacks appropriés et réinitialise les timers.
     */
    public void setState(BeeActivityState newState) {
        if (currentState == newState) return;

        BeeActivityState oldState = currentState;

        // Exit de l'ancien état
        onExitState(oldState);

        // Changement d'état
        currentState = newState;
        stateTimer = 0;
        stateTimeout = getDefaultTimeout(newState);

        // Entry du nouvel état
        onEnterState(newState);

        // Callback externe
        if (onStateChange != null) {
            onStateChange.accept(newState);
        }
    }

    /**
     * Appelé à chaque tick pour mettre à jour les timers.
     * @return true si un timeout s'est produit
     */
    public boolean tick() {
        stateTimer++;

        // Vérifier le timeout
        if (stateTimeout > 0 && stateTimer >= stateTimeout) {
            if (onTimeout != null) {
                onTimeout.run();
            }
            return true;
        }

        // Décrémenter le workTimer si actif
        if (workTimer > 0) {
            workTimer--;
        }

        return false;
    }

    /**
     * Retourne le timeout par défaut pour un état donné.
     */
    private int getDefaultTimeout(BeeActivityState state) {
        return switch (state) {
            case SEEKING_FLOWER -> DEFAULT_SEEK_TIMEOUT;
            case WORKING -> 0; // Pas de timeout, géré par workTimer
            case RETURNING -> DEFAULT_SEEK_TIMEOUT; // Timeout si bloqué en retour
            default -> 0; // Pas de timeout pour IDLE, LEAVING_HIVE, RESTING
        };
    }

    /**
     * Callback d'entrée dans un état.
     */
    private void onEnterState(BeeActivityState state) {
        switch (state) {
            case SEEKING_FLOWER -> {
                targetPos = null;
            }
            case WORKING -> {
                // workTimer doit être défini par setWorkTimer()
            }
            case RETURNING -> {
                bee.setReturning(true);
            }
            case IDLE -> {
                targetPos = null;
                workTimer = 0;
            }
            default -> {}
        }
    }

    /**
     * Callback de sortie d'un état.
     */
    private void onExitState(BeeActivityState state) {
        switch (state) {
            case RETURNING -> {
                bee.setReturning(false);
            }
            default -> {}
        }
    }

    // --- Getters/Setters ---

    public BeeActivityState getState() {
        return currentState;
    }

    public int getStateTimer() {
        return stateTimer;
    }

    public boolean hasTimedOut() {
        return stateTimeout > 0 && stateTimer >= stateTimeout;
    }

    @Nullable
    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos pos) {
        this.targetPos = pos;
    }

    public boolean hasTarget() {
        return targetPos != null;
    }

    public void clearTarget() {
        this.targetPos = null;
    }

    public int getWorkTimer() {
        return workTimer;
    }

    public void setWorkTimer(int ticks) {
        this.workTimer = ticks;
    }

    public boolean isWorkComplete() {
        return workTimer <= 0;
    }

    /**
     * Définit un timeout personnalisé pour l'état courant.
     */
    public void setStateTimeout(int ticks) {
        this.stateTimeout = ticks;
    }

    /**
     * Définit le callback appelé lors d'un changement d'état.
     */
    public void setOnStateChange(@Nullable Consumer<BeeActivityState> callback) {
        this.onStateChange = callback;
    }

    /**
     * Définit le callback appelé lors d'un timeout.
     */
    public void setOnTimeout(@Nullable Runnable callback) {
        this.onTimeout = callback;
    }

    /**
     * Réinitialise la machine à l'état IDLE.
     */
    public void reset() {
        currentState = BeeActivityState.IDLE;
        stateTimer = 0;
        stateTimeout = 0;
        targetPos = null;
        workTimer = 0;
    }
}
