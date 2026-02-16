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

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.debug.DebugWandItem;
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
import java.util.Map;
import java.util.TreeMap;

public class MultiblockSection extends CodexBookSection {

    private static final float ITEM_SCALE = 1.28f;
    private static final int SPACING_X = 11;
    private static final int SPACING_Z = 8;
    private static final int PADDING_TOP = -55;
    private static final int PADDING_BOTTOM = 4;
    private static final int ITEM_SIZE = 16;
    private static final int GROUP_SPACING = -20;

    // Ground background texture
    private static final ResourceLocation GROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/down_multibloc.png");
    private static final int GROUND_TEX_W = 101;
    private static final int GROUND_TEX_H = 55;
    private static final int GROUND_PADDING_Y = 59;

    private final String patternId;
    private final String controllerId;
    private final int paddingY;
    private final int groundOffsetY;

    private boolean resolved = false;
    private ItemStack airStack = null;
    private final List<FloorGroup> floorGroups = new ArrayList<>();
    private int computedHeight = 0;

    public MultiblockSection(String patternId, String controllerId, int paddingY, int groundOffsetY) {
        this.patternId = patternId;
        this.controllerId = controllerId;
        this.paddingY = paddingY;
        this.groundOffsetY = groundOffsetY;
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
        if (floorGroups.isEmpty()) return;

        int currentY = y + PADDING_TOP + paddingY;
        float zOffset = 0;

        for (int g = 0; g < floorGroups.size(); g++){
            FloorGroup group = floorGroups.get(g);
            currentY += (group.maxPy - group.minPy);
            if (g < floorGroups.size() - 1) {
                currentY += GROUP_SPACING;
            }
        }

        // Ground texture: dessine en premier (derriere tout) au bas de la structure
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int groundX = x + pageWidth / 2 - GROUND_TEX_W / 2;
        int groundY = currentY - GROUND_TEX_H + GROUND_PADDING_Y + groundOffsetY;
        graphics.blit(GROUND_TEXTURE, groundX, groundY, GROUND_TEX_W, GROUND_TEX_H,
                0, 0, GROUND_TEX_W, GROUND_TEX_H, GROUND_TEX_W, GROUND_TEX_H);

        for (int g = floorGroups.size() - 1; g >= 0 ; g--) {
            FloorGroup group = floorGroups.get(g);
            int centerX = x + pageWidth / 2 - (group.minPx + group.maxPx) / 2;
            int originY = currentY - group.minPy;

            for (DisplayElement elem : group.elements) {
                int drawX = centerX + elem.px;
                int drawY = originY + elem.py;

                if (elem.isAir) {
                    renderScaledItem(graphics, airStack, drawX, drawY, ITEM_SCALE, zOffset);
                } else {
                    renderScaledItem(graphics, elem.stack, drawX, drawY, ITEM_SCALE, zOffset);
                }
                zOffset += 10.0f;
            }

            currentY -= (group.maxPy - group.minPy);
            currentY -= GROUP_SPACING;
            /*
            if (g < floorGroups.size() - 1) {
                currentY -= GROUP_SPACING + (int)DebugWandItem.value1;
            }*/
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

        // Collecter TOUS les elements groupes par gridY (etage)
        TreeMap<Integer, List<DisplayElement>> byFloor = new TreeMap<>();

        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            Vec3i offset = element.offset();
            boolean isAir = BlockMatcher.isAirMatcher(element.matcher());

            if (isAir) {
                byFloor.computeIfAbsent(offset.getY(), k -> new ArrayList<>())
                        .add(new DisplayElement(
                                offset.getX(), offset.getY(), offset.getZ(),
                                ItemStack.EMPTY, true, 0, 0));
            } else {
                Block block = BlockMatcher.getDisplayBlock(element.matcher());
                if (block == null) continue;

                ItemStack stack = new ItemStack(block.asItem());
                if (stack.isEmpty()) continue;

                byFloor.computeIfAbsent(offset.getY(), k -> new ArrayList<>())
                        .add(new DisplayElement(
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
                    byFloor.computeIfAbsent(0, k -> new ArrayList<>())
                            .add(new DisplayElement(
                                    0, 0, 0, ctrlStack, false, 0, 0));
                }
            }
        }

        // Construire les groupes par etage (Y descendant = etage le plus haut en premier)
        int scaledItem = Math.round(ITEM_SIZE * ITEM_SCALE);
        int totalHeight = PADDING_TOP;

        for (Map.Entry<Integer, List<DisplayElement>> entry : byFloor.descendingMap().entrySet()) {
            List<DisplayElement> elements = entry.getValue();

            // Calculer les positions pixel isometriques pour cet etage (sans composante Y)
            int gMinPx = Integer.MAX_VALUE, gMaxPx = Integer.MIN_VALUE;
            int gMinPy = Integer.MAX_VALUE, gMaxPy = Integer.MIN_VALUE;

            for (int i = 0; i < elements.size(); i++) {
                DisplayElement elem = elements.get(i);
                int px = (elem.gridX - elem.gridZ) * SPACING_X;
                int py = (elem.gridX + elem.gridZ) * SPACING_Z;
                elements.set(i, new DisplayElement(
                        elem.gridX, elem.gridY, elem.gridZ,
                        elem.stack, elem.isAir, px, py));

                gMinPx = Math.min(gMinPx, px);
                gMaxPx = Math.max(gMaxPx, px + scaledItem);
                gMinPy = Math.min(gMinPy, py);
                gMaxPy = Math.max(gMaxPy, py + scaledItem);
            }

            // Trier par py croissant pour le z-order correct
            elements.sort(Comparator.comparingInt((DisplayElement e) -> e.py));

            FloorGroup group = new FloorGroup(entry.getKey(), elements,
                    gMinPx, gMaxPx, gMinPy, gMaxPy);
            floorGroups.add(group);

            totalHeight += (gMaxPy - gMinPy);
        }

        // Ajouter l'espacement entre groupes
        if (floorGroups.size() > 1) {
            totalHeight += GROUP_SPACING * (floorGroups.size() - 1);
        }

        computedHeight = totalHeight + PADDING_BOTTOM + paddingY;
    }

    public static MultiblockSection fromJson(JsonObject json) {
        String pattern = json.has("pattern") ? json.get("pattern").getAsString() : "";
        String controller = json.has("controller") ? json.get("controller").getAsString() : "";
        int paddingY = json.has("padding_y") ? json.get("padding_y").getAsInt() : 0;
        int groundOffsetY = json.has("ground_offset_y") ? json.get("ground_offset_y").getAsInt() : 0;
        return new MultiblockSection(pattern, controller, paddingY, groundOffsetY);
    }

    private static class FloorGroup {
        final int gridY;
        final List<DisplayElement> elements;
        final int minPx, maxPx, minPy, maxPy;

        FloorGroup(int gridY, List<DisplayElement> elements,
                   int minPx, int maxPx, int minPy, int maxPy) {
            this.gridY = gridY;
            this.elements = elements;
            this.minPx = minPx;
            this.maxPx = maxPx;
            this.minPy = minPy;
            this.maxPy = maxPy;
        }
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
