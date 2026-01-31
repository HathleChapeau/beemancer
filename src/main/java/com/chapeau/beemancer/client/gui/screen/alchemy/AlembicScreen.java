/**
 * ============================================================
 * [AlembicScreen.java]
 * Description: GUI pour l'alambic (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AlembicMenu         | Donnees container    | Progress, fluid levels         |
 * | FluidGaugeWidget    | Jauges de fluide     | 3 tanks (honey, RJ, nectar)    |
 * | GuiRenderHelper     | Rendu programmatique | Background, progress           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AlembicScreen extends AbstractContainerScreen<AlembicMenu> {
    private FluidGaugeWidget honeyGauge;
    private FluidGaugeWidget royalJellyGauge;
    private FluidGaugeWidget nectarGauge;

    public AlembicScreen(AlembicMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        honeyGauge = new FluidGaugeWidget(
            17, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getHoneyTank().getFluid(),
            menu::getHoneyAmount
        );
        royalJellyGauge = new FluidGaugeWidget(
            44, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getRoyalJellyTank().getFluid(),
            menu::getRoyalJellyAmount
        );
        nectarGauge = new FluidGaugeWidget(
            143, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getNectarTank().getFluid(),
            menu::getNectarAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        GuiRenderHelper.renderContainerBackground(g, font, x, y, imageWidth, imageHeight,
            "container.beemancer.alembic", 79);

        // Fluid gauges
        honeyGauge.render(g, x, y);
        royalJellyGauge.render(g, x, y);
        nectarGauge.render(g, x, y);

        // Progress arrow
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressArrow(g, x + 68, y + 35, ratio);

        // Player inventory
        GuiRenderHelper.renderPlayerInventory(g, x, y, 83, 141);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
        if (royalJellyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, royalJellyGauge.getTooltip("Royal Jelly"), mouseX, mouseY);
        }
        if (nectarGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, nectarGauge.getTooltip("Nectar"), mouseX, mouseY);
        }
    }
}
