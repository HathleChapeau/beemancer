/**
 * ============================================================
 * [CombItem.java]
 * Description: Item de rayon de miel associé à une espèce d'abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeSpeciesManager   | Couleurs espèce      | Tinting body/stripe            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems (enregistrement de toutes les combs espèce)
 * - ClientSetup (tinting via ItemColor)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import net.minecraft.world.item.Item;

/**
 * Item rayon de miel lié à une espèce.
 * Le speciesId est utilisé côté client pour résoudre les couleurs body/stripe
 * via BeeSpeciesManager.
 */
public class CombItem extends Item {

    private final String speciesId;

    public CombItem(Properties properties, String speciesId) {
        super(properties);
        this.speciesId = speciesId;
    }

    /**
     * Retourne l'ID de l'espèce associée à cette comb.
     */
    public String getSpeciesId() {
        return speciesId;
    }
}
