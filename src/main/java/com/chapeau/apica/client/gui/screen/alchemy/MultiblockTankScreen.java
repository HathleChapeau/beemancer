/**
 * ============================================================
 * [MultiblockTankScreen.java]
 * Description: GUI pour le tank multibloc avec reduced_bg et texture honey bar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MultiblockTankMenu      | Donnees container    | Bucket slot, fluid, block info |
 * | GuiRenderHelper         | Rendu textures       | Honey bar, slots               |
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
import com.chapeau.apica.common.menu.alchemy.MultiblockTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class MultiblockTankScreen extends AbstractApicaScreen<MultiblockTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/reduced_bg.png");

    private static final int PANEL_W = 110;
    private static final int PANEL_OFFSET = (176 - PANEL_W) / 2; // 33

    // Positions relatives au panel (110px) — reservoir a gauche, slot a droite
    private static final int HONEYBAR_X = 16;
    private static final int HONEYBAR_Y = 28;
    private static final int BUCKET_SLOT_X = 62;
    private static final int BUCKET_SLOT_Y = 44;

    public MultiblockTankScreen(MultiblockTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 99, 0);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.multiblock_tank"; }
    @Override protected int getPanelXOffset() { return PANEL_OFFSET; }
    @Override protected int getPanelWidth() { return PANEL_W; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        int px = x + PANEL_OFFSET;

        // Bucket slot
        GuiRenderHelper.renderSlot(g, px + BUCKET_SLOT_X, y + BUCKET_SLOT_Y);

        // Texture honey bar
        int capacity = menu.getCapacity();
        float ratio = capacity > 0 ? (float) menu.getFluidAmount() / capacity : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, px + HONEYBAR_X, y + HONEYBAR_Y, ratio);

        // Block count and capacity text (sous le slot)
        int blockCount = menu.getBlockCount();
        String blocksText = blockCount + " blocks";
        String capText = (capacity / 1000) + "B";
        int slotCenterX = BUCKET_SLOT_X + 9;
        g.drawString(font, blocksText, px + slotCenterX - font.width(blocksText) / 2, y + 66, 0xDDDDDD, false);
        g.drawString(font, capText, px + slotCenterX - font.width(capText) / 2, y + 78, 0xDDDDDD, false);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + PANEL_OFFSET;
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, px, y, mouseX, mouseY)) {
            FluidStack fluid = FluidStack.EMPTY;
            if (menu.getBlockEntity() != null && menu.getBlockEntity().getFluidTank() != null) {
                fluid = menu.getBlockEntity().getFluidTank().getFluid();
            }
            List<Component> tooltip = new ArrayList<>(
                GuiRenderHelper.buildFluidTooltip(fluid, menu.getFluidAmount(), menu.getCapacity()));
            tooltip.add(Component.literal(menu.getBlockCount() + " tank blocks")
                .withStyle(s -> s.withColor(0x808080)));
            g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }
}
