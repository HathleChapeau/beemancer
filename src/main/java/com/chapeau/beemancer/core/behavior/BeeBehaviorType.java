/**
 * ============================================================
 * [BeeBehaviorType.java]
 * Description: Types de comportement des abeilles (butineuse/récolteuse)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeBehaviorConfig.java: Définit le type de comportement
 * - ForagingBehaviorGoal.java: Vérifie si FORAGER
 * - HarvestingBehaviorGoal.java: Vérifie si HARVESTER
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

/**
 * Types de comportement pour les Magic Bees.
 * Chaque espèce est associée à un type de comportement.
 */
public enum BeeBehaviorType {
    /**
     * Abeille butineuse: va sur une fleur, reste un certain temps,
     * puis revient à la ruche avec du pollen.
     */
    FORAGER,

    /**
     * Abeille récolteuse: va sur une fleur, récolte des ressources,
     * puis revient à la ruche avec un inventaire.
     */
    HARVESTER
}
