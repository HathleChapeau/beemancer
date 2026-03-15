/**
 * ============================================================
 * [AlembicCategory.java]
 * Description: Categorie JEI pour les recettes de distillation (fluides -> fluide)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | DistillingRecipe    | Type de recette      | Donnees a afficher             |
 * | ApicaBlocks         | Icone categorie      | Alembic heart block            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.recipe.FluidIngredient;
import com.chapeau.apica.core.recipe.type.DistillingRecipe;
import com.chapeau.apica.core.registry.ApicaBlocks;
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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AlembicCategory implements IRecipeCategory<DistillingRecipe> {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 40;

    // Layout constants for equal spacing
    private static final int MARGIN = 6;
    private static final int GAP = 8;
    private static final int SLOT_SIZE = 18;
    private static final int ARROW_WIDTH = 24;

    // Positions: fluid1(18) + gap(8) + "+"(6) + gap(8) + fluid2(18) + gap(8) + arrow(24) + gap(8) + output(18)
    private static final int FLUID1_X = MARGIN;
    private static final int FLUID2_X = FLUID1_X + SLOT_SIZE + GAP + 6 + GAP;
    private static final int ARROW_X = FLUID2_X + SLOT_SIZE + GAP;
    private static final int OUTPUT_X = ARROW_X + ARROW_WIDTH + GAP;
    private static final int SLOT_Y = 12;

    // Vanilla furnace GUI texture (256x256)
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public AlembicCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.ALEMBIC_HEART.get().asItem().getDefaultInstance());
        // Arrow from furnace GUI at position (79, 34), size 24x17
        this.arrow = guiHelper.createDrawable(FURNACE_TEXTURE, 79, 34, 24, 17);
        this.title = Component.translatable("gui.apica.jei.alembic");
    }

    @Override
    public RecipeType<DistillingRecipe> getRecipeType() {
        return ApicaJeiRecipeTypes.ALEMBIC;
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
    public void setRecipe(IRecipeLayoutBuilder builder, DistillingRecipe recipe, IFocusGroup focuses) {
        List<FluidIngredient> inputs = recipe.fluidIngredients();

        // First fluid input
        if (inputs.size() > 0) {
            FluidStack fluid1 = inputs.get(0).toFluidStack();
            builder.addSlot(RecipeIngredientRole.INPUT, FLUID1_X, SLOT_Y)
                    .addFluidStack(fluid1.getFluid(), fluid1.getAmount())
                    .setFluidRenderer(fluid1.getAmount(), false, 16, 16);
        }

        // Second fluid input
        if (inputs.size() > 1) {
            FluidStack fluid2 = inputs.get(1).toFluidStack();
            builder.addSlot(RecipeIngredientRole.INPUT, FLUID2_X, SLOT_Y)
                    .addFluidStack(fluid2.getFluid(), fluid2.getAmount())
                    .setFluidRenderer(fluid2.getAmount(), false, 16, 16);
        }

        // Fluid output
        FluidStack output = recipe.getFluidOutput();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addFluidStack(output.getFluid(), output.getAmount())
                .setFluidRenderer(output.getAmount(), false, 16, 16);
    }

    @Override
    public void draw(DistillingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        // Draw fluid input slots
        drawSlot(guiGraphics, FLUID1_X - 1, SLOT_Y - 1);
        if (recipe.fluidIngredients().size() > 1) {
            drawSlot(guiGraphics, FLUID2_X - 1, SLOT_Y - 1);
        }

        // Draw output slot
        drawSlot(guiGraphics, OUTPUT_X - 1, SLOT_Y - 1);

        // Draw "+" between input fluids
        int plusX = FLUID1_X + SLOT_SIZE + GAP;
        guiGraphics.drawString(mc.font, "+", plusX, SLOT_Y + 4, 0x404040, false);

        // Draw arrow
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        // Vanilla-style slot (18x18)
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
