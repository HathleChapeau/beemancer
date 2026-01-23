/**
 * ============================================================
 * [PoweredCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse automatique
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PoweredCentrifugeScreen extends AbstractContainerScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/powered_centrifuge.png");

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Progress arrow (center)
        float progress = menu.getProgressRatio();
        if (progress > 0) {
            int progressWidth = (int) (24 * progress);
            guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 0, progressWidth, 17);
        }

        // Fuel tank (left)
        int fuelHeight = (int) (52 * (menu.getFuelAmount() / 8000f));
        if (fuelHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 17 + (52 - fuelHeight), 176, 17, 16, fuelHeight);
        }

        // Output tank (right)
        int outputHeight = (int) (52 * (menu.getOutputAmount() / 8000f));
        if (outputHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 152, y + 17 + (52 - outputHeight), 192, 17, 16, outputHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Tank tooltips
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Fuel: " + menu.getFuelAmount() + " / 8000 mB"), mouseX, mouseY);
        }
        if (isHovering(152, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Output: " + menu.getOutputAmount() + " / 8000 mB"), mouseX, mouseY);
        }
    }
}
