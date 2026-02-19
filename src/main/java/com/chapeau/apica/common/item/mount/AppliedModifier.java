/**
 * ============================================================
 * [AppliedModifier.java]
 * Description: Record représentant un modifier rollé appliqué à une pièce de hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeStatType   | Enum des stats       | Identifie la stat modifiée     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoverbikePartData.java: Stockage/lecture NBT des modifiers
 * - HoverbikeStatRoller.java: Création après roll
 * - HoverbikeSettingsComputer.java: Application des modifiers
 * - AssemblyTableStatsRenderer.java: Affichage billboard
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikeStatType;
import net.minecraft.nbt.CompoundTag;

/**
 * Modifier appliqué à une pièce de hoverbike après un roll.
 * Stocké en NBT sur l'ItemStack via CUSTOM_DATA.
 * La valeur est déjà rollée et ne changera plus.
 *
 * @param modifierName nom du modifier source (ex: "Swift", "of Stability")
 * @param isPrefix     true si prefix, false si suffix
 * @param tier         tier du roll (1-8)
 * @param statType     la statistique affectée
 * @param valueType    "+" pour valeur absolue, "%" pour pourcentage
 * @param value        la valeur rollée
 */
public record AppliedModifier(
        String modifierName,
        boolean isPrefix,
        int tier,
        HoverbikeStatType statType,
        String valueType,
        double value
) {

    private static final String KEY_NAME = "Name";
    private static final String KEY_IS_PREFIX = "IsPrefix";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_STAT_TYPE = "StatType";
    private static final String KEY_VALUE_TYPE = "ValueType";
    private static final String KEY_VALUE = "Value";

    /**
     * Sérialise ce modifier en CompoundTag pour stockage NBT.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_NAME, modifierName);
        tag.putBoolean(KEY_IS_PREFIX, isPrefix);
        tag.putInt(KEY_TIER, tier);
        tag.putString(KEY_STAT_TYPE, statType.getJsonKey());
        tag.putString(KEY_VALUE_TYPE, valueType);
        tag.putDouble(KEY_VALUE, value);
        return tag;
    }

    /**
     * Désérialise un modifier depuis un CompoundTag.
     * Retourne null si le tag est invalide ou contient un type de stat inconnu.
     */
    public static AppliedModifier load(CompoundTag tag) {
        String name = tag.getString(KEY_NAME);
        boolean prefix = tag.getBoolean(KEY_IS_PREFIX);
        int t = tag.getInt(KEY_TIER);
        String statKey = tag.getString(KEY_STAT_TYPE);
        HoverbikeStatType stat = HoverbikeStatType.fromJsonKey(statKey);
        if (stat == null) return null;
        String vType = tag.getString(KEY_VALUE_TYPE);
        double val = tag.getDouble(KEY_VALUE);
        return new AppliedModifier(name, prefix, t, stat, vType, val);
    }
}
