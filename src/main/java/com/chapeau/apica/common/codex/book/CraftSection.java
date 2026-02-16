/**
 * ============================================================
 * [CraftSection.java]
 * Description: Module craft du Codex Book - affiche une recette de crafting
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | RecipeManager       | Acces recettes       | Recherche par item resultat    |
 * | Minecraft           | Client instance      | Acces RecipeManager et rendu   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (sections de craft)
 * - CodexBookScreen (rendu des recettes)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

public class CraftSection extends CodexBookSection {

    private static final ResourceLocation CRAFT_BG = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/codex_book_craft.png");
    private static final ResourceLocation CRAFT_SLOT = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/craft_slot.png");
    private static final int BG_WIDTH = 118;
    private static final int BG_HEIGHT = 57;
    private static final int SLOT_SIZE = 20;

    private static final int PADDING_TOP = -10;
    private static final int PADDING_BOTTOM = -10;
    private static final int RESULT_PADDING_BOTTOM = 2;
    private static final int ITEM_RENDER_SIZE = 16;

    // ============================================================
    // SLOT POSITIONS — Relatifs au coin haut-gauche de l'image
    // Editer ces valeurs pour aligner les items sur la grille en
    // perspective du craft table.
    // Format: {x, y} pour chaque slot, item rendu a ITEM_SCALE
    // ============================================================

    // Taille de rendu des items sur la grille (fraction de 16px)
    private static final float ITEM_SCALE = 1.10f;

    // Grille 3x3 — row-major order [0..8]
    // Slot 0=top-left, 1=top-center, 2=top-right
    // Slot 3=mid-left, 4=mid-center, 5=mid-right
    // Slot 6=bot-left, 7=bot-center, 8=bot-right
    private static final int[][] GRID_SLOTS = {
        {9, 9},   // [0] top-left
        {34, 2},   // [1] top-center
        {60, -5},   // [2] top-right
        {20, 21},   // [3] mid-left
        {45, 14},   // [4] mid-center
        {71, 7},   // [5] mid-right
        {31, 33},   // [6] bot-left
        {56, 26},   // [7] bot-center
        {82, 19},   // [8] bot-right
    };

    // Position du resultat (au-dessus de l'image craft)
    private static final int RESULT_OFFSET_X = 102; // relatif au centre de l'image
    private static final int RESULT_OFFSET_Y = 14; // relatif au centre de l'image
    private static final float RESULT_SCALE = 1.1f;

    // ============================================================

    private final String resultItem;
    private final int heightOffset;
    private boolean resolved = false;
    private ItemStack resultStack = ItemStack.EMPTY;
    private ItemStack[] gridStacks = new ItemStack[9];

    public CraftSection(String resultItem, int heightOffset) {
        this.resultItem = resultItem;
        this.heightOffset = heightOffset;
        for (int i = 0; i < 9; i++) gridStacks[i] = ItemStack.EMPTY;
    }

    @Override
    public SectionType getType() {
        return SectionType.CRAFT;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        int resultHeight = Math.round(ITEM_RENDER_SIZE * RESULT_SCALE) + RESULT_PADDING_BOTTOM;
        return resultHeight + BG_HEIGHT + PADDING_BOTTOM + heightOffset;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        resolveRecipe();

        int resultHeight = Math.round(ITEM_RENDER_SIZE * RESULT_SCALE) + RESULT_PADDING_BOTTOM;

        // Resultat au-dessus de l'image, centre
        if (!resultStack.isEmpty()) {
            int resultX2 = x + (pageWidth - BG_WIDTH) / 2 + RESULT_OFFSET_X;

            // Slot derriere le resultat (activer le blend pour la transparence du PNG)
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            int slotX = resultX2 - (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * RESULT_SCALE)) / 2;
            int slotY = y + PADDING_TOP + RESULT_OFFSET_Y - (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * RESULT_SCALE)) / 2;
            graphics.blit(CRAFT_SLOT, slotX, slotY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            // Item par dessus le slot
            renderScaledItem(graphics, resultStack, resultX2, y + PADDING_TOP + RESULT_OFFSET_Y, RESULT_SCALE);
        }

        // Image de fond (table de craft)
        int bgX = x + (pageWidth - BG_WIDTH) / 2;
        int bgY = y + resultHeight + PADDING_TOP;
        graphics.blit(CRAFT_BG, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        // Items de la grille
        for (int i = 0; i < 9; i++) {
            if (!gridStacks[i].isEmpty()) {
                int slotX = bgX + GRID_SLOTS[i][0];
                int slotY = bgY + GRID_SLOTS[i][1];
                renderScaledItem(graphics, gridStacks[i], slotX, slotY, ITEM_SCALE);
            }
        }
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();

        // Reactiver le blend apres renderItem (qui le desactive)
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    /**
     * Recherche la recette correspondant au resultItem (lazy, une seule fois).
     */
    private void resolveRecipe() {
        if (resolved) return;
        resolved = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ResourceLocation targetId = ResourceLocation.parse(resultItem);
        var targetItem = BuiltInRegistries.ITEM.get(targetId);
        if (targetItem == null) return;

        List<RecipeHolder<CraftingRecipe>> recipes = mc.level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING);

        for (RecipeHolder<CraftingRecipe> holder : recipes) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(mc.level.registryAccess());

            if (result.getItem() == targetItem) {
                resultStack = result;
                extractIngredients(recipe);
                return;
            }
        }
    }

    private void extractIngredients(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            List<Ingredient> ingredients = shaped.getIngredients();

            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int srcIdx = row * w + col;
                    int dstIdx = row * 3 + col;
                    if (srcIdx < ingredients.size() && dstIdx < 9) {
                        ItemStack[] items = ingredients.get(srcIdx).getItems();
                        gridStacks[dstIdx] = items.length > 0 ? items[0] : ItemStack.EMPTY;
                    }
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<Ingredient> ingredients = shapeless.getIngredients();
            for (int i = 0; i < Math.min(ingredients.size(), 9); i++) {
                ItemStack[] items = ingredients.get(i).getItems();
                gridStacks[i] = items.length > 0 ? items[0] : ItemStack.EMPTY;
            }
        }
    }

    public static CraftSection fromJson(JsonObject json) {
        String result = json.has("result") ? json.get("result").getAsString() : "";
        int height = json.has("height") ? json.get("height").getAsInt() : 0;
        return new CraftSection(result, height);
    }
}
