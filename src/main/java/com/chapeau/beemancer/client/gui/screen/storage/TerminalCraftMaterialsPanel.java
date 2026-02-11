/**
 * ============================================================
 * [TerminalCraftMaterialsPanel.java]
 * Description: Panneau des materiaux requis pour un craft dans le terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation              |
 * |--------------------------|----------------------|--------------------------|
 * | CraftableRecipe          | Donnees recette      | Ingredients a afficher   |
 * | StorageTerminalMenu      | Items reseau         | Disponibilite stock      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - TerminalRequestPopup.java (composition, affichage materiaux)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.common.data.CraftableRecipe;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Panneau affiche a droite du popup request quand craftCount > 0.
 * Liste les ingredients requis, leur disponibilite, et les manquants.
 */
public class TerminalCraftMaterialsPanel {

    private static final int PANEL_WIDTH = 110;
    private static final int LINE_HEIGHT = 14;
    private static final int MAX_VISIBLE = 4;

    private CraftableRecipe recipe;
    private int craftCount;
    private int scrollOffset = 0;

    public void update(CraftableRecipe recipe, int craftCount) {
        this.recipe = recipe;
        this.craftCount = craftCount;
        this.scrollOffset = 0;
    }

    public void clear() {
        this.recipe = null;
        this.craftCount = 0;
        this.scrollOffset = 0;
    }

    public boolean isActive() {
        return recipe != null && craftCount > 0;
    }

    /**
     * Rend le panneau des materiaux a la position donnee.
     */
    public void render(GuiGraphics g, Font font, int panelX, int panelY,
                       StorageTerminalMenu menu) {
        if (!isActive()) return;

        int resultPerCraft = recipe.result().getCount();
        int craftsNeeded = (craftCount + resultPerCraft - 1) / resultPerCraft;
        List<ItemStack> ingredients = recipe.ingredients();

        // Background
        int totalLines = ingredients.size();
        int visibleLines = Math.min(totalLines, MAX_VISIBLE);
        int panelHeight = 14 + visibleLines * LINE_HEIGHT + 4;
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + panelHeight + 1, 0xFF000000);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, 0xFF3C3C3C);

        // Title
        g.drawString(font, Component.translatable("gui.beemancer.terminal.materials"),
                panelX + 4, panelY + 3, 0xFFFFFF, false);

        // Ingredient lines
        int lineY = panelY + 14;
        for (int i = 0; i < visibleLines; i++) {
            int dataIdx = i + scrollOffset;
            if (dataIdx >= ingredients.size()) break;

            ItemStack ingredient = ingredients.get(dataIdx);
            int required = ingredient.getCount() * craftsNeeded;
            int available = getAvailableInNetwork(ingredient, menu);
            boolean isCraftable = menu.getCraftableRecipeFor(ingredient) != null;

            // Icon + name
            g.renderItem(ingredient, panelX + 2, lineY + i * LINE_HEIGHT - 1);
            String name = ingredient.getHoverName().getString();
            if (name.length() > 8) name = name.substring(0, 7) + "..";
            g.drawString(font, name, panelX + 20, lineY + i * LINE_HEIGHT + 1, 0xCCCCCC, false);

            // Required count (right-aligned)
            String reqStr = "x" + required;
            int reqW = font.width(reqStr);
            int countColor = available >= required ? 0xFF80FF80 : (isCraftable ? 0xFFFFFF80 : 0xFFFF8080);
            g.drawString(font, reqStr, panelX + PANEL_WIDTH - reqW - 4,
                    lineY + i * LINE_HEIGHT + 1, countColor, false);
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            g.drawString(font, "\u25B2", panelX + PANEL_WIDTH - 10, panelY + 3, 0x808080, false);
        }
        if (scrollOffset + MAX_VISIBLE < totalLines) {
            g.drawString(font, "\u25BC", panelX + PANEL_WIDTH - 10,
                    panelY + panelHeight - 10, 0x808080, false);
        }
    }

    /**
     * Verifie si tous les materiaux sont disponibles (en stock ou craftables).
     */
    public boolean canLaunchCraft(StorageTerminalMenu menu) {
        if (!isActive()) return false;

        int resultPerCraft = recipe.result().getCount();
        int craftsNeeded = (craftCount + resultPerCraft - 1) / resultPerCraft;

        for (ItemStack ingredient : recipe.ingredients()) {
            int required = ingredient.getCount() * craftsNeeded;
            int available = getAvailableInNetwork(ingredient, menu);
            if (available < required) {
                // Check if the missing ingredient is itself craftable
                boolean subCraftable = menu.getCraftableRecipeFor(ingredient) != null;
                if (!subCraftable) return false;
            }
        }
        return true;
    }

    public boolean handleScroll(double scrollY) {
        if (recipe == null) return false;
        int maxScroll = Math.max(0, recipe.ingredients().size() - MAX_VISIBLE);
        if (maxScroll <= 0) return false;
        scrollOffset = Mth.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
        return true;
    }

    private int getAvailableInNetwork(ItemStack ingredient, StorageTerminalMenu menu) {
        for (ItemStack s : menu.getAggregatedItems()) {
            if (ItemStack.isSameItemSameComponents(s, ingredient)) {
                return s.getCount();
            }
        }
        return 0;
    }
}
