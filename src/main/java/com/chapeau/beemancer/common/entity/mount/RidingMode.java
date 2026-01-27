/**
 * ============================================================
 * [RidingMode.java]
 * Description: Enum définissant les modes de déplacement de l'abeille chevauchable
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | Enum autonome        | -                              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingState.java: Stockage du mode actuel
 * - RideableBeeController.java: Transitions entre modes
 * - RideableBeeMovement.java: Calcul vélocité selon mode
 * - RidingCameraController.java: Position caméra selon mode
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Modes de déplacement pour l'abeille chevauchable.
 *
 * WALK: Mouvement libre dans toutes les directions, caméra 3ème personne libre
 * RUN: Accélération/décélération uniquement, rotation avec inertie, caméra fixe épaule
 */
public enum RidingMode {

    /**
     * Mode marche - contrôle libre.
     * - Mouvement: WASD libre selon orientation caméra
     * - Caméra: 3ème personne, joueur peut regarder librement
     * - Saut: Sur place (normal)
     */
    WALK,

    /**
     * Mode course - contrôle restreint avec inertie.
     * - Mouvement: W accélère, S décélère, A/D rotation avec inertie
     * - Caméra: Fixée à l'épaule droite, ne peut pas bouger
     * - Saut: Bond vers l'avant sans contrôle aérien
     */
    RUN;

    /**
     * Convertit le mode en byte pour synchronisation réseau.
     */
    public byte toByte() {
        return (byte) this.ordinal();
    }

    /**
     * Convertit un byte en mode.
     */
    public static RidingMode fromByte(byte value) {
        if (value >= 0 && value < values().length) {
            return values()[value];
        }
        return WALK;
    }
}
