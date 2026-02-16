/**
 * ============================================================
 * [ApicaRecipeSerializers.java]
 * Description: Registre des serializers de recettes Apica
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
 * - Apica.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.recipe;

import com.chapeau.apica.Apica;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.chapeau.apica.core.recipe.type.CentrifugeRecipe;
import com.chapeau.apica.core.recipe.type.CrystallizingRecipe;
import com.chapeau.apica.core.recipe.type.DistillingRecipe;
import com.chapeau.apica.core.recipe.type.InfusingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Apica.MOD_ID);

    public static final Supplier<RecipeSerializer<CentrifugeRecipe>> CENTRIFUGING =
            RECIPE_SERIALIZERS.register("centrifuging", CentrifugeRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<InfusingRecipe>> INFUSING =
            RECIPE_SERIALIZERS.register("infusing", InfusingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<CrystallizingRecipe>> CRYSTALLIZING =
            RECIPE_SERIALIZERS.register("crystallizing", CrystallizingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<DistillingRecipe>> DISTILLING =
            RECIPE_SERIALIZERS.register("distilling", DistillingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<AltarRecipe>> ALTAR =
            RECIPE_SERIALIZERS.register("altar", AltarRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
