/**
 * ============================================================
 * [AbstractApicaScreen.java]
 * Description: Classe de base pour les screens de machines Apica
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
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.widget.PlayerInventoryWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Base pour tous les screens de machines Apica.
 * Gere le rendu commun: background texture, titre, inventaire joueur, tooltips.
 * Les sous-classes implementent le contenu machine-specifique via hooks.
 */
public abstract class AbstractApicaScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    protected final PlayerInventoryWidget playerInventory;

    /** Standard alchemy constructor (190-wide bg, inventory centered +7px). */
    protected AbstractApicaScreen(T menu, Inventory playerInventory, Component title,
                                       int playerInvY) {
        this(menu, playerInventory, title, 190, playerInvY, 7);
    }

    /** Full control constructor for custom container widths. */
    protected AbstractApicaScreen(T menu, Inventory playerInventory, Component title,
                                       int containerWidth, int playerInvY, int invXOffset) {
        super(menu, playerInventory, title);
        this.imageWidth = containerWidth;
        this.imageHeight = playerInvY + 90;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
        this.playerInventory = new PlayerInventoryWidget(playerInvY, invXOffset);
    }

    protected abstract ResourceLocation getTexture();

    protected abstract String getTitleKey();

    /** Rendu du contenu specifique a la machine (slots, jauges, progress). */
    protected abstract void renderMachineContent(GuiGraphics g, int x, int y, float partialTick);

    protected int getTitleColor() { return 0xDDDDDD; }

    protected int getTitleY() { return 7; }

    protected int getBlitHeight() { return 95; }

    /** Decalage X du panneau machine par rapport au container. 0 par defaut. */
    protected int getPanelXOffset() { return 0; }

    /** Largeur du panneau machine (texture bg). Par defaut = imageWidth. */
    protected int getPanelWidth() { return imageWidth; }

    /** Rendu des tooltips specifiques a la machine. Appele apres renderTooltip. */
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int blitH = getBlitHeight();
        int panelX = x + getPanelXOffset();
        int panelW = getPanelWidth();
        g.blit(getTexture(), panelX, y, 0, 0, panelW, blitH, panelW, blitH);
        g.drawString(font, Component.translatable(getTitleKey()),
            panelX + 8, y + getTitleY(), getTitleColor(), false);

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
