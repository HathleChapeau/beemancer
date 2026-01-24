/**
 * ============================================================
 * [BeeActivityState.java]
 * Description: États d'activité des abeilles magiques
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java: Tracking de l'etat courant
 * - ForagingBehaviorGoal.java: Machine a etats
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.bee;

/**
 * États d'activité pour les Magic Bees.
 * Utilisé par les goals de comportement pour gérer la machine à états.
 */
public enum BeeActivityState {
    /**
     * L'abeille est dans la ruche, en attente du cooldown.
     */
    IDLE,
    
    /**
     * L'abeille quitte la ruche pour commencer son travail.
     */
    LEAVING_HIVE,
    
    /**
     * L'abeille cherche et se dirige vers une fleur.
     */
    SEEKING_FLOWER,
    
    /**
     * L'abeille travaille sur la fleur (butinage ou récolte).
     */
    WORKING,
    
    /**
     * L'abeille retourne à la ruche après avoir travaillé.
     */
    RETURNING,
    
    /**
     * L'abeille se repose dans la ruche (cooldown actif).
     */
    RESTING
}
