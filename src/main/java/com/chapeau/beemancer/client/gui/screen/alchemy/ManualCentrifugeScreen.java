/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ManualCentrifugeScreen extends AbstractContainerScreen<ManualCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/manual_centrifuge.png");

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fluid tank display
        int fluidHeight = (int) (52 * (menu.getFluidAmount() / 4000f));
        if (fluidHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 134, y + 17 + (52 - fluidHeight), 176, 0, 16, fluidHeight);
        }

        // Spin progress (spins / 5)
        int spinProgress = (int) (24 * (menu.getSpinCount() / 5f));
        if (spinProgress > 0) {
            guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 52, spinProgress, 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Fluid: " + menu.getFluidAmount() + " / 4000 mB"), mouseX, mouseY);
        }
    }
}
