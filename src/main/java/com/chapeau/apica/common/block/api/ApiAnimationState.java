/**
 * ============================================================
 * [ApiAnimationState.java]
 * Description: Enum des etats d'animation pour Api
 * ============================================================
 */
package com.chapeau.apica.common.block.api;

/**
 * Etats d'animation pour Api.
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
