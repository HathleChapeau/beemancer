/**
 * ============================================================
 * [MagicHiveScreen.java]
 * Description: GUI de la ruche magique avec layout honeycomb
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MagicHiveScreen extends AbstractContainerScreen<MagicHiveMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/magic_hive.png");

    public MagicHiveScreen(MagicHiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render breeding indicator
        if (menu.isBreedingMode()) {
            renderBreedingIndicator(guiGraphics, mouseX, mouseY);
        }
    }
    
    private void renderBreedingIndicator(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + imageWidth - 20;
        int y = topPos + 6;
        
        // Draw indicator icon (pink heart-like symbol)
        guiGraphics.fill(x, y, x + 14, y + 14, 0xFFFF69B4); // Pink background
        guiGraphics.drawCenteredString(font, "\u2665", x + 7, y + 3, 0xFFFFFFFF); // Heart symbol
        
        // Tooltip on hover
        if (isHovering(imageWidth - 20, 6, 14, 14, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, 
                    Component.translatable("gui.beemancer.magic_hive.breeding_mode")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), 
                    mouseX, mouseY);
        }
    }
}
