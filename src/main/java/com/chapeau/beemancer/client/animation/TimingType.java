/**
 * ============================================================
 * [TimingType.java]
 * Description: Courbes d'easing pour le systeme d'animation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | (aucune)                      | Enum standalone      | -                              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Animation.java (calcul du progress)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

/**
 * Courbes d'easing qui transforment un progress lineaire [0,1] en valeur easee [0,1].
 * Le parametre power (stocke dans Animation) controle l'intensite de EASE_IN et EASE_OUT.
 */
public enum TimingType {
    /** Lineaire : t -> t */
    NORMAL,
    /** Acceleration : lent au debut, rapide a la fin. t -> t^power */
    EASE_IN,
    /** Deceleration : rapide au debut, lent a la fin. t -> 1-(1-t)^power */
    EASE_OUT,
    /** Smoothstep : lent aux extremes, rapide au milieu. t -> 3t^2 - 2t^3 */
    SLOW_IN_SLOW_OUT;

    /**
     * Applique la courbe d'easing sur une valeur t [0,1].
     *
     * @param t     valeur de progress brute (0.0 a 1.0)
     * @param power exposant pour EASE_IN/EASE_OUT (ignore par NORMAL et SLOW_IN_SLOW_OUT)
     * @return valeur easee entre 0.0 et 1.0
     */
    public float apply(float t, float power) {
        return switch (this) {
            case NORMAL -> t;
            case EASE_IN -> (float) Math.pow(t, power);
            case EASE_OUT -> 1f - (float) Math.pow(1f - t, power);
            case SLOW_IN_SLOW_OUT -> t * t * (3f - 2f * t);
        };
    }
}
