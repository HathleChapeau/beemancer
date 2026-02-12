/**
 * ============================================================
 * [MagicHiveScreen.java]
 * Description: GUI de la ruche magique avec textures beehive
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MagicHiveMenu           | Donnees container    | Bee slots, output, status      |
 * | MagicHiveBlockEntity    | Constantes           | BEE_SLOTS                      |
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
import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MagicHiveScreen extends AbstractBeemancerScreen<MagicHiveMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/beehive/bg_beehive.png");
    private static final ResourceLocation BEE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/beehive/beehive-slot.png");
    private static final ResourceLocation COMB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/beehive/beehive-comb.png");

    private static final String ICON_PATH = "textures/gui/beehive/";
    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 2;

    private static final int HONEYCOMB_CENTER_X = 108;
    private static final int HONEYCOMB_CENTER_Y = 70;
    private static final int COMB_W = 66;
    private static final int COMB_H = 62;

    public MagicHiveScreen(MagicHiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 216, 114, 20);
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.beemancer.magic_hive"; }
    @Override protected int getTitleColor() { return 0xDDDDDD; }
    @Override protected int getTitleY() { return 6; }
    @Override protected int getBlitHeight() { return 110; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        // Bee assignment slots (5 orange slots at top)
        for (int i = 0; i < 5; i++) {
            g.blit(BEE_SLOT_TEXTURE, x + 63 + i * 18, y + 23, 0, 0, 18, 18, 18, 18);
        }

        // Honeycomb output area (single image for 7 slots)
        int combX = x + HONEYCOMB_CENTER_X - COMB_W / 2;
        int combY = y + HONEYCOMB_CENTER_Y - COMB_H / 2;
        g.blit(COMB_TEXTURE, combX, combY, 0, 0, COMB_W, COMB_H, COMB_W, COMB_H);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        renderStatusIcons(g, x, y, mouseX, mouseY);
        renderBeeSmileys(g);
    }

    private void renderStatusIcons(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int iconX = x + imageWidth - ICON_SIZE - 6;
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
        }
    }

    private void renderBeeSmileys(GuiGraphics g) {
        int beeSlotY = topPos + 24;
        int beeSlotStartX = leftPos + 64;
        for (int i = 0; i < MagicHiveBlockEntity.BEE_SLOTS; i++) {
            if (menu.getContainer().getItem(i).isEmpty()) continue;
            int slotX = beeSlotStartX + i * 18;
            boolean canForage = menu.canBeeForage(i);
            g.drawString(font, canForage ? "\u263A" : "\u2639",
                slotX + 12, beeSlotY - 2, canForage ? 0xFF00FF00 : 0xFFFF0000, false);
        }
    }

    private static ResourceLocation getIcon(String name) {
        return ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, ICON_PATH + name + ".png");
    }
}
