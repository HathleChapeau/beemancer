/**
 * ============================================================
 * [MagicHiveScreen.java]
 * Description: GUI de la ruche magique (small et multibloc) avec textures beehive
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MagicHiveMenu           | Donnees container    | Bee slots, output, status      |
 * | AbstractApicaScreen | Base screen          | Boilerplate GUI                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.menu.MagicHiveMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MagicHiveScreen extends AbstractApicaScreen<MagicHiveMenu> {
    private static final ResourceLocation SMALL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/beehive/bg_beehive_small.png");
    private static final ResourceLocation LARGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/beehive/bg_beehive.png");
    private static final ResourceLocation BEE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/beehive/beehive-slot2.png");
    private static final ResourceLocation COMB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/beehive/beehive-comb.png");

    private static final String ICON_PATH = "textures/gui/beehive/";
    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 2;
    private static final int COMB_W = 66;
    private static final int COMB_H = 62;
    private static final int BEE_SLOT_SIZE = 22;
    private static final int BEE_SLOT_BORDER = 3;

    // Small layout (142x110 panel in 176px container)
    // 2 slots * 22px = 44, centered: (142-44)/2 = 49, texture at 17+49=66
    private static final int SMALL_PANEL_W = 142;
    private static final int SMALL_PANEL_OFFSET = (176 - SMALL_PANEL_W) / 2; // 17
    private static final int SMALL_BEE_TEX_X = SMALL_PANEL_OFFSET + 49; // 66
    private static final int SMALL_BEE_TEX_Y = 14;
    private static final int SMALL_COMB_CENTER_X = SMALL_PANEL_OFFSET + 71; // 88
    private static final int SMALL_COMB_CENTER_Y = 70;

    // Large layout (216x110 panel)
    // 5 slots * 22px = 110, centered: (216-110)/2 = 53, texture at 53
    private static final int LARGE_BEE_TEX_X = 53;
    private static final int LARGE_BEE_TEX_Y = 14;
    private static final int LARGE_COMB_CENTER_X = 108;
    private static final int LARGE_COMB_CENTER_Y = 70;

    private final boolean isMultiblock;
    private final int panelOffset;
    private final int panelW;

    public MagicHiveScreen(MagicHiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title,
              menu.isMultiblock() ? 216 : 176,
              122,
              menu.isMultiblock() ? 20 : 0);
        this.isMultiblock = menu.isMultiblock();
        this.panelOffset = isMultiblock ? 0 : SMALL_PANEL_OFFSET;
        this.panelW = isMultiblock ? 216 : SMALL_PANEL_W;
    }

    @Override protected ResourceLocation getTexture() { return isMultiblock ? LARGE_TEXTURE : SMALL_TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.magic_hive"; }
    @Override protected int getTitleColor() { return 0xDDDDDD; }
    @Override protected int getTitleY() { return 6; }
    @Override protected int getBlitHeight() { return 110; }
    @Override protected int getPanelXOffset() { return panelOffset; }
    @Override protected int getPanelWidth() { return panelW; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        int beeTexX = isMultiblock ? LARGE_BEE_TEX_X : SMALL_BEE_TEX_X;
        int beeTexY = isMultiblock ? LARGE_BEE_TEX_Y : SMALL_BEE_TEX_Y;

        // Bee assignment slot textures
        for (int i = 0; i < menu.getBeeSlotCount(); i++) {
            g.blit(BEE_SLOT_TEXTURE, x + beeTexX + i * BEE_SLOT_SIZE, y + beeTexY,
                   0, 0, BEE_SLOT_SIZE, BEE_SLOT_SIZE, BEE_SLOT_SIZE, BEE_SLOT_SIZE);
        }

        // Honeycomb output area
        int combCX = isMultiblock ? LARGE_COMB_CENTER_X : SMALL_COMB_CENTER_X;
        int combCY = isMultiblock ? LARGE_COMB_CENTER_Y : SMALL_COMB_CENTER_Y;
        g.blit(COMB_TEXTURE, x + combCX - COMB_W / 2, y + combCY - COMB_H / 2,
               0, 0, COMB_W, COMB_H, COMB_W, COMB_H);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        renderStatusIcons(g, x, y, mouseX, mouseY);
        renderBeeSmileys(g);
    }

    private void renderStatusIcons(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int px = x + panelOffset;
        int iconX = px + panelW - ICON_SIZE - 6;
        int currentY = y + 22;

        // Day/Night
        ResourceLocation dayNightIcon = getIcon(menu.isDaytime() ? "day" : "night");
        g.blit(dayNightIcon, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal(menu.isDaytime() ? "Day" : "Night")
                .withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
        }
        currentY += ICON_SIZE + ICON_SPACING;

        // Temperature
        int temp = menu.getTemperature();
        String tempIcon;
        String tempDesc;
        switch (temp) {
            case -2 -> { tempIcon = "thundra"; tempDesc = "Glacial"; }
            case -1 -> { tempIcon = "forest"; tempDesc = "Cold"; }
            case 0 -> { tempIcon = "flower"; tempDesc = "Temperate"; }
            case 1 -> { tempIcon = "desert"; tempDesc = "Hot"; }
            default -> { tempIcon = "neither"; tempDesc = "Burning"; }
        }
        ResourceLocation tempTexture = getIcon(tempIcon);
        g.blit(tempTexture, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            g.renderTooltip(font, Component.literal("Temperature: " + tempDesc + " (" + temp + ")")
                .withStyle(ChatFormatting.GOLD), mouseX, mouseY);
        }
        currentY += ICON_SIZE + ICON_SPACING;

        // Flowers
        if (menu.hasFlowers()) {
            ResourceLocation flowerIcon = getIcon("flower");
            g.blit(flowerIcon, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
                g.renderTooltip(font, Component.literal("Flowers available")
                    .withStyle(ChatFormatting.GREEN), mouseX, mouseY);
            }
            currentY += ICON_SIZE + ICON_SPACING;
        }

        // Mushrooms
        if (menu.hasMushrooms()) {
            ResourceLocation mushroomIcon = getIcon("mushroom");
            g.blit(mushroomIcon, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
                g.renderTooltip(font, Component.literal("Mushrooms available")
                    .withStyle(ChatFormatting.GOLD), mouseX, mouseY);
            }
            currentY += ICON_SIZE + ICON_SPACING;
        }

        // Antibreeding crystal
        if (menu.isAntibreedingMode()) {
            ResourceLocation crystalIcon = getIcon("crystal");
            g.blit(crystalIcon, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
                List<Component> tt = new ArrayList<>();
                tt.add(Component.literal("Crystal Antibreeding").withStyle(ChatFormatting.RED));
                tt.add(Component.literal("Bees cannot breed").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tt, mouseX, mouseY);
            }
            currentY += ICON_SIZE + ICON_SPACING;
        }

        // Crowded (nearby hive too close)
        if (menu.isCrowded()) {
            ResourceLocation crowdedIcon = getIcon("none");
            g.blit(crowdedIcon, iconX, currentY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (isHovering(iconX - leftPos, currentY - topPos, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
                List<Component> tt = new ArrayList<>();
                tt.add(Component.literal("Crowded").withStyle(ChatFormatting.RED));
                tt.add(Component.literal("Another hive is too close").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tt, mouseX, mouseY);
            }
        }
    }

    private void renderBeeSmileys(GuiGraphics g) {
        int beeTexX = isMultiblock ? LARGE_BEE_TEX_X : SMALL_BEE_TEX_X;
        int beeTexY = isMultiblock ? LARGE_BEE_TEX_Y : SMALL_BEE_TEX_Y;
        for (int i = 0; i < menu.getBeeSlotCount(); i++) {
            if (menu.getContainer().getItem(i).isEmpty()) continue;
            int slotTexX = leftPos + beeTexX + i * BEE_SLOT_SIZE;
            boolean canForage = menu.canBeeForage(i);
            g.drawString(font, canForage ? "\u263A" : "\u2639",
                slotTexX + 16, topPos + beeTexY - 2, canForage ? 0xFF00FF00 : 0xFFFF0000, false);
        }
    }

    private static ResourceLocation getIcon(String name) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, ICON_PATH + name + ".png");
    }
}
