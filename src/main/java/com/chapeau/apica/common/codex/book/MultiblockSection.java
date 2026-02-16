/**
 * ============================================================
 * [MultiblockSection.java]
 * Description: Module multibloc du Codex Book - affiche une structure multibloc en vue isometrique
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookSection    | Classe parente       | Systeme de sections modulaires |
 * | MultiblockPatterns  | Registre patterns    | Recuperation du pattern        |
 * | MultiblockPattern   | Donnees structure    | Elements et positions          |
 * | BlockMatcher        | Acces blocs display  | Recuperation des blocs         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookContent (sections de multibloc)
 * - CodexBookScreen (rendu des structures)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.core.multiblock.BlockMatcher;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.multiblock.MultiblockPatterns;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MultiblockSection extends CodexBookSection {

    private static final float ITEM_SCALE = 1.28f;
    private static final int SPACING_X = 16;
    private static final int SPACING_Z = 10;
    private static final int SPACING_Y_UP = 44;
    private static final int PADDING_TOP = 4;
    private static final int PADDING_BOTTOM = 4;
    private static final int ITEM_SIZE = 16;

    private final String patternId;
    private final String controllerId;

    private boolean resolved = false;
    private ItemStack airStack = null;
    private final List<DisplayElement> displayElements = new ArrayList<>();
    private int computedHeight = 0;
    private int minPx = 0;
    private int maxPx = 0;
    private int minPy = 0;
    private int maxPy = 0;

    public MultiblockSection(String patternId, String controllerId) {
        this.patternId = patternId;
        this.controllerId = controllerId;
    }

    @Override
    public SectionType getType() {
        return SectionType.MULTIBLOCK;
    }

    @Override
    public int getHeight(Font font, int pageWidth) {
        resolve();
        return computedHeight;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y,
                       int pageWidth, String nodeTitle, long relativeDay) {
        resolve();
        if (displayElements.isEmpty()) return;

        int centerX = x + pageWidth / 2 - (minPx + maxPx) / 2;
        int originY = y + PADDING_TOP - minPy;

        float zOffset = 0;
        for (DisplayElement elem : displayElements) {
            int drawX = centerX + elem.px;
            int drawY = originY + elem.py;

            if (elem.isAir) {
                renderScaledItem(graphics, airStack, drawX, drawY, ITEM_SCALE, zOffset);
            } else {
                renderScaledItem(graphics, elem.stack, drawX, drawY, ITEM_SCALE, zOffset);
            }
            zOffset += 10.0f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack stack,
                                   int x, int y, float scale, float zOffset) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, zOffset);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void resolve() {
        if (resolved) return;
        resolved = true;

        airStack = new ItemStack(ApicaBlocks.AIR_PLACEHOLDER.get().asItem());

        MultiblockPattern pattern = MultiblockPatterns.get(patternId);
        if (pattern == null) return;

        // Collecter TOUS les elements (air et non-air)
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            Vec3i offset = element.offset();
            boolean isAir = BlockMatcher.isAirMatcher(element.matcher());

            if (isAir) {
                displayElements.add(new DisplayElement(
                        offset.getX(), offset.getY(), offset.getZ(),
                        ItemStack.EMPTY, true, 0, 0));
            } else {
                Block block = BlockMatcher.getDisplayBlock(element.matcher());
                if (block == null) continue;

                ItemStack stack = new ItemStack(block.asItem());
                if (stack.isEmpty()) continue;

                displayElements.add(new DisplayElement(
                        offset.getX(), offset.getY(), offset.getZ(),
                        stack, false, 0, 0));
            }
        }

        // Ajouter le controleur (0,0,0) qui n'est pas dans le pattern
        if (controllerId != null && !controllerId.isEmpty()) {
            ResourceLocation ctrlLoc = ResourceLocation.parse(controllerId);
            Block ctrlBlock = BuiltInRegistries.BLOCK.get(ctrlLoc);
            if (ctrlBlock != null) {
                ItemStack ctrlStack = new ItemStack(ctrlBlock.asItem());
                if (!ctrlStack.isEmpty()) {
                    displayElements.add(new DisplayElement(
                            0, 0, 0, ctrlStack, false, 0, 0));
                }
            }
        }

        // Calculer les positions pixel isometriques
        int scaledItem = Math.round(ITEM_SIZE * ITEM_SCALE);
        minPx = Integer.MAX_VALUE;
        maxPx = Integer.MIN_VALUE;
        minPy = Integer.MAX_VALUE;
        maxPy = Integer.MIN_VALUE;

        for (int i = 0; i < displayElements.size(); i++) {
            DisplayElement elem = displayElements.get(i);
            int px = (elem.gridX - elem.gridZ) * SPACING_X;
            int py = (elem.gridX + elem.gridZ) * SPACING_Z - elem.gridY * SPACING_Y_UP;
            displayElements.set(i, new DisplayElement(
                    elem.gridX, elem.gridY, elem.gridZ,
                    elem.stack, elem.isAir, px, py));

            minPx = Math.min(minPx, px);
            maxPx = Math.max(maxPx, px + scaledItem);
            minPy = Math.min(minPy, py);
            maxPy = Math.max(maxPy, py + scaledItem);
        }

        // Trier back-to-front: Y asc, Z desc, X asc
        displayElements.sort(Comparator
                .comparingInt((DisplayElement e) -> e.gridY)
                .thenComparingInt(e -> -e.gridZ)
                .thenComparingInt(e -> e.gridX));

        computedHeight = PADDING_TOP + (maxPy - minPy) + PADDING_BOTTOM;
    }

    public static MultiblockSection fromJson(JsonObject json) {
        String pattern = json.has("pattern") ? json.get("pattern").getAsString() : "";
        String controller = json.has("controller") ? json.get("controller").getAsString() : "";
        return new MultiblockSection(pattern, controller);
    }

    private static class DisplayElement {
        final int gridX, gridY, gridZ;
        final ItemStack stack;
        final boolean isAir;
        final int px, py;

        DisplayElement(int gridX, int gridY, int gridZ, ItemStack stack,
                       boolean isAir, int px, int py) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.gridZ = gridZ;
            this.stack = stack;
            this.isAir = isAir;
            this.px = px;
            this.py = py;
        }
    }
}
