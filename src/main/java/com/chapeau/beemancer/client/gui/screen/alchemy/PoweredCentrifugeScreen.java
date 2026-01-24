/**
 * ============================================================
 * [PoweredCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse automatique avec jauges ameliorees
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredCentrifugeScreen extends AbstractContainerScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/powered_centrifuge.png");

    private FluidGaugeWidget fuelGauge;
    private FluidGaugeWidget outputGauge;

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Jauge de fuel (gauche)
        fuelGauge = new FluidGaugeWidget(
            8, 17, 16, 52, 8000,
            () -> menu.getBlockEntity().getFuelTank().getFluid(),
            menu::getFuelAmount
        );

        // Jauge de sortie (droite)
        outputGauge = new FluidGaugeWidget(
            152, 17, 16, 52, 8000,
            () -> menu.getBlockEntity().getOutputTank().getFluid(),
            menu::getOutputAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fluid tanks avec les widgets
        fuelGauge.render(guiGraphics, x, y);
        outputGauge.render(guiGraphics, x, y);

        // Progress arrow (center)
        float progress = menu.getProgressRatio();
        if (progress > 0) {
            int progressWidth = (int) (24 * progress);
            guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 0, progressWidth, 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Tooltips pour les jauges
        if (fuelGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, fuelGauge.getTooltip("Fuel"), mouseX, mouseY);
        }
        if (outputGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, outputGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
