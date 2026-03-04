/**
 * ============================================================
 * [BackpackScreen.java]
 * Description: Ecran du backpack — texture vanilla coffre simple + tabs navigation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackMenu        | Donnees container    | Menu reference                 |
 * | AccessoryClientCache| Icone backpack       | Tab icon rendering             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.AccessoryClientCache;
import com.chapeau.apica.common.menu.BackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ecran du backpack utilisant la texture vanilla du coffre simple (3 rangees).
 * Affiche 2 tabs en haut: Player (cliquable → retour inventaire) et Backpack (selectionne).
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    /** Texture vanilla du coffre (generic_54 gere 1 a 6 rangees). */
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int ROWS = 3;

    // Tab sprites (vanilla advancement above-type tabs)
    private static final ResourceLocation TAB_SELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left_selected");
    private static final ResourceLocation TAB_UNSELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left");
    private static final ResourceLocation TAB_SELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle_selected");
    private static final ResourceLocation TAB_UNSELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle");

    private static final int TAB_W = 28;
    private static final int TAB_H = 32;
    private static final int TAB_PROTRUDE = 28;

    private static ItemStack playerIcon;
    private static ItemStack getPlayerIcon() {
        if (playerIcon == null) playerIcon = new ItemStack(Items.CRAFTING_TABLE);
        return playerIcon;
    }

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Top part: titre + rangees de slots container
        graphics.blit(CHEST_TEXTURE, x, y, 0, 0, imageWidth, ROWS * 18 + 17);

        // Bottom part: inventaire joueur (toujours a l'offset 126 dans la texture)
        graphics.blit(CHEST_TEXTURE, x, y + ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // --- Tabs ---
        int tabY = topPos - TAB_PROTRUDE;
        int tab0X = leftPos + 4;
        int tab1X = tab0X + TAB_W;

        // Player tab (unselected — we're on BackpackScreen)
        graphics.blitSprite(TAB_UNSELECTED_LEFT, tab0X, tabY, TAB_W, TAB_H);
        graphics.renderItem(getPlayerIcon(), tab0X + 6, tabY + 9);

        // Backpack tab (selected)
        graphics.blitSprite(TAB_SELECTED_MIDDLE, tab1X, tabY, TAB_W, TAB_H);
        int bpSlot = AccessoryClientCache.findBackpackSlot();
        if (bpSlot >= 0) {
            graphics.renderItem(AccessoryClientCache.getSlot(bpSlot), tab1X + 6, tabY + 9);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tabY = topPos - TAB_PROTRUDE;
            int tab0X = leftPos + 4;

            // Click on Player tab → close backpack, open inventory
            if (mouseX >= tab0X && mouseX < tab0X + TAB_W
                    && mouseY >= tabY && mouseY < tabY + TAB_H) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.closeContainer();
                    mc.setScreen(new InventoryScreen(mc.player));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
