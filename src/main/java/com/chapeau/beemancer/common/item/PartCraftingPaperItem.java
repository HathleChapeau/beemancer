/**
 * ============================================================
 * [PartCraftingPaperItem.java]
 * Description: Item Part Crafting Paper pour recettes machine (INPUT/OUTPUT)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PartCraftingPaperData   | Donnees recette      | Lecture/ecriture sur le stack   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterBlockEntity (slot output A/B)
 * - NetworkInterfaceBlockEntity (craft mode filter slot)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item;

import com.chapeau.beemancer.common.data.PartCraftingPaperData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PartCraftingPaperItem extends Item {

    public PartCraftingPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (!PartCraftingPaperData.hasData(stack)) {
            tooltip.add(Component.translatable("tooltip.beemancer.part_crafting_paper.blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        if (context.registries() == null) return;

        PartCraftingPaperData data = PartCraftingPaperData.readFromStack(stack, context.registries());
        if (data == null) return;

        // Mode indicator
        boolean isInput = data.mode() == PartCraftingPaperData.PartMode.INPUT;
        String modeKey = isInput
                ? "tooltip.beemancer.part_crafting_paper.mode_input"
                : "tooltip.beemancer.part_crafting_paper.mode_output";
        ChatFormatting modeColor = isInput ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE;
        tooltip.add(Component.translatable(modeKey).withStyle(modeColor));

        // Craft ID (short)
        String shortId = data.craftId().toString().substring(0, 4);
        tooltip.add(Component.translatable("tooltip.beemancer.part_crafting_paper.craft_id")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(": " + shortId)
                        .withStyle(ChatFormatting.DARK_GRAY)));

        // Items list
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.literal("")); // separator
            for (ItemStack item : data.items()) {
                tooltip.add(Component.literal("  \u2022 ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(item.getHoverName().copy()
                                .withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" x" + item.getCount())
                                .withStyle(ChatFormatting.YELLOW)));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.beemancer.shift_for_details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return PartCraftingPaperData.hasData(stack);
    }
}
