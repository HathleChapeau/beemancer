/**
 * ============================================================
 * [EssenceValue.java]
 * Description: Record de valeur d'essence pour l'injection (stat points + hunger cost)
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
 * - InjectionConfigManager.java (stockage des valeurs)
 * - InjectorBlockEntity.java (lecture lors du processing)
 *
 * ============================================================
 */
package com.chapeau.apica.core.config;

/**
 * Valeur d'une essence pour le systeme d'injection.
 * Definit combien de points de stat l'essence donne et combien de faim elle coute.
 *
 * @param statPoints points de stat accordes (50 = 1 niveau complet)
 * @param hungerCost cout en faim pour l'abeille
 */
public record EssenceValue(int statPoints, int hungerCost) {

    /** Valeur par defaut pour les essences non configurees. */
    public static final EssenceValue DEFAULT = new EssenceValue(5, 10);
}
