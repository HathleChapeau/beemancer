/**
 * ============================================================
 * [AlembicScreen.java]
 * Description: GUI pour l'alambic
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlembicScreen extends AbstractContainerScreen<AlembicMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/alembic.png");

    public AlembicScreen(AlembicMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Input tank (left)
        int inputHeight = (int) (52 * (menu.getInputAmount() / 4000f));
        if (inputHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 26, y + 17 + (52 - inputHeight), 176, 0, 16, inputHeight);
        }

        // Output tank (right)
        int outputHeight = (int) (52 * (menu.getOutputAmount() / 4000f));
        if (outputHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 134, y + 17 + (52 - outputHeight), 192, 0, 16, outputHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(26, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Input: " + menu.getInputAmount() + " / 4000 mB"), mouseX, mouseY);
        }
        if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Output: " + menu.getOutputAmount() + " / 4000 mB"), mouseX, mouseY);
        }
    }
}
