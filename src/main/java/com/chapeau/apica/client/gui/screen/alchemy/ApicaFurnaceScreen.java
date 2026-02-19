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
        Apica.MOD_ID, "textures/gui/reduced_bg.png");

    private static final int PANEL_W = 110;
    private static final int PANEL_OFFSET = (176 - PANEL_W) / 2;

    private static final int FUEL_BAR_X = 14;
    private static final int FUEL_BAR_Y = 22;

    public ApicaFurnaceScreen(ApicaFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 99, 0);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected int getPanelXOffset() { return PANEL_OFFSET; }
    @Override protected int getPanelWidth() { return PANEL_W; }

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
        int px = x + PANEL_OFFSET;

        if (menu.isDualSlot()) {
            GuiRenderHelper.renderSlot(g, px + 38, y + 30);
            GuiRenderHelper.renderSlot(g, px + 38, y + 50);
            GuiRenderHelper.renderSlot(g, px + 80, y + 30);
            GuiRenderHelper.renderSlot(g, px + 80, y + 50);

            GuiRenderHelper.renderTextureProgressBar(g, px + 57, y + 34, menu.getProgressRatio0());
            GuiRenderHelper.renderTextureProgressBar(g, px + 57, y + 54, menu.getProgressRatio1());
        } else {
            GuiRenderHelper.renderSlot(g, px + 38, y + 40);
            GuiRenderHelper.renderSlot(g, px + 80, y + 40);

            GuiRenderHelper.renderTextureProgressBar(g, px + 57, y + 44, menu.getProgressRatio0());
        }

        int fuelCap = menu.getFuelCapacity();
        float fuelRatio = fuelCap > 0 ? (float) menu.getFuelAmount() / fuelCap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, px + FUEL_BAR_X, y + FUEL_BAR_Y, fuelRatio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + PANEL_OFFSET;
        if (GuiRenderHelper.isHoneyBarHovered(FUEL_BAR_X, FUEL_BAR_Y, px, y, mouseX, mouseY)) {
            String name = getAcceptedFluidName();
            int amount = menu.getFuelAmount();
            int cap = menu.getFuelCapacity();
            String line1 = name + ": " + amount + " / " + cap + " mB";
            g.renderComponentTooltip(font, List.of(
                Component.literal(line1),
                Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                    .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }
    }

    private String getAcceptedFluidName() {
        FluidStack tankFluid = menu.getFuelTank().getFluid();
        if (!tankFluid.isEmpty()) {
            return GuiRenderHelper.getFluidName(tankFluid);
        }
        if (menu.getBlockEntity() instanceof ApicaFurnaceBlockEntity furnace) {
            return GuiRenderHelper.getFluidName(new FluidStack(furnace.getAcceptedFluid(), 1));
        }
        return "Empty";
    }
}
