/**
 * ============================================================
 * [HoneyTankScreen.java]
 * Description: GUI simple pour le tank de miel avec jauge amelioree
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.HoneyTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HoneyTankScreen extends AbstractContainerScreen<HoneyTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/honey_tank.png");

    private FluidGaugeWidget storageGauge;

    public HoneyTankScreen(HoneyTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Grande jauge de stockage (centre)
        storageGauge = new FluidGaugeWidget(
                62, 17, 52, 52, 16000,
                () -> menu.getBlockEntity().getFluid(),
                menu::getFluidAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Tank avec le widget
        storageGauge.render(guiGraphics, x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Tooltip pour la jauge
        if (storageGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, storageGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
