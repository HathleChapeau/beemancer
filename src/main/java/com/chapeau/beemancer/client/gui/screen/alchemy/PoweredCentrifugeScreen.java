/**
 * ============================================================
 * [PoweredCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse automatique (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PoweredCentrifugeMenu   | Donnees container    | Slots, progress, fluids        |
 * | FluidGaugeWidget        | Jauges de fluide     | Fuel et output tanks           |
 * | GuiRenderHelper         | Rendu programmatique | Background, slots, progress    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredCentrifugeScreen extends AbstractContainerScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/powered_centrifuge.png");
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);
    private FluidGaugeWidget fuelGauge;
    private FluidGaugeWidget outputGauge;

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        fuelGauge = new FluidGaugeWidget(
            8, 17, 16, 52, 8000,
            () -> menu.getBlockEntity().getFuelTank().getFluid(),
            menu::getFuelAmount
        );
        outputGauge = new FluidGaugeWidget(
            152, 17, 16, 52, 8000,
            () -> menu.getBlockEntity().getOutputTank().getFluid(),
            menu::getOutputAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 76, 176, 76);
        g.drawString(font, Component.translatable("container.beemancer.powered_centrifuge"),
            x + 8, y + 6, 0x404040, false);

        // Input slot (44, 35)
        GuiRenderHelper.renderSlot(g, x + 43, y + 34);

        // Output slots 2x2
        GuiRenderHelper.renderSlot(g, x + 106, y + 25);
        GuiRenderHelper.renderSlot(g, x + 124, y + 25);
        GuiRenderHelper.renderSlot(g, x + 106, y + 43);
        GuiRenderHelper.renderSlot(g, x + 124, y + 43);

        // Progress arrow
        GuiRenderHelper.renderProgressArrow(g, x + 65, y + 35, menu.getProgressRatio());

        // Fluid gauges
        fuelGauge.render(g, x, y);
        outputGauge.render(g, x, y);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (fuelGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, fuelGauge.getTooltip("Fuel"), mouseX, mouseY);
        }
        if (outputGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getOutputTank().getFluid());
            g.renderComponentTooltip(font, outputGauge.getTooltip(name), mouseX, mouseY);
        }
    }
}
