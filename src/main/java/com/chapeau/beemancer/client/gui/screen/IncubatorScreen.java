/**
 * ============================================================
 * [IncubatorScreen.java]
 * Description: GUI de l'incubateur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | IncubatorMenu           | Donnees container    | Larva slot, progress           |
 * | GuiRenderHelper         | Rendu programmatique | Background, slot, progress     |
 * | AbstractBeemancerScreen | Base screen          | Boilerplate GUI                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.menu.IncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IncubatorScreen extends AbstractBeemancerScreen<IncubatorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/bg.png");

    public IncubatorScreen(IncubatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.incubator"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + 86, y + 44);
        GuiRenderHelper.renderTextureProgressBar(g, x + 107, y + 45, menu.getProgressRatio());
    }
}
