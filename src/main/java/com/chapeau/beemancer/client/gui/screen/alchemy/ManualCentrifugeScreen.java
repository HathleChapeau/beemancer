/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ManualCentrifugeMenu| Donnees container    | Slots, progress, fluid         |
 * | FluidGaugeWidget    | Jauge de fluide      | Affichage tank                 |
 * | GuiRenderHelper     | Rendu programmatique | Background, slots, progress    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ManualCentrifugeScreen extends AbstractContainerScreen<ManualCentrifugeMenu> {
    private FluidGaugeWidget fluidGauge;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        GuiRenderHelper.renderContainerBackground(g, font, x, y, imageWidth, imageHeight,
            "container.beemancer.manual_centrifuge", 79);

        // Input slot (44, 35)
        GuiRenderHelper.renderSlot(g, x + 43, y + 34);

        // Output slots 2x2
        GuiRenderHelper.renderSlot(g, x + 106, y + 25);
        GuiRenderHelper.renderSlot(g, x + 124, y + 25);
        GuiRenderHelper.renderSlot(g, x + 106, y + 43);
        GuiRenderHelper.renderSlot(g, x + 124, y + 43);

        // Progress arrow
        GuiRenderHelper.renderProgressArrow(g, x + 65, y + 35, menu.getProgressRatio());

        // Fluid gauge
        fluidGauge.render(g, x, y);

        // Player inventory
        GuiRenderHelper.renderPlayerInventory(g, x, y, 83, 141);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

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
