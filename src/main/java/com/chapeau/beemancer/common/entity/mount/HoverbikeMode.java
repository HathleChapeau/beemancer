/**
 * ============================================================
 * [HoverbikeMode.java]
 * Description: Modes de deplacement du Hoverbike
 * ============================================================
 *
 * UTILISÉ PAR:
 * - HoverbikeEntity.java: Etat courant du mode
 * - HoverbikePhysics.java: Branchement logique par mode
 * - HoverbikeDebugHud.java: Affichage du mode
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Les deux modes de deplacement du Hoverbike.
 * HOVER: deplacement libre en 4 directions, basse vitesse.
 * RUN: deplacement vers l'avant uniquement, haute vitesse.
 */
public enum HoverbikeMode {
    HOVER,
    RUN
}
