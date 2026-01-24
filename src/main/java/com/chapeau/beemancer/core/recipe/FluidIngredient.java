/**
 * ============================================================
 * [FluidIngredient.java]
 * Description: Ingredient fluide pour les recettes de processing
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidStack          | Comparaison fluides  | Test du fluide input           |
 * | Fluid               | Type de fluide       | Stockage reference             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ProcessingRecipe (classe abstraite)
 * - InfusingRecipe
 * - CrystallizingRecipe
 * - DistillingRecipe
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidIngredient(Fluid fluid, int amount) {

    public static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidIngredient::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidIngredient::amount)
        ).apply(instance, FluidIngredient::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.registry(BuiltInRegistries.FLUID.key()), FluidIngredient::fluid,
            ByteBufCodecs.INT, FluidIngredient::amount,
            FluidIngredient::new
        );

    /**
     * Test if the given FluidStack matches this ingredient
     * @param stack FluidStack to test
     * @return true if the fluid type matches and amount is sufficient
     */
    public boolean test(FluidStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getFluid() == fluid && stack.getAmount() >= amount;
    }

    /**
     * Create a FluidStack from this ingredient for display or output purposes
     */
    public FluidStack toFluidStack() {
        return new FluidStack(fluid, amount);
    }
}
