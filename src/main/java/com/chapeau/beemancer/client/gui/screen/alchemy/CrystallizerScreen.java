/**
 * ============================================================
 * [CrystallizerScreen.java]
 * Description: GUI pour le cristalliseur avec jauge de fluide amelioree
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.CrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/crystallizer.png");

    private FluidGaugeWidget inputGauge;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Initialiser la jauge d'input
        inputGauge = new FluidGaugeWidget(
            26, 17,     // Position relative au GUI
            16, 52,     // Dimensions
            4000,       // Capacite
            () -> menu.getBlockEntity().getInputTank().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Input fluid tank avec le nouveau widget
        inputGauge.render(guiGraphics, x, y);

        // Progress bar
        int processTime = menu.getProcessTime();
        if (processTime > 0) {
            int progress = menu.getProgress();
            float ratio = (float) progress / processTime;

            int barX = x + 52;
            int barY = y + 39;
            int barWidth = 56;
            int barHeight = 6;

            // Background (dark)
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF1A1A1A);
            // Border
            guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFF3A3A3A);
            guiGraphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFF3A3A3A);
            guiGraphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFF3A3A3A);
            guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFF3A3A3A);

            // Fill (honey gold gradient)
            int fillWidth = (int) (barWidth * ratio);
            if (fillWidth > 0) {
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFD4A017);
                // Highlight top pixel
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + 1, 0xFFE8B830);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Fluid tank tooltip
        if (inputGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String fluidName = getFluidName();
            guiGraphics.renderComponentTooltip(font, inputGauge.getTooltip(fluidName), mouseX, mouseY);
        }
    }

    private String getFluidName() {
        var fluid = menu.getBlockEntity().getInputTank().getFluid();
        if (fluid.isEmpty()) return "Empty";
        String path = fluid.getFluid().builtInRegistryHolder().key().location().getPath();
        if (path.contains("honey")) return "Honey";
        if (path.contains("royal_jelly")) return "Royal Jelly";
        return "Fluid";
    }
}
