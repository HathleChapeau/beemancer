/**
 * ============================================================
 * [StorageControllerScreen.java]
 * Description: Écran GUI du Storage Controller (stats + 4 slots essence)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerMenu         | Menu associé         | Lecture des stats              |
 * | AbstractContainerScreen       | Base GUI             | Rendu standard                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (enregistrement écran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.storage.StorageControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Écran du Storage Controller.
 * Affiche 4 stats (vitesse de vol, recherche, craft, quantité)
 * et 4 slots pour les essences.
 */
public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/storage_controller.png");

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        // Stats
        int statX = 8;
        int statY = 20;
        int lineHeight = 12;

        drawStat(guiGraphics, statX, statY,
            Component.translatable("gui.beemancer.storage_controller.flight_speed"),
            menu.getFlightSpeed() + "%");

        drawStat(guiGraphics, statX, statY + lineHeight,
            Component.translatable("gui.beemancer.storage_controller.search_speed"),
            menu.getSearchSpeed() + "%");

        drawStat(guiGraphics, statX, statY + lineHeight * 2,
            Component.translatable("gui.beemancer.storage_controller.craft_speed"),
            menu.getCraftSpeed() + "%");

        drawStat(guiGraphics, statX, statY + lineHeight * 3,
            Component.translatable("gui.beemancer.storage_controller.quantity"),
            String.valueOf(menu.getQuantity()));

        drawStat(guiGraphics, statX, statY + lineHeight * 4,
            Component.translatable("gui.beemancer.storage_controller.honey_consumption"),
            menu.getHoneyConsumption() + " mB/s");

        drawStat(guiGraphics, statX, statY + lineHeight * 5,
            Component.translatable("gui.beemancer.storage_controller.honey_efficiency"),
            menu.getHoneyEfficiency() + "%");

        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void drawStat(GuiGraphics guiGraphics, int x, int y, Component label, String value) {
        guiGraphics.drawString(font, label, x, y, 0x404040, false);
        int valueWidth = font.width(value);
        guiGraphics.drawString(font, value, imageWidth - 8 - valueWidth, y, 0x206040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
