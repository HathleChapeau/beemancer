/**
 * ============================================================
 * [CentrifugeRecipe.java]
 * Description: Recette de centrifugation (comb -> fluide + items)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ProcessingOutput    | Outputs probabilistes| Items produits avec chance     |
 * | FluidIngredient     | Output fluide        | Miel produit                   |
 * | Ingredient          | Input item           | Rayon a centrifuger            |
 * ------------------------------------------------------------
 *
 * FORMAT JSON:
 * {
 *   "type": "beemancer:centrifuging",
 *   "ingredient": { "item": "beemancer:cupric_comb" },
 *   "results": [
 *     { "item": { "id": "beemancer:raw_copper_shard" }, "chance": 0.8 }
 *   ],
 *   "fluid_result": { "fluid": "beemancer:honey", "amount": 250 },
 *   "processing_time": 60
 * }
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe.type;

import com.chapeau.beemancer.core.recipe.BeemancerRecipeSerializers;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.FluidIngredient;
import com.chapeau.beemancer.core.recipe.ProcessingOutput;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

public record CentrifugeRecipe(
    Ingredient ingredient,
    List<ProcessingOutput> results,
    Optional<FluidIngredient> fluidResult,
    int processingTime
) implements Recipe<ProcessingRecipeInput> {

    public static final int DEFAULT_PROCESSING_TIME = 60;

    @Override
    public boolean matches(ProcessingRecipeInput input, Level level) {
        if (!input.hasItems()) return false;
        return ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) {
        // Return the first guaranteed output for display
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).getDisplayStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).getDisplayStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BeemancerRecipeSerializers.CENTRIFUGING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BeemancerRecipeTypes.CENTRIFUGING.get();
    }

    public FluidStack getFluidOutput() {
        return fluidResult.map(FluidIngredient::toFluidStack).orElse(FluidStack.EMPTY);
    }

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        public static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CentrifugeRecipe::ingredient),
                ProcessingOutput.CODEC.listOf().fieldOf("results").forGetter(CentrifugeRecipe::results),
                FluidIngredient.CODEC.optionalFieldOf("fluid_result").forGetter(CentrifugeRecipe::fluidResult),
                Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME).forGetter(CentrifugeRecipe::processingTime)
            ).apply(instance, CentrifugeRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CentrifugeRecipe::ingredient,
                ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), CentrifugeRecipe::results,
                ByteBufCodecs.optional(FluidIngredient.STREAM_CODEC), CentrifugeRecipe::fluidResult,
                ByteBufCodecs.INT, CentrifugeRecipe::processingTime,
                CentrifugeRecipe::new
            );

        @Override
        public MapCodec<CentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
