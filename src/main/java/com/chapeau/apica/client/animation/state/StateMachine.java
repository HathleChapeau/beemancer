/**
 * ============================================================
 * [StateMachine.java]
 * Description: Gestionnaire generique de state machine pour animations
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AnimationState      | Interface etat       | Gestion des etats              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApiAnimator.java (orchestration animations Api)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.state;

import java.util.EnumMap;
import java.util.Map;

/**
 * State machine generique pour gerer les transitions entre etats d'animation.
 * Supporte le blending entre etats pendant les transitions.
 *
 * @param <S> Enum des etats possibles
 */
public class StateMachine<S extends Enum<S>> {

    private final Map<S, AnimationState> states;
    private final Class<S> stateClass;

    private S currentStateKey;
    private S previousStateKey;
    private AnimationState currentState;

    private float stateStartTime;
    private float blendDuration;
    private float blendStartTime;
    private boolean isBlending;

    public StateMachine(Class<S> stateClass, S initialState) {
        this.stateClass = stateClass;
        this.states = new EnumMap<>(stateClass);
        this.currentStateKey = initialState;
        this.previousStateKey = null;
        this.blendDuration = 10f; // 10 ticks par defaut
    }

    /** Enregistre un etat avec son implementation. */
    public void registerState(S key, AnimationState state) {
        states.put(key, state);
        if (key == currentStateKey) {
            this.currentState = state;
        }
    }

    /** Definit la duree de blend par defaut. */
    public void setBlendDuration(float ticks) {
        this.blendDuration = ticks;
    }

    /** Transition vers un nouvel etat. */
    public void setState(S newState, float currentTime) {
        if (newState == currentStateKey) return;

        AnimationState newStateImpl = states.get(newState);
        if (newStateImpl == null) return;

        if (currentState != null) {
            currentState.onExit();
        }

        previousStateKey = currentStateKey;
        currentStateKey = newState;
        currentState = newStateImpl;
        stateStartTime = currentTime;

        isBlending = blendDuration > 0;
        blendStartTime = currentTime;

        currentState.onEnter(currentTime);
    }

    /** Met a jour la state machine. */
    public void update(float currentTime, float deltaTime) {
        if (currentState == null) return;

        // Fin du blending
        if (isBlending && (currentTime - blendStartTime) >= blendDuration) {
            isBlending = false;
        }

        currentState.update(currentTime, deltaTime);
    }

    /** Applique l'animation courante. */
    public void apply(float currentTime) {
        if (currentState == null) return;
        currentState.apply(currentTime);
    }

    /** Retourne l'etat courant. */
    public S getCurrentState() {
        return currentStateKey;
    }

    /** Retourne le temps ecoule dans l'etat courant. */
    public float getStateTime(float currentTime) {
        return currentTime - stateStartTime;
    }

    /** Retourne le ratio de blend (0 = debut, 1 = fin). */
    public float getBlendRatio(float currentTime) {
        if (!isBlending) return 1f;
        return Math.min(1f, (currentTime - blendStartTime) / blendDuration);
    }

    /** Retourne true si en transition. */
    public boolean isBlending() {
        return isBlending;
    }

    /** Cycle vers l'etat suivant dans l'enum. */
    public void cycleToNextState(float currentTime) {
        S[] values = stateClass.getEnumConstants();
        int currentIndex = currentStateKey.ordinal();
        int nextIndex = (currentIndex + 1) % values.length;
        setState(values[nextIndex], currentTime);
    }
}
