/**
 * ============================================================
 * [MultiblockCategory.java]
 * Description: Categorie JEI pour afficher les structures multiblocs en vue isometrique 3D
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | JEI API             | Interface categorie  | IRecipeCategory, draw          |
 * | MultiblockInfo      | Donnees multibloc    | Pattern et blocs               |
 * | MultiblockPattern   | Elements structure   | Positions isometriques         |
 * | BlockMatcher        | Acces blocs display  | Recuperation des blocs         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaJeiPlugin (enregistrement categorie)
 *
 * Affiche une vue isometrique 3D du multibloc (comme dans le Codex Book).
 * Accessible via "U" sur n'importe quel bloc du multibloc.
 *
 * ============================================================
 */
package com.chapeau.apica.compat.jei.category;

import com.chapeau.apica.Apica;
import com.chapeau.apica.compat.jei.ApicaJeiRecipeTypes;
import com.chapeau.apica.core.multiblock.BlockMatcher;
import com.chapeau.apica.core.multiblock.MultiblockPattern;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MultiblockCategory implements IRecipeCategory<MultiblockInfo> {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 130;
    private static final int TEXT_COLOR = 0x404040;

    // Isometric rendering constants (from MultiblockSection)
    private static final float ITEM_SCALE = 0.8f;
    private static final int SPACING_X = 9;
    private static final int SPACING_Z = 6;
    private static final int ITEM_SIZE = 16;
    private static final float Y_OFFSET_MULTIPLIER = 0.7f;

    // Ground texture
    private static final ResourceLocation GROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/codex_book/down_multibloc.png");
    private static final int GROUND_TEX_W = 80;
    private static final int GROUND_TEX_H = 44;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public MultiblockCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ApicaBlocks.HONEY_RESERVOIR.get().asItem().getDefaultInstance());
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
        // No slots - we render items directly in draw()
    }

    @Override
    public void draw(MultiblockInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        MultiblockPattern pattern = recipe.pattern();
        if (pattern == null) return;

        // Collect elements grouped by floor (Y level)
        TreeMap<Integer, List<DisplayElement>> byFloor = new TreeMap<>();
        ItemStack airStack = new ItemStack(ApicaBlocks.AIR_PLACEHOLDER.get().asItem());

        // Add controller at origin
        byFloor.computeIfAbsent(0, k -> new ArrayList<>())
                .add(new DisplayElement(0, 0, 0, recipe.controllerStack(), false, 0, 0));

        // Add pattern elements
        for (MultiblockPattern.PatternElement element : pattern.getElements()) {
            Vec3i offset = element.offset();
            boolean isAir = BlockMatcher.isAirMatcher(element.matcher());

            if (isAir) {
                byFloor.computeIfAbsent(offset.getY(), k -> new ArrayList<>())
                        .add(new DisplayElement(
                                offset.getX(), offset.getY(), offset.getZ(),
                                airStack, true, 0, 0));
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

        // Calculate isometric positions and bounds
        int scaledItem = Math.round(ITEM_SIZE * ITEM_SCALE);
        List<FloorGroup> floorGroups = new ArrayList<>();

        int globalMinPx = Integer.MAX_VALUE, globalMaxPx = Integer.MIN_VALUE;
        int globalMinPy = Integer.MAX_VALUE, globalMaxPy = Integer.MIN_VALUE;

        for (Map.Entry<Integer, List<DisplayElement>> entry : byFloor.descendingMap().entrySet()) {
            List<DisplayElement> elements = entry.getValue();
            List<DisplayElement> positioned = new ArrayList<>();

            int gMinPx = Integer.MAX_VALUE, gMaxPx = Integer.MIN_VALUE;
            int gMinPy = Integer.MAX_VALUE, gMaxPy = Integer.MIN_VALUE;

            for (DisplayElement elem : elements) {
                int px = (elem.gridX - elem.gridZ) * SPACING_X;
                int py = (elem.gridX + elem.gridZ) * SPACING_Z;
                positioned.add(new DisplayElement(
                        elem.gridX, elem.gridY, elem.gridZ,
                        elem.stack, elem.isAir, px, py));

                gMinPx = Math.min(gMinPx, px);
                gMaxPx = Math.max(gMaxPx, px + scaledItem);
                gMinPy = Math.min(gMinPy, py);
                gMaxPy = Math.max(gMaxPy, py + scaledItem);
            }

            positioned.sort(Comparator.comparingInt(e -> e.py));
            floorGroups.add(new FloorGroup(entry.getKey(), positioned, gMinPx, gMaxPx, gMinPy, gMaxPy));

            globalMinPx = Math.min(globalMinPx, gMinPx);
            globalMaxPx = Math.max(globalMaxPx, gMaxPx);
            globalMinPy = Math.min(globalMinPy, gMinPy);
            globalMaxPy = Math.max(globalMaxPy, gMaxPy);
        }

        if (floorGroups.isEmpty()) return;

        // Calculate rendering position - anchor ground at fixed Y position
        int floorHeight = globalMaxPy - globalMinPy;
        int centerX = WIDTH / 2 - (globalMinPx + globalMaxPx) / 2;
        int groundBaseY = HEIGHT - 60;  // Fixed ground position (leaves room for text)
        int startY = groundBaseY;

        // Draw ground texture
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int groundX = WIDTH / 2 - GROUND_TEX_W / 2;
        int groundY = groundBaseY - GROUND_TEX_H + 35 - globalMinPy;
        guiGraphics.blit(GROUND_TEXTURE, groundX, groundY, GROUND_TEX_W, GROUND_TEX_H,
                0, 0, GROUND_TEX_W, GROUND_TEX_H, GROUND_TEX_W, GROUND_TEX_H);

        // Render floors from bottom to top
        int currentY = startY + 5;
        float zOffset = 0;

        for (int g = floorGroups.size() - 1; g >= 0; g--) {
            FloorGroup group = floorGroups.get(g);
            int originY = currentY - globalMinPy;

            for (DisplayElement elem : group.elements) {
                int drawX = centerX + elem.px;
                int drawY = originY + (int)(elem.py * Y_OFFSET_MULTIPLIER);

                renderScaledItem(guiGraphics, elem.stack, drawX, drawY, ITEM_SCALE, zOffset);
                zOffset += 10.0f;
            }

            currentY -= floorHeight - (int)(15 * (2 - Y_OFFSET_MULTIPLIER));
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw multiblock name at bottom
        Component name = Component.translatable(recipe.name());
        guiGraphics.drawString(font, name, 5, HEIGHT - 12, TEXT_COLOR, false);
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

    private record DisplayElement(int gridX, int gridY, int gridZ, ItemStack stack,
                                   boolean isAir, int px, int py) {}

    private record FloorGroup(int gridY, List<DisplayElement> elements,
                               int minPx, int maxPx, int minPy, int maxPy) {}
}
