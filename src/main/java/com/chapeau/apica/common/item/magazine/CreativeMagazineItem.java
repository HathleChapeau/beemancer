/**
 * ============================================================
 * [CreativeMagazineItem.java]
 * Description: Magazine creatif infini (nectar, ne se vide jamais)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagazineItem        | Base class           | Comportement magazine          |
 * | MagazineFluidData   | Constantes           | MAX_CAPACITY                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - ApicaCreativeTabs.java (creative tab)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Magazine creatif — toujours plein de nectar, ne se vide jamais.
 * Ideal pour les tests et le mode creatif.
 */
public class CreativeMagazineItem extends MagazineItem {

    public static final String NECTAR_FLUID_ID = "apica:nectar";
    private static final int NECTAR_COLOR = 0xB050FF;

    public CreativeMagazineItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13; // Toujours plein
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return NECTAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.apica.creative_magazine")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Nectar: ∞/" + MagazineFluidData.MAX_CAPACITY + " mB")
                .withStyle(ChatFormatting.GOLD));
    }
}
