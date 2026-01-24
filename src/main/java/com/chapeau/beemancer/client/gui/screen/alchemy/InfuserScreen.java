/**
 * ============================================================
 * [InfuserScreen.java]
 * Description: GUI pour l'infuseur avec jauge de fluide amelioree
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

public class InfuserScreen extends AbstractContainerScreen<InfuserMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/infuser.png");

    private FluidGaugeWidget honeyGauge;

    public InfuserScreen(InfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Initialiser la jauge de miel
        honeyGauge = new FluidGaugeWidget(
            17, 17,     // Position relative au GUI
            16, 52,     // Dimensions
            4000,       // Capacite
            () -> menu.getBlockEntity().getHoneyTank().getFluid(),
            menu::getHoneyAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Honey tank avec le nouveau widget
        honeyGauge.render(guiGraphics, x, y);

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

        // Honey tank tooltip
        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
