/**
 * ============================================================
 * [RidingStyle.java]
 * Description: Enum des styles de déplacement (LAND, LIQUID, AIR)
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/RidingStyle.kt
 * - Catégorise le type de terrain pour le comportement
 * - Utilisé pour les transitions de controller
 *
 * UTILISÉ PAR:
 * - RidingController.java: Transitions entre styles
 * - RidingBehaviour.java: Identification du style
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

/**
 * Style général de déplacement pour le riding.
 * Simplifié pour Beemancer: uniquement LAND pour l'instant.
 */
public enum RidingStyle {
    LAND,
    LIQUID,
    AIR
}
