/**
 * ============================================================
 * [ControllerStats.java]
 * Description: Calcul des 4 stats du Storage Controller depuis les slots essence
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | EssenceItem    | Type/Level essence   | Calcul bonus                   |
 * | ItemStackHandler | Slots essence      | Lecture des essences           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (calcul stats)
 * - StorageControllerMenu.java (sync ContainerData)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.item.essence.EssenceItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Calcule les 4 stats du Storage Controller à partir des 4 slots essence.
 *
 * Chaque slot accepte n'importe quelle essence. Chaque essence dans chaque slot
 * contribue au bonus du type correspondant. Mettre 2 essences du même type
 * dans 2 slots différents cumule les bonus.
 *
 * Formule bonus par essence: essenceLevel * 0.25f
 *   LESSER=+25%, NORMAL=+50%, GREATER=+75%, PERFECT=+100%
 *
 * Stats:
 *   SPEED    → Vitesse de vol (base 100%)
 *   FORAGING → Vitesse de recherche (base 100%)
 *   TOLERANCE → Vitesse de craft (base 100%)
 *   DROP     → Quantité par déplacement (base 32)
 */
public class ControllerStats {

    public static final int BASE_SPEED = 100;
    public static final int BASE_FORAGING = 100;
    public static final int BASE_TOLERANCE = 100;
    public static final int BASE_DROP = 32;

    /**
     * Calcule le pourcentage de vitesse de vol (100 = 100%).
     */
    public static int getFlightSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, EssenceItem.EssenceType.SPEED, BASE_SPEED);
    }

    /**
     * Calcule le pourcentage de vitesse de recherche (100 = 100%).
     */
    public static int getSearchSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, EssenceItem.EssenceType.FORAGING, BASE_FORAGING);
    }

    /**
     * Calcule le pourcentage de vitesse de craft (100 = 100%).
     */
    public static int getCraftSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, EssenceItem.EssenceType.TOLERANCE, BASE_TOLERANCE);
    }

    /**
     * Calcule la quantité par déplacement (base 32).
     */
    public static int getQuantity(ItemStackHandler essenceSlots) {
        float bonus = getTotalBonus(essenceSlots, EssenceItem.EssenceType.DROP);
        return Math.round(BASE_DROP * (1.0f + bonus));
    }

    /**
     * Calcule un stat en pourcentage (base + bonus cumulé de toutes les essences du type).
     */
    private static int computePercentStat(ItemStackHandler essenceSlots,
                                           EssenceItem.EssenceType type, int base) {
        float bonus = getTotalBonus(essenceSlots, type);
        return Math.round(base * (1.0f + bonus));
    }

    /**
     * Somme les bonus de toutes les essences du type donné dans tous les slots.
     * Mettre 2 SPEED PERFECT = 2 × 1.00 = +200%.
     * LESSER=0.25, NORMAL=0.50, GREATER=0.75, PERFECT=1.00
     */
    private static float getTotalBonus(ItemStackHandler essenceSlots, EssenceItem.EssenceType type) {
        float total = 0.0f;
        for (int i = 0; i < essenceSlots.getSlots(); i++) {
            ItemStack stack = essenceSlots.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof EssenceItem essence)) {
                continue;
            }
            if (essence.getEssenceType() == type) {
                total += essence.getLevelValue() * 0.25f;
            }
        }
        return total;
    }

    /**
     * Retourne le multiplicateur de vitesse de vol (1.0 = normal, 2.0 = double).
     */
    public static float getFlightSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getTotalBonus(essenceSlots, EssenceItem.EssenceType.SPEED);
    }

    /**
     * Retourne le multiplicateur de vitesse de recherche.
     */
    public static float getSearchSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getTotalBonus(essenceSlots, EssenceItem.EssenceType.FORAGING);
    }

    /**
     * Retourne le multiplicateur de vitesse de craft.
     */
    public static float getCraftSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getTotalBonus(essenceSlots, EssenceItem.EssenceType.TOLERANCE);
    }
}
