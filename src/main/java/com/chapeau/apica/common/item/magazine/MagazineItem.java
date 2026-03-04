/**
 * ============================================================
 * [MagazineItem.java]
 * Description: Item magazine stockant jusqu'a 1 bucket de fluide
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagazineFluidData   | Stockage fluide      | R/W donnees CUSTOM_DATA        |
 * | ApicaItems          | Registre             | createFilled() factory         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - ApicaCreativeTabs.java (creative tab)
 * - MagazineEquipPacket.java (validation)
 * - MagazineData.java (creation au retrait)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Magazine — reservoir de fluide portable (max 1000 mB).
 * Affiche une barre de durabilite coloree selon le fluide contenu.
 * Se remplit dans l'Infuser et s'equipe sur les items IMagazineHolder.
 */
public class MagazineItem extends Item {

    private static final int HONEY_COLOR = 0xE8A317;
    private static final int ROYAL_JELLY_COLOR = 0xFFF8DC;
    private static final int NECTAR_COLOR = 0xB050FF;
    private static final int DEFAULT_COLOR = 0x888888;

    public MagazineItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !MagazineFluidData.isEmpty(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int amount = MagazineFluidData.getFluidAmount(stack);
        return Math.round((float) amount / MagazineFluidData.MAX_CAPACITY * 13f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        String fluidId = MagazineFluidData.getFluidId(stack);
        if (fluidId.contains("honey")) return HONEY_COLOR;
        if (fluidId.contains("royal_jelly")) return ROYAL_JELLY_COLOR;
        if (fluidId.contains("nectar")) return NECTAR_COLOR;
        return DEFAULT_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (MagazineFluidData.isEmpty(stack)) {
            tooltip.add(Component.translatable("tooltip.apica.magazine.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            String fluidId = MagazineFluidData.getFluidId(stack);
            int amount = MagazineFluidData.getFluidAmount(stack);
            String fluidName = getFluidDisplayName(fluidId);
            tooltip.add(Component.literal(fluidName + ": " + amount + "/" +
                    MagazineFluidData.MAX_CAPACITY + " mB")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * Cree un MagazineItem rempli avec le fluide specifie.
     * Utilise pour le creative tab.
     */
    public static ItemStack createFilled(String fluidId, int amount) {
        ItemStack stack = new ItemStack(ApicaItems.MAGAZINE.get());
        MagazineFluidData.setFluid(stack, fluidId, amount);
        return stack;
    }

    /** Nom affichable du fluide. */
    private static String getFluidDisplayName(String fluidId) {
        if (fluidId.contains("honey")) return "Honey";
        if (fluidId.contains("royal_jelly")) return "Royal Jelly";
        if (fluidId.contains("nectar")) return "Nectar";
        return "Fluid";
    }
}
