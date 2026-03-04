/**
 * ============================================================
 * [BackpackItem.java]
 * Description: Item sac a dos avec inventaire interne de 27 slots
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Item                | Base Minecraft       | Classe parente                 |
 * | IAccessory          | Slot accessoire      | Interface equippable           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - BackpackMenu.java (type check)
 * - BackpackOpenPacket.java (validation)
 * - AccessoryEquipPacket.java (equip en slot accessoire)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import com.chapeau.apica.common.item.accessory.IAccessory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item backpack portable. S'equipe en slot accessoire et s'ouvre via l'onglet Backpack.
 * L'inventaire (27 slots) est stocke dans DataComponents.CONTAINER sur l'ItemStack.
 */
public class BackpackItem extends Item implements IAccessory {

    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        // Rien de special a l'equip pour l'instant
    }

    @Override
    public void onUnequip(Player player, ItemStack stack) {
        // Rien de special au desequip pour l'instant
    }
}
