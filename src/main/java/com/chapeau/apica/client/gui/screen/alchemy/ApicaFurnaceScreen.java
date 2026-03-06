/**
 * ============================================================
 * [ApicaFurnaceScreen.java]
 * Description: GUI pour les fours Apica (honey, royal, nectar)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaFurnaceMenu        | Donnees container    | Slots, progress, fuel          |
 * | GuiRenderHelper         | Rendu programmatique | Slots, barres, tooltips        |
 * | AbstractApicaScreen     | Base screen          | Boilerplate GUI                |
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
import com.chapeau.apica.common.blockentity.alchemy.ApicaFurnaceBlockEntity;
import com.chapeau.apica.common.menu.alchemy.ApicaFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class ApicaFurnaceScreen extends AbstractApicaScreen<ApicaFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/bg.png");

    private static final int FUEL_BAR_X = 15;
    private static final int FUEL_BAR_Y = 27;

    public ApicaFurnaceScreen(ApicaFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }

    @Override protected String getTitleKey() {
        return menu.getBlockEntity() instanceof ApicaFurnaceBlockEntity
            ? "container.apica." + getBlockId()
            : "container.apica.honey_furnace";
    }

    private String getBlockId() {
        var pos = menu.getBlockEntity().getBlockPos();
        var level = menu.getBlockEntity().getLevel();
        if (level != null) {
            var state = level.getBlockState(pos);
            var block = state.getBlock();
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (key != null) return key.getPath();
        }
        return "honey_furnace";
    }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        if (menu.isDualSlot()) {
            GuiRenderHelper.renderSlot(g, x + 47, y + 35);
            GuiRenderHelper.renderSlot(g, x + 47, y + 55);
            GuiRenderHelper.renderSlot(g, x + 123, y + 35);
            GuiRenderHelper.renderSlot(g, x + 123, y + 55);

            GuiRenderHelper.renderTextureProgressBar(g, x + 69, y + 39, menu.getProgressRatio0());
            GuiRenderHelper.renderTextureProgressBar(g, x + 69, y + 59, menu.getProgressRatio1());
        } else {
            GuiRenderHelper.renderSlot(g, x + 47, y + 44);
            GuiRenderHelper.renderSlot(g, x + 123, y + 44);

            GuiRenderHelper.renderTextureProgressBar(g, x + 69, y + 48, menu.getProgressRatio0());
        }

        int fuelCap = menu.getFuelCapacity();
        float fuelRatio = fuelCap > 0 ? (float) menu.getFuelAmount() / fuelCap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + FUEL_BAR_X, y + FUEL_BAR_Y, fuelRatio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (GuiRenderHelper.isHoneyBarHovered(FUEL_BAR_X, FUEL_BAR_Y, x, y, mouseX, mouseY)) {
            FluidStack fluid = menu.getFuelTank().getFluid();
            if (fluid.isEmpty() && menu.getBlockEntity() instanceof ApicaFurnaceBlockEntity furnace) {
                fluid = new FluidStack(furnace.getAcceptedFluid(), 1);
            }
            g.renderComponentTooltip(font, GuiRenderHelper.buildFluidTooltip(
                fluid, menu.getFuelAmount(), menu.getFuelCapacity()
            ), mouseX, mouseY);
        }
    }
}
