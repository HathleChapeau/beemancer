/**
 * ============================================================
 * [AlembicScreen.java]
 * Description: GUI pour l'alambic (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AlembicMenu             | Donnees container    | Progress, fluid levels         |
 * | FluidGaugeWidget        | Jauges de fluide     | 3 tanks (honey, RJ, nectar)    |
 * | GuiRenderHelper         | Rendu programmatique | Background, progress           |
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
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlembicScreen extends AbstractBeemancerScreen<AlembicMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private FluidGaugeWidget honeyGauge;
    private FluidGaugeWidget royalJellyGauge;
    private FluidGaugeWidget nectarGauge;

    public AlembicScreen(AlembicMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 80);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.alembic"; }

    @Override
    protected void init() {
        super.init();
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
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        honeyGauge.render(g, x, y);
        royalJellyGauge.render(g, x, y);
        nectarGauge.render(g, x, y);
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressArrow(g, x + 68, y + 35, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
        if (royalJellyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, royalJellyGauge.getTooltip("Royal Jelly"), mouseX, mouseY);
        }
        if (nectarGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, nectarGauge.getTooltip("Nectar"), mouseX, mouseY);
        }
    }
}
