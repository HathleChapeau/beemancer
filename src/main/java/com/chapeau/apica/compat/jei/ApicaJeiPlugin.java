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
import com.chapeau.apica.compat.jei.category.AlembicCategory;
import com.chapeau.apica.compat.jei.category.AltarCategory;
import com.chapeau.apica.compat.jei.category.CentrifugeCategory;
import com.chapeau.apica.compat.jei.category.CrystallizerCategory;
import com.chapeau.apica.compat.jei.category.InfusingCategory;
import com.chapeau.apica.compat.jei.category.MultiblockCategory;
import com.chapeau.apica.compat.jei.category.MultiblockInfo;
import com.chapeau.apica.core.multiblock.MultiblockPatterns;
import com.chapeau.apica.core.recipe.ApicaRecipeTypes;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.chapeau.apica.core.recipe.type.CentrifugeRecipe;
import com.chapeau.apica.core.recipe.type.CrystallizingRecipe;
import com.chapeau.apica.core.recipe.type.DistillingRecipe;
import com.chapeau.apica.core.recipe.type.InfusingRecipe;
import com.chapeau.apica.core.registry.ApicaBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
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
                new CrystallizerCategory(guiHelper),
                new AlembicCategory(guiHelper),
                new AltarCategory(guiHelper),
                new MultiblockCategory(guiHelper)
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

        // Crystallizer recipes
        List<CrystallizingRecipe> crystallizerRecipes = recipeManager
                .getAllRecipesFor(ApicaRecipeTypes.CRYSTALLIZING.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ApicaJeiRecipeTypes.CRYSTALLIZER, crystallizerRecipes);

        // Alembic (distilling) recipes
        List<DistillingRecipe> alembicRecipes = recipeManager
                .getAllRecipesFor(ApicaRecipeTypes.DISTILLING.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ApicaJeiRecipeTypes.ALEMBIC, alembicRecipes);

        // Multiblock info
        List<MultiblockInfo> multiblocks = createMultiblockInfos();
        registration.addRecipes(ApicaJeiRecipeTypes.MULTIBLOCK, multiblocks);
    }

    private List<MultiblockInfo> createMultiblockInfos() {
        List<MultiblockInfo> list = new ArrayList<>();

        // Centrifuge Multiblock
        list.add(MultiblockInfo.create(
                "centrifuge",
                "gui.apica.jei.multiblock.centrifuge",
                ApicaBlocks.CENTRIFUGE_HEART.get(),
                MultiblockPatterns.CENTRIFUGE_MULTIBLOCK
        ));

        // Altar
        list.add(MultiblockInfo.create(
                "altar",
                "gui.apica.jei.multiblock.altar",
                ApicaBlocks.ALTAR_HEART.get(),
                MultiblockPatterns.HONEY_ALTAR
        ));

        // Storage Controller
        list.add(MultiblockInfo.create(
                "storage_controller",
                "gui.apica.jei.multiblock.storage_controller",
                ApicaBlocks.STORAGE_CONTROLLER.get(),
                MultiblockPatterns.STORAGE_CONTROLLER
        ));

        // Alembic
        list.add(MultiblockInfo.create(
                "alembic",
                "gui.apica.jei.multiblock.alembic",
                ApicaBlocks.ALEMBIC_HEART.get(),
                MultiblockPatterns.ALEMBIC_MULTIBLOCK
        ));

        // Essence Extractor
        list.add(MultiblockInfo.create(
                "extractor",
                "gui.apica.jei.multiblock.extractor",
                ApicaBlocks.EXTRACTOR_HEART.get(),
                MultiblockPatterns.ESSENCE_EXTRACTOR
        ));

        return list;
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

        // Crystallizer catalyst
        registration.addRecipeCatalyst(
                ApicaBlocks.CRYSTALLIZER.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.CRYSTALLIZER
        );

        // Alembic catalyst
        registration.addRecipeCatalyst(
                ApicaBlocks.ALEMBIC_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.ALEMBIC
        );

        // Multiblock catalysts - tous les blocs qui font partie des multiblocs
        // Centrifuge
        registration.addRecipeCatalyst(
                ApicaBlocks.CENTRIFUGE_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.HONEY_RESERVOIR.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.IRON_FOUNDATION.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );


        // Altar
        registration.addRecipeCatalyst(
                ApicaBlocks.ALTAR_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.HONEY_PEDESTAL.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.HONEY_CRYSTAL_CONDUIT.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );

        // Storage Controller
        registration.addRecipeCatalyst(
                ApicaBlocks.STORAGE_CONTROLLER.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.STORAGE_TERMINAL.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.CONTROLLED_HIVE.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );

        // Alembic
        registration.addRecipeCatalyst(
                ApicaBlocks.ALEMBIC_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.ROYAL_GOLD_BLOCK.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
        registration.addRecipeCatalyst(
                ApicaBlocks.HONEYED_GLASS.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );

        // Extractor
        registration.addRecipeCatalyst(
                ApicaBlocks.EXTRACTOR_HEART.get().asItem().getDefaultInstance(),
                ApicaJeiRecipeTypes.MULTIBLOCK
        );
    }
}
