/**
 * ============================================================
 * [ControllerStats.java]
 * Description: Calcul des stats du Storage Controller depuis les slots essence
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | EssenceItem    | Type/Level essence   | Calcul bonus                   |
 * | ItemStackHandler | Slots essence      | Lecture des essences           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageControllerBlockEntity.java (calcul stats)
 * - StorageTerminalMenu.java (sync ContainerData)
 * - HoneyEnergyManager.java (consommation miel)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.item.essence.EssenceItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Calcule les stats du Storage Controller a partir des slots essence.
 *
 * Chaque slot accepte n'importe quelle essence. Chaque essence dans chaque slot
 * contribue au bonus du type correspondant. Mettre 2 essences du meme type
 * dans 2 slots differents cumule les bonus.
 *
 * Formule bonus par essence: essenceLevel * 0.25f
 *   LESSER=+25%, NORMAL=+50%, GREATER=+75%, PERFECT=+100%
 *
 * Stats (delivery):
 *   SPEED    -> Vitesse de vol (base 100%)
 *   FORAGING -> Vitesse de recherche (base 100%)
 *   DROP     -> Quantite par deplacement (base 32)
 *
 * Stats (honey):
 *   Consumption = (base + chestCost + relayCost + interfaceCost + essenceCost) * hiveMultiplier * (1 - efficiency/100)
 *     base = 20 mB/s, chestCost = chests * 10 mB/s, relayCost = relays * 5 mB/s
 *     essenceCost = sum(tierCost) pour DROP/SPEED/FORAGING (quadratique: 5/15/30/50)
 *   Efficiency = sum(level * 10%) pour INSOMNIA (toujours), DIURNAL (jour), NOCTURNAL (nuit)
 *   TOLERANCE -> Augmente la capacite du buffer miel (+1000/+2000/+4000/+8000)
 */
public class ControllerStats {

    public static final int BASE_SPEED = 100;
    public static final int BASE_FORAGING = 100;
    public static final int BASE_DROP = 32;

    // Honey consumption constants (mB per second)
    public static final int BASE_HONEY_CONSUMPTION = 20;
    public static final int HONEY_PER_CHEST = 10;
    public static final int HONEY_PER_RELAY = 5;
    public static final int EFFICIENCY_PER_LEVEL = 10;

    // Hive honey consumption multipliers (multiplicative between hives)
    public static final float HIVE_MULTIPLIER_T1 = 1.15f;
    public static final float HIVE_MULTIPLIER_T2 = 1.30f;
    public static final float HIVE_MULTIPLIER_T3 = 1.50f;

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
     * Calcule la quantite par deplacement (base 32).
     */
    public static int getQuantity(ItemStackHandler essenceSlots) {
        float bonus = getTotalBonus(essenceSlots, EssenceItem.EssenceType.DROP);
        return Math.round(BASE_DROP * (1.0f + bonus));
    }

    /**
     * Calcule un stat en pourcentage (base + bonus cumule de toutes les essences du type).
     */
    private static int computePercentStat(ItemStackHandler essenceSlots,
                                           EssenceItem.EssenceType type, int base) {
        float bonus = getTotalBonus(essenceSlots, type);
        return Math.round(base * (1.0f + bonus));
    }

    /**
     * Somme les bonus de toutes les essences du type donne dans tous les slots.
     * Mettre 2 SPEED PERFECT = 2 x 1.00 = +200%.
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

    // === Honey Capacity ===

    /**
     * Calcule le bonus de capacite miel apporte par les essences TOLERANCE.
     * LESSER=+1000, NORMAL=+2000, GREATER=+4000, PERFECT=+8000 mB.
     * Plusieurs TOLERANCE se cumulent.
     */
    public static int getHoneyCapacityBonus(ItemStackHandler essenceSlots) {
        int bonus = 0;
        for (int i = 0; i < essenceSlots.getSlots(); i++) {
            ItemStack stack = essenceSlots.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof EssenceItem essence)) {
                continue;
            }
            if (essence.getEssenceType() == EssenceItem.EssenceType.TOLERANCE) {
                bonus += getToleranceTierBonus(essence.getLevelValue());
            }
        }
        return bonus;
    }

    /**
     * Bonus de capacite par tier de TOLERANCE.
     */
    private static int getToleranceTierBonus(int level) {
        return switch (level) {
            case 1 -> 1000;
            case 2 -> 2000;
            case 3 -> 4000;
            case 4 -> 8000;
            default -> 0;
        };
    }

    // === Honey Consumption ===

    /**
     * Calcule la consommation effective de miel en mB/s.
     * Formula: (base + chests * perChest + essenceCost) * (1 - efficiency/100)
     */
    public static int getHoneyConsumption(ItemStackHandler essenceSlots, int chestCount) {
        return getHoneyConsumption(essenceSlots, chestCount, 1.0f, 0, 0, true);
    }

    /**
     * Calcule la consommation effective de miel en mB/s avec multiplicateur hive et relays.
     * Formula: (base + chests * perChest + relays * perRelay + interfaceCost + essenceCost) * hiveMultiplier * (1 - efficiency/100)
     */
    public static int getHoneyConsumption(ItemStackHandler essenceSlots, int chestCount,
                                           float hiveMultiplier, int relayCount) {
        return getHoneyConsumption(essenceSlots, chestCount, hiveMultiplier, relayCount, 0, true);
    }

    /**
     * Calcule la consommation effective de miel en mB/s avec tous les parametres.
     * Formula: (base + chestCost + relayCost + interfaceCost + essenceCost) * hiveMultiplier * (1 - efficiency/100)
     */
    public static int getHoneyConsumption(ItemStackHandler essenceSlots, int chestCount,
                                           float hiveMultiplier, int relayCount,
                                           int interfaceCost, boolean isDaytime) {
        int base = BASE_HONEY_CONSUMPTION;
        int chestCost = chestCount * HONEY_PER_CHEST;
        int relayCost = relayCount * HONEY_PER_RELAY;
        int essenceCost = getEssenceConsumptionCost(essenceSlots);
        int total = base + chestCost + relayCost + interfaceCost + essenceCost;

        int efficiency = getHoneyEfficiency(essenceSlots, isDaytime);
        return Math.max(1, Math.round(total * hiveMultiplier * (1.0f - efficiency / 100.0f)));
    }

    /**
     * Calcule le pourcentage d'efficacite du miel (reduction de consommation).
     * Tier 2: INSOMNIA contribue toujours (level * 10%).
     * Tier 1: DIURNAL contribue le jour, NOCTURNAL contribue la nuit (level * 10%).
     * Aucun plafond: les bonus se cumulent sans limite.
     */
    public static int getHoneyEfficiency(ItemStackHandler essenceSlots, boolean isDaytime) {
        int total = 0;
        for (int i = 0; i < essenceSlots.getSlots(); i++) {
            ItemStack stack = essenceSlots.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof EssenceItem essence)) {
                continue;
            }
            EssenceItem.EssenceType type = essence.getEssenceType();
            if (type == EssenceItem.EssenceType.INSOMNIA) {
                total += essence.getLevelValue() * EFFICIENCY_PER_LEVEL;
            } else if (type == EssenceItem.EssenceType.DIURNAL && isDaytime) {
                total += essence.getLevelValue() * EFFICIENCY_PER_LEVEL;
            } else if (type == EssenceItem.EssenceType.NOCTURNAL && !isDaytime) {
                total += essence.getLevelValue() * EFFICIENCY_PER_LEVEL;
            }
        }
        return total;
    }

    /**
     * Surcharge sans isDaytime (assume jour pour compatibilite).
     */
    public static int getHoneyEfficiency(ItemStackHandler essenceSlots) {
        return getHoneyEfficiency(essenceSlots, true);
    }

    /**
     * Somme le cout en miel des essences de production (DROP, SPEED, FORAGING).
     * Cout quadratique par tier: LESSER=5, NORMAL=15, GREATER=30, PERFECT=50 mB/s.
     * TOLERANCE, DIURNAL, NOCTURNAL, INSOMNIA ne coutent pas de miel.
     */
    private static int getEssenceConsumptionCost(ItemStackHandler essenceSlots) {
        int cost = 0;
        for (int i = 0; i < essenceSlots.getSlots(); i++) {
            ItemStack stack = essenceSlots.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof EssenceItem essence)) {
                continue;
            }
            EssenceItem.EssenceType type = essence.getEssenceType();
            if (type == EssenceItem.EssenceType.DROP || type == EssenceItem.EssenceType.SPEED
                || type == EssenceItem.EssenceType.FORAGING) {
                cost += getEssenceTierCost(essence.getLevelValue());
            }
        }
        return cost;
    }

    /**
     * Cout quadratique par tier d'essence.
     */
    private static int getEssenceTierCost(int level) {
        return switch (level) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 30;
            case 4 -> 50;
            default -> 0;
        };
    }
}
