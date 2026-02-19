/**
 * ============================================================
 * [HoverbikeSettingsComputer.java]
 * Description: Calcule les HoverbikeSettings finaux a partir des pieces equipees
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoverbikeSettings       | Settings de base     | Base globale, factory fromMap  |
 * | HoverbikeStatType       | Enum stats           | Iteration, cles                |
 * | HoverbikePartData       | Lecture stats item   | Base stats + modifiers         |
 * | AppliedStat             | Base stat record     | Valeur par stat type           |
 * | AppliedModifier         | Modifier record      | Valeur + valueType (+ / %)     |
 * | HoverbikeConfigManager  | Config globale       | Base stats de reference        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEntity.java: Recalcul des settings quand les pieces changent
 *
 * ============================================================
 */
package com.chapeau.apica.common.entity.mount;

import com.chapeau.apica.common.item.mount.AppliedModifier;
import com.chapeau.apica.common.item.mount.AppliedStat;
import com.chapeau.apica.common.item.mount.HoverbikePartData;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule les HoverbikeSettings finaux a partir des 4 pieces equipees.
 * Algorithme:
 * 1. Commencer par les base stats globales (depuis config JSON)
 * 2. Ajouter les base stats de chaque piece (valeurs fixes du variant)
 * 3. Multiplier par tous les modifiers de type "%" (percentage)
 * 4. Ajouter tous les modifiers de type "+" (flat)
 * 5. Construire un HoverbikeSettings final via fromMap()
 */
public final class HoverbikeSettingsComputer {

    private HoverbikeSettingsComputer() {}

    /**
     * Calcule les settings finaux a partir des 4 part stacks.
     * Les stacks vides sont ignores (pas de contribution).
     *
     * @param partStacks tableau de 4 ItemStacks (chassis, coeur, propulseur, radiateur)
     * @return HoverbikeSettings avec toutes les contributions des pieces
     */
    public static HoverbikeSettings compute(ItemStack[] partStacks) {
        HoverbikeSettings base = HoverbikeConfigManager.getBaseStats();

        // Etape 1: Copier les base stats globales dans une map mutable
        Map<HoverbikeStatType, Double> stats = new EnumMap<>(HoverbikeStatType.class);
        for (HoverbikeStatType type : HoverbikeStatType.values()) {
            stats.put(type, base.getStat(type));
        }

        // Etape 2: Ajouter les base stats de chaque piece (flat add)
        for (ItemStack stack : partStacks) {
            if (stack.isEmpty()) continue;
            List<AppliedStat> baseStats = HoverbikePartData.getBaseStats(stack);
            for (AppliedStat stat : baseStats) {
                stats.merge(stat.statType(), stat.value(), Double::sum);
            }
        }

        // Etape 3: Collecter tous les modifiers % et les appliquer
        Map<HoverbikeStatType, Double> percentTotals = new EnumMap<>(HoverbikeStatType.class);
        for (ItemStack stack : partStacks) {
            if (stack.isEmpty()) continue;
            for (AppliedModifier mod : HoverbikePartData.getAllModifiers(stack)) {
                if ("%".equals(mod.valueType())) {
                    percentTotals.merge(mod.statType(), mod.value(), Double::sum);
                }
            }
        }
        for (Map.Entry<HoverbikeStatType, Double> entry : percentTotals.entrySet()) {
            double current = stats.getOrDefault(entry.getKey(), 0.0);
            stats.put(entry.getKey(), current * (1.0 + entry.getValue()));
        }

        // Etape 4: Ajouter tous les modifiers flat (+)
        for (ItemStack stack : partStacks) {
            if (stack.isEmpty()) continue;
            for (AppliedModifier mod : HoverbikePartData.getAllModifiers(stack)) {
                if ("+".equals(mod.valueType())) {
                    stats.merge(mod.statType(), mod.value(), Double::sum);
                }
            }
        }

        // Etape 5: Construire les settings finaux
        return HoverbikeSettings.fromMap(stats);
    }
}
