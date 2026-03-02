/**
 * ============================================================
 * [UncraftingTableScreen.java]
 * Description: GUI pour l'Uncrafting Table
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | UncraftingTableMenu         | Donnees container    | Slots, progress, fluid         |
 * | GuiRenderHelper             | Rendu programmatique | Slots, barres, tooltips        |
 * | AbstractApicaScreen         | Base screen          | Boilerplate GUI                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.alchemy;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.client.gui.screen.AbstractApicaScreen;
import com.chapeau.apica.common.menu.alchemy.UncraftingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class UncraftingTableScreen extends AbstractApicaScreen<UncraftingTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/bg.png");

    private static final int FUEL_BAR_X = 15;
    private static final int FUEL_BAR_Y = 27;

    public UncraftingTableScreen(UncraftingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override
    protected ResourceLocation getTexture() { return TEXTURE; }

    @Override
    protected String getTitleKey() { return "container.apica.uncrafting_table"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        // Input slot
        GuiRenderHelper.renderSlot(g, x + 29, y + 44);

        // Output slots (3x3 grid)
        GuiRenderHelper.renderSlotGrid(g, x + 107, y + 26, 3, 3);

        // Progress bar between input and output
        GuiRenderHelper.renderTextureProgressBar(g, x + 53, y + 48, menu.getProgressRatio());

        // Nectar bar
        int fuelCap = menu.getFluidCapacity();
        float fuelRatio = fuelCap > 0 ? (float) menu.getFluidAmount() / fuelCap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + FUEL_BAR_X, y + FUEL_BAR_Y, fuelRatio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (GuiRenderHelper.isHoneyBarHovered(FUEL_BAR_X, FUEL_BAR_Y, x, y, mouseX, mouseY)) {
            int amount = menu.getFluidAmount();
            int cap = menu.getFluidCapacity();
            FluidStack tankFluid = menu.getNectarTank().getFluid();
            String name = tankFluid.isEmpty() ? "Nectar" : GuiRenderHelper.getFluidName(tankFluid);
            String line1 = name + ": " + amount + " / " + cap + " mB";
            g.renderComponentTooltip(font, List.of(
                    Component.literal(line1),
                    Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                            .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }
    }
}
