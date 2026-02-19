/**
 * ============================================================
 * [HoverbikePartData.java]
 * Description: Utilitaire pour lire/écrire les stats d'une pièce de hoverbike sur un ItemStack
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AppliedStat             | Base stats           | Lecture dynamique              |
 * | AppliedModifier         | Modifiers per-stack  | Stockage/lecture NBT           |
 * | HoverbikeConfigManager  | Config base stats    | Source des base stats          |
 * | HoverbikeStatType       | Enum des stats       | Parsing des clés               |
 * | HoverbikePartItem       | Item de pièce        | Catégorie et variant index     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoverbikeStatRoller.java: Ajout de modifiers
 * - HoverbikeSettingsComputer.java: Lecture des stats
 * - AssemblyTableStatsRenderer.java: Affichage billboard
 * - CreativeFocusItem.java: Via le roller
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikeConfigManager;
import com.chapeau.apica.common.entity.mount.HoverbikeStatType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire statique pour accéder aux données de stats sur un HoverbikePartItem stack.
 * Les base stats sont lues dynamiquement depuis HoverbikeConfigManager (pas en NBT).
 * Les modifiers (prefix/suffix) sont stockés en NBT via DataComponents.CUSTOM_DATA.
 */
public final class HoverbikePartData {

    private static final String ROOT_KEY = "HoverbikePartData";
    private static final String MODIFIERS_KEY = "Modifiers";
    private static final String MK_KEY = "MK";
    private static final int MAX_PREFIX = 3;
    private static final int MAX_SUFFIX = 3;

    private HoverbikePartData() {}

    // =========================================================================
    // BASE STATS (dynamiques, depuis config)
    // =========================================================================

    /**
     * Retourne les 2 base stats de cette pièce, lues depuis la config JSON.
     * Retourne une liste vide si le stack n'est pas un HoverbikePartItem ou si pas de config.
     */
    public static List<AppliedStat> getBaseStats(ItemStack stack) {
        if (!(stack.getItem() instanceof HoverbikePartItem part)) return List.of();
        String category = part.getCategory().name().toLowerCase();
        int variant = part.getVariantIndex();

        String[] keys = HoverbikeConfigManager.getPartBaseStatKeys(category, variant);
        double[] vals = HoverbikeConfigManager.getPartBaseStatValues(category, variant);
        if (keys == null || vals == null) return List.of();

        List<AppliedStat> stats = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            HoverbikeStatType type = HoverbikeStatType.fromJsonKey(keys[i]);
            if (type != null) {
                stats.add(new AppliedStat(type, vals[i]));
            }
        }
        return stats;
    }

    // =========================================================================
    // MODIFIERS (per-stack, NBT)
    // =========================================================================

    /** Retourne tous les modifiers appliqués à ce stack. */
    public static List<AppliedModifier> getAllModifiers(ItemStack stack) {
        CompoundTag root = getDataTag(stack);
        if (root == null || !root.contains(MODIFIERS_KEY, Tag.TAG_LIST)) return List.of();
        ListTag list = root.getList(MODIFIERS_KEY, Tag.TAG_COMPOUND);
        List<AppliedModifier> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            AppliedModifier mod = AppliedModifier.load(list.getCompound(i));
            if (mod != null) result.add(mod);
        }
        return result;
    }

    /** Retourne uniquement les prefix. */
    public static List<AppliedModifier> getPrefixes(ItemStack stack) {
        return getAllModifiers(stack).stream().filter(AppliedModifier::isPrefix).toList();
    }

    /** Retourne uniquement les suffix. */
    public static List<AppliedModifier> getSuffixes(ItemStack stack) {
        return getAllModifiers(stack).stream().filter(m -> !m.isPrefix()).toList();
    }

    /** Ajoute un modifier au stack et recalcule le MK. */
    public static void addModifier(ItemStack stack, AppliedModifier modifier) {
        CompoundTag root = getOrCreateDataTag(stack);
        ListTag list = root.contains(MODIFIERS_KEY, Tag.TAG_LIST)
                ? root.getList(MODIFIERS_KEY, Tag.TAG_COMPOUND) : new ListTag();
        list.add(modifier.save());
        root.put(MODIFIERS_KEY, list);
        recalcMK(root);
        saveDataTag(stack, root);
    }

    /** Le MK est le nombre total de modifiers appliqués. */
    public static int getMK(ItemStack stack) {
        CompoundTag root = getDataTag(stack);
        if (root == null) return 0;
        return root.getInt(MK_KEY);
    }

    public static boolean canAddPrefix(ItemStack stack) {
        return getPrefixes(stack).size() < MAX_PREFIX;
    }

    public static boolean canAddSuffix(ItemStack stack) {
        return getSuffixes(stack).size() < MAX_SUFFIX;
    }

    /** Vérifie si le stack a des données de modifier (peut n'avoir que le tag vide). */
    public static boolean hasModifiers(ItemStack stack) {
        return !getAllModifiers(stack).isEmpty();
    }

    // =========================================================================
    // NBT HELPERS (pattern MagicBeeItem)
    // =========================================================================

    private static CompoundTag getDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return null;
        return tag.getCompound(ROOT_KEY);
    }

    private static CompoundTag getOrCreateDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag outer = (customData != null) ? customData.copyTag() : new CompoundTag();
        if (!outer.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            outer.put(ROOT_KEY, new CompoundTag());
        }
        return outer.getCompound(ROOT_KEY);
    }

    private static void saveDataTag(ItemStack stack, CompoundTag dataRoot) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag outer = (customData != null) ? customData.copyTag() : new CompoundTag();
        outer.put(ROOT_KEY, dataRoot);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(outer));
    }

    private static void recalcMK(CompoundTag root) {
        if (!root.contains(MODIFIERS_KEY, Tag.TAG_LIST)) {
            root.putInt(MK_KEY, 0);
            return;
        }
        int count = root.getList(MODIFIERS_KEY, Tag.TAG_COMPOUND).size();
        int currentMK = root.getInt(MK_KEY);
        // MK ne descend jamais
        if (count > currentMK) {
            root.putInt(MK_KEY, count);
        }
    }
}
