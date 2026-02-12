/**
 * ============================================================
 * [InfuserScreen.java]
 * Description: GUI pour l'infuseur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InfuserMenu             | Donnees container    | Slots, progress, honey         |
 * | FluidGaugeWidget        | Jauge de fluide      | Honey tank                     |
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
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class InfuserScreen extends AbstractBeemancerScreen<InfuserMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg.png");
    private FluidGaugeWidget honeyGauge;

    public InfuserScreen(InfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.infuser"; }

    @Override
    protected void init() {
        super.init();
        honeyGauge = new FluidGaugeWidget(
            24, 27, 16, 52, 4000,
            () -> menu.getHoneyTank().getFluid(),
            menu::getHoneyAmount
        );
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 50, y + 44);
        GuiRenderHelper.renderSlot(g, x + 122, y + 44);
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressArrow(g, x + 75, y + 45, ratio);
        honeyGauge.render(g, x, y);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
