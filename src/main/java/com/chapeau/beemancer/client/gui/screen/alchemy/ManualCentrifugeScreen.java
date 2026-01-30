/**
 * ============================================================
 * [ManualCentrifugeScreen.java]
 * Description: GUI pour la centrifugeuse manuelle (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ManualCentrifugeMenu| Donnees container    | Slots, progress, fluid         |
 * | FluidGaugeWidget    | Jauge de fluide      | Affichage tank                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.alchemy;

import com.chapeau.beemancer.client.gui.widget.FluidGaugeWidget;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ManualCentrifugeScreen extends AbstractContainerScreen<ManualCentrifugeMenu> {
    private FluidGaugeWidget fluidGauge;

    public ManualCentrifugeScreen(ManualCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        fluidGauge = new FluidGaugeWidget(
            152, 17, 16, 52, 4000,
            () -> menu.getBlockEntity().getFluidTank().getFluid(),
            menu::getFluidAmount
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Vanilla container background
        renderContainerBackground(guiGraphics, x, y);

        // Slot backgrounds (dark insets)
        // Input slot (44, 35)
        renderSlotBackground(guiGraphics, x + 43, y + 34);

        // Output slots 2x2 (107,26), (125,26), (107,44), (125,44)
        renderSlotBackground(guiGraphics, x + 106, y + 25);
        renderSlotBackground(guiGraphics, x + 124, y + 25);
        renderSlotBackground(guiGraphics, x + 106, y + 43);
        renderSlotBackground(guiGraphics, x + 124, y + 43);

        // Arrow area: input → output
        renderProgressArrow(guiGraphics, x + 65, y + 35);

        // Fluid gauge
        fluidGauge.render(guiGraphics, x, y);

        // Player inventory slot backgrounds
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderSlotBackground(guiGraphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            renderSlotBackground(guiGraphics, x + 7 + col * 18, y + 141);
        }
    }

    private void renderContainerBackground(GuiGraphics g, int x, int y) {
        // Main background (Minecraft GUI gray)
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // 3D border - top and left (light)
        g.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFFDBDBDB);
        g.fill(x + 1, y + 1, x + 2, y + imageHeight - 1, 0xFFDBDBDB);

        // 3D border - bottom and right (dark)
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        g.fill(x + 1, y + imageHeight - 2, x + imageWidth - 1, y + imageHeight - 1, 0xFF7B7B7B);
        g.fill(x + imageWidth - 2, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF7B7B7B);

        // Separator line above player inventory
        g.fill(x + 7, y + 79, x + imageWidth - 7, y + 80, 0xFF8B8B8B);

        // Labels
        g.drawString(font, Component.translatable("container.beemancer.manual_centrifuge"),
                     x + 8, y + 6, 0x404040, false);
        g.drawString(font, Component.translatable("container.inventory"),
                     x + 8, y + imageHeight - 94, 0x404040, false);
    }

    private void renderSlotBackground(GuiGraphics g, int x, int y) {
        // Standard Minecraft slot inset (18x18 border around 16x16 slot)
        g.fill(x, y, x + 18, y + 1, 0xFF373737);     // Top
        g.fill(x, y, x + 1, y + 18, 0xFF373737);     // Left
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF); // Bottom
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF); // Right
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);  // Inner
    }

    private void renderProgressArrow(GuiGraphics g, int x, int y) {
        int arrowWidth = 36;
        int arrowHeight = 17;
        float ratio = menu.getProgressRatio();

        // Arrow background (dark groove)
        g.fill(x, y, x + arrowWidth, y + arrowHeight, 0xFF8B8B8B);
        g.fill(x, y, x + arrowWidth, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + arrowHeight, 0xFF373737);
        g.fill(x + 1, y + arrowHeight - 1, x + arrowWidth, y + arrowHeight, 0xFFFFFFFF);
        g.fill(x + arrowWidth - 1, y + 1, x + arrowWidth, y + arrowHeight, 0xFFFFFFFF);

        // Arrow fill (honey gold)
        int fillWidth = (int) (arrowWidth * ratio);
        if (fillWidth > 0) {
            g.fill(x + 1, y + 1, x + 1 + fillWidth, y + arrowHeight - 1, 0xFFD4A017);
            g.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0xFFE8B830);
        }

        // Arrow tip triangle (decorative, right side)
        int tipX = x + arrowWidth + 2;
        int midY = y + arrowHeight / 2;
        for (int i = 0; i < 5; i++) {
            g.fill(tipX + i, midY - 4 + i, tipX + i + 1, midY + 5 - i, 0xFF8B8B8B);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Fluid gauge tooltip
        if (fluidGauge.isMouseOver(x, y, mouseX, mouseY)) {
            String fluidName = getFluidName();
            guiGraphics.renderComponentTooltip(font, fluidGauge.getTooltip(fluidName), mouseX, mouseY);
        }

        // Progress tooltip
        float ratio = menu.getProgressRatio();
        if (ratio > 0 && mouseX >= x + 65 && mouseX < x + 101 && mouseY >= y + 35 && mouseY < y + 52) {
            guiGraphics.renderTooltip(font, Component.literal(String.format("%.0f%%", ratio * 100)), mouseX, mouseY);
        }
    }

    private String getFluidName() {
        var fluid = menu.getBlockEntity().getFluidTank().getFluid();
        if (fluid.isEmpty()) return "Empty";
        String path = fluid.getFluid().builtInRegistryHolder().key().location().getPath();
        if (path.contains("honey")) return "Honey";
        if (path.contains("royal_jelly")) return "Royal Jelly";
        if (path.contains("nectar")) return "Nectar";
        return "Fluid";
    }
}
