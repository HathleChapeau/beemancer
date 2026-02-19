/**
 * ============================================================
 * [HoverbikePartItem.java]
 * Description: Item representant une piece modulaire du hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePart       | Enum categories      | Identifie la categorie         |
 * | HoverbikePartData   | Stats/modifiers      | Tooltip et nom MK              |
 * | AppliedStat         | Base stats           | Affichage tooltip              |
 * | AppliedModifier     | Modifiers            | Affichage tooltip (shift)      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java: Enregistrement des 12 items de pieces
 * - AssemblyTableBlock.java: Validation du type d'item
 * - ApicaCreativeTabs.java: Ajout dans le tab hoverbike
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikePart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item pour une piece de hoverbike.
 * Chaque variante (3 par categorie, 4 categories = 12 items) a sa propre instance.
 * Affiche les base stats en tooltip, et prefix/suffix quand Shift est maintenu.
 */
public class HoverbikePartItem extends Item {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI"};

    private final HoverbikePart category;
    private final int variantIndex;

    public HoverbikePartItem(Properties properties, HoverbikePart category, int variantIndex) {
        super(properties);
        this.category = category;
        this.variantIndex = variantIndex;
    }

    public HoverbikePart getCategory() {
        return category;
    }

    public int getVariantIndex() {
        return variantIndex;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = (MutableComponent) super.getName(stack);
        int mk = HoverbikePartData.getMK(stack);
        if (mk > 0) {
            String roman = mk < ROMAN.length ? ROMAN[mk] : String.valueOf(mk);
            name = name.append(Component.literal(" MK " + roman).withStyle(ChatFormatting.YELLOW));
        }
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Base stats (toujours visibles)
        List<AppliedStat> baseStats = HoverbikePartData.getBaseStats(stack);
        if (!baseStats.isEmpty()) {
            for (AppliedStat stat : baseStats) {
                String sign = stat.value() >= 0 ? "+" : "";
                tooltip.add(Component.literal("  " + stat.statType().getJsonKey() + ": " +
                        sign + formatValue(stat.value())).withStyle(ChatFormatting.GRAY));
            }
        }

        // Prefix/suffix (visibles avec Shift)
        List<AppliedModifier> prefixes = HoverbikePartData.getPrefixes(stack);
        List<AppliedModifier> suffixes = HoverbikePartData.getSuffixes(stack);
        boolean hasModifiers = !prefixes.isEmpty() || !suffixes.isEmpty();

        if (hasModifiers) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (!prefixes.isEmpty()) {
                    tooltip.add(Component.literal("--- Prefix ---").withStyle(ChatFormatting.GOLD));
                    for (AppliedModifier mod : prefixes) {
                        tooltip.add(Component.literal("  T" + mod.tier() + " " +
                                mod.statType().getJsonKey() + " " + formatModValue(mod))
                                .withStyle(ChatFormatting.GOLD));
                    }
                }
                if (!suffixes.isEmpty()) {
                    tooltip.add(Component.literal("--- Suffix ---").withStyle(ChatFormatting.AQUA));
                    for (AppliedModifier mod : suffixes) {
                        tooltip.add(Component.literal("  T" + mod.tier() + " " +
                                mod.statType().getJsonKey() + " " + formatModValue(mod))
                                .withStyle(ChatFormatting.AQUA));
                    }
                }
            } else {
                tooltip.add(Component.literal("[Shift] for details").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static String formatValue(double value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.3f", value);
    }

    private static String formatModValue(AppliedModifier mod) {
        String sign = mod.value() >= 0 ? "+" : "";
        if ("%".equals(mod.valueType())) {
            return sign + String.format("%.1f", mod.value()) + "%";
        }
        return sign + formatValue(mod.value());
    }

    /**
     * Verifie si un ItemStack est une piece de hoverbike.
     */
    public static boolean isHoverbikePart(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof HoverbikePartItem;
    }
}
