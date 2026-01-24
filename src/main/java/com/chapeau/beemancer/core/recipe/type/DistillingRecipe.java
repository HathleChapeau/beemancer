/**
 * ============================================================
 * [DistillingRecipe.java]
 * Description: Recette de distillation (2 fluides -> 1 fluide)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidIngredient     | Inputs fluides       | Honey + Royal Jelly            |
 * | FluidStack          | Output fluide        | Nectar produit                 |
 * ------------------------------------------------------------
 *
 * FORMAT JSON:
 * {
 *   "type": "beemancer:distilling",
 *   "fluid_ingredients": [
 *     { "fluid": "beemancer:honey", "amount": 500 },
 *     { "fluid": "beemancer:royal_jelly", "amount": 250 }
 *   ],
 *   "fluid_result": { "fluid": "beemancer:nectar", "amount": 500 },
 *   "processing_time": 80
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record DistillingRecipe(
    List<FluidIngredient> fluidIngredients,
    FluidIngredient fluidResult,
    int processingTime
) implements Recipe<ProcessingRecipeInput> {

    public static final int DEFAULT_PROCESSING_TIME = 80;

    @Override
    public boolean matches(ProcessingRecipeInput input, Level level) {
        if (input.fluidSize() < fluidIngredients.size()) return false;

        // Check each fluid ingredient matches
        for (int i = 0; i < fluidIngredients.size(); i++) {
            if (!fluidIngredients.get(i).test(input.getFluid(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public FluidStack getFluidOutput() {
        return fluidResult.toFluidStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BeemancerRecipeSerializers.DISTILLING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return BeemancerRecipeTypes.DISTILLING.get();
    }

    public static class Serializer implements RecipeSerializer<DistillingRecipe> {
        public static final MapCodec<DistillingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                FluidIngredient.CODEC.listOf().fieldOf("fluid_ingredients").forGetter(DistillingRecipe::fluidIngredients),
                FluidIngredient.CODEC.fieldOf("fluid_result").forGetter(DistillingRecipe::fluidResult),
                Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME).forGetter(DistillingRecipe::processingTime)
            ).apply(instance, DistillingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                FluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), DistillingRecipe::fluidIngredients,
                FluidIngredient.STREAM_CODEC, DistillingRecipe::fluidResult,
                ByteBufCodecs.INT, DistillingRecipe::processingTime,
                DistillingRecipe::new
            );

        @Override
        public MapCodec<DistillingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
