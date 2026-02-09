/**
 * ============================================================
 * [CreativeTankScreen.java]
 * Description: GUI pour le tank creatif (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CreativeTankMenu        | Donnees container    | Bucket slot, fluid data        |
 * | FluidGaugeWidget        | Jauge de fluide      | Affichage stockage infini      |
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
import com.chapeau.beemancer.common.menu.alchemy.CreativeTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CreativeTankScreen extends AbstractBeemancerScreen<CreativeTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private FluidGaugeWidget storageGauge;

    public CreativeTankScreen(CreativeTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 80);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.creative_tank"; }

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
            g.renderComponentTooltip(font, storageGauge.getTooltip("Infinite"), mouseX, mouseY);
        }
    }
}
