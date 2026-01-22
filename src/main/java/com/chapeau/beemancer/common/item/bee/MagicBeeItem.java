/**
 * ============================================================
 * [MagicBeeItem.java]
 * Description: Item représentant une MagicBee avec gènes et lifetime
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
                
                // Load genes from item (includes lifetime)
                BeeGeneData itemGenes = getGeneData(stack);
                bee.getGeneData().copyFrom(itemGenes);
                for (Gene gene : itemGenes.getAllGenes()) {
                    bee.setGene(gene);
                }
                
                // Load hive assignment
                BlockPos hivePos = getAssignedHivePos(stack);
                int slot = getAssignedSlot(stack);
                if (hivePos != null && slot >= 0) {
                    bee.setAssignedHive(hivePos, slot);
                }
                
                // Load stored health
                float storedHealth = getStoredHealth(stack, bee.getMaxHealth());
                bee.setStoredHealth(storedHealth);
                
                level.addFreshEntity(bee);
                
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // --- Durability Bar (Lifetime Display) ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        BeeGeneData data = getGeneData(stack);
        return data.getMaxLifetime() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BeeGeneData data = getGeneData(stack);
        float ratio = data.getLifetimeRatio();
        return Math.round(13.0F * ratio);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        BeeGeneData data = getGeneData(stack);
        float ratio = data.getLifetimeRatio();
        // Green (1.0) to Red (0.0) gradient
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }

    // --- Factory Methods ---

    public static ItemStack createWithGenes(BeeGeneData geneData) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        saveGeneData(stack, geneData);
        return stack;
    }

    public static ItemStack captureFromEntity(MagicBeeEntity bee) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        // Copy gene data (includes lifetime)
        saveGeneData(stack, bee.getGeneData());
        // Copy hive assignment
        if (bee.hasAssignedHive()) {
            setAssignedHive(stack, bee.getAssignedHivePos(), bee.getAssignedSlot());
        }
        // Copy current health
        setStoredHealth(stack, bee.getHealth());
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

    // --- Hive Assignment ---

    public static void setAssignedHive(ItemStack stack, @Nullable BlockPos hivePos, int slot) {
        CompoundTag tag = getOrCreateTag(stack);
        if (hivePos != null && slot >= 0) {
            tag.putInt("HiveX", hivePos.getX());
            tag.putInt("HiveY", hivePos.getY());
            tag.putInt("HiveZ", hivePos.getZ());
            tag.putInt("HiveSlot", slot);
        } else {
            tag.remove("HiveX");
            tag.remove("HiveY");
            tag.remove("HiveZ");
            tag.remove("HiveSlot");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    public static BlockPos getAssignedHivePos(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("HiveX")) {
                return new BlockPos(tag.getInt("HiveX"), tag.getInt("HiveY"), tag.getInt("HiveZ"));
            }
        }
        return null;
    }

    public static int getAssignedSlot(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getInt("HiveSlot");
        }
        return -1;
    }

    public static void clearAssignedHive(ItemStack stack) {
        setAssignedHive(stack, null, -1);
    }

    public static boolean hasAssignedHive(ItemStack stack) {
        return getAssignedHivePos(stack) != null && getAssignedSlot(stack) >= 0;
    }

    // --- Stored Health ---

    public static void setStoredHealth(ItemStack stack, float health) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putFloat("StoredHealth", health);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static float getStoredHealth(ItemStack stack, float defaultHealth) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("StoredHealth")) {
                return tag.getFloat("StoredHealth");
            }
        }
        return defaultHealth;
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
            
            // Show lifetime
            int remaining = geneData.getRemainingLifetime() / 20; // seconds
            int max = geneData.getMaxLifetime() / 20;
            tooltip.add(Component.literal("")
                    .append(Component.translatable("gene.category.beemancer.lifetime").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(formatTime(remaining) + " / " + formatTime(max)).withStyle(ChatFormatting.WHITE)));
            
            // Show hive assignment if any
            if (hasAssignedHive(stack)) {
                BlockPos hivePos = getAssignedHivePos(stack);
                tooltip.add(Component.literal("")
                        .append(Component.translatable("item.beemancer.magic_bee.assigned_hive").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(": " + hivePos.toShortString()).withStyle(ChatFormatting.WHITE)));
            }
        } else {
            tooltip.add(Component.translatable("item.beemancer.magic_bee.shift_hint")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
