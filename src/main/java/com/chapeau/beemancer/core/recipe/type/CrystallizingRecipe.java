/**
 * ============================================================
 * [CrystallizingRecipe.java]
 * Description: Recette de cristallisation (fluide -> item)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidIngredient     | Input fluide         | Miel/royal jelly a cristalliser|
 * | ItemStack           | Output               | Cristal produit                |
 * ------------------------------------------------------------
 *
 * FORMAT JSON:
 * {
 *   "type": "beemancer:crystallizing",
 *   "fluid_ingredient": { "fluid": "beemancer:honey", "amount": 500 },
 *   "result": { "item": "beemancer:honey_crystal" },
 *   "processing_time": 100
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CrystallizingRecipe(
    FluidIngredient fluidIngredient,
    ItemStack result,
    int processingTime
) implements Recipe<ProcessingRecipeInput> {

    public static final int DEFAULT_PROCESSING_TIME = 100;

    @Override
    public boolean matches(ProcessingRecipeInput input, Level level) {
        if (!input.hasFluids()) return false;
        return fluidIngredient.test(input.getFluid(0));
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
        return BeemancerRecipeSerializers.CRYSTALLIZING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BeemancerRecipeTypes.CRYSTALLIZING.get();
    }

    public static class Serializer implements RecipeSerializer<CrystallizingRecipe> {
        public static final MapCodec<CrystallizingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                FluidIngredient.CODEC.fieldOf("fluid_ingredient").forGetter(CrystallizingRecipe::fluidIngredient),
                ItemStack.CODEC.fieldOf("result").forGetter(CrystallizingRecipe::result),
                Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME).forGetter(CrystallizingRecipe::processingTime)
            ).apply(instance, CrystallizingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                FluidIngredient.STREAM_CODEC, CrystallizingRecipe::fluidIngredient,
                ItemStack.STREAM_CODEC, CrystallizingRecipe::result,
                ByteBufCodecs.INT, CrystallizingRecipe::processingTime,
                CrystallizingRecipe::new
            );

        @Override
        public MapCodec<CrystallizingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
