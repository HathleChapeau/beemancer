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
 * Slots:
 *   0 → SPEED    → Vitesse de vol (base 100%)
 *   1 → FORAGING → Vitesse de recherche (base 100%)
 *   2 → TOLERANCE → Vitesse de craft (base 100%)
 *   3 → DROP     → Quantité par déplacement (base 32)
 *
 * Formule bonus: essenceLevel * 0.25f
 *   LESSER=+25%, NORMAL=+50%, GREATER=+75%, PERFECT=+100%
 *
 * Seul le bon type dans le bon slot donne un bonus.
 */
public class ControllerStats {

    public static final int SLOT_SPEED = 0;
    public static final int SLOT_FORAGING = 1;
    public static final int SLOT_TOLERANCE = 2;
    public static final int SLOT_DROP = 3;

    public static final int BASE_SPEED = 100;
    public static final int BASE_FORAGING = 100;
    public static final int BASE_TOLERANCE = 100;
    public static final int BASE_DROP = 32;

    private static final EssenceItem.EssenceType[] SLOT_TYPES = {
        EssenceItem.EssenceType.SPEED,
        EssenceItem.EssenceType.FORAGING,
        EssenceItem.EssenceType.TOLERANCE,
        EssenceItem.EssenceType.DROP
    };

    /**
     * Calcule le pourcentage de vitesse de vol (100 = 100%).
     */
    public static int getFlightSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, SLOT_SPEED, BASE_SPEED);
    }

    /**
     * Calcule le pourcentage de vitesse de recherche (100 = 100%).
     */
    public static int getSearchSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, SLOT_FORAGING, BASE_FORAGING);
    }

    /**
     * Calcule le pourcentage de vitesse de craft (100 = 100%).
     */
    public static int getCraftSpeed(ItemStackHandler essenceSlots) {
        return computePercentStat(essenceSlots, SLOT_TOLERANCE, BASE_TOLERANCE);
    }

    /**
     * Calcule la quantité par déplacement (base 32).
     */
    public static int getQuantity(ItemStackHandler essenceSlots) {
        float bonus = getEssenceBonus(essenceSlots, SLOT_DROP);
        return Math.round(BASE_DROP * (1.0f + bonus));
    }

    /**
     * Calcule un stat en pourcentage (base + bonus essence).
     */
    private static int computePercentStat(ItemStackHandler essenceSlots, int slot, int base) {
        float bonus = getEssenceBonus(essenceSlots, slot);
        return Math.round(base * (1.0f + bonus));
    }

    /**
     * Retourne le bonus d'une essence dans un slot (0.0 si vide ou mauvais type).
     * LESSER=0.25, NORMAL=0.50, GREATER=0.75, PERFECT=1.00
     */
    private static float getEssenceBonus(ItemStackHandler essenceSlots, int slot) {
        ItemStack stack = essenceSlots.getStackInSlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof EssenceItem essence)) {
            return 0.0f;
        }
        if (essence.getEssenceType() != SLOT_TYPES[slot]) {
            return 0.0f;
        }
        return essence.getLevelValue() * 0.25f;
    }

    /**
     * Retourne le multiplicateur de vitesse de vol (1.0 = normal, 2.0 = double).
     */
    public static float getFlightSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getEssenceBonus(essenceSlots, SLOT_SPEED);
    }

    /**
     * Retourne le multiplicateur de vitesse de recherche.
     */
    public static float getSearchSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getEssenceBonus(essenceSlots, SLOT_FORAGING);
    }

    /**
     * Retourne le multiplicateur de vitesse de craft.
     */
    public static float getCraftSpeedMultiplier(ItemStackHandler essenceSlots) {
        return 1.0f + getEssenceBonus(essenceSlots, SLOT_TOLERANCE);
    }
}
