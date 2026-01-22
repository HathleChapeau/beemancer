/**
 * ============================================================
 * [SpeedThreshold.java]
 * Description: Seuil de vitesse basé sur le nombre d'items transportés
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
 * - BeeBehaviorConfig.java: Liste des seuils de vitesse
 * - HarvestingBehaviorGoal.java: Calcul vitesse effective
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

/**
 * Représente un seuil de vitesse pour les abeilles récolteuses.
 * Quand l'inventaire atteint itemCount items, la vitesse est multipliée par speedMultiplier.
 *
 * @param itemCount Nombre d'items à partir duquel ce seuil s'applique
 * @param speedMultiplier Multiplicateur de vitesse (0.0 à 1.0, 1.0 = vitesse normale)
 */
public record SpeedThreshold(int itemCount, double speedMultiplier) {

    /**
     * Vérifie si le seuil est valide.
     */
    public boolean isValid() {
        return itemCount >= 0 && speedMultiplier >= 0.0 && speedMultiplier <= 1.0;
    }
}
