/**
 * ============================================================
 * [ApicaRecipeTypes.java]
 * Description: Registre des types de recettes Apica
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DeferredRegister    | Enregistrement       | Registre NeoForge              |
 * | RecipeType          | Type de recette      | Definition types               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Apica.java (enregistrement)
 * - AbstractProcessingBlockEntity (lookup recettes)
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
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Apica.MOD_ID);

    public static final Supplier<RecipeType<CentrifugeRecipe>> CENTRIFUGING =
            RECIPE_TYPES.register("centrifuging", () -> RecipeType.simple(
                    Apica.modLoc("centrifuging")));

    public static final Supplier<RecipeType<InfusingRecipe>> INFUSING =
            RECIPE_TYPES.register("infusing", () -> RecipeType.simple(
                    Apica.modLoc("infusing")));

    public static final Supplier<RecipeType<CrystallizingRecipe>> CRYSTALLIZING =
            RECIPE_TYPES.register("crystallizing", () -> RecipeType.simple(
                    Apica.modLoc("crystallizing")));

    public static final Supplier<RecipeType<DistillingRecipe>> DISTILLING =
            RECIPE_TYPES.register("distilling", () -> RecipeType.simple(
                    Apica.modLoc("distilling")));

    public static final Supplier<RecipeType<AltarRecipe>> ALTAR =
            RECIPE_TYPES.register("altar", () -> RecipeType.simple(
                    Apica.modLoc("altar")));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
