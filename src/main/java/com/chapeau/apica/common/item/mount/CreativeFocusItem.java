/**
 * ============================================================
 * [CreativeFocusItem.java]
 * Description: Item créatif pour ajouter des stats aléatoires aux pièces de hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune directe)    |                      | L'interaction passe par le     |
 * |                     |                      | InteractionMarkerEntity        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java: Enregistrement
 * - ApicaCreativeTabs.java: Ajout dans le debug tab
 * - InteractionMarkerTypes (assembly_focus handler): Vérification item en main
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item créatif qui permet d'ajouter des stats aléatoires (prefix/suffix)
 * aux pièces de hoverbike posées sur une Assembly Table.
 * L'interaction se fait en cliquant droit sur l'entité InteractionMarker
 * au-dessus de la table — le handler est enregistré dans InteractionMarkerTypes.
 */
public class CreativeFocusItem extends Item {

    public CreativeFocusItem(Properties properties) {
        super(properties);
    }

    /**
     * Vérifie si un ItemStack est un Creative Focus.
     */
    public static boolean isCreativeFocus(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CreativeFocusItem;
    }
}
