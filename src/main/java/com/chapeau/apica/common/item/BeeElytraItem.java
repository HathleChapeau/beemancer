/**
 * ============================================================
 * [BeeElytraItem.java]
 * Description: Elytra reskin with bee wings — identical flight behavior
 * ============================================================
 *
 * DEPENDENCIES:
 * ------------------------------------------------------------
 * | Dependency          | Reason                | Usage                          |
 * |---------------------|----------------------|--------------------------------|
 * | ElytraItem          | Vanilla elytra base  | Flight, equip, durability      |
 * | ApicaItems          | Registry reference   | Repair material (royal comb)   |
 * ------------------------------------------------------------
 *
 * USED BY:
 * - ApicaItems (registration)
 * - BeeElytraLayer (rendering)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;

public class BeeElytraItem extends ElytraItem {

    public BeeElytraItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ApicaItems.ROYAL_COMB.get());
    }
}
