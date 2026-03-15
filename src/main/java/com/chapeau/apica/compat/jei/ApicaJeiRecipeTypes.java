/**
 * ============================================================
 * [ApicaJeiRecipeTypes.java]
 * Description: Types de recettes JEI pour Apica
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI RecipeType      | Identification       | Lien categories/recettes       |
 * | Apica recipes       | Types source         | InfusingRecipe, etc.           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement)
 * - Categories JEI (identification)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei;

import com.chapeau.apica.Apica;
import com.chapeau.apica.compat.jei.category.MultiblockInfo;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.chapeau.apica.core.recipe.type.CentrifugeRecipe;
import com.chapeau.apica.core.recipe.type.CrystallizingRecipe;
import com.chapeau.apica.core.recipe.type.DistillingRecipe;
import com.chapeau.apica.core.recipe.type.InfusingRecipe;
import mezz.jei.api.recipe.RecipeType;

public final class ApicaJeiRecipeTypes {

    public static final RecipeType<InfusingRecipe> INFUSING = RecipeType.create(
            Apica.MOD_ID, "infusing", InfusingRecipe.class);

    public static final RecipeType<CentrifugeRecipe> CENTRIFUGE = RecipeType.create(
            Apica.MOD_ID, "centrifuge", CentrifugeRecipe.class);

    public static final RecipeType<AltarRecipe> ALTAR = RecipeType.create(
            Apica.MOD_ID, "altar", AltarRecipe.class);

    public static final RecipeType<CrystallizingRecipe> CRYSTALLIZER = RecipeType.create(
            Apica.MOD_ID, "crystallizer", CrystallizingRecipe.class);

    public static final RecipeType<DistillingRecipe> ALEMBIC = RecipeType.create(
            Apica.MOD_ID, "alembic", DistillingRecipe.class);

    public static final RecipeType<MultiblockInfo> MULTIBLOCK = RecipeType.create(
            Apica.MOD_ID, "multiblock", MultiblockInfo.class);

    private ApicaJeiRecipeTypes() {}
}
