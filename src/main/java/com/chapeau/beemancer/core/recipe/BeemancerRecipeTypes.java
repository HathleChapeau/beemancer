/**
 * ============================================================
 * [BeemancerRecipeTypes.java]
 * Description: Registre des types de recettes Beemancer
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
 * - Beemancer.java (enregistrement)
 * - AbstractProcessingBlockEntity (lookup recettes)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.recipe;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.recipe.type.AltarRecipe;
import com.chapeau.beemancer.core.recipe.type.CentrifugeRecipe;
import com.chapeau.beemancer.core.recipe.type.CrystallizingRecipe;
import com.chapeau.beemancer.core.recipe.type.DistillingRecipe;
import com.chapeau.beemancer.core.recipe.type.InfusingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Beemancer.MOD_ID);

    public static final Supplier<RecipeType<CentrifugeRecipe>> CENTRIFUGING =
            RECIPE_TYPES.register("centrifuging", () -> RecipeType.simple(
                    Beemancer.modLoc("centrifuging")));

    public static final Supplier<RecipeType<InfusingRecipe>> INFUSING =
            RECIPE_TYPES.register("infusing", () -> RecipeType.simple(
                    Beemancer.modLoc("infusing")));

    public static final Supplier<RecipeType<CrystallizingRecipe>> CRYSTALLIZING =
            RECIPE_TYPES.register("crystallizing", () -> RecipeType.simple(
                    Beemancer.modLoc("crystallizing")));

    public static final Supplier<RecipeType<DistillingRecipe>> DISTILLING =
            RECIPE_TYPES.register("distilling", () -> RecipeType.simple(
                    Beemancer.modLoc("distilling")));

    public static final Supplier<RecipeType<AltarRecipe>> ALTAR =
            RECIPE_TYPES.register("altar", () -> RecipeType.simple(
                    Beemancer.modLoc("altar")));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
