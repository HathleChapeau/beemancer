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

    /** Texture du four vanilla pour extraire la fleche de progression. */
    private static final ResourceLocation FURNACE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

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
        this.inventoryLabelX = 28;
        this.inventoryLabelY = 73;
        this.titleLabelX = 28;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Fond via texture PNG
        g.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // Barre de nectar
        int cap = menu.getFluidCapacity();
        float ratio = cap > 0 ? (float) menu.getFluidAmount() / cap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + NECTAR_BAR_X, y + NECTAR_BAR_Y, ratio);

        // Fleche de progression vanilla (extraite de la texture du four)
        renderVanillaArrow(g, x + 73, y + 35);
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
        // Fleche vide (arriere-plan) — UV (79, 35) dans la texture du four, taille 24x17
        g.blit(FURNACE_TEXTURE, x, y, 79, 35, 24, 17);

        // Remplissage proportionnel — UV (176, 14) dans la texture du four
        int progressWidth = (int) (menu.getProgressRatio() * 24);
        if (progressWidth > 0) {
            g.blit(FURNACE_TEXTURE, x, y, 176, 14, progressWidth + 1, 16);
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

        if (!ItemStack.isSameItem(input, lastInput)) {
            lastInput = input.copy();
            ghostItems = findIngredients(input);
        }
    }

    /**
     * Rend les ghost items avec opacite oscillante dans les output slots.
     * Visible uniquement pendant le process (progress > 0, outputs vides).
     */
    private void renderGhostItems(GuiGraphics g) {
        if (ghostItems == null || menu.getProgress() <= 0) return;
        if (!outputSlotsEmpty()) return;

        float alpha = 0.3f + 0.35f * (float) Math.sin(System.currentTimeMillis() * 0.004);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        for (int i = 0; i < 9; i++) {
            ItemStack ghost = ghostItems.get(i);
            if (ghost.isEmpty()) continue;

            int gx = leftPos + 106 + (i % 3) * 18;
            int gy = topPos + 17 + (i / 3) * 18;
            g.renderItem(ghost, gx, gy);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
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
            if (ItemStack.isSameItem(result, target)) {
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
