/**
 * ============================================================
 * [MagicBeeItem.java]
 * Description: Item representant une MagicBee avec genes
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.species.DataDrivenSpeciesGene;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
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
import org.jetbrains.annotations.Nullable;

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

    // --- Factory Methods ---

    public static ItemStack createWithGenes(BeeGeneData geneData) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        saveGeneData(stack, geneData);
        return stack;
    }

    public static ItemStack captureFromEntity(MagicBeeEntity bee) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.MAGIC_BEE.get());
        // Copy gene data
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

        BeeGeneData geneData = getGeneData(stack);
        Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            // Recuperer les donnees de l'espece
            BeeSpeciesManager.BeeSpeciesData speciesData = null;
            if (speciesGene instanceof DataDrivenSpeciesGene ddGene) {
                speciesData = BeeSpeciesManager.getSpecies(ddGene.getId());
            }

            // Espece
            if (speciesGene != null) {
                tooltip.add(Component.translatable("tooltip.beemancer.species")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(speciesGene.getDisplayName().copy().withStyle(ChatFormatting.GOLD)));
            }

            if (speciesData != null) {
                // Activite (Diurne/Nocturne/Insomniac) - couleurs essences
                String activityKey = switch (speciesData.dayNight) {
                    case "night" -> "tooltip.beemancer.activity.nocturnal";
                    case "both" -> "tooltip.beemancer.activity.insomniac";
                    default -> "tooltip.beemancer.activity.diurnal";
                };
                ChatFormatting activityColor = switch (speciesData.dayNight) {
                    case "night" -> ChatFormatting.DARK_AQUA;    // NOCTURNAL
                    case "both" -> ChatFormatting.LIGHT_PURPLE;  // INSOMNIA
                    default -> ChatFormatting.DARK_GREEN;        // DIURNAL
                };
                tooltip.add(Component.translatable("tooltip.beemancer.activity")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(activityKey).withStyle(activityColor)));

                // Type de fleur
                String flowerKey = "tooltip.beemancer.flower." + speciesData.flowerType;
                ChatFormatting flowerColor = switch (speciesData.flowerType) {
                    case "mushroom" -> ChatFormatting.GOLD;
                    case "crystal" -> ChatFormatting.LIGHT_PURPLE;
                    default -> ChatFormatting.GREEN; // flower
                };
                tooltip.add(Component.translatable("tooltip.beemancer.flower")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(flowerKey).withStyle(flowerColor)));

                // Stats avec etoiles - couleurs essences
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.beemancer.drop")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(speciesData.dropLevel)).withStyle(ChatFormatting.GOLD)));

                tooltip.add(Component.translatable("tooltip.beemancer.flyspeed")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(speciesData.flyingSpeedLevel)).withStyle(ChatFormatting.AQUA)));

                tooltip.add(Component.translatable("tooltip.beemancer.foraging")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(speciesData.foragingDurationLevel)).withStyle(ChatFormatting.GREEN)));

                tooltip.add(Component.translatable("tooltip.beemancer.tolerance")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(speciesData.toleranceLevel)).withStyle(ChatFormatting.RED)));
            }

        } else {
            // Affichage simple: juste le nom de l'espece
            if (speciesGene != null) {
                tooltip.add(speciesGene.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
            }
            tooltip.add(Component.translatable("tooltip.beemancer.shift_for_details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private String formatStars(int level) {
        return "\u2605".repeat(Math.max(0, level)) + "\u2606".repeat(Math.max(0, 4 - level));
    }
}
