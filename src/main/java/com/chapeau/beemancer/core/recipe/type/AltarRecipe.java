/**
 * ============================================================
 * [AltarRecipe.java]
 * Description: Recette d'altar (items pedestals + item centre + pollen -> item)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Ingredient          | Input items          | Items pedestals et centre      |
 * | ItemStack           | Output               | Resultat du craft              |
 * ------------------------------------------------------------
 *
 * FORMAT JSON:
 * {
 *   "type": "beemancer:altar",
 *   "center_item": { "item": "minecraft:diamond" },
 *   "pedestal_items": [ { "item": "beemancer:honeyed_iron" }, ... ],
 *   "pollen": { "beemancer:common_pollen": 4 },
 *   "result": { "id": "beemancer:bee_wand" }
 * }
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe.type;

import com.chapeau.beemancer.core.recipe.AltarRecipeInput;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeSerializers;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AltarRecipe(
    Ingredient centerItem,
    List<Ingredient> pedestalItems,
    Map<ResourceLocation, Integer> pollen,
    ItemStack result
) implements Recipe<AltarRecipeInput> {

    @Override
    public boolean matches(AltarRecipeInput input, Level level) {
        // Verifier l'item central
        if (!centerItem.test(input.centerItem())) {
            return false;
        }

        // Verifier les items des pedestals (ordre non important)
        List<ItemStack> availableItems = new ArrayList<>(input.pedestalItems());
        for (Ingredient required : pedestalItems) {
            boolean found = false;
            for (int i = 0; i < availableItems.size(); i++) {
                if (required.test(availableItems.get(i))) {
                    availableItems.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        // Note: On ne verifie PAS le pollen ici - c'est gere separement
        // pour permettre la consommation partielle
        return true;
    }

    /**
     * Verifie si le pollen disponible est suffisant.
     */
    public boolean hasEnoughPollen(Map<Item, Integer> availablePollen) {
        for (Map.Entry<ResourceLocation, Integer> entry : pollen.entrySet()) {
            Item pollenItem = BuiltInRegistries.ITEM.get(entry.getKey());
            int required = entry.getValue();
            int available = availablePollen.getOrDefault(pollenItem, 0);
            if (available < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcule combien de pollen peut etre consomme (pour consommation partielle).
     * @return Map de Item -> quantite a consommer
     */
    public Map<Item, Integer> getPollenToConsume(Map<Item, Integer> availablePollen) {
        Map<Item, Integer> toConsume = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : pollen.entrySet()) {
            Item pollenItem = BuiltInRegistries.ITEM.get(entry.getKey());
            int required = entry.getValue();
            int available = availablePollen.getOrDefault(pollenItem, 0);
            int consume = Math.min(required, available);
            if (consume > 0) {
                toConsume.put(pollenItem, consume);
            }
        }
        return toConsume;
    }

    /**
     * Retourne le pollen encore requis apres consommation partielle.
     */
    public Map<Item, Integer> getRemainingPollenRequired(Map<Item, Integer> consumed) {
        Map<Item, Integer> remaining = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : pollen.entrySet()) {
            Item pollenItem = BuiltInRegistries.ITEM.get(entry.getKey());
            int required = entry.getValue();
            int alreadyConsumed = consumed.getOrDefault(pollenItem, 0);
            int stillNeeded = required - alreadyConsumed;
            if (stillNeeded > 0) {
                remaining.put(pollenItem, stillNeeded);
            }
        }
        return remaining;
    }

    @Override
    public ItemStack assemble(AltarRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BeemancerRecipeSerializers.ALTAR.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BeemancerRecipeTypes.ALTAR.get();
    }

    public static class Serializer implements RecipeSerializer<AltarRecipe> {
        public static final MapCodec<AltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("center_item").forGetter(AltarRecipe::centerItem),
                Ingredient.CODEC.listOf().fieldOf("pedestal_items").forGetter(AltarRecipe::pedestalItems),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .optionalFieldOf("pollen", Map.of())
                    .forGetter(AltarRecipe::pollen),
                ItemStack.CODEC.fieldOf("result").forGetter(AltarRecipe::result)
            ).apply(instance, AltarRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AltarRecipe::centerItem,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), AltarRecipe::pedestalItems,
                ByteBufCodecs.map(
                    HashMap::new,
                    ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.INT
                ), AltarRecipe::pollen,
                ItemStack.STREAM_CODEC, AltarRecipe::result,
                AltarRecipe::new
            );

        @Override
        public MapCodec<AltarRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
