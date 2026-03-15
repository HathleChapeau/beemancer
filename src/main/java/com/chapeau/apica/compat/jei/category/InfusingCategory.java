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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class InfusingCategory implements IRecipeCategory<InfusingRecipe> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 41;

    // Layout: equal spacing between elements
    // Elements: Input(18) + gap(12) + Fluid(18) + gap(8) + Arrow(24) + gap(8) + Output(18) = 106
    // Margins: (120 - 106) / 2 = 7, +1 for input slots
    private static final int MARGIN = 8;  // +1px for input slots shift
    private static final int GAP = 8;
    private static final int INPUT_GAP = 12;  // Larger gap between input and fluid
    private static final int SLOT_SIZE = 18;
    private static final int ARROW_WIDTH = 24;

    private static final int INPUT_X = MARGIN;
    private static final int FLUID_X = INPUT_X + SLOT_SIZE + INPUT_GAP;
    private static final int ARROW_X = FLUID_X + SLOT_SIZE + GAP;
    private static final int OUTPUT_X = ARROW_X + ARROW_WIDTH + GAP;
    private static final int SLOT_Y = 6;  // Equal margins top/bottom (6px each)

    // Vanilla furnace GUI texture (256x256)
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public InfusingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.INFUSER.get().asItem().getDefaultInstance());
        // Arrow from furnace GUI at position (79, 34), size 24x17
        this.arrow = guiHelper.createDrawable(FURNACE_TEXTURE, 79, 34, 24, 17);
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
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.ingredient());

        // Fluid input (center) - honey bucket representation
        FluidStack fluidStack = new FluidStack(
                ApicaFluids.HONEY_SOURCE.get(),
                recipe.fluidIngredient().amount()
        );
        builder.addSlot(RecipeIngredientRole.CATALYST, FLUID_X, SLOT_Y)
                .addFluidStack(fluidStack.getFluid(), fluidStack.getAmount())
                .setFluidRenderer(fluidStack.getAmount(), false, 16, 16);

        // Output item slot (right)
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(InfusingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        // Draw slot backgrounds (using vanilla inventory slot style)
        drawSlot(guiGraphics, INPUT_X - 1, SLOT_Y - 1);
        drawSlot(guiGraphics, FLUID_X - 1, SLOT_Y - 1);
        drawSlot(guiGraphics, OUTPUT_X - 1, SLOT_Y - 1);

        // Draw "+" between input and fluid slots (centered in larger gap)
        int plusX = INPUT_X + SLOT_SIZE + (INPUT_GAP - 6) / 2;
        guiGraphics.drawString(mc.font, "+", plusX, SLOT_Y + 4, 0x404040, false);

        // Draw arrow (vanilla furnace arrow texture)
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y);

        // Draw fluid amount below fluid slot
        int fluidAmount = recipe.fluidIngredient().amount();
        String amountText = fluidAmount + " mB";
        int textWidth = mc.font.width(amountText);
        int textX = FLUID_X + (SLOT_SIZE - textWidth) / 2;
        guiGraphics.drawString(mc.font, amountText, textX, SLOT_Y + SLOT_SIZE + 2, 0x404040, false);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        // Vanilla-style slot (18x18)
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
