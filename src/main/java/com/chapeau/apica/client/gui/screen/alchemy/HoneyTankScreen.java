/**
 * ============================================================
 * [HoneyTankScreen.java]
 * Description: GUI pour le tank de miel avec reduced_bg et texture honey bar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoneyTankMenu           | Donnees container    | Bucket slot, fluid data        |
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
import com.chapeau.apica.common.menu.alchemy.HoneyTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class HoneyTankScreen extends AbstractApicaScreen<HoneyTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/reduced_bg.png");

    private static final int PANEL_W = 110;
    private static final int PANEL_OFFSET = (176 - PANEL_W) / 2; // 33

    // Positions relatives au panel (110px) — reservoir a gauche, slot a droite
    private static final int HONEYBAR_X = 16;
    private static final int HONEYBAR_Y = 28;
    private static final int BUCKET_SLOT_X = 62;
    private static final int BUCKET_SLOT_Y = 44;

    public HoneyTankScreen(HoneyTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 99, 0);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.honey_tank"; }
    @Override protected int getPanelXOffset() { return PANEL_OFFSET; }
    @Override protected int getPanelWidth() { return PANEL_W; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        int px = x + PANEL_OFFSET;

        // Bucket slot
        GuiRenderHelper.renderSlot(g, px + BUCKET_SLOT_X, y + BUCKET_SLOT_Y);

        // Texture honey bar
        int cap = 16000;
        float ratio = cap > 0 ? (float) menu.getFluidAmount() / cap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, px + HONEYBAR_X, y + HONEYBAR_Y, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + PANEL_OFFSET;
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, px, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getBlockEntity().getFluid());
            if (name.isEmpty() && menu.getFluidAmount() > 0) {
                name = "Honey";
            }
            int amount = menu.getFluidAmount();
            int cap = 16000;
            String line1 = name.isEmpty()
                    ? amount + " / " + cap + " mB"
                    : name + ": " + amount + " / " + cap + " mB";
            g.renderComponentTooltip(font, List.of(
                Component.literal(line1),
                Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                    .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }
    }
}
