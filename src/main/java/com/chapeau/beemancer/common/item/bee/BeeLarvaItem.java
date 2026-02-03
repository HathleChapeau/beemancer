/**
 * ============================================================
 * [BeeLarvaItem.java]
 * Description: Item représentant une larve d'abeille avec gènes hérités
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeGeneData         | Stockage gènes       | Héritage des parents           |
 * | GeneRegistry        | Accès catégories     | Affichage tooltip              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity (création via breeding)
 * - IncubatorBlockEntity (transformation en bee)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.Comparator;
import java.util.List;

public class BeeLarvaItem extends Item {
    public BeeLarvaItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // --- Factory Methods ---

    public static ItemStack createWithGenes(BeeGeneData geneData) {
        ItemStack stack = new ItemStack(BeemancerItems.BEE_LARVA.get());
        saveGeneData(stack, geneData);
        return stack;
    }

    // --- Gene Data ---

    public static void saveGeneData(ItemStack stack, BeeGeneData geneData) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.put("GeneData", geneData.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static BeeGeneData getGeneData(ItemStack stack) {
        BeeGeneData data = new BeeGeneData();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("GeneData")) {
                data.load(tag.getCompound("GeneData"));
            }
        }
        return data;
    }

    // --- Helpers ---

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag();
        }
        return new CompoundTag();
    }

    // --- Tooltip ---

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        BeeGeneData geneData = getGeneData(stack);
        Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
        if (speciesGene != null) {
            tooltip.add(speciesGene.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
        }
    }
}
