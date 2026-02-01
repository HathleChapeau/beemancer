/**
 * ============================================================
 * [InfuserScreen.java]
 * Description: GUI pour l'infuseur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | InfuserMenu         | Donnees container    | Slots, progress, honey         |
 * | FluidGaugeWidget    | Jauge de fluide      | Honey tank                     |
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
import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class InfuserScreen extends AbstractContainerScreen<InfuserMenu> {
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);
    private FluidGaugeWidget honeyGauge;

    public InfuserScreen(InfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        honeyGauge = new FluidGaugeWidget(
            17, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getHoneyTank().getFluid(),
            menu::getHoneyAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, imageWidth, 76);
        g.drawString(font, Component.translatable("container.beemancer.infuser"),
            x + 8, y + 6, 0x404040, false);

        // Input slot (44, 35)
        GuiRenderHelper.renderSlot(g, x + 43, y + 34);

        // Output slot (116, 35)
        GuiRenderHelper.renderSlot(g, x + 115, y + 34);

        // Progress arrow
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressArrow(g, x + 68, y + 35, ratio);

        // Honey gauge
        honeyGauge.render(g, x, y);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (honeyGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, honeyGauge.getTooltip("Honey"), mouseX, mouseY);
        }
    }
}
