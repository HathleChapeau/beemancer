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
 * Represente une entree de loot pour la pollinisation.
 * L'itemId peut etre un item direct (ex: "minecraft:honeycomb") ou un tag (ex: "#minecraft:small_flowers").
 * Quand un tag est utilise, un item aleatoire est choisi parmi les items du tag.
 *
 * @param itemId ID de l'item ou tag prefixe par "#"
 * @param minQty Quantite minimum
 * @param maxQty Quantite maximum
 * @param chance Pourcentage de chance (0-100)
 */
public record LootEntry(String itemId, int minQty, int maxQty, int chance) {

    /**
     * Verifie si cette entree est valide.
     */
    public boolean isValid() {
        return itemId != null && !itemId.isEmpty()
            && minQty >= 0 && maxQty >= minQty && chance >= 0 && chance <= 100;
    }

    /**
     * Verifie si cette entree utilise un tag au lieu d'un item direct.
     */
    public boolean isTag() {
        return itemId != null && itemId.startsWith("#");
    }
}
