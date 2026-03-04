/**
 * ============================================================
 * [BackpackScreen.java]
 * Description: Ecran du backpack — texture vanilla coffre simple
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackMenu        | Donnees container    | Menu reference                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.common.menu.BackpackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran du backpack utilisant la texture vanilla du coffre simple (3 rangees).
 * Layout identique au coffre vanilla: 27 slots container + inventaire joueur.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    /** Texture vanilla du coffre (generic_54 gere 1 a 6 rangees). */
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int ROWS = 3;

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
        renderTooltip(graphics, mouseX, mouseY);
    }
}
