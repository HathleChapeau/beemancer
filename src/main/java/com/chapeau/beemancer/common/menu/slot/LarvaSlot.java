/**
 * ============================================================
 * [LarvaSlot.java]
 * Description: Slot acceptant uniquement les larves d'abeilles
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.slot;

import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LarvaSlot extends Slot {
    
    public LarvaSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Only accept BeeLarvaItem
        return stack.is(BeemancerItems.BEE_LARVA.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
