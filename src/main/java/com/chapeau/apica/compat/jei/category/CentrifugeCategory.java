/**
 * ============================================================
 * [CentrifugeCategory.java]
 * Description: Categorie JEI pour les recettes de centrifugation (combs -> outputs)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | CentrifugeRecipe    | Type de recette      | Donnees a afficher             |
 * | ApicaBlocks         | Icone categorie      | Centrifuge block               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.recipe.ProcessingOutput;
import com.chapeau.apica.core.recipe.type.CentrifugeRecipe;
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

public class CentrifugeCategory implements IRecipeCategory<CentrifugeRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    // Vanilla furnace GUI texture (256x256)
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public CentrifugeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.MANUAL_CENTRIFUGE.get().asItem().getDefaultInstance());
        // Arrow from furnace GUI at position (79, 34), size 24x17
        this.arrow = guiHelper.createDrawable(FURNACE_TEXTURE, 79, 34, 24, 17);
        this.title = Component.translatable("gui.apica.jei.centrifuge");
    }

    @Override
    public RecipeType<CentrifugeRecipe> getRecipeType() {
        return ApicaJeiRecipeTypes.CENTRIFUGE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeRecipe recipe, IFocusGroup focuses) {
        // Layout: input(18) + gap(6) + arrow(24) + gap(6) + items(38) + gap(6) + fluid(18) = 116
        // Center offset: (150 - 116) / 2 = 17
        int inputX = 17;
        int inputY = 17;

        // Input comb slot (left)
        builder.addSlot(RecipeIngredientRole.INPUT, inputX, inputY)
                .addIngredients(recipe.ingredient());

        // Output item slots (2x2 grid)
        List<ProcessingOutput> outputs = recipe.results();
        int outputX = inputX + 18 + 6 + 24 + 6;  // 71
        int outputY = 8;

        for (int i = 0; i < Math.min(outputs.size(), 4); i++) {
            ProcessingOutput output = outputs.get(i);
            int x = outputX + (i % 2) * 20;
            int y = outputY + (i / 2) * 20;

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(output.stack());
        }

        // Fluid output slot (right side)
        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            int fluidX = outputX + 38 + 6;  // After items grid + gap
            builder.addSlot(RecipeIngredientRole.OUTPUT, fluidX, inputY)
                    .addFluidStack(fluidOutput.getFluid(), fluidOutput.getAmount())
                    .setFluidRenderer(fluidOutput.getAmount(), false, 16, 16);
        }
    }

    @Override
    public void draw(CentrifugeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        int inputX = 17;
        int inputY = 17;

        // Draw input slot
        drawSlot(guiGraphics, inputX - 1, inputY - 1);

        // Draw arrow (vanilla furnace arrow texture)
        int arrowX = inputX + 18 + 6;  // 41
        arrow.draw(guiGraphics, arrowX, inputY);

        // Draw output item slots (2x2 grid)
        List<ProcessingOutput> outputs = recipe.results();
        int outputX = inputX + 18 + 6 + 24 + 6;  // 71
        int outputY = 8;

        for (int i = 0; i < Math.min(outputs.size(), 4); i++) {
            int x = outputX + (i % 2) * 20 - 1;
            int y = outputY + (i / 2) * 20 - 1;
            drawSlot(guiGraphics, x, y);
        }

        // Draw fluid output slot
        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            int fluidX = outputX + 38 + 6;  // After items grid + gap
            drawSlot(guiGraphics, fluidX - 1, inputY - 1);
        }

        // Draw chance percentages
        for (int i = 0; i < Math.min(outputs.size(), 4); i++) {
            ProcessingOutput output = outputs.get(i);
            if (output.chance() < 1.0f) {
                int x = outputX + (i % 2) * 20 + 17;
                int y = outputY + (i / 2) * 20 + 9;
                String chanceText = Math.round(output.chance() * 100) + "%";
                guiGraphics.drawString(mc.font, chanceText, x, y, 0xFF8B8B8B, false);
            }
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
