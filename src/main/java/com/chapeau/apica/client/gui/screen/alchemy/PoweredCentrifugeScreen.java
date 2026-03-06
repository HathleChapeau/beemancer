/**
 * ============================================================
 * [PoweredCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse automatique (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PoweredCentrifugeMenu   | Donnees container    | Slots, progress, fluids        |
 * | GuiRenderHelper         | Rendu programmatique | Background, slots, honey bars  |
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
import com.chapeau.apica.common.menu.alchemy.PoweredCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class PoweredCentrifugeScreen extends AbstractApicaScreen<PoweredCentrifugeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/bg.png");

    // Layout centre: barL(16) +8+ slot(18) +9+ progress(54) +9+ 2x2(36) +8+ barR(16) = 190
    private static final int FUEL_BAR_X = 8;
    private static final int FUEL_BAR_Y = 27;
    private static final int INPUT_SLOT_X = 32;
    private static final int PROGRESS_X = 59;
    private static final int SLOTS_2X2_X = 122;
    private static final int OUTPUT_BAR_X = 166;
    private static final int OUTPUT_BAR_Y = 27;

    public PoweredCentrifugeScreen(PoweredCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 99);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.powered_centrifuge"; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        GuiRenderHelper.renderSlot(g, x + INPUT_SLOT_X, y + 44);
        GuiRenderHelper.renderSlots2x2(g, x + SLOTS_2X2_X, y + 35);
        GuiRenderHelper.renderTextureProgressBar(g, x + PROGRESS_X, y + 48, menu.getProgressRatio());

        // Honey bars: gauche = fuel, droite = output
        int fuelCap = menu.getFuelCapacity();
        float fuelRatio = fuelCap > 0 ? (float) menu.getFuelAmount() / fuelCap : 0;
        GuiRenderHelper.renderLeftHoneyBar(g, x + FUEL_BAR_X, y + FUEL_BAR_Y, fuelRatio);

        int outputCap = menu.getOutputCapacity();
        float outputRatio = outputCap > 0 ? (float) menu.getOutputAmount() / outputCap : 0;
        GuiRenderHelper.renderRightHoneyBar(g, x + OUTPUT_BAR_X, y + OUTPUT_BAR_Y, outputRatio);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // Fuel gauge tooltip (gauche)
        if (GuiRenderHelper.isHoneyBarHovered(FUEL_BAR_X, FUEL_BAR_Y, x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getFuelTank().getFluid());
            if (name.isEmpty() && menu.getFuelAmount() > 0) {
                name = "Honey";
            }
            int amount = menu.getFuelAmount();
            int cap = menu.getFuelCapacity();
            String line1 = name.isEmpty()
                    ? amount + " / " + cap + " mB"
                    : name + ": " + amount + " / " + cap + " mB";
            g.renderComponentTooltip(font, List.of(
                Component.literal(line1),
                Component.literal(String.format("%.1f%%", cap > 0 ? (float) amount / cap * 100 : 0))
                    .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
        }

        // Output gauge tooltip (droite)
        if (GuiRenderHelper.isHoneyBarHovered(OUTPUT_BAR_X, OUTPUT_BAR_Y, x, y, mouseX, mouseY)) {
            String name = GuiRenderHelper.getFluidName(menu.getOutputTank().getFluid());
            if (name.isEmpty() && menu.getOutputAmount() > 0) {
                name = "Honey";
            }
            int amount = menu.getOutputAmount();
            int cap = menu.getOutputCapacity();
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
