/**
 * ============================================================
 * [IncubatorScreen.java]
 * Description: GUI de l'incubateur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IncubatorMenu       | Donnees container    | Larva slot, progress           |
 * | GuiRenderHelper     | Rendu programmatique | Background, slot, progress     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
import com.chapeau.beemancer.common.menu.IncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class IncubatorScreen extends AbstractContainerScreen<IncubatorMenu> {
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);

    public IncubatorScreen(IncubatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, imageWidth, 76);
        g.drawString(font, Component.translatable("container.beemancer.incubator"),
            x + 8, y + 6, 0x404040, false);

        // Larva slot (80, 35)
        GuiRenderHelper.renderSlot(g, x + 79, y + 34);

        // Progress arrow
        GuiRenderHelper.renderProgressArrow(g, x + 100, y + 35, menu.getProgressRatio());

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
