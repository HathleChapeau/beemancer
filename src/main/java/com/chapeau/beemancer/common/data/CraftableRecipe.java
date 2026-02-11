/**
 * ============================================================
 * [CraftableRecipe.java]
 * Description: Recette craftable synchronisee depuis la bibliotheque du crafter
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CraftingPaperData   | Source des recettes  | Conversion depuis paper data   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageItemAggregator.java (construction de la liste)
 * - StorageCraftablesSyncPacket.java (sync reseau)
 * - StorageTerminalMenu.java (cache client)
 * - TerminalRequestPopup.java (calcul materiaux)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente une recette craftable depuis la bibliotheque du crafter.
 * Les ingredients sont compactes (pas de slots vides, counts agreges).
 */
public record CraftableRecipe(
        ItemStack result,
        List<ItemStack> ingredients
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftableRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, CraftableRecipe::result,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), CraftableRecipe::ingredients,
                    CraftableRecipe::new
            );

    /**
     * Convertit un CraftingPaperData en CraftableRecipe.
     * Compacte les ingredients: fusionne les doublons et retire les vides.
     */
    public static CraftableRecipe fromCraftingPaperData(CraftingPaperData data) {
        List<ItemStack> compacted = new ArrayList<>();
        for (ItemStack ingredient : data.ingredients()) {
            if (ingredient.isEmpty()) continue;
            boolean merged = false;
            for (ItemStack existing : compacted) {
                if (ItemStack.isSameItemSameComponents(existing, ingredient)) {
                    existing.grow(ingredient.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                compacted.add(ingredient.copy());
            }
        }
        return new CraftableRecipe(data.result().copy(), compacted);
    }
}
