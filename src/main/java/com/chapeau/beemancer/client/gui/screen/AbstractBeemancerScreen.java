/**
 * ============================================================
 * [AbstractBeemancerScreen.java]
 * Description: Classe de base pour les screens de machines Beemancer
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PlayerInventoryWidget   | Rendu inventaire     | Fond inventaire joueur         |
 * | AbstractContainerScreen | Base Minecraft       | Rendu GUI container            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ManualCentrifugeScreen, PoweredCentrifugeScreen, InfuserScreen,
 *   CrystallizerScreen, AlembicScreen, HoneyTankScreen, CreativeTankScreen,
 *   MultiblockTankScreen, IncubatorScreen, MagicHiveScreen
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base pour tous les screens de machines Beemancer.
 * Gere le rendu commun: background texture, titre, inventaire joueur, tooltips.
 * Les sous-classes implementent le contenu machine-specifique via hooks.
 */
public abstract class AbstractBeemancerScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    protected final PlayerInventoryWidget playerInventory;

    protected AbstractBeemancerScreen(T menu, Inventory playerInventory, Component title,
                                       int playerInvY) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
        this.playerInventory = new PlayerInventoryWidget(playerInvY);
    }

    protected abstract ResourceLocation getTexture();

    protected abstract String getTitleKey();

    /** Rendu du contenu specifique a la machine (slots, jauges, progress). */
    protected abstract void renderMachineContent(GuiGraphics g, int x, int y, float partialTick);

    protected int getTitleColor() { return 0xDDDDDD; }

    protected int getTitleY() { return 7; }

    protected int getBlitHeight() { return 76; }

    /** Rendu des tooltips specifiques a la machine. Appele apres renderTooltip. */
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int blitH = getBlitHeight();
        g.blit(getTexture(), x, y, 0, 0, 176, blitH, 176, blitH);
        g.drawString(font, Component.translatable(getTitleKey()),
            x + 8, y + getTitleY(), getTitleColor(), false);

        renderMachineContent(g, x, y, partialTick);
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        renderMachineTooltips(g, x, y, mouseX, mouseY);
    }
}
