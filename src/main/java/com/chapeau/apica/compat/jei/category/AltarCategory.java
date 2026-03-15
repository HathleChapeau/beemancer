/**
 * ============================================================
 * [AltarCategory.java]
 * Description: Categorie JEI pour les recettes d'altar (pedestals + centre + pollen -> result)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, slots         |
 * | AltarRecipe         | Type de recette      | Donnees a afficher             |
 * | ApicaBlocks         | Icone categorie      | Altar heart block              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * Layout inspire du module codex AltarCraftSection:
 * - Items pedestaux en cercle autour du centre
 * - Fleche vers le resultat
 * - Pollen affiche en bas
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.chapeau.apica.core.registry.ApicaBlocks;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AltarCategory implements IRecipeCategory<AltarRecipe> {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 90;

    private static final int CENTER_X = 40;
    private static final int CENTER_Y = 28;
    private static final int PEDESTAL_RADIUS = 24;

    // Couleur texte standard Minecraft (gris fonce)
    private static final int TEXT_COLOR = 0x404040;

    // Vanilla furnace GUI texture (256x256)
    private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final Component title;

    public AltarCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.ALTAR_HEART.get().asItem().getDefaultInstance());
        // Arrow from furnace GUI at position (79, 34), size 24x17
        this.arrow = guiHelper.createDrawable(FURNACE_TEXTURE, 79, 34, 24, 17);
        this.title = Component.translatable("gui.apica.jei.altar");
    }

    @Override
    public RecipeType<AltarRecipe> getRecipeType() {
        return ApicaJeiRecipeTypes.ALTAR;
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
    public void setRecipe(IRecipeLayoutBuilder builder, AltarRecipe recipe, IFocusGroup focuses) {
        // Center item slot
        builder.addSlot(RecipeIngredientRole.INPUT, CENTER_X, CENTER_Y)
                .addIngredients(recipe.centerItem());

        // Pedestal items in a circle around the center
        List<Ingredient> pedestals = recipe.pedestalItems();
        int pedestalCount = Math.min(pedestals.size(), 8);

        for (int i = 0; i < pedestalCount; i++) {
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / pedestalCount;
            int px = CENTER_X + (int) Math.round(PEDESTAL_RADIUS * Math.cos(angle));
            int py = CENTER_Y + (int) Math.round(PEDESTAL_RADIUS * Math.sin(angle));

            builder.addSlot(RecipeIngredientRole.INPUT, px, py)
                    .addIngredients(pedestals.get(i));
        }

        // Result slot (right side)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, CENTER_Y)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(AltarRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // Draw center slot
        drawSlot(guiGraphics, CENTER_X - 1, CENTER_Y - 1);

        // Draw pedestal slots in circle
        List<Ingredient> pedestals = recipe.pedestalItems();
        int pedestalCount = Math.min(pedestals.size(), 8);
        for (int i = 0; i < pedestalCount; i++) {
            double angle = -Math.PI / 2.0 + 2.0 * Math.PI * i / pedestalCount;
            int px = CENTER_X + (int) Math.round(PEDESTAL_RADIUS * Math.cos(angle)) - 1;
            int py = CENTER_Y + (int) Math.round(PEDESTAL_RADIUS * Math.sin(angle)) - 1;
            drawSlot(guiGraphics, px, py);
        }

        // Draw result slot
        drawSlot(guiGraphics, 129, CENTER_Y - 1);

        // Draw arrow texture
        arrow.draw(guiGraphics, 90, CENTER_Y);

        // Draw pollen requirements at the bottom
        Map<ResourceLocation, Integer> pollen = recipe.pollen();
        if (!pollen.isEmpty()) {
            List<PollenEntry> entries = new ArrayList<>();
            for (Map.Entry<ResourceLocation, Integer> entry : pollen.entrySet()) {
                var item = BuiltInRegistries.ITEM.get(entry.getKey());
                if (item != null) {
                    String shortName = extractPollenShortName(
                            new ItemStack(item).getHoverName().getString());
                    entries.add(new PollenEntry(new ItemStack(item), entry.getValue(), shortName));
                }
            }

            int pollenY = 68;
            int spacing = 36;
            int totalWidth = entries.size() * spacing - (spacing - 16);
            int startX = (WIDTH - totalWidth) / 2;

            for (int i = 0; i < entries.size(); i++) {
                PollenEntry entry = entries.get(i);
                int px = startX + i * spacing;

                // Render pollen item (scaled down)
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(px, pollenY, 0);
                guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                guiGraphics.renderItem(entry.stack, 0, 0);
                guiGraphics.pose().popPose();

                // Render count (standard Minecraft text color)
                String countStr = "x" + entry.count;
                guiGraphics.drawString(font, countStr, px + 12, pollenY + 4, TEXT_COLOR, false);

                // Render short name below
                int nameWidth = font.width(entry.shortName);
                int nameX = px + 8 - nameWidth / 2;
                guiGraphics.drawString(font, entry.shortName, nameX, pollenY + 14, TEXT_COLOR, false);
            }
        }
    }

    private record PollenEntry(ItemStack stack, int count, String shortName) {}

    private static String extractPollenShortName(String displayName) {
        String shortName;
        if (displayName.startsWith("Pollen of ")) {
            shortName = displayName.substring("Pollen of ".length());
        } else if (displayName.endsWith(" Pollen")) {
            shortName = displayName.substring(0, displayName.length() - " Pollen".length());
        } else if (displayName.endsWith(" Spore")) {
            shortName = displayName.substring(0, displayName.length() - " Spore".length());
        } else {
            shortName = displayName;
        }

        int maxLength = 5;
        if (shortName.length() > maxLength) {
            shortName = shortName.substring(0, maxLength) + ".";
        }
        return shortName;
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
    }
}
