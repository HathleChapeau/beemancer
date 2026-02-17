/**
 * ============================================================
 * [SpeciesEssenceItem.java]
 * Description: Essence d'espece avec metadata species dans CustomData
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeSpeciesManager   | Nom d'espece         | Tooltip display name           |
 * | DataComponents      | Stockage metadata    | CustomData avec species id     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaItems.java (enregistrement)
 * - ExtractorHeartBlockEntity.java (drop possible)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.essence;

import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item unique representant l'essence d'une espece d'abeille.
 * L'espece est stockee en metadata (CustomData), permettant le stacking par espece identique.
 * Pour le moment, cet item n'a aucun effet dans l'injecteur d'essence.
 */
public class SpeciesEssenceItem extends Item {

    private static final String SPECIES_KEY = "species";

    public SpeciesEssenceItem(Properties properties) {
        super(properties);
    }

    // ========== FACTORY ==========

    /**
     * Cree un ItemStack d'essence pour l'espece donnee.
     */
    public static ItemStack createForSpecies(String speciesId) {
        ItemStack stack = new ItemStack(ApicaItems.SPECIES_ESSENCE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(SPECIES_KEY, speciesId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    // ========== GETTERS ==========

    /**
     * Retourne l'identifiant de l'espece stockee dans l'item.
     */
    @Nullable
    public static String getSpeciesId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        return tag.contains(SPECIES_KEY) ? tag.getString(SPECIES_KEY) : null;
    }

    // ========== TOOLTIP ==========

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String speciesId = getSpeciesId(stack);
        if (speciesId != null) {
            String displayName = speciesId.substring(0, 1).toUpperCase() + speciesId.substring(1);
            BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
            if (data != null) {
                String tier = data.tier;
                tooltip.add(Component.literal(displayName)
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" (Tier " + tier + ")")
                                .withStyle(ChatFormatting.GRAY)));
            } else {
                tooltip.add(Component.literal(displayName).withStyle(ChatFormatting.GOLD));
            }
        }

        tooltip.add(Component.translatable("item.apica.species_essence.desc")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
