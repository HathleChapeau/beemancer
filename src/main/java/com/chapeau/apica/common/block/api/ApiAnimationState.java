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

    /** Effort pour se soulever puis epuisement. Joue une fois puis retourne a IDLE. */
    HITSTOP,

    /** Content, se balance energiquement. Joue une fois puis retourne a IDLE. */
    HAPPY,

    /** Dort, immobile. Reste jusqu'a changement manuel. */
    SLEEP
}
