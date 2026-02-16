/**
 * ============================================================
 * [ProcessSection.java]
 * Description: Module process du Codex Book - affiche input -> output
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | ItemStack           | Items a afficher     | Input et output                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (sections de process)
 * - CodexBookScreen (rendu du process)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.Apica;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ProcessSection extends CodexBookSection {

    private static final ResourceLocation CRAFT_SLOT = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/craft_slot.png");
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/container/furnace/burn_progress.png");

    private static final int SLOT_SIZE = 20;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;
    private static final int SPACING = 8;
    private static final int PADDING_BOTTOM = 6;
    private static final float ITEM_SCALE = 1.1f;
    private static final int ITEM_RENDER_SIZE = 16;

    private final String inputItem;
    private final String outputItem;
    private ItemStack inputStack = ItemStack.EMPTY;
    private ItemStack outputStack = ItemStack.EMPTY;
    private boolean resolved = false;

    public ProcessSection(String inputItem, String outputItem) {
        this.inputItem = inputItem;
        this.outputItem = outputItem;
    }

    @Override
    public SectionType getType() {
        return SectionType.PROCESS;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        return SLOT_SIZE + PADDING_BOTTOM;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        resolveItems();

        int totalWidth = SLOT_SIZE + SPACING + ARROW_WIDTH + SPACING + SLOT_SIZE;
        int startX = x + (pageWidth - totalWidth) / 2;

        // Input slot + item
        graphics.blit(CRAFT_SLOT, startX, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (!inputStack.isEmpty()) {
            int itemX = startX + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            int itemY = y + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            renderScaledItem(graphics, inputStack, itemX, itemY, ITEM_SCALE);
        }

        // Arrow
        int arrowX = startX + SLOT_SIZE + SPACING;
        int arrowY = y + (SLOT_SIZE - ARROW_HEIGHT) / 2;
        graphics.blit(ARROW, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);

        // Output slot + item
        int outputX = arrowX + ARROW_WIDTH + SPACING;
        graphics.blit(CRAFT_SLOT, outputX, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (!outputStack.isEmpty()) {
            int itemX = outputX + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            int itemY = y + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            renderScaledItem(graphics, outputStack, itemX, itemY, ITEM_SCALE);
        }
    }

    private void resolveItems() {
        if (resolved) return;
        resolved = true;

        ResourceLocation inputId = ResourceLocation.parse(inputItem);
        var input = BuiltInRegistries.ITEM.get(inputId);
        if (input != null) {
            inputStack = new ItemStack(input);
        }

        ResourceLocation outputId = ResourceLocation.parse(outputItem);
        var output = BuiltInRegistries.ITEM.get(outputId);
        if (output != null) {
            outputStack = new ItemStack(output);
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

    public static ProcessSection fromJson(JsonObject json) {
        String input = json.has("input") ? json.get("input").getAsString() : "";
        String output = json.has("output") ? json.get("output").getAsString() : "";
        return new ProcessSection(input, output);
    }
}
