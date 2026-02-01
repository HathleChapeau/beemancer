/**
 * ============================================================
 * [CreativeTankScreen.java]
 * Description: GUI pour le tank creatif (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CreativeTankMenu    | Donnees container    | Bucket slot, fluid data        |
 * | FluidGaugeWidget    | Jauge de fluide      | Affichage stockage infini      |
 * | GuiRenderHelper     | Rendu programmatique | Background, slots              |
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
import com.chapeau.beemancer.common.menu.alchemy.CreativeTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CreativeTankScreen extends AbstractContainerScreen<CreativeTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/creative_tank.png");
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);
    private FluidGaugeWidget storageGauge;

    public CreativeTankScreen(CreativeTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
    }

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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 76, 176, 76);
        g.drawString(font, Component.translatable("container.beemancer.creative_tank"),
            x + 8, y + 6, 0x404040, false);

        // Bucket slot (26, 35)
        GuiRenderHelper.renderSlot(g, x + 25, y + 34);

        // Fluid gauge
        storageGauge.render(g, x, y);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (storageGauge.isMouseOver(x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, storageGauge.getTooltip("Infinite"), mouseX, mouseY);
        }
    }
}
