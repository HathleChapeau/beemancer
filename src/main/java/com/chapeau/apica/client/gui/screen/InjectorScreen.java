/**
 * ============================================================
 * [InjectorScreen.java]
 * Description: GUI de l'injecteur d'essence avec jauges de stats et barre de faim
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InjectorMenu            | Donnees container    | Slots, progress, jauges        |
 * | GuiRenderHelper         | Rendu programmatique | Slots, progress bar            |
 * | NotchedGaugeWidget      | Jauges crantees      | 5 jauges de stats              |
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
import com.chapeau.apica.client.gui.widget.NotchedGaugeWidget;
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

    private static final int GAUGE_WIDTH = 14;
    private static final int GAUGE_HEIGHT = 52;
    private static final int GAUGE_Y = 68;
    private static final int GAUGE_SPACING = 24;
    private static final int FIRST_GAUGE_X = 27;

    private NotchedGaugeWidget dropGauge;
    private NotchedGaugeWidget speedGauge;
    private NotchedGaugeWidget foragingGauge;
    private NotchedGaugeWidget toleranceGauge;
    private NotchedGaugeWidget activityGauge;

    public InjectorScreen(InjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 129);
    }

    @Override
    protected void init() {
        super.init();
        int ppl = InjectionConfigManager.isLoaded() ? InjectionConfigManager.getPointsPerLevel() : 50;

        dropGauge = new NotchedGaugeWidget(FIRST_GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
                4, ppl, menu::getDropPoints, 0xFFE8A317, "Drop");
        speedGauge = new NotchedGaugeWidget(FIRST_GAUGE_X + GAUGE_SPACING, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
                4, ppl, menu::getSpeedPoints, 0xFF55FFFF, "Speed");
        foragingGauge = new NotchedGaugeWidget(FIRST_GAUGE_X + GAUGE_SPACING * 2, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
                4, ppl, menu::getForagingPoints, 0xFF55FF55, "Forage");
        toleranceGauge = new NotchedGaugeWidget(FIRST_GAUGE_X + GAUGE_SPACING * 3, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
                4, ppl, menu::getTolerancePoints, 0xFFFF5555, "Toler.");
        activityGauge = new NotchedGaugeWidget(FIRST_GAUGE_X + GAUGE_SPACING * 4 + 8, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT,
                2, ppl, menu::getActivityPoints, 0xFFAA00FF, "Activ.");
    }

    @Override protected ResourceLocation getTexture() { return TEXTURE; }
    @Override protected String getTitleKey() { return "container.apica.injector"; }
    @Override protected int getBlitHeight() { return 95; }

    @Override
    protected void renderMachineContent(GuiGraphics g, int x, int y, float partialTick) {
        // Slots backgrounds
        GuiRenderHelper.renderSlot(g, x + 34, y + 37);
        GuiRenderHelper.renderSlot(g, x + 136, y + 37);

        // Progress bar entre les deux slots
        GuiRenderHelper.renderProgressBar(g, x + 58, y + 42, 72, 6, menu.getProgressRatio());

        // Hunger bar (horizontal en haut)
        renderHungerBar(g, x + 15, y + 22, 160, 8);

        // Satiation indicator
        if (menu.isSatiated() && menu.hasBee()) {
            g.drawCenteredString(font, Component.translatable("gui.apica.injector.satiated")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), x + 95, y + 56, 0xFFFFFF);
        }

        // 5 jauges de stats
        if (menu.hasBee()) {
            dropGauge.render(g, x, y);
            speedGauge.render(g, x, y);
            foragingGauge.render(g, x, y);
            toleranceGauge.render(g, x, y);
            activityGauge.render(g, x, y);

            // Labels au-dessus des jauges
            renderGaugeLabels(g, x, y);
        }
    }

    private void renderHungerBar(GuiGraphics g, int bx, int by, int bw, int bh) {
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
            int color = menu.isSatiated() ? 0xFFAA00FF : 0xFFE8A317;
            g.fill(bx + 1, by + 1, bx + 1 + fillW, by + bh - 1, color);
        }
    }

    private void renderGaugeLabels(GuiGraphics g, int x, int y) {
        int labelY = y + GAUGE_Y - 8;
        g.drawCenteredString(font, "D", x + FIRST_GAUGE_X + GAUGE_WIDTH / 2, labelY, 0xE8A317);
        g.drawCenteredString(font, "S", x + FIRST_GAUGE_X + GAUGE_SPACING + GAUGE_WIDTH / 2, labelY, 0x55FFFF);
        g.drawCenteredString(font, "F", x + FIRST_GAUGE_X + GAUGE_SPACING * 2 + GAUGE_WIDTH / 2, labelY, 0x55FF55);
        g.drawCenteredString(font, "T", x + FIRST_GAUGE_X + GAUGE_SPACING * 3 + GAUGE_WIDTH / 2, labelY, 0xFF5555);
        g.drawCenteredString(font, "A", x + FIRST_GAUGE_X + GAUGE_SPACING * 4 + 8 + GAUGE_WIDTH / 2, labelY, 0xAA00FF);
    }

    @Override
    protected void renderMachineTooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        if (!menu.hasBee()) return;

        // Hunger bar tooltip
        if (mouseX >= x + 15 && mouseX < x + 175 && mouseY >= y + 22 && mouseY < y + 30) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.apica.injector.hunger"),
                    Component.literal(menu.getHunger() + " / " + menu.getMaxHunger())
                            .withStyle(s -> s.withColor(0xAAAAAA))
            ), mouseX, mouseY);
            return;
        }

        // Gauge tooltips
        for (NotchedGaugeWidget gauge : List.of(dropGauge, speedGauge, foragingGauge, toleranceGauge, activityGauge)) {
            if (gauge.isMouseOver(x, y, mouseX, mouseY)) {
                g.renderComponentTooltip(font, gauge.getTooltip(), mouseX, mouseY);
                return;
            }
        }
    }
}
