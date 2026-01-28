/**
 * ============================================================
 * [RidingBehaviourSettings.java]
 * Description: Interface pour les settings statiques d'un comportement
 * ============================================================
 *
 * PATTERN: Copié de Cobblemon api/riding/behaviour/RidingBehaviourSettings.kt L27-35
 * - Settings constants pendant le ride
 * - Initialisé depuis JSON/config au démarrage
 * - Partagé entre toutes les entités utilisant ce comportement
 *
 * SIMPLIFICATION Beemancer:
 * - Pas de MoLang expressions (valeurs directes)
 * - Pas de RidingStat map (valeurs directes)
 *
 * UTILISÉ PAR:
 * - RidingBehaviour.java: Passé à toutes les méthodes
 * - RidingController.java: Stocké dans ActiveRidingContext
 * - HorseSettings.java: Implémentation concrète
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.entity.mount.behaviour;

import net.minecraft.resources.ResourceLocation;

/**
 * Settings statiques d'un comportement de ride.
 * Les valeurs sont constantes pendant toute la durée du ride.
 *
 * Pattern Cobblemon: RidingBehaviourSettings
 */
public interface RidingBehaviourSettings {

    /**
     * Clé unique identifiant ce type de settings.
     * Ex: "beemancer:land/horse"
     */
    ResourceLocation getKey();
}
