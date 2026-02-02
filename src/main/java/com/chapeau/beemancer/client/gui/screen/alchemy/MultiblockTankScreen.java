/**
 * ============================================================
 * [MultiblockTankScreen.java]
 * Description: GUI pour le tank multibloc (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MultiblockTankMenu  | Donnees container    | Bucket slot, fluid, block info |
 * | GuiRenderHelper     | Rendu programmatique | Background, slots              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
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
        Beemancer.MOD_ID, "textures/gui/bg_iron_wood.png");
    private static final int GAUGE_X = 62;
    private static final int GAUGE_Y = 17;
    private static final int GAUGE_W = 52;
    private static final int GAUGE_H = 52;
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(80);

    public MultiblockTankScreen(MultiblockTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 76, 176, 76);
        g.drawString(font, Component.translatable("container.beemancer.multiblock_tank"),
            x + 8, y + 7, 0xDDDDDD, false);

        // Bucket slot (26, 35)
        GuiRenderHelper.renderSlot(g, x + 25, y + 34);

        // Fluid gauge
        renderFluidGauge(g, x + GAUGE_X, y + GAUGE_Y);

        // Status info
        int blockCount = menu.getBlockCount();
        g.drawString(font, blockCount + " blocks", x + 120, y + 20, 0x404040, false);
        g.drawString(font, (menu.getCapacity() / 1000) + "B", x + 120, y + 32, 0x404040, false);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    private void renderFluidGauge(GuiGraphics g, int gx, int gy) {
        // Frame (inset style)
        g.fill(gx, gy, gx + GAUGE_W, gy + 1, 0xFF373737);
        g.fill(gx, gy, gx + 1, gy + GAUGE_H, 0xFF373737);
        g.fill(gx, gy + GAUGE_H - 1, gx + GAUGE_W, gy + GAUGE_H, 0xFFFFFFFF);
        g.fill(gx + GAUGE_W - 1, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFFFFFFFF);
        g.fill(gx + 1, gy + 1, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF8B8B8B);

        // Fluid fill
        int amount = menu.getFluidAmount();
        int capacity = menu.getCapacity();
        if (amount > 0 && capacity > 0) {
            int fluidH = (int) ((float) amount / capacity * (GAUGE_H - 2));
            if (fluidH > 0) {
                int color = getFluidColor();
                int fy = gy + GAUGE_H - 1 - fluidH;
                g.fill(gx + 1, fy, gx + GAUGE_W - 1, fy + fluidH, 0xFF000000 | color);

                // Highlight + shadow edges
                int r = (color >> 16) & 0xFF, gr = (color >> 8) & 0xFF, b = color & 0xFF;
                g.fill(gx + 1, fy, gx + 2, fy + fluidH,
                    0xFF000000 | (Math.min(255, r + 40) << 16) | (Math.min(255, gr + 40) << 8) | Math.min(255, b + 40));
                g.fill(gx + GAUGE_W - 2, fy, gx + GAUGE_W - 1, fy + fluidH,
                    0xFF000000 | (Math.max(0, r - 40) << 16) | (Math.max(0, gr - 40) << 8) | Math.max(0, b - 40));
            }
        }

        // Overlay graduations (25%, 50%, 75%)
        int inner = GAUGE_H - 2;
        for (int i = 1; i <= 3; i++) {
            int gradY = gy + GAUGE_H - 1 - (inner * i / 4);
            g.fill(gx + 1, gradY, gx + GAUGE_W - 1, gradY + 1, 0x40FFFFFF);
        }
    }

    private int getFluidColor() {
        if (menu.getBlockEntity() != null && menu.getBlockEntity().getFluidTank() != null) {
            FluidStack stack = menu.getBlockEntity().getFluidTank().getFluid();
            if (!stack.isEmpty()) {
                String name = stack.getFluid().builtInRegistryHolder().key().location().getPath();
                if (name.contains("honey")) return 0xE8A317;
                if (name.contains("royal_jelly")) return 0xFFF8DC;
                if (name.contains("nectar")) return 0xFFD700;
            }
        }
        return 0xE8A317;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int gx = x + GAUGE_X, gy = y + GAUGE_Y;
        if (mouseX >= gx && mouseX < gx + GAUGE_W && mouseY >= gy && mouseY < gy + GAUGE_H) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getCapacity();
            float pct = capacity > 0 ? (float) amount / capacity * 100 : 0;
            String fluidName = "Fluid";
            if (menu.getBlockEntity() != null && menu.getBlockEntity().getFluidTank() != null) {
                FluidStack stack = menu.getBlockEntity().getFluidTank().getFluid();
                if (!stack.isEmpty()) fluidName = stack.getHoverName().getString();
            }
            g.renderComponentTooltip(font, List.of(
                Component.literal(fluidName + ": " + amount + " / " + capacity + " mB"),
                Component.literal(String.format("%.1f%%", pct)).withStyle(s -> s.withColor(0xAAAAAA)),
                Component.literal(menu.getBlockCount() + " tank blocks").withStyle(s -> s.withColor(0x808080))
            ), mouseX, mouseY);
        }
    }
}
