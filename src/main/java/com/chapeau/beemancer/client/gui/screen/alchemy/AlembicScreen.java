/**
 * ============================================================
 * [AlembicScreen.java]
 * Description: GUI pour l'alambic avec jauges de fluide ameliorees
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlembicScreen extends AbstractContainerScreen<AlembicMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/alembic.png");

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

        // Initialiser les jauges de fluide
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fluid tanks avec les nouveaux widgets
        honeyGauge.render(guiGraphics, x, y);
        royalJellyGauge.render(guiGraphics, x, y);
        nectarGauge.render(guiGraphics, x, y);

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

        // Tooltips pour chaque jauge
        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
        if (royalJellyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, royalJellyGauge.getTooltip("Royal Jelly"), mouseX, mouseY);
        }
        if (nectarGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, nectarGauge.getTooltip("Nectar"), mouseX, mouseY);
        }
    }
}
