/**
 * ============================================================
 * [BeeAnimationState.java]
 * Description: Enum des etats d'animation pour les abeilles Apica
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
 * - BeeModelAnimator.java: Determine quelle animation jouer
 * - ApicaBeeModel.java: Stocke l'etat courant
 * - MagicBeeRenderer.java: Change l'etat selon le comportement
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation.bee;

/**
 * Etats d'animation possibles pour une abeille Apica.
 * Chaque etat definit un ensemble de parametres d'animation
 * (battement d'ailes, pitch, repli des pattes).
 */
public enum BeeAnimationState {

    /**
     * Etat par defaut: pas d'animation.
     * Ailes fixes, pas de pitch, pattes pendantes.
     */
    IDLE(0f, 0f, 0f, 0f, 0f),

    /**
     * Etat de vol: ailes battantes, leger pitch vers l'avant, pattes repliees.
     * Valeurs proches vanilla: speed ~2.1, amplitude = pi*0.15 (~0.47 rad).
     */
    FLYING(2.1f, 0.47f, 0f, 0.1f, 0.7854f),

    /**
     * Etat de vol stationnaire: battements normaux, meme pitch que FLYING.
     * Pour le hovering sur place.
     */
    HOVERING(2.1f, 0.47f, 0f, 0.1f, 0.5236f);

    /** Vitesse de battement des ailes (cycles par tick). */
    public final float wingSpeed;

    /** Amplitude du battement des ailes (radians). */
    public final float wingAmplitude;

    /** Biais vers le haut des ailes (radians). */
    public final float wingUpBias;

    /** Rotation pitch du corps vers l'avant (radians). */
    public final float bodyPitch;

    /** Angle de repli des pattes (radians). */
    public final float legTuckAngle;

    BeeAnimationState(float wingSpeed, float wingAmplitude, float wingUpBias,
                      float bodyPitch, float legTuckAngle) {
        this.wingSpeed = wingSpeed;
        this.wingAmplitude = wingAmplitude;
        this.wingUpBias = wingUpBias;
        this.bodyPitch = bodyPitch;
        this.legTuckAngle = legTuckAngle;
    }

    /**
     * Verifie si cet etat a des ailes animees.
     */
    public boolean hasWingAnimation() {
        return wingSpeed > 0.001f && wingAmplitude > 0.001f;
    }

    /**
     * Verifie si cet etat a un pitch du corps.
     */
    public boolean hasBodyPitch() {
        return Math.abs(bodyPitch) > 0.001f;
    }
}
