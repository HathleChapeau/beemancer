/**
 * ============================================================
 * [OutputOnlySlot.java]
 * Description: Slot qui permet uniquement l'extraction (pattern Create)
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OutputOnlySlot extends Slot {
    
    public OutputOnlySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false; // Forbid insertion
    }
}
