/**
 * ============================================================
 * [InfuserScreen.java]
 * Description: GUI pour l'infuseur
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class InfuserScreen extends AbstractContainerScreen<InfuserMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/infuser.png");

    public InfuserScreen(InfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Honey tank (left side)
        int honeyHeight = (int) (52 * (menu.getHoneyAmount() / 4000f));
        if (honeyHeight > 0) {
            guiGraphics.blit(TEXTURE, x + 17, y + 17 + (52 - honeyHeight), 176, 0, 16, honeyHeight);
        }

        // Progress arrow
        int processTime = menu.getProcessTime();
        if (processTime > 0) {
            int progress = menu.getProgress();
            int arrowWidth = (int) (24 * ((float) progress / processTime));
            if (arrowWidth > 0) {
                guiGraphics.blit(TEXTURE, x + 76, y + 35, 176, 52, arrowWidth, 17);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Honey tank tooltip
        if (isHovering(17, 17, 16, 52, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, 
                Component.literal("Honey: " + menu.getHoneyAmount() + " / 4000 mB"), 
                mouseX, mouseY);
        }
    }
}
