/**
 * ============================================================
 * [StorageControllerScreen.java]
 * Description: Ecran GUI du Storage Controller (stats + 8 slots essence dynamiques)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerMenu         | Menu associe         | Lecture des stats              |
 * | AbstractContainerScreen       | Base GUI             | Rendu standard                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
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
import net.minecraft.world.inventory.Slot;

/**
 * Ecran du Storage Controller.
 * Affiche 7 stats (vol, recherche, craft, quantite, consommation miel, efficacite, delivery bees)
 * et jusqu'a 8 slots essence (4 de base + 4 bonus par hive).
 *
 * Les 4 bonus slots sont rendus avec un fond de slot uniquement quand actifs.
 */
public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/storage_controller.png");

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Draw slot backgrounds for active bonus slots (slots 4-7)
        for (int i = 4; i < 8; i++) {
            Slot slot = menu.slots.get(i);
            if (slot.isActive()) {
                drawSlotBackground(guiGraphics, x + slot.x - 1, y + slot.y - 1);
            }
        }
    }

    /**
     * Dessine un fond de slot 18x18 identique au style vanilla.
     */
    private void drawSlotBackground(GuiGraphics guiGraphics, int x, int y) {
        // Top and left edges (dark)
        guiGraphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        guiGraphics.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        // Bottom and right edges (light)
        guiGraphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        guiGraphics.fill(x + 17, y, x + 18, y + 17, 0xFFFFFFFF);
        // Inner area
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        // Stats
        int statX = 8;
        int statY = 18;
        int lineHeight = 10;

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

        int efficiencyBonus = menu.getHoneyEfficiency();
        String efficiencyText = efficiencyBonus > 0 ? "100% + " + efficiencyBonus + "%" : "100%";
        drawStat(guiGraphics, statX, statY + lineHeight * 5,
            Component.translatable("gui.beemancer.storage_controller.honey_efficiency"),
            efficiencyText);

        int maxBees = menu.getMaxDeliveryBees();
        drawStat(guiGraphics, statX, statY + lineHeight * 6,
            Component.translatable("gui.beemancer.storage_controller.delivery_bees"),
            String.valueOf(maxBees));

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
