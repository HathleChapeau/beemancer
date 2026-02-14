/**
 * ============================================================
 * [CreativeTankScreen.java]
 * Description: GUI pour le tank creatif avec reduced_bg et texture honey bar
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CreativeTankMenu        | Donnees container    | Bucket slot, fluid data        |
 * | GuiRenderHelper         | Rendu textures       | Honey bar, slots               |
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
import com.chapeau.beemancer.common.menu.alchemy.CreativeTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CreativeTankScreen extends AbstractBeemancerScreen<CreativeTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/reduced_bg.png");

    private static final int PANEL_W = 110;
    private static final int PANEL_OFFSET = (176 - PANEL_W) / 2; // 33

    // Positions relatives au panel (110px)
    private static final int BUCKET_SLOT_X = 14;
    private static final int BUCKET_SLOT_Y = 37;
    private static final int HONEYBAR_X = 60;
    private static final int HONEYBAR_Y = 22;

    public CreativeTankScreen(CreativeTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 99, 0);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.creative_tank"; }
    @Override protected int getPanelXOffset() { return PANEL_OFFSET; }
    @Override protected int getPanelWidth() { return PANEL_W; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        int px = x + PANEL_OFFSET;

        // Bucket slot
        GuiRenderHelper.renderSlot(g, px + BUCKET_SLOT_X, y + BUCKET_SLOT_Y);

        // Texture honey bar (always full for creative)
        float ratio = 1.0f;
        GuiRenderHelper.renderLeftHoneyBar(g, px + HONEYBAR_X, y + HONEYBAR_Y, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + PANEL_OFFSET;
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, px, y, mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(
                Component.literal("Infinite")
            ), mouseX, mouseY);
        }
    }
}
