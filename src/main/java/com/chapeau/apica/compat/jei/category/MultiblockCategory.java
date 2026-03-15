/**
 * ============================================================
 * [MultiblockCategory.java]
 * Description: Categorie JEI pour afficher les structures multiblocs
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | MultiblockInfo      | Donnees multibloc    | Blocs requis et pattern        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * Affiche la liste des blocs requis pour construire un multibloc.
 * Accessible via "U" sur n'importe quel bloc du multibloc.
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.Apica;
import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiblockCategory implements IRecipeCategory<MultiblockInfo> {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 80;
    private static final int TEXT_COLOR = 0x404040;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public MultiblockCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawable(
                Apica.modLoc("textures/gui/codex/codex_book/down_multibloc.png"),
                0, 0, 16, 16);
        this.title = Component.translatable("gui.apica.jei.multiblock");
    }

    @Override
    public RecipeType<MultiblockInfo> getRecipeType() {
        return ApicaJeiRecipeTypes.MULTIBLOCK;
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
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockInfo recipe, IFocusGroup focuses) {
        List<ItemStack> blocks = recipe.requiredBlocks();

        // Controller en premier (OUTPUT pour highlight)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 5, 5)
                .addItemStack(recipe.controllerStack());

        // Autres blocs requis en grille
        int startX = 30;
        int startY = 5;
        int cols = 6;

        for (int i = 1; i < Math.min(blocks.size(), 19); i++) {
            int idx = i - 1;
            int x = startX + (idx % cols) * 20;
            int y = startY + (idx / cols) * 20;

            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(blocks.get(i));
        }
    }

    @Override
    public void draw(MultiblockInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // Draw controller slot
        drawSlot(guiGraphics, 4, 4);

        // Draw other slots
        List<ItemStack> blocks = recipe.requiredBlocks();
        int startX = 30;
        int startY = 5;
        int cols = 6;

        for (int i = 1; i < Math.min(blocks.size(), 19); i++) {
            int idx = i - 1;
            int x = startX + (idx % cols) * 20 - 1;
            int y = startY + (idx / cols) * 20 - 1;
            drawSlot(guiGraphics, x, y);
        }

        // Draw multiblock name
        Component name = Component.translatable(recipe.name());
        guiGraphics.drawString(font, name, 5, HEIGHT - 12, TEXT_COLOR, false);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
