/**
 * ============================================================
 * [ItemFilterItem.java]
 * Description: Item filtre pour les item pipes
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Item                | Classe parente       | Item de base Minecraft         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (enregistrement)
 * - AbstractPipeBlock.java (detection lors du placement)
 * - ItemPipeBlock.java (interactions)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import net.minecraft.world.item.Item;

/**
 * Item filtre placable au centre d'un item pipe.
 * Configure via GUI : 9 ghost slots, mode Accept/Deny, priority.
 */
public class ItemFilterItem extends Item {

    public ItemFilterItem(Properties properties) {
        super(properties);
    }
}
