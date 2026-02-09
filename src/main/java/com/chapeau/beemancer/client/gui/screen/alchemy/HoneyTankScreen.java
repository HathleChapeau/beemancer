/**
 * ============================================================
 * [HoneyTankScreen.java]
 * Description: GUI pour le tank de miel (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoneyTankMenu           | Donnees container    | Bucket slot, fluid data        |
 * | FluidGaugeWidget        | Jauge de fluide      | Affichage stockage             |
 * | GuiRenderHelper         | Rendu programmatique | Background, slots              |
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
import com.chapeau.beemancer.common.menu.alchemy.HoneyTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HoneyTankScreen extends AbstractBeemancerScreen<HoneyTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private FluidGaugeWidget storageGauge;

    public HoneyTankScreen(HoneyTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 80);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.honey_tank"; }

    @Override
    protected void init() {
        super.init();
        storageGauge = new FluidGaugeWidget(
            62, 17, 52, 52, 16000,
            () -> menu.getBlockEntity().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 25, y + 34);
        storageGauge.render(g, x, y);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (storageGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getFluid());
            g.renderComponentTooltip(font, storageGauge.getTooltip(name), mouseX, mouseY);
        }
    }
}
