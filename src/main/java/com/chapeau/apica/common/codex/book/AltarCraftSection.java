/**
 * ============================================================
 * [AltarCraftSection.java]
 * Description: Module altar_craft du Codex Book - affiche une recette d'altar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | AltarRecipe         | Type de recette      | Resolution recette altar       |
 * | ApicaRecipeTypes    | Registre recettes    | Lookup par type ALTAR          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (sections de altar_craft)
 * - CodexBookScreen (rendu des recettes d'altar)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.Apica;
import com.chapeau.apica.core.recipe.ApicaRecipeTypes;
import com.chapeau.apica.core.recipe.type.AltarRecipe;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AltarCraftSection extends CodexBookSection {

    private static final ResourceLocation CRAFT_SLOT = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/craft_slot.png");
    private static final ResourceLocation POLLEN_POT_SLOT = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/pollen_pot_slot.png");
    private static final int POLLEN_POT_SLOT_W = 16;
    private static final int POLLEN_POT_SLOT_H = 18;
    private static final int SLOT_SIZE = 20;
    private static final int ITEM_SIZE = 16;
    private static final float ITEM_SCALE = 1.10f;

    private static final int CROSS_SPACING = 24;
    private static final int ARROW_WIDTH = 20;
    private static final int POLLEN_ROW_HEIGHT = 20;
    private static final int SECTION_HEIGHT = CROSS_SPACING * 2 + SLOT_SIZE + POLLEN_ROW_HEIGHT + 8;

    private final String resultItem;
    private boolean resolved = false;
    private ItemStack resultStack = ItemStack.EMPTY;
    private ItemStack centerStack = ItemStack.EMPTY;
    private final List<ItemStack> pedestalStacks = new ArrayList<>();
    private final List<PollenEntry> pollenEntries = new ArrayList<>();

    private record PollenEntry(ItemStack stack, int count) {}

    public AltarCraftSection(String resultItem) {
        this.resultItem = resultItem;
    }

    @Override
    public SectionType getType() {
        return SectionType.ALTAR_CRAFT;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return SECTION_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        resolveRecipe();

        int crossCenterX = x + pageWidth / 2 - ARROW_WIDTH / 2 - SLOT_SIZE / 2;
        int crossCenterY = y + CROSS_SPACING;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        renderSlotWithItem(graphics, centerStack, crossCenterX, crossCenterY);

        if (pedestalStacks.size() > 0) {
            renderSlotWithItem(graphics, pedestalStacks.get(0),
                    crossCenterX, crossCenterY - CROSS_SPACING);
        }
        if (pedestalStacks.size() > 1) {
            renderSlotWithItem(graphics, pedestalStacks.get(1),
                    crossCenterX + CROSS_SPACING, crossCenterY);
        }
        if (pedestalStacks.size() > 2) {
            renderSlotWithItem(graphics, pedestalStacks.get(2),
                    crossCenterX, crossCenterY + CROSS_SPACING);
        }
        if (pedestalStacks.size() > 3) {
            renderSlotWithItem(graphics, pedestalStacks.get(3),
                    crossCenterX - CROSS_SPACING, crossCenterY);
        }

        int arrowX = crossCenterX + CROSS_SPACING + SLOT_SIZE + 4;
        int arrowY = crossCenterY + SLOT_SIZE / 2 - 6;
        float arrowScale = 1.5f;
        graphics.pose().pushPose();
        graphics.pose().translate(arrowX, arrowY, 0);
        graphics.pose().scale(arrowScale, arrowScale, 1.0f);
        graphics.drawString(font, "\u2192", 0, 0, 0xFF8B6914, false);
        graphics.pose().popPose();

        int resultX = arrowX + ARROW_WIDTH;
        int resultY = crossCenterY;
        renderSlotWithItem(graphics, resultStack, resultX, resultY);

        if (!pollenEntries.isEmpty()) {
            int pollenY = crossCenterY + CROSS_SPACING + SLOT_SIZE + 4;
            int totalPollenWidth = pollenEntries.size() * 28 - 8;
            int pollenStartX = x + (pageWidth - totalPollenWidth) / 2;

            for (int i = 0; i < pollenEntries.size(); i++) {
                PollenEntry entry = pollenEntries.get(i);
                int px = pollenStartX + i * 28;
                int potX = px - 1;
                int potY = pollenY - 2;
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                graphics.blit(POLLEN_POT_SLOT, potX, potY, 0, 0,
                        POLLEN_POT_SLOT_W, POLLEN_POT_SLOT_H, POLLEN_POT_SLOT_W, POLLEN_POT_SLOT_H);
                renderScaledItem(graphics, entry.stack, px, pollenY + 1, 0.8f);
                String countStr = "x" + entry.count;
                graphics.drawString(font, countStr, px + 14, pollenY + 5, 0xFF8B6914, false);
            }
        }
    }

    private void renderSlotWithItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(CRAFT_SLOT, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (!stack.isEmpty()) {
            int itemOffset = (SLOT_SIZE - Math.round(ITEM_SIZE * ITEM_SCALE)) / 2;
            renderScaledItem(graphics, stack, x + itemOffset, y + itemOffset, ITEM_SCALE);
        }
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    private void resolveRecipe() {
        if (resolved) return;
        resolved = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ResourceLocation targetId = ResourceLocation.parse(resultItem);
        var targetItem = BuiltInRegistries.ITEM.get(targetId);
        if (targetItem == null) return;

        List<RecipeHolder<AltarRecipe>> recipes = mc.level.getRecipeManager()
                .getAllRecipesFor(ApicaRecipeTypes.ALTAR.get());

        for (RecipeHolder<AltarRecipe> holder : recipes) {
            AltarRecipe recipe = holder.value();
            ItemStack result = recipe.result();
            if (result.getItem() == targetItem) {
                resultStack = result.copy();

                ItemStack[] centerItems = recipe.centerItem().getItems();
                centerStack = centerItems.length > 0 ? centerItems[0] : ItemStack.EMPTY;

                for (Ingredient pedestal : recipe.pedestalItems()) {
                    ItemStack[] items = pedestal.getItems();
                    pedestalStacks.add(items.length > 0 ? items[0] : ItemStack.EMPTY);
                }

                for (Map.Entry<ResourceLocation, Integer> entry : recipe.pollen().entrySet()) {
                    var pollenItem = BuiltInRegistries.ITEM.get(entry.getKey());
                    if (pollenItem != null) {
                        pollenEntries.add(new PollenEntry(
                                new ItemStack(pollenItem), entry.getValue()));
                    }
                }
                return;
            }
        }
    }

    public static AltarCraftSection fromJson(JsonObject json) {
        String result = json.has("result") ? json.get("result").getAsString() : "";
        return new AltarCraftSection(result);
    }
}
