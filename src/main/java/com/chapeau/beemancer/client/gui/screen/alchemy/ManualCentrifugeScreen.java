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
        Beemancer.MOD_ID, "textures/gui/bg.png");
    private FluidGaugeWidget fluidGauge;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.manual_centrifuge"; }

    @Override
    protected void init() {
        super.init();
        fluidGauge = new FluidGaugeWidget(
            159, 27, 16, 52, 4000,
            () -> menu.getBlockEntity().getFluidTank().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 39, y + 44);
        GuiRenderHelper.renderSlots2x2(g, x + 115, y + 35);
        GuiRenderHelper.renderProgressBar(g, x + 61, y + 50, 50, 6, menu.getProgressRatio());
        fluidGauge.render(g, x, y);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (fluidGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getFluidTank().getFluid());
            g.renderComponentTooltip(font, fluidGauge.getTooltip(name), mouseX, mouseY);
        }
        float ratio = menu.getProgressRatio();
        if (ratio > 0 && mouseX >= x + 72 && mouseX < x + 108 && mouseY >= y + 45 && mouseY < y + 62) {
            g.renderTooltip(font, Component.literal(String.format("%.0f%%", ratio * 100)), mouseX, mouseY);
        }
    }
}
