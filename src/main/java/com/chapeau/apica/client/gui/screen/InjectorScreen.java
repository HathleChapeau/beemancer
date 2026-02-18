/**
 * ============================================================
 * [InjectorScreen.java]
 * Description: GUI de l'injecteur d'essence avec barres teintees et barre de saturation
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InjectorMenu            | Donnees container    | Slots, progress, jauges        |
 * | GuiRenderHelper         | Rendu programmatique | Slots, progress bar, barres    |
 * | AbstractApicaScreen     | Base screen          | Boilerplate GUI                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.menu.InjectorMenu;
import com.chapeau.apica.core.config.InjectionConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class InjectorScreen extends AbstractApicaScreen<InjectorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/bg.png");

    // Barre de saturation horizontale (au-dessus des barres de stats)
    private static final int SAT_BAR_X = 42;
    private static final int SAT_BAR_Y = 24;
    private static final int SAT_BAR_W = 130;
    private static final int SAT_BAR_H = 8;

    // Barres de stats: 5 barres teintees cote a cote (sous la barre de saturation)
    private static final int FIRST_BAR_X = 42;
    private static final int BAR_SPACING = 20;
    private static final int BAR_Y = 40;

    // Couleurs des stats
    private static final int COLOR_DROP = 0xE8A317;
    private static final int COLOR_SPEED = 0x55FFFF;
    private static final int COLOR_FORAGE = 0x55FF55;
    private static final int COLOR_TOLERANCE = 0xFF5555;
    private static final int COLOR_ACTIVITY = 0xAA00FF;
    private static final int COLOR_SATURATION = 0xE8A317;
    private static final int COLOR_SATIATED = 0xAA00FF;

    // Niveaux max par stat (notchCount)
    private static final int[] STAT_MAX_LEVELS = {4, 4, 4, 4, 2};
    private static final String[] STAT_NAMES = {"Drop", "Speed", "Forage", "Toler.", "Activ."};
    private static final int[] STAT_COLORS = {COLOR_DROP, COLOR_SPEED, COLOR_FORAGE, COLOR_TOLERANCE, COLOR_ACTIVITY};

    private int pointsPerLevel;

    public InjectorScreen(InjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 129);
    }

    @Override
    protected void init() {
        super.init();
        pointsPerLevel = InjectionConfigManager.isLoaded() ? InjectionConfigManager.getPointsPerLevel() : 50;
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.injector"; }
    @Override protected int getBlitHeight() { return 105; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        // Essence slot (haut-gauche)
        GuiRenderHelper.renderSlot(g, x + 15, y + 22);

        // Barre de progression verticale (texture centrifuge, reduite)
        GuiRenderHelper.renderVerticalTextureProgressBar(g, x + 19, y + 44, menu.getProgressRatio(), 30);

        // Bee slot (bas-gauche, sous la barre de progression)
        GuiRenderHelper.renderSlot(g, x + 15, y + 80);

        // Barre de saturation horizontale (au-dessus des barres de stats)
        renderSaturationBar(g, x + SAT_BAR_X, y + SAT_BAR_Y, SAT_BAR_W, SAT_BAR_H);

        // Indicateur "Satiated/Harmonisee"
        if (menu.isSatiated() && menu.hasBee()) {
            g.drawCenteredString(font, Component.translatable("gui.apica.injector.satiated")
                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                    x + SAT_BAR_X + SAT_BAR_W / 2, y + SAT_BAR_Y - 10, 0xFFFFFF);
        }

        // 5 barres de stats teintees (sans labels)
        if (menu.hasBee()) {
            int[] points = {
                    menu.getDropPoints(), menu.getSpeedPoints(), menu.getForagingPoints(),
                    menu.getTolerancePoints(), menu.getActivityPoints()
            };
            for (int i = 0; i < 5; i++) {
                int barX = FIRST_BAR_X + i * BAR_SPACING;
                int maxPts = STAT_MAX_LEVELS[i] * pointsPerLevel;
                float ratio = maxPts > 0 ? Math.min(1f, (float) points[i] / maxPts) : 0f;
                GuiRenderHelper.renderTintedBar(g, x + barX, y + BAR_Y, ratio, STAT_COLORS[i], STAT_MAX_LEVELS[i]);
            }
        }
    }

    private void renderSaturationBar(GuiGraphics g, int bx, int by, int bw, int bh) {
        // Cadre enfonce
        g.fill(bx, by, bx + bw, by + 1, 0xFF373737);
        g.fill(bx, by, bx + 1, by + bh, 0xFF373737);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFFFFFFFF);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFFFFFFFF);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0xFF8B8B8B);

        // Remplissage
        float ratio = menu.getHungerRatio();
        int fillW = (int) ((bw - 2) * ratio);
        if (fillW > 0) {
            int color = menu.isSatiated() ? COLOR_SATIATED : COLOR_SATURATION;
            g.fill(bx + 1, by + 1, bx + 1 + fillW, by + bh - 1, 0xFF000000 | color);
        }
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (!menu.hasBee()) return;

        // Saturation bar tooltip
        int sbx = x + SAT_BAR_X;
        int sby = y + SAT_BAR_Y;
        if (mouseX >= sbx && mouseX < sbx + SAT_BAR_W
                && mouseY >= sby && mouseY < sby + SAT_BAR_H) {
            g.renderComponentTooltip(font, List.of(
                    Component.literal("Saturation"),
                    Component.literal(menu.getHunger() + " / " + menu.getMaxHunger())
                            .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
            return;
        }

        // Stat bar tooltips
        int[] points = {
                menu.getDropPoints(), menu.getSpeedPoints(), menu.getForagingPoints(),
                menu.getTolerancePoints(), menu.getActivityPoints()
        };
        for (int i = 0; i < 5; i++) {
            int barX = FIRST_BAR_X + i * BAR_SPACING;
            if (GuiRenderHelper.isTintedBarHovered(barX, BAR_Y, x, y, mouseX, mouseY)) {
                int filledLevels = points[i] / pointsPerLevel;
                int partialPts = points[i] % pointsPerLevel;
                g.renderComponentTooltip(font, List.of(
                        Component.literal(STAT_NAMES[i] + ": " + filledLevels + "/" + STAT_MAX_LEVELS[i]),
                        Component.literal(partialPts + "/" + pointsPerLevel + " pts")
                                .withStyle(s -> s.withColor(0xAAAAAA))
                ), mouseX, mouseY);
                return;
            }
        }
    }
}
