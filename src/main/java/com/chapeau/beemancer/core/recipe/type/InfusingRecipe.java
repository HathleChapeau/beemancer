/**
 * ============================================================
 * [InfusingRecipe.java]
 * Description: Recette d'infusion (item + fluide -> item)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidIngredient     | Input fluide         | Miel requis                    |
 * | Ingredient          | Input item           | Bois a infuser                 |
 * | ItemStack           | Output               | Bois emmiele                   |
 * ------------------------------------------------------------
 *
 * FORMAT JSON:
 * {
 *   "type": "beemancer:infusing",
 *   "ingredient": { "tag": "minecraft:logs" },
 *   "fluid_ingredient": { "fluid": "beemancer:honey", "amount": 250 },
 *   "result": { "item": "beemancer:honeyed_wood" },
 *   "processing_time": 200
 * }
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe.type;

import com.chapeau.beemancer.core.recipe.BeemancerRecipeSerializers;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.FluidIngredient;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record InfusingRecipe(
    Ingredient ingredient,
    FluidIngredient fluidIngredient,
    ItemStack result,
    int processingTime
) implements Recipe<ProcessingRecipeInput> {

    public static final int DEFAULT_PROCESSING_TIME = 200;

    @Override
    public boolean matches(ProcessingRecipeInput input, Level level) {
        if (!input.hasItems() || !input.hasFluids()) return false;
        return ingredient.test(input.getItem(0)) && fluidIngredient.test(input.getFluid(0));
    }

    @Override
    public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) {
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
        return BeemancerRecipeSerializers.INFUSING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BeemancerRecipeTypes.INFUSING.get();
    }

    public static class Serializer implements RecipeSerializer<InfusingRecipe> {
        public static final MapCodec<InfusingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(InfusingRecipe::ingredient),
                FluidIngredient.CODEC.fieldOf("fluid_ingredient").forGetter(InfusingRecipe::fluidIngredient),
                ItemStack.CODEC.fieldOf("result").forGetter(InfusingRecipe::result),
                Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME).forGetter(InfusingRecipe::processingTime)
            ).apply(instance, InfusingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, InfusingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, InfusingRecipe::ingredient,
                FluidIngredient.STREAM_CODEC, InfusingRecipe::fluidIngredient,
                ItemStack.STREAM_CODEC, InfusingRecipe::result,
                ByteBufCodecs.INT, InfusingRecipe::processingTime,
                InfusingRecipe::new
            );

        @Override
        public MapCodec<InfusingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InfusingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
