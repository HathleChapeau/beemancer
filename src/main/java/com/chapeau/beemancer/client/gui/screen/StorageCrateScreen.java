/**
 * ============================================================
 * [StorageCrateScreen.java]
 * Description: Screen d'affichage pour l'inventaire StorageCrate
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation        |
 * |---------------------|----------------------|--------------------|
 * | Beemancer           | MOD_ID               | Chemin texture     |
 * | StorageCrateMenu    | Menu associé         | Données à afficher |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement du screen)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.StorageCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StorageCrateScreen extends AbstractContainerScreen<StorageCrateMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/storage_crate.png");

    public StorageCrateScreen(StorageCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Double chest dimensions: 176 x 222
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
