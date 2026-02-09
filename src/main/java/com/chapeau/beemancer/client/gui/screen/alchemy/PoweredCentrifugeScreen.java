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
 * | AbstractBeemancerScreen | Base screen          | Boilerplate GUI                |
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
import com.chapeau.beemancer.client.gui.screen.AbstractBeemancerScreen;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredCentrifugeScreen extends AbstractBeemancerScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private FluidGaugeWidget fuelGauge;
    private FluidGaugeWidget outputGauge;

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 80);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.powered_centrifuge"; }

    @Override
    protected void init() {
        super.init();
        fuelGauge = new FluidGaugeWidget(
            8, 17, 16, 52, menu::getFuelCapacity,
            () -> menu.getFuelTank().getFluid(),
            menu::getFuelAmount
        );
        outputGauge = new FluidGaugeWidget(
            152, 17, 16, 52, menu::getOutputCapacity,
            () -> menu.getOutputTank().getFluid(),
            menu::getOutputAmount
        );
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 32, y + 34);
        GuiRenderHelper.renderSlot(g, x + 108, y + 25);
        GuiRenderHelper.renderSlot(g, x + 126, y + 25);
        GuiRenderHelper.renderSlot(g, x + 108, y + 43);
        GuiRenderHelper.renderSlot(g, x + 126, y + 43);
        GuiRenderHelper.renderProgressBar(g, x + 54, y + 40, 50, 6, menu.getProgressRatio());
        fuelGauge.render(g, x, y);
        outputGauge.render(g, x, y);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (fuelGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, fuelGauge.getTooltip("Fuel"), mouseX, mouseY);
        }
        if (outputGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getOutputTank().getFluid());
            g.renderComponentTooltip(font, outputGauge.getTooltip(name), mouseX, mouseY);
        }
    }
}
