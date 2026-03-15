/**
 * ============================================================
 * [CrystallizerCategory.java]
 * Description: Categorie JEI pour les recettes de cristallisation (fluide -> cristal)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | CrystallizingRecipe | Type de recette      | Donnees a afficher             |
 * | ApicaBlocks         | Icone categorie      | Crystallizer block             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.recipe.type.CrystallizingRecipe;
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

public class CrystallizerCategory implements IRecipeCategory<CrystallizingRecipe> {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 50;

    // Layout constants for equal spacing
    // Content: SLOT(18) + GAP(12) + ARROW(24) + GAP(12) + SLOT(18) = 84
    private static final int MARGIN = 8;
    private static final int GAP = 9;
    private static final int SLOT_SIZE = 18;
    private static final int ARROW_WIDTH = 24;

    private static final int FLUID_X = MARGIN + 2;  // +2px shift right
    private static final int ARROW_X = FLUID_X + SLOT_SIZE + GAP + 1;  // +1px shift right
    private static final int OUTPUT_X = ARROW_X + ARROW_WIDTH + GAP;
    private static final int SLOT_Y = 17;  // Equal margins top/bottom (6px each)

    // Vanilla furnace GUI texture (256x256)
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public CrystallizerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.CRYSTALLIZER.get().asItem().getDefaultInstance());
        // Arrow from furnace GUI at position (79, 34), size 24x17
        this.arrow = guiHelper.createDrawable(FURNACE_TEXTURE, 79, 34, 24, 17);
        this.title = Component.translatable("gui.apica.jei.crystallizer");
    }

    @Override
    public RecipeType<CrystallizingRecipe> getRecipeType() {
        return ApicaJeiRecipeTypes.CRYSTALLIZER;
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrystallizingRecipe recipe, IFocusGroup focuses) {
        // Fluid input slot (left)
        FluidStack fluidStack = new FluidStack(
                recipe.fluidIngredient().fluid(),
                recipe.fluidIngredient().amount()
        );
        builder.addSlot(RecipeIngredientRole.INPUT, FLUID_X, SLOT_Y)
                .addFluidStack(fluidStack.getFluid(), fluidStack.getAmount())
                .setFluidRenderer(fluidStack.getAmount(), false, 16, 16);

        // Output item slot (right)
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(CrystallizingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        // Draw slot backgrounds
        drawSlot(guiGraphics, FLUID_X - 1, SLOT_Y - 1);
        drawSlot(guiGraphics, OUTPUT_X - 1, SLOT_Y - 1);

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
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
