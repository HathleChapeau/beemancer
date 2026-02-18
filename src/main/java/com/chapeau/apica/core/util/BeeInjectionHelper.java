/**
 * ============================================================
 * [BeeInjectionHelper.java]
 * Description: Utilitaire pour lire/ecrire les donnees d'injection sur les items abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MagicBeeItem            | Pattern CustomData   | Lecture/ecriture tags          |
 * | EssenceItem.EssenceType | Types d'essence      | Mapping stat cible             |
 * | InjectionConfigManager  | Config globale       | Max hunger, points par niveau  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InjectorBlockEntity.java (modification des stats abeille)
 * - MagicBeeItem.java (lecture isFoil pour abeilles rassasiees)
 *
 * ============================================================
 */
package com.chapeau.apica.core.util;

import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.core.config.InjectionConfigManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Methodes statiques pour manipuler les donnees d'injection d'essence
 * stockees dans le CustomData des items MagicBee.
 * Toutes les donnees sont sous la cle "InjectionData" du CompoundTag.
 */
public class BeeInjectionHelper {

    private static final String ROOT_KEY = "InjectionData";
    private static final String BONUS_DROP = "BonusDrop";
    private static final String BONUS_SPEED = "BonusSpeed";
    private static final String BONUS_FORAGING = "BonusForaging";
    private static final String BONUS_TOLERANCE = "BonusTolerance";
    private static final String BONUS_ACTIVITY = "BonusActivity";
    private static final String DROP_POINTS = "DropPoints";
    private static final String SPEED_POINTS = "SpeedPoints";
    private static final String FORAGING_POINTS = "ForagingPoints";
    private static final String TOLERANCE_POINTS = "TolerancePoints";
    private static final String ACTIVITY_POINTS = "ActivityPoints";
    private static final String HUNGER = "Hunger";
    private static final String SATIATED = "Satiated";
    private static final String HARMONIZED = "Harmonized";

    // ========== LECTURE ==========

    public static boolean isSatiated(ItemStack stack) {
        CompoundTag injection = getInjectionTag(stack);
        return injection != null && injection.getBoolean(SATIATED);
    }

    public static boolean isHarmonized(ItemStack stack) {
        CompoundTag injection = getInjectionTag(stack);
        return injection != null && injection.getBoolean(HARMONIZED);
    }

    public static int getHunger(ItemStack stack) {
        CompoundTag injection = getInjectionTag(stack);
        return injection != null ? injection.getInt(HUNGER) : 0;
    }

    public static int getBonusLevel(ItemStack stack, EssenceItem.EssenceType type) {
        CompoundTag injection = getInjectionTag(stack);
        if (injection == null) return 0;
        return injection.getInt(getBonusKey(type));
    }

    public static int getAccumulatedPoints(ItemStack stack, EssenceItem.EssenceType type) {
        CompoundTag injection = getInjectionTag(stack);
        if (injection == null) return 0;
        return injection.getInt(getPointsKey(type));
    }

    /**
     * Calcule les points totaux pour la jauge (baseLevel * pointsPerLevel + bonusLevel * pointsPerLevel + accumulated).
     */
    public static int getTotalGaugePoints(ItemStack stack, EssenceItem.EssenceType type, int baseLevel) {
        int ppl = InjectionConfigManager.getPointsPerLevel();
        int bonus = getBonusLevel(stack, type);
        int accumulated = getAccumulatedPoints(stack, type);
        return (baseLevel + bonus) * ppl + accumulated;
    }

    // ========== ECRITURE ==========

    /**
     * Ajoute des points de stat a l'abeille pour un type donne.
     * Respecte le cap par niveau d'essence et le cap max (4 pour stats, 2 pour activite).
     *
     * @param stack    l'item abeille
     * @param type     le type d'essence
     * @param points   les points a ajouter
     * @param baseLevel le niveau de base de l'espece pour ce stat
     * @param essenceLevelCap le niveau max impose par le niveau de l'essence utilisee
     */
    public static void addStatPoints(ItemStack stack, EssenceItem.EssenceType type,
                                     int points, int baseLevel, int essenceLevelCap) {
        int maxLevel = isActivityType(type) ? 2 : 4;
        int currentBonus = getBonusLevel(stack, type);
        int currentLevel = baseLevel + currentBonus;

        // Cap check: si le niveau actuel >= cap de l'essence, pas de gain de stat
        if (currentLevel >= essenceLevelCap || currentLevel >= maxLevel) return;

        int ppl = InjectionConfigManager.getPointsPerLevel();
        int accumulated = getAccumulatedPoints(stack, type);
        accumulated += points;

        // Promotion de niveau
        while (accumulated >= ppl && (baseLevel + currentBonus + 1) <= Math.min(essenceLevelCap, maxLevel)) {
            accumulated -= ppl;
            currentBonus++;
        }

        // Si on a atteint le cap, ne pas accumuler au-dela
        if ((baseLevel + currentBonus) >= Math.min(essenceLevelCap, maxLevel)) {
            accumulated = 0;
        }

        CompoundTag injection = getOrCreateInjectionTag(stack);
        injection.putInt(getBonusKey(type), currentBonus);
        injection.putInt(getPointsKey(type), accumulated);
        saveInjectionTag(stack, injection);
    }

    /**
     * Ajoute de la faim a l'abeille. Si la faim depasse le max, marque comme rassasiee.
     */
    public static void addHunger(ItemStack stack, int cost) {
        CompoundTag injection = getOrCreateInjectionTag(stack);
        int hunger = injection.getInt(HUNGER) + cost;
        int maxHunger = InjectionConfigManager.getMaxHunger();
        if (hunger >= maxHunger) {
            hunger = maxHunger;
            injection.putBoolean(SATIATED, true);
        }
        injection.putInt(HUNGER, hunger);
        saveInjectionTag(stack, injection);
    }

    /**
     * Marque l'abeille comme harmonized ou non.
     */
    public static void setHarmonized(ItemStack stack, boolean harmonized) {
        CompoundTag injection = getOrCreateInjectionTag(stack);
        injection.putBoolean(HARMONIZED, harmonized);
        saveInjectionTag(stack, injection);
    }

    /**
     * Sature l'abeille instantanement (hunger = max, satiated = true).
     */
    public static void saturateInstantly(ItemStack stack) {
        CompoundTag injection = getOrCreateInjectionTag(stack);
        int maxHunger = InjectionConfigManager.getMaxHunger();
        injection.putInt(HUNGER, maxHunger);
        injection.putBoolean(SATIATED, true);
        saveInjectionTag(stack, injection);
    }

    // ========== HELPERS INTERNES ==========

    public static boolean isActivityType(EssenceItem.EssenceType type) {
        return type == EssenceItem.EssenceType.DIURNAL
            || type == EssenceItem.EssenceType.NOCTURNAL
            || type == EssenceItem.EssenceType.INSOMNIA;
    }

    /**
     * Convertit un type d'activite en niveau pour le cap check.
     * DIURNAL=0 (base), NOCTURNAL=1, INSOMNIA=2.
     */
    public static int getActivityLevel(String dayNight) {
        return switch (dayNight) {
            case "night" -> 1;
            case "both" -> 2;
            default -> 0;
        };
    }

    /**
     * Retourne le niveau effectif d'activite d'une abeille (base + bonus).
     */
    public static int getEffectiveActivityLevel(ItemStack stack, int baseActivityLevel) {
        CompoundTag injection = getInjectionTag(stack);
        int bonus = injection != null ? injection.getInt(BONUS_ACTIVITY) : 0;
        return Math.min(baseActivityLevel + bonus, 2);
    }

    /**
     * Retourne le cap de niveau d'une essence d'activite.
     */
    public static int getActivityEssenceCap(EssenceItem.EssenceType type) {
        return switch (type) {
            case DIURNAL -> 0;
            case NOCTURNAL -> 1;
            case INSOMNIA -> 2;
            default -> 0;
        };
    }

    private static String getBonusKey(EssenceItem.EssenceType type) {
        return switch (type) {
            case DROP -> BONUS_DROP;
            case SPEED -> BONUS_SPEED;
            case FORAGING -> BONUS_FORAGING;
            case TOLERANCE -> BONUS_TOLERANCE;
            case DIURNAL, NOCTURNAL, INSOMNIA -> BONUS_ACTIVITY;
        };
    }

    private static String getPointsKey(EssenceItem.EssenceType type) {
        return switch (type) {
            case DROP -> DROP_POINTS;
            case SPEED -> SPEED_POINTS;
            case FORAGING -> FORAGING_POINTS;
            case TOLERANCE -> TOLERANCE_POINTS;
            case DIURNAL, NOCTURNAL, INSOMNIA -> ACTIVITY_POINTS;
        };
    }

    private static CompoundTag getInjectionTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        return tag.contains(ROOT_KEY) ? tag.getCompound(ROOT_KEY) : null;
    }

    private static CompoundTag getOrCreateInjectionTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        return tag.contains(ROOT_KEY) ? tag.getCompound(ROOT_KEY) : new CompoundTag();
    }

    private static void saveInjectionTag(ItemStack stack, CompoundTag injection) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.put(ROOT_KEY, injection);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
