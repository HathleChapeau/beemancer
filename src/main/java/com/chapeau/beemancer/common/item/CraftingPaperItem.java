/**
 * ============================================================
 * [CraftingPaperItem.java]
 * Description: Item Crafting Paper pour inscrire des recettes vanilla
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CraftingPaperData   | Donnees recette      | Lecture/ecriture sur le stack   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterBlockEntity (slot reserve + bibliotheque)
 * - CrafterMenu (slot validation)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item;

import com.chapeau.beemancer.common.data.CraftingPaperData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CraftingPaperItem extends Item {

    public CraftingPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (!CraftingPaperData.hasData(stack)) {
            tooltip.add(Component.translatable("tooltip.beemancer.crafting_paper.blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        if (context.registries() == null) return;

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            CraftingPaperData data = CraftingPaperData.readFromStack(stack, context.registries());
            if (data == null) return;

            tooltip.add(Component.translatable("tooltip.beemancer.crafting_paper.recipe")
                    .withStyle(ChatFormatting.GOLD));

            // Render 3x3 grid
            for (int row = 0; row < 3; row++) {
                StringBuilder line = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    int index = row * 3 + col;
                    ItemStack ingredient = data.ingredients().get(index);
                    if (ingredient.isEmpty()) {
                        line.append("[_] ");
                    } else {
                        String name = ingredient.getHoverName().getString();
                        String shortName = name.length() > 8 ? name.substring(0, 8) : name;
                        line.append("[").append(shortName).append("] ");
                    }
                }
                tooltip.add(Component.literal(line.toString().trim())
                        .withStyle(ChatFormatting.GRAY));
            }

            // Result
            if (!data.result().isEmpty()) {
                tooltip.add(Component.literal("")); // separator
                tooltip.add(Component.literal("\u2192 ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(data.result().getHoverName().copy()
                                .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" x" + data.result().getCount())
                                .withStyle(ChatFormatting.GREEN)));
            }
        } else {
            CraftingPaperData data = CraftingPaperData.readFromStack(stack, context.registries());
            if (data != null && !data.result().isEmpty()) {
                tooltip.add(data.result().getHoverName().copy().withStyle(ChatFormatting.GOLD));
            }
            tooltip.add(Component.translatable("tooltip.beemancer.shift_for_details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return CraftingPaperData.hasData(stack);
    }
}
