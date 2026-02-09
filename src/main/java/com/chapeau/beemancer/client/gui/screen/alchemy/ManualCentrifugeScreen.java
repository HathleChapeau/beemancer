/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ManualCentrifugeMenu    | Donnees container    | Slots, progress, fluid         |
 * | FluidGaugeWidget        | Jauge de fluide      | Affichage tank                 |
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
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManualCentrifugeScreen extends AbstractBeemancerScreen<ManualCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private FluidGaugeWidget fluidGauge;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 80);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.manual_centrifuge"; }

    @Override
    protected void init() {
        super.init();
        fluidGauge = new FluidGaugeWidget(
            152, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getFluidTank().getFluid(),
            menu::getFluidAmount
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
        fluidGauge.render(g, x, y);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (fluidGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getFluidTank().getFluid());
            g.renderComponentTooltip(font, fluidGauge.getTooltip(name), mouseX, mouseY);
        }
        float ratio = menu.getProgressRatio();
        if (ratio > 0 && mouseX >= x + 65 && mouseX < x + 101 && mouseY >= y + 35 && mouseY < y + 52) {
            g.renderTooltip(font, Component.literal(String.format("%.0f%%", ratio * 100)), mouseX, mouseY);
        }
    }
}
