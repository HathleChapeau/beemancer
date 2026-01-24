/**
 * ============================================================
 * [ProcessingOutput.java]
 * Description: Output avec probabilite pour les recettes de processing
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Output de la recette | Item produit                   |
 * | RandomSource        | Generation probas    | Calcul si output est produit   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ProcessingRecipe (classe abstraite)
 * - CentrifugeRecipe
 * - Toutes les recettes de processing
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record ProcessingOutput(ItemStack stack, float chance) {

    public static final Codec<ProcessingOutput> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(ProcessingOutput::stack),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(ProcessingOutput::chance)
        ).apply(instance, ProcessingOutput::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> STREAM_CODEC =
        StreamCodec.composite(
            ItemStack.STREAM_CODEC, ProcessingOutput::stack,
            ByteBufCodecs.FLOAT, ProcessingOutput::chance,
            ProcessingOutput::new
        );

    public ProcessingOutput(ItemStack stack) {
        this(stack, 1.0f);
    }

    /**
     * Roll the chance and return the output if successful.
     * @param random RandomSource for probability calculation
     * @return Optional containing the ItemStack if rolled successfully, empty otherwise
     */
    public Optional<ItemStack> roll(RandomSource random) {
        if (chance >= 1.0f || random.nextFloat() < chance) {
            return Optional.of(stack.copy());
        }
        return Optional.empty();
    }

    /**
     * Get the output without rolling chance (for display purposes)
     */
    public ItemStack getDisplayStack() {
        return stack.copy();
    }

    /**
     * Check if this output is guaranteed (100% chance)
     */
    public boolean isGuaranteed() {
        return chance >= 1.0f;
    }
}
