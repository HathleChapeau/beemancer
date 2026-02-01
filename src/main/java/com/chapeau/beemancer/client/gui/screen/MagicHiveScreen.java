/**
 * ============================================================
 * [MagicHiveScreen.java]
 * Description: GUI de la ruche magique (rendu programmatique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicHiveMenu       | Donnees container    | Bee slots, output, status      |
 * | MagicHiveBlockEntity| Constantes           | BEE_SLOTS                      |
 * | GuiRenderHelper     | Rendu programmatique | Background, slots              |
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
import com.chapeau.beemancer.client.gui.widget.PlayerInventoryWidget;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MagicHiveScreen extends AbstractContainerScreen<MagicHiveMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/magic_hive.png");
    private static final int ICON_SIZE = 12;
    private static final int ICON_SPACING = 2;
    private final PlayerInventoryWidget playerInventory = new PlayerInventoryWidget(104);

    public MagicHiveScreen(MagicHiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        g.blit(TEXTURE, x, y, 0, 0, 176, 100, 176, 100);
        g.drawString(font, Component.translatable("container.beemancer.magic_hive"),
            x + 8, y + 6, 0x404040, false);

        // Bee assignment slots (5 slots at Y=20, starting X=44, spaced 18px)
        for (int i = 0; i < 5; i++) {
            GuiRenderHelper.renderSlot(g, x + 43 + i * 18, y + 19);
        }

        // Honeycomb output slots (7 slots around center 88, 65)
        int cx = x + 79, cy = y + 56;
        GuiRenderHelper.renderSlot(g, cx, cy); // center
        GuiRenderHelper.renderSlot(g, cx - 10, cy - 17);
        GuiRenderHelper.renderSlot(g, cx + 10, cy - 17);
        GuiRenderHelper.renderSlot(g, cx - 20, cy);
        GuiRenderHelper.renderSlot(g, cx + 20, cy);
        GuiRenderHelper.renderSlot(g, cx - 10, cy + 17);
        GuiRenderHelper.renderSlot(g, cx + 10, cy + 17);

        // Player inventory
        playerInventory.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        renderStatusIcons(g, mouseX, mouseY);
        renderBeeSmileys(g);
    }

    private void renderStatusIcons(GuiGraphics g, int mouseX, int mouseY) {
        int iconX = leftPos + imageWidth - ICON_SIZE - 4;
        int currentY = topPos + 4;

        renderIcon(g, iconX, currentY, mouseX, mouseY,
            menu.isDaytime() ? 0xFFFFEB3B : 0xFF3F51B5,
            menu.isDaytime() ? "\u2600" : "\u263D",
            menu.isDaytime() ? "Jour" : "Nuit", ChatFormatting.YELLOW);
        currentY += ICON_SIZE + ICON_SPACING;

        int temp = menu.getTemperature();
        int color;
        String symbol, desc;
        switch (temp) {
            case -2 -> { color = 0xFF00BFFF; symbol = "\u2744"; desc = "Glacial"; }
            case -1 -> { color = 0xFF87CEEB; symbol = "~"; desc = "Froid"; }
            case 0 -> { color = 0xFF90EE90; symbol = "\u25C9"; desc = "Temper\u00e9"; }
            case 1 -> { color = 0xFFFFD700; symbol = "\u263C"; desc = "Chaud"; }
            default -> { color = 0xFFFF4500; symbol = "\u2668"; desc = "Br\u00fblant"; }
        }
        renderIcon(g, iconX, currentY, mouseX, mouseY, color, symbol,
            "Temp\u00e9rature: " + desc + " (" + temp + ")", ChatFormatting.GOLD);
        currentY += ICON_SIZE + ICON_SPACING;

        if (menu.hasFlowers()) {
            renderIcon(g, iconX, currentY, mouseX, mouseY, 0xFF4CAF50, "\u273F",
                "Fleurs disponibles", ChatFormatting.GREEN);
            currentY += ICON_SIZE + ICON_SPACING;
        }
        if (menu.hasMushrooms()) {
            renderIcon(g, iconX, currentY, mouseX, mouseY, 0xFF8B4513, "\uD83C\uDF44",
                "Champignons disponibles", ChatFormatting.GOLD);
            currentY += ICON_SIZE + ICON_SPACING;
        }
        if (menu.isAntibreedingMode()) {
            g.fill(iconX, currentY, iconX + ICON_SIZE, currentY + ICON_SIZE, 0xFFCC0000);
            g.drawCenteredString(font, "\u2298", iconX + ICON_SIZE / 2, currentY + 2, 0xFFFFFFFF);
            if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
                List<Component> tt = new ArrayList<>();
                tt.add(Component.literal("Crystal Antibreeding actif").withStyle(ChatFormatting.RED));
                tt.add(Component.literal("Les abeilles ne se reproduisent pas").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tt, mouseX, mouseY);
            }
        }
    }

    private void renderIcon(GuiGraphics g, int x, int y, int mx, int my,
                            int bgColor, String symbol, String tooltip, ChatFormatting color) {
        g.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
        g.drawCenteredString(font, symbol, x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);
        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mx, my)) {
            g.renderTooltip(font, Component.literal(tooltip).withStyle(color), mx, my);
        }
    }

    private void renderBeeSmileys(GuiGraphics g) {
        int beeSlotY = topPos + 20;
        int beeSlotStartX = leftPos + 44;
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (menu.getContainer().getItem(i).isEmpty()) continue;
            int slotX = beeSlotStartX + i * 18;
            boolean canForage = menu.canBeeForage(i);
            g.drawString(font, canForage ? "\u263A" : "\u2639",
                slotX + 12, beeSlotY - 2, canForage ? 0xFF00FF00 : 0xFFFF0000, false);
        }
    }
}
