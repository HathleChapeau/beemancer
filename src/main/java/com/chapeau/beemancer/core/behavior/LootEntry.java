/**
 * ============================================================
 * [LootEntry.java]
 * Description: Entrée de loot pour la pollinisation des abeilles
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
 * - BeeBehaviorConfig.java: Liste des loots de pollinisation
 * - BeeBehaviorManager.java: Chargement depuis JSON
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.behavior;

/**
 * Représente une entrée de loot pour la pollinisation.
 * 
 * @param itemId ID de l'item (ex: "minecraft:honeycomb")
 * @param minQty Quantité minimum
 * @param maxQty Quantité maximum
 * @param chance Pourcentage de chance (0-100)
 */
public record LootEntry(String itemId, int minQty, int maxQty, int chance) {
    
    /**
     * Vérifie si cette entrée est valide.
     */
    public boolean isValid() {
        return itemId != null && !itemId.isEmpty() 
            && minQty >= 0 && maxQty >= minQty && chance >= 0 && chance <= 100;
    }
}
