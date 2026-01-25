/**
 * ============================================================
 * [MultiblockTankScreen.java]
 * Description: GUI pour le tank multibloc avec infos dynamiques
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.alchemy.MultiblockTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class MultiblockTankScreen extends AbstractContainerScreen<MultiblockTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/multiblock_tank.png");

    // Gauge position and size
    private static final int GAUGE_X = 62;
    private static final int GAUGE_Y = 17;
    private static final int GAUGE_WIDTH = 52;
    private static final int GAUGE_HEIGHT = 52;

    // Fluid colors
    private static final int HONEY_COLOR = 0xFFE8A317;
    private static final int ROYAL_JELLY_COLOR = 0xFFFFF8DC;
    private static final int NECTAR_COLOR = 0xFFFFD700;

    public MultiblockTankScreen(MultiblockTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Render fluid gauge
        renderFluidGauge(guiGraphics, x + GAUGE_X, y + GAUGE_Y);

        // Render block count and validity indicator
        renderStatusInfo(guiGraphics, x, y);
    }

    private void renderFluidGauge(GuiGraphics graphics, int gaugeX, int gaugeY) {
        // Render gauge frame
        renderGaugeFrame(graphics, gaugeX, gaugeY);

        // Calculate fluid height
        int amount = menu.getFluidAmount();
        int capacity = menu.getCapacity();

        if (amount > 0 && capacity > 0) {
            int fluidHeight = (int) ((float) amount / capacity * (GAUGE_HEIGHT - 2));
            if (fluidHeight > 0) {
                renderFluidBar(graphics, gaugeX + 1, gaugeY + GAUGE_HEIGHT - 1 - fluidHeight,
                    GAUGE_WIDTH - 2, fluidHeight);
            }
        }

        // Render overlay
        renderGaugeOverlay(graphics, gaugeX, gaugeY);
    }

    private void renderGaugeFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + GAUGE_WIDTH, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + GAUGE_HEIGHT, 0xFF373737);
        graphics.fill(x, y + GAUGE_HEIGHT - 1, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x + GAUGE_WIDTH - 1, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + GAUGE_WIDTH - 1, y + GAUGE_HEIGHT - 1, 0xFF8B8B8B);
    }

    private void renderFluidBar(GuiGraphics graphics, int x, int y, int w, int h) {
        int color = getFluidColor();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        graphics.fill(x, y, x + w, y + h, 0xFF000000 | color);

        int highlightColor = 0xFF000000 |
            (Math.min(255, r + 40) << 16) |
            (Math.min(255, g + 40) << 8) |
            Math.min(255, b + 40);
        graphics.fill(x, y, x + 1, y + h, highlightColor);

        int shadowColor = 0xFF000000 |
            (Math.max(0, r - 40) << 16) |
            (Math.max(0, g - 40) << 8) |
            Math.max(0, b - 40);
        graphics.fill(x + w - 1, y, x + w, y + h, shadowColor);
    }

    private void renderGaugeOverlay(GuiGraphics graphics, int x, int y) {
        int gradColor = 0x40FFFFFF;
        int innerHeight = GAUGE_HEIGHT - 2;

        for (int i = 1; i <= 3; i++) {
            int gradY = y + GAUGE_HEIGHT - 1 - (innerHeight * i / 4);
            graphics.fill(x + 1, gradY, x + GAUGE_WIDTH - 1, gradY + 1, gradColor);
        }
    }

    private int getFluidColor() {
        if (menu.getBlockEntity() != null && menu.getBlockEntity().getFluidTank() != null) {
            FluidStack stack = menu.getBlockEntity().getFluidTank().getFluid();
            if (!stack.isEmpty()) {
                String fluidName = stack.getFluid().builtInRegistryHolder().key().location().getPath();
                if (fluidName.contains("honey")) {
                    return HONEY_COLOR;
                } else if (fluidName.contains("royal_jelly")) {
                    return ROYAL_JELLY_COLOR;
                } else if (fluidName.contains("nectar")) {
                    return NECTAR_COLOR;
                }
            }
        }
        return HONEY_COLOR;
    }

    private void renderStatusInfo(GuiGraphics graphics, int x, int y) {
        int blockCount = menu.getBlockCount();

        // Block count text
        String blockText = blockCount + " blocks";
        graphics.drawString(font, blockText, x + 120, y + 20, 0x404040, false);

        // Capacity info
        String capacityText = (menu.getCapacity() / 1000) + "B";
        graphics.drawString(font, capacityText, x + 120, y + 32, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Tooltip for fluid gauge
        if (isMouseOverGauge(x, y, mouseX, mouseY)) {
            List<Component> tooltip = getGaugeTooltip();
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isMouseOverGauge(int screenX, int screenY, int mouseX, int mouseY) {
        int gaugeX = screenX + GAUGE_X;
        int gaugeY = screenY + GAUGE_Y;
        return mouseX >= gaugeX && mouseX < gaugeX + GAUGE_WIDTH &&
               mouseY >= gaugeY && mouseY < gaugeY + GAUGE_HEIGHT;
    }

    private List<Component> getGaugeTooltip() {
        int amount = menu.getFluidAmount();
        int capacity = menu.getCapacity();
        float percent = capacity > 0 ? (float) amount / capacity * 100 : 0;

        String fluidName = "Fluid";
        if (menu.getBlockEntity() != null && menu.getBlockEntity().getFluidTank() != null) {
            FluidStack stack = menu.getBlockEntity().getFluidTank().getFluid();
            if (!stack.isEmpty()) {
                fluidName = stack.getHoverName().getString();
            }
        }

        return List.of(
            Component.literal(fluidName + ": " + amount + " / " + capacity + " mB"),
            Component.literal(String.format("%.1f%%", percent))
                .withStyle(style -> style.withColor(0xAAAAAA)),
            Component.literal(menu.getBlockCount() + " tank blocks")
                .withStyle(style -> style.withColor(0x808080))
        );
    }
}
