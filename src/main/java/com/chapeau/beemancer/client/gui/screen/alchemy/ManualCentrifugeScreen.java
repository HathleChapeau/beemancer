/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle avec jauge amelioree
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManualCentrifugeScreen extends AbstractContainerScreen<ManualCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/manual_centrifuge.png");

    private FluidGaugeWidget fluidGauge;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Jauge de fluide (droite)
        fluidGauge = new FluidGaugeWidget(
            134, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getFluidTank().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fluid tank avec le widget
        fluidGauge.render(guiGraphics, x, y);

        // Spin progress (spins / 5)
        int spinProgress = (int) (24 * (menu.getSpinCount() / 5f));
        if (spinProgress > 0) {
            guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 52, spinProgress, 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Tooltip pour la jauge
        if (fluidGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, fluidGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
