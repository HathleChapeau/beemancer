/**
 * ============================================================
 * [BeemancerRecipeSerializers.java]
 * Description: Registre des serializers de recettes Beemancer
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DeferredRegister    | Enregistrement       | Registre NeoForge              |
 * | RecipeSerializer    | Serializer recette   | Lecture/ecriture JSON          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Beemancer.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.recipe.type.CentrifugeRecipe;
import com.chapeau.beemancer.core.recipe.type.CrystallizingRecipe;
import com.chapeau.beemancer.core.recipe.type.DistillingRecipe;
import com.chapeau.beemancer.core.recipe.type.InfusingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Beemancer.MOD_ID);

    public static final Supplier<RecipeSerializer<CentrifugeRecipe>> CENTRIFUGING =
            RECIPE_SERIALIZERS.register("centrifuging", CentrifugeRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<InfusingRecipe>> INFUSING =
            RECIPE_SERIALIZERS.register("infusing", InfusingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<CrystallizingRecipe>> CRYSTALLIZING =
            RECIPE_SERIALIZERS.register("crystallizing", CrystallizingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<DistillingRecipe>> DISTILLING =
            RECIPE_SERIALIZERS.register("distilling", DistillingRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
