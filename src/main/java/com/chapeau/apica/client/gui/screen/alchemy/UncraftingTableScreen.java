/**
 * ============================================================
 * [UncraftingTableScreen.java]
 * Description: GUI pour l'Uncrafting Table avec texture background et preview ghost items
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | UncraftingTableMenu         | Donnees container    | Slots, progress, fluid         |
 * | GuiRenderHelper             | Rendu barres         | Nectar bar, tooltips           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.alchemy;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.chapeau.apica.common.menu.alchemy.UncraftingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class UncraftingTableScreen extends AbstractContainerScreen<UncraftingTableMenu> {

    /** Texture de fond du GUI (176x166). */
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/uncrafting_table.png");

    /** Texture du four vanilla pour la fleche vide (background). */
    private static final ResourceLocation FURNACE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    /** Sprite vanilla pour le remplissage de la fleche de progression (1.21+). */
    private static final ResourceLocation BURN_PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** Positions nectar bar relative au container. */
    private static final int NECTAR_BAR_X = 10;
    private static final int NECTAR_BAR_Y = 18;

    /** Cache du reverse-recipe lookup cote client (pour ghost items). */
    private ItemStack lastInput = ItemStack.EMPTY;
    private NonNullList<ItemStack> ghostItems;

    public UncraftingTableScreen(UncraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Fond via texture PNG (taille reelle 176x166)
        g.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        // Slot input
        drawSlot(g, x + 47, y + 34);

        // Slots output 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(g, x + 105 + col * 18, y + 16 + row * 18);
            }
        }

        // Slots inventaire joueur (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, x + 7 + col * 18, y + 83 + row * 18);
            }
        }

        // Slots hotbar
        for (int col = 0; col < 9; col++) {
            drawSlot(g, x + 7 + col * 18, y + 141);
        }

        // Barre de nectar
        int cap = menu.getFluidCapacity();
        float ratio = cap > 0 ? (float) menu.getFluidAmount() / cap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + NECTAR_BAR_X, y + NECTAR_BAR_Y, ratio);

        // Fleche de progression vanilla (extraite de la texture du four)
        renderVanillaArrow(g, x + 73, y + 35);
    }

    /**
     * Dessine un slot vanilla: bordure sombre en haut-gauche, claire en bas-droite, fond gris.
     */
    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 18, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Labels rendus une seule fois via la methode par defaut de AbstractContainerScreen
        super.renderLabels(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        // Ghost items (preview oscillant pendant le process)
        updateGhostItems();
        renderGhostItems(g);

        renderTooltip(g, mouseX, mouseY);

        // Tooltip nectar bar
        int x = leftPos;
        int y = topPos;
        if (GuiRenderHelper.isHoneyBarHovered(NECTAR_BAR_X, NECTAR_BAR_Y, x, y, mouseX, mouseY)) {
            int amount = menu.getFluidAmount();
            int cap = menu.getFluidCapacity();
            FluidStack tankFluid = menu.getNectarTank().getFluid();
            String name = tankFluid.isEmpty() ? "Nectar" : GuiRenderHelper.getFluidName(tankFluid);
            g.renderComponentTooltip(font, List.of(
                    Component.literal(name + ": " + amount + " / " + cap + " mB"),
                    Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                            .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }
    }

    /**
     * Fleche de four vanilla: fond vide puis remplissage proportionnel au progres.
     * UV extraits de minecraft:textures/gui/container/furnace.png.
     */
    private void renderVanillaArrow(GuiGraphics g, int x, int y) {
        // Fleche vide (arriere-plan) — UV (79, 35) dans la texture du four
        g.blit(FURNACE_TEXTURE, x, y, 79, 35, 24, 17);

        // Remplissage proportionnel via sprite vanilla (1.21+)
        int progressWidth = (int) (menu.getProgressRatio() * 24);
        if (progressWidth > 0) {
            g.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x, y, progressWidth + 1, 16);
        }
    }

    /**
     * Met a jour le cache des ghost items en faisant un reverse recipe lookup cote client.
     * Ne recalcule que si l'input change.
     */
    private void updateGhostItems() {
        ItemStack input = menu.getSlot(0).getItem();

        if (input.isEmpty()) {
            ghostItems = null;
            lastInput = ItemStack.EMPTY;
            return;
        }

        if (!ItemStack.isSameItem(input, lastInput) || input.getCount() != lastInput.getCount()) {
            lastInput = input.copy();
            ghostItems = findIngredients(input);
        }
    }

    /**
     * Rend les ghost items dans les output slots avec un overlay de slot par dessus.
     * L'overlay pulse entre 0.1 et 0.2 d'opacite pendant le processing.
     * Opacite 0 quand pas d'item en input ou craft fini.
     */
    private void renderGhostItems(GuiGraphics g) {
        if (ghostItems == null || menu.getProgress() <= 0) return;
        if (!outputSlotsEmpty()) return;

        // Rendu des ghost items a opacite pleine
        for (int i = 0; i < 9; i++) {
            ItemStack ghost = ghostItems.get(i);
            if (ghost.isEmpty()) continue;

            int gx = leftPos + 106 + (i % 3) * 18;
            int gy = topPos + 17 + (i / 3) * 18;
            g.renderItem(ghost, gx, gy);
        }

        // Overlay slot bg par dessus avec opacite oscillante (0.1 - 0.2)
        float t = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.0020);
        float alpha = 0.2f + 0.25f * t;
        int color = ((int) (alpha * 255) << 24) | 0x8B8B8B;

        g.pose().pushPose();
        g.pose().translate(0, 0, 300);

        for (int i = 0; i < 9; i++) {
            if (ghostItems.get(i).isEmpty()) continue;

            int gx = leftPos + 106 + (i % 3) * 18;
            int gy = topPos + 17 + (i / 3) * 18;
            g.fill(gx + 1, gy + 1, gx + 17, gy + 17, color);
        }

        g.pose().popPose();
    }

    private boolean outputSlotsEmpty() {
        for (int i = 1; i <= 9; i++) {
            if (!menu.getSlot(i).getItem().isEmpty()) return false;
        }
        return true;
    }

    /**
     * Reverse recipe lookup: trouve les ingredients d'un item via le RecipeManager.
     */
    private NonNullList<ItemStack> findIngredients(ItemStack target) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;

        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (ItemStack.isSameItem(result, target) && target.getCount() >= result.getCount()) {
                NonNullList<Ingredient> ingredients = recipe.getIngredients();
                NonNullList<ItemStack> resolved = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < ingredients.size() && i < 9; i++) {
                    ItemStack[] items = ingredients.get(i).getItems();
                    if (items.length > 0) {
                        resolved.set(i, items[0].copy());
                    }
                }
                return resolved;
            }
        }
        return null;
    }
}
