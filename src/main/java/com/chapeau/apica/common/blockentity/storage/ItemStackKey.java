/**
 * ============================================================
 * [ItemStackKey.java]
 * Description: Cle d'agregation pour ItemStack (compare item + components, ignore count)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Type Minecraft        | Comparaison et hashing         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AggregationCache.java (cle des maps de cache)
 * - DepositStrategy.java (lookups dans le cache)
 * - ViewerSyncManager.java (snapshots pour delta sync)
 * - StorageItemAggregator.java (coordination)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Cle d'agregation pour les ItemStack.
 * Compare item type + components, ignore le count.
 * Utilisee comme cle de Map dans tout le systeme d'agregation.
 */
class ItemStackKey {
    private final ItemStack template;

    ItemStackKey(ItemStack stack) {
        this.template = stack.copyWithCount(1);
    }

    ItemStack toStack() {
        return template.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStackKey other)) return false;
        return ItemStack.isSameItemSameComponents(template, other.template);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(template);
    }
}
