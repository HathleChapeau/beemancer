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

    private static final int SLOT_SIZE = 20;
    private static final int PADDING_BOTTOM = 6;
    private static final int TOP_SLOT_GAP = 2;
    private static final float ITEM_SCALE = 1.1f;
    private static final int ITEM_RENDER_SIZE = 16;

    private final String inputItem;
    private final String outputItem;
    private final String topSlotItem;
    private ItemStack inputStack = ItemStack.EMPTY;
    private ItemStack outputStack = ItemStack.EMPTY;
    private ItemStack topSlotStack = ItemStack.EMPTY;
    private boolean resolved = false;

    public ProcessSection(String inputItem, String outputItem, String topSlotItem) {
        this.inputItem = inputItem;
        this.outputItem = outputItem;
        this.topSlotItem = topSlotItem;
    }

    @Override
    public SectionType getType() {
        return SectionType.PROCESS;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        int height = SLOT_SIZE + PADDING_BOTTOM;
        if (!topSlotItem.isEmpty()) {
            height += SLOT_SIZE + TOP_SLOT_GAP;
        }
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        resolveItems();

        float arrowScale = 2.0f;
        int arrowRenderedW = Math.round(font.width("\u2192") * arrowScale);
        int gap = 4;
        int totalWidth = SLOT_SIZE + gap + arrowRenderedW + gap + SLOT_SIZE;
        int startX = x + (pageWidth - totalWidth) / 2;

        int topSlotOffset = 0;
        if (!topSlotStack.isEmpty()) {
            topSlotOffset = SLOT_SIZE + TOP_SLOT_GAP;
            int arrowCenterX = startX + SLOT_SIZE + gap + arrowRenderedW / 2;
            int topSlotX = arrowCenterX - SLOT_SIZE / 2;
            graphics.blit(CRAFT_SLOT, topSlotX, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            int itemX = topSlotX + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            int itemY = y + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            renderScaledItem(graphics, topSlotStack, itemX, itemY, ITEM_SCALE);
        }

        int rowY = y + topSlotOffset;

        // Input slot + item
        graphics.blit(CRAFT_SLOT, startX, rowY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (!inputStack.isEmpty()) {
            int itemX = startX + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            int itemY = rowY + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            renderScaledItem(graphics, inputStack, itemX, itemY, ITEM_SCALE);
        }

        // Arrow
        int arrowX = startX + SLOT_SIZE + gap;
        int arrowY = rowY + SLOT_SIZE / 2 - 8;
        graphics.pose().pushPose();
        graphics.pose().translate(arrowX, arrowY, 0);
        graphics.pose().scale(arrowScale, arrowScale, 1.0f);
        graphics.drawString(font, "\u2192", 0, 0, 0xFF8B6914, false);
        graphics.pose().popPose();

        // Output slot + item
        int outputX = arrowX + arrowRenderedW + gap;
        graphics.blit(CRAFT_SLOT, outputX, rowY, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (!outputStack.isEmpty()) {
            int itemX = outputX + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
            int itemY = rowY + (SLOT_SIZE - Math.round(ITEM_RENDER_SIZE * ITEM_SCALE)) / 2;
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

        if (!topSlotItem.isEmpty()) {
            ResourceLocation topId = ResourceLocation.parse(topSlotItem);
            var topItem = BuiltInRegistries.ITEM.get(topId);
            if (topItem != null) {
                topSlotStack = new ItemStack(topItem);
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

    public static ProcessSection fromJson(JsonObject json) {
        String input = json.has("input") ? json.get("input").getAsString() : "";
        String output = json.has("output") ? json.get("output").getAsString() : "";
        String topSlot = json.has("top_slot") ? json.get("top_slot").getAsString() : "";
        return new ProcessSection(input, output, topSlot);
    }
}
