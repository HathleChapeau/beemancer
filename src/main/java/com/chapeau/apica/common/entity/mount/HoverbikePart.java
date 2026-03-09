/**
 * ============================================================
 * [HoverbikePart.java]
 * Description: Enum des types de parties modulaires du HoverBee
 * ============================================================
 *
 * UTILISE PAR:
 * - HoverbikePartModel.java: Identification du type de partie
 * - HoverbikePartLayer.java: Iteration sur les parties
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

/**
 * Types de parties modulaires du HoverBee.
 * Chaque partie correspond a un modele 3D separe rendu par-dessus le modele de base.
 */
public enum HoverbikePart {
    SADDLE,
    WING_PROTECTOR,
    CONTROL_LEFT,
    CONTROL_RIGHT
}
