/**
 * ============================================================
 * [NectarBreadItem.java]
 * Description: Nectar-infused bread with enchantment glint
 * ============================================================
 *
 * DEPENDENCIES:
 * ------------------------------------------------------------
 * | Dependency          | Reason                | Usage                          |
 * |---------------------|----------------------|--------------------------------|
 * | Item                | Base class           | Food item behavior             |
 * ------------------------------------------------------------
 *
 * USED BY:
 * - ApicaItems (registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NectarBreadItem extends Item {

    public NectarBreadItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
