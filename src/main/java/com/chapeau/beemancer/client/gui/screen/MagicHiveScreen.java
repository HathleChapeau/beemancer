/**
 * ============================================================
 * [MagicHiveScreen.java]
 * Description: GUI de la ruche magique avec layout honeycomb
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
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

    // Positions des icônes de status (coin supérieur droit)
    private static final int ICON_SIZE = 12;
    private static final int ICON_SPACING = 2;

    public MagicHiveScreen(MagicHiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Icônes de status permanents
        renderStatusIcons(guiGraphics, mouseX, mouseY);

        // Smileys pour chaque abeille
        renderBeeSmileys(guiGraphics, mouseX, mouseY);
    }

    private void renderStatusIcons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int iconX = leftPos + imageWidth - ICON_SIZE - 4;
        int iconY = topPos + 4;
        int currentY = iconY;

        // Icône Jour/Nuit (permanent)
        renderDayNightIcon(guiGraphics, iconX, currentY, mouseX, mouseY);
        currentY += ICON_SIZE + ICON_SPACING;

        // Icône Température (permanent)
        renderTemperatureIcon(guiGraphics, iconX, currentY, mouseX, mouseY);
        currentY += ICON_SIZE + ICON_SPACING;

        // Icône Fleur (si disponible)
        if (menu.hasFlowers()) {
            renderFlowerIcon(guiGraphics, iconX, currentY, mouseX, mouseY);
            currentY += ICON_SIZE + ICON_SPACING;
        }

        // Icône Champignon (si disponible)
        if (menu.hasMushrooms()) {
            renderMushroomIcon(guiGraphics, iconX, currentY, mouseX, mouseY);
            currentY += ICON_SIZE + ICON_SPACING;
        }

        // Icône Antibreeding (si crystal présent)
        if (menu.isAntibreedingMode()) {
            renderAntibreedingIcon(guiGraphics, iconX, currentY, mouseX, mouseY);
        }
    }

    private void renderDayNightIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        boolean isDay = menu.isDaytime();
        int bgColor = isDay ? 0xFFFFEB3B : 0xFF3F51B5; // Jaune jour, bleu nuit
        String symbol = isDay ? "☀" : "☽";

        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);
        guiGraphics.drawCenteredString(font, symbol, x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);

        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(isDay ? "Jour" : "Nuit").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }
    }

    private void renderTemperatureIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int temp = menu.getTemperature(); // -2 à 2
        int color;
        String symbol;
        String tempDesc;

        switch (temp) {
            case -2 -> { color = 0xFF00BFFF; symbol = "❄"; tempDesc = "Glacial"; }
            case -1 -> { color = 0xFF87CEEB; symbol = "~"; tempDesc = "Froid"; }
            case 0 -> { color = 0xFF90EE90; symbol = "◉"; tempDesc = "Tempéré"; }
            case 1 -> { color = 0xFFFFD700; symbol = "☼"; tempDesc = "Chaud"; }
            default -> { color = 0xFFFF4500; symbol = "♨"; tempDesc = "Brûlant"; } // 2+
        }

        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, color);
        guiGraphics.drawCenteredString(font, symbol, x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);

        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal("Température: " + tempDesc + " (" + temp + ")").withStyle(ChatFormatting.GOLD),
                    mouseX, mouseY);
        }
    }

    private void renderFlowerIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF4CAF50); // Vert
        guiGraphics.drawCenteredString(font, "✿", x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);

        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal("Fleurs disponibles").withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
        }
    }

    private void renderMushroomIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF8B4513); // Marron
        guiGraphics.drawCenteredString(font, "🍄", x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);

        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal("Champignons disponibles").withStyle(ChatFormatting.GOLD),
                    mouseX, mouseY);
        }
    }

    private void renderAntibreedingIcon(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        // Rond rouge barré
        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFFCC0000); // Rouge
        guiGraphics.drawCenteredString(font, "⊘", x + ICON_SIZE / 2, y + 2, 0xFFFFFFFF);

        if (isHovering(x - leftPos, y - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Crystal Antibreeding actif").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Les abeilles ne se reproduisent pas").withStyle(ChatFormatting.GRAY));
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderBeeSmileys(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Position des slots d'abeilles
        int beeSlotY = topPos + 20;
        int beeSlotStartX = leftPos + 44;

        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            // Vérifier si le slot a une abeille
            if (menu.getContainer().getItem(i).isEmpty()) continue;

            int slotX = beeSlotStartX + i * 18;
            int smileyX = slotX + 12; // Coin supérieur droit du slot
            int smileyY = beeSlotY - 2;

            boolean canForage = menu.canBeeForage(i);
            int color = canForage ? 0xFF00FF00 : 0xFFFF0000; // Vert ou rouge
            String smiley = canForage ? "☺" : "☹";

            // Dessiner le smiley
            guiGraphics.drawString(font, smiley, smileyX, smileyY, color, false);
        }
    }
}
