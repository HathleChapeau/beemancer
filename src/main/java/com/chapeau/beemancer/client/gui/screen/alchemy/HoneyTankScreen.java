/**
 * ============================================================
 * [HoneyTankScreen.java]
 * Description: GUI simple pour le tank de miel
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.HoneyTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HoneyTankScreen extends AbstractContainerScreen<HoneyTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/honey_tank.png");

    public HoneyTankScreen(HoneyTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Large tank display (center)
        int fluidHeight = (int) (52 * (menu.getFluidAmount() / 16000f));
        if (fluidHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 62, y + 17 + (52 - fluidHeight), 176, 0, 52, fluidHeight);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(62, 17, 52, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.literal("Storage: " + menu.getFluidAmount() + " / 16000 mB"), mouseX, mouseY);
        }
    }
}
