/**
 * ============================================================
 * [InfusingCategory.java]
 * Description: Categorie JEI pour les recettes d'infusion (item + miel -> honeyed item)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | InfusingRecipe      | Type de recette      | Donnees a afficher             |
 * | ApicaBlocks         | Icone categorie      | Infuser block                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.recipe.type.InfusingRecipe;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class InfusingCategory implements IRecipeCategory<InfusingRecipe> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public InfusingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.INFUSER.get().asItem().getDefaultInstance());
        this.title = Component.translatable("gui.apica.jei.infusing");
    }

    @Override
    public RecipeType<InfusingRecipe> getRecipeType() {
        return ApicaJeiRecipeTypes.INFUSING;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusingRecipe recipe, IFocusGroup focuses) {
        // Input item slot (left)
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 12)
                .addIngredients(recipe.ingredient());

        // Fluid input (center-left) - honey bucket representation
        FluidStack fluidStack = new FluidStack(
                ApicaFluids.HONEY_SOURCE.get(),
                recipe.fluidIngredient().amount()
        );
        builder.addSlot(RecipeIngredientRole.CATALYST, 35, 12)
                .addFluidStack(fluidStack.getFluid(), fluidStack.getAmount())
                .setFluidRenderer(fluidStack.getAmount(), false, 16, 16);

        // Output item slot (right)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 12)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(InfusingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw slot backgrounds (using vanilla inventory slot style)
        drawSlot(guiGraphics, 4, 11);
        drawSlot(guiGraphics, 34, 11);
        drawSlot(guiGraphics, 98, 11);

        // Draw arrow
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        guiGraphics.drawString(mc.font, "+", 25, 15, 0xFF8B6914, false);
        guiGraphics.drawString(mc.font, "\u2192", 60, 15, 0xFF8B6914, false);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        // Vanilla-style slot (18x18)
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
