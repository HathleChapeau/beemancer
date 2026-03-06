/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ManualCentrifugeMenu    | Donnees container    | Slots, progress, fluid         |
 * | GuiRenderHelper         | Rendu programmatique | Background, slots, honey bar   |
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
import com.chapeau.apica.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ManualCentrifugeScreen extends AbstractApicaScreen<ManualCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/bg.png");

    // Meme placement que PoweredCentrifugeScreen
    private static final int INPUT_SLOT_X = 32;
    private static final int PROGRESS_X = 59;
    private static final int SLOTS_2X2_X = 122;
    private static final int HONEYBAR_X = 166;
    private static final int HONEYBAR_Y = 27;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.manual_centrifuge"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + INPUT_SLOT_X, y + 44);
        GuiRenderHelper.renderSlots2x2(g, x + SLOTS_2X2_X, y + 35);
        GuiRenderHelper.renderTextureProgressBar(g, x + PROGRESS_X, y + 48, menu.getProgressRatio());

        // Honey bar droite (output)
        int cap = 4000;
        float ratio = cap > 0 ? (float) menu.getFluidAmount() / cap : 0;
        GuiRenderHelper.renderRightHoneyBar(g, x + HONEYBAR_X, y + HONEYBAR_Y, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, x, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, GuiRenderHelper.buildFluidTooltip(
                menu.getBlockEntity().getFluidTank().getFluid(), menu.getFluidAmount(), 4000
            ), mouseX, mouseY);
        }

        // Progress bar tooltip
        float ratio = menu.getProgressRatio();
        if (ratio > 0 && mouseX >= x + PROGRESS_X && mouseX < x + PROGRESS_X + 54 && mouseY >= y + 48 && mouseY < y + 58) {
            g.renderTooltip(font, Component.literal(String.format("%.0f%%", ratio * 100)), mouseX, mouseY);
        }
    }
}
