/**
 * ============================================================
 * [HoverbikePart.java]
 * Description: Enum des types de parties modulaires du Hoverbike
 * ============================================================
 *
 * UTILISE PAR:
 * - HoverbikePartModel.java: Identification du type de partie
 * - HoverbikePartLayer.java: Iteration sur les parties
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount;

/**
 * Types de parties modulaires du Hoverbike.
 * Chaque partie correspond a un modele 3D separe rendu par-dessus le modele de base.
 */
public enum HoverbikePart {
    CHASSIS,
    COEUR,
    PROPULSEUR,
    RADIATEUR
}
