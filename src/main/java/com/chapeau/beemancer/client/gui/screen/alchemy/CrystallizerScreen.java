/**
 * ============================================================
 * [CrystallizerScreen.java]
 * Description: GUI pour le cristalliseur
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.CrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/crystallizer.png");

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Royal Jelly tank (left)
        int rjHeight = (int) (52 * (menu.getRoyalJellyAmount() / 8000f));
        if (rjHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 17 + (52 - rjHeight), 176, 0, 16, rjHeight);
        }

        // Honey tank (center-left)
        int honeyHeight = (int) (52 * (menu.getHoneyAmount() / 4000f));
        if (honeyHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 44, y + 17 + (52 - honeyHeight), 192, 0, 16, honeyHeight);
        }

        // Nectar tank (right)
        int nectarHeight = (int) (52 * (menu.getNectarAmount() / 8000f));
        if (nectarHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 17 + (52 - nectarHeight), 208, 0, 16, nectarHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Royal Jelly: " + menu.getRoyalJellyAmount() + " / 8000 mB"), mouseX, mouseY);
        }
        if (isHovering(44, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Honey: " + menu.getHoneyAmount() + " / 4000 mB"), mouseX, mouseY);
        }
        if (isHovering(152, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Nectar: " + menu.getNectarAmount() + " / 8000 mB"), mouseX, mouseY);
        }
    }
}
