/**
 * ============================================================
 * [CrystallizerScreen.java]
 * Description: GUI pour le cristalliseur avec reduced_bg et texture honey bar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CrystallizerMenu        | Donnees container    | Output slot, progress, fluid   |
 * | GuiRenderHelper         | Rendu textures       | Honey bar, slots, progress     |
 * | AbstractApicaScreen | Base screen          | Boilerplate GUI                |
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
import com.chapeau.apica.common.menu.alchemy.CrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CrystallizerScreen extends AbstractApicaScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/reduced_bg.png");

    private static final int PANEL_W = 110;
    private static final int PANEL_OFFSET = (176 - PANEL_W) / 2; // 33

    // Positions relatives au panel (110px)
    // Reservoir a gauche, slot+jauge centres entre reservoir et bord droit
    private static final int HONEYBAR_X = 16;
    private static final int HONEYBAR_Y = 28;
    private static final int SLOT_X = 62;
    private static final int SLOT_Y = 38;
    private static final int PROGRESS_X = 6;
    private static final int PROGRESS_Y = 61;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 99, 0);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.crystallizer"; }
    @Override protected int getPanelXOffset() { return PANEL_OFFSET; }
    @Override protected int getPanelWidth() { return PANEL_W; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        int px = x + PANEL_OFFSET;

        // Texture honey bar (gauche)
        int cap = 4000;
        float fluidRatio = cap > 0 ? (float) menu.getFluidAmount() / cap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, px + HONEYBAR_X, y + HONEYBAR_Y, fluidRatio);

        // Output slot
        GuiRenderHelper.renderSlot(g, px + SLOT_X, y + SLOT_Y);

        // Progress bar (ancienne version programmatique conservee)
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderProgressBar(g, px + PROGRESS_X, y + PROGRESS_Y, 58, 6, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + PANEL_OFFSET;
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, px, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, GuiRenderHelper.buildFluidTooltip(
                menu.getBlockEntity().getInputTank().getFluid(), menu.getFluidAmount(), 4000
            ), mouseX, mouseY);
        }
    }
}
