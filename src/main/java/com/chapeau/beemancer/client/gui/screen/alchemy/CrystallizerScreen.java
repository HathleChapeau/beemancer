/**
 * ============================================================
 * [CrystallizerScreen.java]
 * Description: GUI pour le cristalliseur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CrystallizerMenu    | Donnees container    | Output slot, progress, fluid   |
 * | FluidGaugeWidget    | Jauge de fluide      | Input tank                     |
 * | GuiRenderHelper     | Rendu programmatique | Background, slots, progress    |
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
import com.chapeau.beemancer.common.menu.alchemy.CrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);
    private FluidGaugeWidget inputGauge;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        inputGauge = new FluidGaugeWidget(
            8, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getInputTank().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 76, 176, 76);
        g.drawString(font, Component.translatable("container.beemancer.crystallizer"),
            x + 8, y + 7, 0xDDDDDD, false);

        // Output slot (116, 35)
        GuiRenderHelper.renderSlot(g, x + 88 - 9, y + 30);

        // Progress bar
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressBar(g, x + 88 - 28, y + 55, 56, 6, ratio);

        // Input fluid gauge
        inputGauge.render(g, x, y);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (inputGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getInputTank().getFluid());
            g.renderComponentTooltip(font, inputGauge.getTooltip(name), mouseX, mouseY);
        }
    }
}
