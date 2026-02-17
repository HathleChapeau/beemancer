/**
 * ============================================================
 * [MagicBeeItem.java]
 * Description: Item representant une MagicBee avec genes
 * ============================================================
 */
package com.chapeau.apica.common.item.bee;

import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.content.gene.species.DataDrivenSpeciesGene;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.chapeau.apica.core.gene.BeeGeneData;
import com.chapeau.apica.core.gene.Gene;
import com.chapeau.apica.core.gene.GeneCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MagicBeeItem extends Item {
    public MagicBeeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Retourne l'espece de l'abeille stockee dans l'item.
     * @return l'identifiant de l'espece (ex: "meadow") ou null si absent
     */
    @Nullable
    public static String getSpeciesId(ItemStack stack) {
        BeeGeneData geneData = getGeneData(stack);
        Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
        return speciesGene != null ? speciesGene.getId() : null;
    }

    // --- Factory Methods ---

    public static ItemStack createWithGenes(BeeGeneData geneData) {
        ItemStack stack = new ItemStack(com.chapeau.apica.core.registry.ApicaItems.MAGIC_BEE.get());
        saveGeneData(stack, geneData);
        return stack;
    }

    public static ItemStack captureFromEntity(MagicBeeEntity bee) {
        ItemStack stack = new ItemStack(com.chapeau.apica.core.registry.ApicaItems.MAGIC_BEE.get());
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

    // --- Foil (satiated bees glow) ---

    @Override
    public boolean isFoil(ItemStack stack) {
        return BeeInjectionHelper.isSatiated(stack);
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

            // Espece (violet si rassasiee, or sinon)
            boolean satiated = BeeInjectionHelper.isSatiated(stack);
            if (speciesGene != null) {
                ChatFormatting nameColor = satiated ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD;
                tooltip.add(Component.translatable("tooltip.apica.species")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(speciesGene.getDisplayName().copy().withStyle(nameColor)));
            }

            if (speciesData != null) {
                // Activite (Diurne/Nocturne/Insomniac) - couleurs essences
                String activityKey = switch (speciesData.dayNight) {
                    case "night" -> "tooltip.apica.activity.nocturnal";
                    case "both" -> "tooltip.apica.activity.insomniac";
                    default -> "tooltip.apica.activity.diurnal";
                };
                ChatFormatting activityColor = switch (speciesData.dayNight) {
                    case "night" -> ChatFormatting.DARK_AQUA;    // NOCTURNAL
                    case "both" -> ChatFormatting.LIGHT_PURPLE;  // INSOMNIA
                    default -> ChatFormatting.DARK_GREEN;        // DIURNAL
                };
                tooltip.add(Component.translatable("tooltip.apica.activity")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(activityKey).withStyle(activityColor)));

                // Type de fleur
                String flowerKey = "tooltip.apica.flower." + speciesData.flowerType;
                ChatFormatting flowerColor = switch (speciesData.flowerType) {
                    case "mushroom" -> ChatFormatting.GOLD;
                    case "crystal" -> ChatFormatting.LIGHT_PURPLE;
                    default -> ChatFormatting.GREEN; // flower
                };
                tooltip.add(Component.translatable("tooltip.apica.flower")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(flowerKey).withStyle(flowerColor)));

                // Climat
                String climateKey = switch (speciesData.environment) {
                    case -2 -> "tooltip.apica.climate.frozen";
                    case -1 -> "tooltip.apica.climate.cold";
                    case 1 -> "tooltip.apica.climate.warm";
                    case 2 -> "tooltip.apica.climate.hot";
                    default -> "tooltip.apica.climate.temperate";
                };
                ChatFormatting climateColor = switch (speciesData.environment) {
                    case -2 -> ChatFormatting.DARK_PURPLE;
                    case -1 -> ChatFormatting.BLUE;
                    case 1 -> ChatFormatting.YELLOW;
                    case 2 -> ChatFormatting.RED;
                    default -> ChatFormatting.GREEN;
                };
                tooltip.add(Component.translatable("tooltip.apica.climate")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(climateKey).withStyle(climateColor)));

                // Stats avec etoiles - base + bonus injection
                int dropTotal = speciesData.dropLevel + BeeInjectionHelper.getBonusLevel(stack, EssenceItem.EssenceType.DROP);
                int speedTotal = speciesData.flyingSpeedLevel + BeeInjectionHelper.getBonusLevel(stack, EssenceItem.EssenceType.SPEED);
                int foragingTotal = speciesData.foragingDurationLevel + BeeInjectionHelper.getBonusLevel(stack, EssenceItem.EssenceType.FORAGING);
                int toleranceTotal = speciesData.toleranceLevel + BeeInjectionHelper.getBonusLevel(stack, EssenceItem.EssenceType.TOLERANCE);

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.apica.drop")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(dropTotal)).withStyle(ChatFormatting.GOLD)));

                tooltip.add(Component.translatable("tooltip.apica.flyspeed")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(speedTotal)).withStyle(ChatFormatting.AQUA)));

                tooltip.add(Component.translatable("tooltip.apica.foraging")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(foragingTotal)).withStyle(ChatFormatting.GREEN)));

                tooltip.add(Component.translatable("tooltip.apica.tolerance")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatStars(toleranceTotal)).withStyle(ChatFormatting.RED)));
            }

        } else {
            // Affichage simple: nom de l'espece (violet + shimmer si rassasiee)
            boolean satiated = BeeInjectionHelper.isSatiated(stack);
            if (speciesGene != null) {
                ChatFormatting nameColor = satiated ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD;
                tooltip.add(speciesGene.getDisplayName().copy().withStyle(nameColor));
            }
            tooltip.add(Component.translatable("tooltip.apica.shift_for_details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private String formatStars(int level) {
        return "\u2605".repeat(Math.max(0, level)) + "\u2606".repeat(Math.max(0, 4 - level));
    }
}
