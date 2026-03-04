/**
 * ============================================================
 * [AccessoryClientCache.java]
 * Description: Cache client-side des accessoires equipes du joueur local
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Items accessoire     | Stockage client                |
 * | BackpackItem        | Type check           | Detection backpack equipe      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AccessorySyncPacket.java (mise a jour)
 * - InventoryScreenAccessoryMixin.java (lecture pour rendu)
 * - BackpackScreen.java (verification tab)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui;

import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.item.BackpackItem;
import com.chapeau.apica.common.item.accessory.IAccessory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache statique client-side des accessoires equipes.
 * Mis a jour par AccessorySyncPacket (S2C).
 */
public final class AccessoryClientCache {

    private static final ItemStack[] slots = new ItemStack[]{
        ItemStack.EMPTY, ItemStack.EMPTY
    };

    private AccessoryClientCache() {
    }

    public static void update(ItemStack slot0, ItemStack slot1) {
        slots[0] = slot0;
        slots[1] = slot1;
    }

    public static ItemStack getSlot(int index) {
        if (index < 0 || index >= AccessoryPlayerData.SLOT_COUNT) return ItemStack.EMPTY;
        return slots[index];
    }

    /**
     * Retourne l'index du slot contenant un backpack, ou -1 si aucun.
     */
    public static int findBackpackSlot() {
        for (int i = 0; i < AccessoryPlayerData.SLOT_COUNT; i++) {
            if (slots[i].getItem() instanceof BackpackItem) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasBackpack() {
        return findBackpackSlot() >= 0;
    }

    /**
     * Retourne la liste des slots accessoire qui fournissent un onglet inventaire.
     */
    public static List<Integer> getTabSlots() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < AccessoryPlayerData.SLOT_COUNT; i++) {
            if (!slots[i].isEmpty()
                    && slots[i].getItem() instanceof IAccessory acc
                    && acc.hasInventoryTab()) {
                result.add(i);
            }
        }
        return result;
    }

    public static void clear() {
        slots[0] = ItemStack.EMPTY;
        slots[1] = ItemStack.EMPTY;
    }
}
