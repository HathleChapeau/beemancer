/**
 * ============================================================
 * [InfuserScreen.java]
 * Description: GUI pour l'infuseur (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InfuserMenu             | Donnees container    | Slots, progress, honey         |
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
import com.chapeau.apica.common.menu.alchemy.InfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class InfuserScreen extends AbstractApicaScreen<InfuserMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/bg.png");

    // Layout centre dans 190px: honey(16) + 12 + slot(18) + 6 + progress(54) + 6 + slot(18) = 130
    private static final int HONEYBAR_X = 30;
    private static final int HONEYBAR_Y = 27;
    private static final int INPUT_SLOT_X = 58;
    private static final int OUTPUT_SLOT_X = 142;
    private static final int SLOT_Y = 44;
    private static final int PROGRESS_BAR_X = 82;
    private static final int PROGRESS_BAR_Y = 45;

    public InfuserScreen(InfuserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.infuser"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        // Reservoir (texture honey bar)
        int cap = 4000;
        float fluidRatio = cap > 0 ? (float) menu.getHoneyAmount() / cap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + HONEYBAR_X, y + HONEYBAR_Y, fluidRatio);

        // Slots
        GuiRenderHelper.renderSlot(g, x + INPUT_SLOT_X, y + SLOT_Y);
        GuiRenderHelper.renderSlot(g, x + OUTPUT_SLOT_X, y + SLOT_Y);

        // Progress bar
        int processTime = menu.getProcessTime();
        float ratio = processTime > 0 ? (float) menu.getProgress() / processTime : 0;
        GuiRenderHelper.renderTextureProgressBar(g, x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y, ratio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (GuiRenderHelper.isHoneyBarHovered(HONEYBAR_X, HONEYBAR_Y, x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getHoneyTank().getFluid());
            int amount = menu.getHoneyAmount();
            g.renderComponentTooltip(font, List.of(
                Component.literal(name + ": " + amount + " / 4000 mB"),
                Component.literal(String.format("%.1f%%", amount / 4000f * 100))
                    .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }
    }
}
