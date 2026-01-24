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

        // Progress arrow
        int processTime = menu.getProcessTime();
        if (processTime > 0) {
            int progress = menu.getProgress();
            int arrowWidth = (int) (24 * ((float) progress / processTime));
            if (arrowWidth > 0) {
                guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 52, arrowWidth, 17);
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
