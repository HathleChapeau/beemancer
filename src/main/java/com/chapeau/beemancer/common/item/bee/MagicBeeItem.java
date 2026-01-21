/**
 * ============================================================
 * [MagicBeeItem.java]
 * Description: Item représentant une MagicBee avec gènes
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

public class MagicBeeItem extends Item {
    public MagicBeeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            BlockPos spawnPos = pos.above();
            MagicBeeEntity bee = BeemancerEntities.MAGIC_BEE.get().create(level);
            
            if (bee != null) {
                bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
                
                // Load genes from item
                BeeGeneData itemGenes = getGeneData(stack);
                for (Gene gene : itemGenes.getAllGenes()) {
                    bee.setGene(gene);
                }
                
                level.addFreshEntity(bee);
                
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static ItemStack createWithGenes(BeeGeneData geneData) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        saveGeneData(stack, geneData);
        return stack;
    }

    public static ItemStack captureFromEntity(MagicBeeEntity bee) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        saveGeneData(stack, bee.getGeneData());
        return stack;
    }

    public static void saveGeneData(ItemStack stack, BeeGeneData geneData) {
        CompoundTag tag = new CompoundTag();
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            BeeGeneData geneData = getGeneData(stack);
            
            // Sort categories by display order
            List<GeneCategory> categories = GeneRegistry.getAllCategories();
            categories.sort(Comparator.comparingInt(GeneCategory::getDisplayOrder));
            
            for (GeneCategory category : categories) {
                Gene gene = geneData.getGene(category);
                if (gene != null) {
                    tooltip.add(Component.literal("")
                            .append(category.getDisplayName())
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(gene.getDisplayName().copy().withStyle(ChatFormatting.WHITE)));
                }
            }
        } else {
            tooltip.add(Component.translatable("item.beemancer.magic_bee.shift_hint")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
