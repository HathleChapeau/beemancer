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
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - BackpackMenu.java (type check)
 * - BackpackOpenPacket.java (validation)
 * - ContainerScreenMagazineMixin.java (detection clic droit)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import net.minecraft.world.item.Item;

/**
 * Item backpack portable. S'ouvre depuis l'inventaire via clic droit (mixin).
 * L'inventaire (27 slots) est stocke dans DataComponents.CONTAINER sur l'ItemStack.
 */
public class BackpackItem extends Item {

    public BackpackItem(Properties properties) {
        super(properties);
    }
}
