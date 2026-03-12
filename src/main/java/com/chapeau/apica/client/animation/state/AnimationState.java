/**
 * ============================================================
 * [AnimationState.java]
 * Description: Interface pour un etat d'animation dans une state machine
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | Interface standalone | -                              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StateMachine.java (gestion des etats)
 * - ApiAnimator.java (implementations concretes)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.state;

/**
 * Interface definissant un etat d'animation.
 * Chaque etat gere son entree, sa mise a jour et sa sortie.
 */
public interface AnimationState {

    /** Appele quand on entre dans cet etat. */
    void onEnter(float currentTime);

    /** Appele quand on quitte cet etat. */
    void onExit();

    /** Met a jour l'etat. Retourne true si l'animation est terminee. */
    boolean update(float currentTime, float deltaTime);

    /** Applique l'animation au modele. */
    void apply(float currentTime);

    /** Retourne true si l'animation de cet etat est complete (pour etats non-looping). */
    boolean isComplete();
}
