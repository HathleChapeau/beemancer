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
 * | AbstractApicaScreen | Base screen          | Boilerplate GUI                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.alchemy;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.client.gui.screen.AbstractApicaScreen;
import com.chapeau.apica.client.gui.widget.FluidGaugeWidget;
import com.chapeau.apica.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredCentrifugeScreen extends AbstractApicaScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/bg.png");
    private FluidGaugeWidget fuelGauge;
    private FluidGaugeWidget outputGauge;

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.powered_centrifuge"; }

    @Override
    protected void init() {
        super.init();
        fuelGauge = new FluidGaugeWidget(
            15, 27, 16, 52, menu::getFuelCapacity,
            () -> menu.getFuelTank().getFluid(),
            menu::getFuelAmount
        );
        outputGauge = new FluidGaugeWidget(
            159, 27, 16, 52, menu::getOutputCapacity,
            () -> menu.getOutputTank().getFluid(),
            menu::getOutputAmount
        );
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 39, y + 44);
        GuiRenderHelper.renderSlots2x2(g, x + 115, y + 35);
        GuiRenderHelper.renderTextureProgressBar(g, x + 61, y + 48, menu.getProgressRatio());
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
