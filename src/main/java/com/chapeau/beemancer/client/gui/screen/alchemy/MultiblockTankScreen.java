/**
 * ============================================================
 * [MultiblockTankScreen.java]
 * Description: GUI pour le tank multibloc (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MultiblockTankMenu      | Donnees container    | Bucket slot, fluid, block info |
 * | GuiRenderHelper         | Rendu programmatique | Background, slots              |
 * | AbstractBeemancerScreen | Base screen          | Boilerplate GUI                |
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
import com.chapeau.beemancer.client.gui.screen.AbstractBeemancerScreen;
import com.chapeau.beemancer.common.menu.alchemy.MultiblockTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class MultiblockTankScreen extends AbstractBeemancerScreen<MultiblockTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg.png");
    private static final int GAUGE_X = 69;
    private static final int GAUGE_Y = 27;
    private static final int GAUGE_W = 52;
    private static final int GAUGE_H = 52;

    public MultiblockTankScreen(MultiblockTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.multiblock_tank"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 32, y + 44);
        renderFluidGauge(g, x + GAUGE_X, y + GAUGE_Y);
        int blockCount = menu.getBlockCount();
        g.drawString(font, blockCount + " blocks", x + 127, y + 30, 0xDDDDDD, false);
        g.drawString(font, (menu.getCapacity() / 1000) + "B", x + 127, y + 42, 0xDDDDDD, false);
    }

    private void renderFluidGauge(GuiGraphics g, int gx, int gy) {
        g.fill(gx, gy, gx + GAUGE_W, gy + 1, 0xFF373737);
        g.fill(gx, gy, gx + 1, gy + GAUGE_H, 0xFF373737);
        g.fill(gx, gy + GAUGE_H - 1, gx + GAUGE_W, gy + GAUGE_H, 0xFFFFFFFF);
        g.fill(gx + GAUGE_W - 1, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFFFFFFFF);
        g.fill(gx + 1, gy + 1, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF8B8B8B);

        int amount = menu.getFluidAmount();
        int capacity = menu.getCapacity();
        if (amount > 0 && capacity > 0) {
            int fluidH = (int) ((float) amount / capacity * (GAUGE_H - 2));
            if (fluidH > 0) {
                int color = getFluidColor();
                int fy = gy + GAUGE_H - 1 - fluidH;
                g.fill(gx + 1, fy, gx + GAUGE_W - 1, fy + fluidH, 0xFF000000 | color);
                int r = (color >> 16) & 0xFF, gr = (color >> 8) & 0xFF, b = color & 0xFF;
                g.fill(gx + 1, fy, gx + 2, fy + fluidH,
                    0xFF000000 | (Math.min(255, r + 40) << 16) | (Math.min(255, gr + 40) << 8) | Math.min(255, b + 40));
                g.fill(gx + GAUGE_W - 2, fy, gx + GAUGE_W - 1, fy + fluidH,
                    0xFF000000 | (Math.max(0, r - 40) << 16) | (Math.max(0, gr - 40) << 8) | Math.max(0, b - 40));
            }
        }

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
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
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
