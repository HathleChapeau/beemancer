/**
 * ============================================================
 * [ApicaJeiPlugin.java]
 * Description: Plugin principal JEI pour Apica
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Integration recettes | IModPlugin, categories         |
 * | ApicaRecipeTypes    | Types de recettes    | Enregistrement categories      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - JEI (decouverte automatique via @JeiPlugin)
 *
 * NOTE: Ce fichier n'est charge que si JEI est present.
 * Le mod fonctionne normalement sans JEI.
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei;

import com.chapeau.apica.Apica;
import com.chapeau.apica.compat.jei.category.AltarCategory;
import com.chapeau.apica.compat.jei.category.CentrifugeCategory;
import com.chapeau.apica.compat.jei.category.InfusingCategory;
import com.chapeau.apica.core.recipe.ApicaRecipeTypes;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.chapeau.apica.core.recipe.type.CentrifugeRecipe;
import com.chapeau.apica.core.recipe.type.InfusingRecipe;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class ApicaJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new InfusingCategory(guiHelper),
                new CentrifugeCategory(guiHelper),
                new AltarCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RecipeManager recipeManager = mc.level.getRecipeManager();

        // Infusing recipes
        List<InfusingRecipe> infusingRecipes = recipeManager
                .getAllRecipesFor(ApicaRecipeTypes.INFUSING.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ApicaJeiRecipeTypes.INFUSING, infusingRecipes);

        // Centrifuge recipes
        List<CentrifugeRecipe> centrifugeRecipes = recipeManager
                .getAllRecipesFor(ApicaRecipeTypes.CENTRIFUGING.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ApicaJeiRecipeTypes.CENTRIFUGE, centrifugeRecipes);

        // Altar recipes
        List<AltarRecipe> altarRecipes = recipeManager
                .getAllRecipesFor(ApicaRecipeTypes.ALTAR.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ApicaJeiRecipeTypes.ALTAR, altarRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Infuser catalyst
        registration.addRecipeCatalyst(
                ApicaBlocks.INFUSER.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.INFUSING
        );

        // Centrifuge catalysts
        registration.addRecipeCatalyst(
                ApicaBlocks.MANUAL_CENTRIFUGE.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.CENTRIFUGE
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.POWERED_CENTRIFUGE.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.CENTRIFUGE
        );

        // Altar catalyst
        registration.addRecipeCatalyst(
                ApicaBlocks.ALTAR_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.ALTAR
        );
        registration.addRecipeCatalyst(
                ApicaItems.HONEY_ARTIFACT_CORE.get().getDefaultInstance(),
                ApicaJeiRecipeTypes.ALTAR
        );
    }
}
