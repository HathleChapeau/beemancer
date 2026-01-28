/**
 * ============================================================
 * [Side.java]
 * Description: Enum pour identifier le côté d'exécution (CLIENT/SERVER/BOTH)
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviourState.kt L88-90
 * - Permet de restreindre les modifications d'état à un côté
 * - CLIENT: seul le client peut modifier
 * - SERVER: seul le serveur peut modifier
 * - BOTH: les deux peuvent modifier
 *
 * UTILISÉ PAR:
 * - SidedRidingState.java: Filtrage des modifications
 * - RidingBehaviourState.java: Définition des états
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

/**
 * Identifie quel côté (client/server) peut modifier un état.
 */
public enum Side {
    SERVER,
    CLIENT,
    BOTH
}
