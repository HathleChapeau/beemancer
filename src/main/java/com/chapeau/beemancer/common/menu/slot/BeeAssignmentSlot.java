/**
 * ============================================================
 * [BeeAssignmentSlot.java]
 * Description: Slot pour assigner une abeille à une ruche
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.slot;

import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BeeAssignmentSlot extends Slot {
    
    public BeeAssignmentSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Only accept MagicBeeItem
        return stack.is(BeemancerItems.MAGIC_BEE.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1; // Only 1 bee per slot
    }
}
