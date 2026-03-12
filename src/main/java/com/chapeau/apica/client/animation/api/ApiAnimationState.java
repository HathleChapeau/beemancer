/**
 * ============================================================
 * [ApiAnimationState.java]
 * Description: Enum des etats d'animation possibles pour Api
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | Enum standalone      | -                              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApiAnimator.java (state machine)
 * - ApiBlockEntity.java (sync etat)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.api;

/**
 * Etats d'animation pour Api.
 * Chaque etat correspond a un comportement visuel distinct.
 */
public enum ApiAnimationState {
    /** Respiration, leger balancement. Loop infini. */
    IDLE,

    /** 2 petits sauts sur place. Joue une fois puis retourne a IDLE. */
    JUMP,

    /** Effort pour se soulever puis epuisement. Joue une fois puis retourne a IDLE. */
    HITSTOP,

    /** Dort, immobile. Reste jusqu'a changement manuel. */
    SLEEP
}
