/**
 * ============================================================
 * [ResonatorScreen.java]
 * Description: GUI du resonateur avec onde, slider Hz et 3 potards rotatifs
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorMenu           | Menu associe         | Lecture ContainerData          |
 * | WaveformRenderer        | Rendu onde           | Affichage waveform             |
 * | GuiRenderHelper         | Rendu GUI            | Background, bordures           |
 * | ResonatorUpdatePacket   | Sync C2S             | Envoi parametres au serveur    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup (registerScreens)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.menu.ResonatorMenu;
import com.chapeau.apica.core.network.packets.ResonatorUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.ThreadLocalRandom;

public class ResonatorScreen extends AbstractContainerScreen<ResonatorMenu> {

    // Layout
    private static final int GUI_W = 200;
    private static final int GUI_H = 170;
    private static final int PANEL_W = 80;
    private static final int PANEL_GAP = 6;
    private static final int WAVE_X = 12;
    private static final int WAVE_Y = 18;
    private static final int WAVE_W = 176;
    private static final int WAVE_H = 50;
    private static final int SLIDER_X = 40;
    private static final int SLIDER_Y = 76;
    private static final int SLIDER_W = 148;
    private static final int SLIDER_H = 10;
    private static final int KNOB_Y = 125;
    private static final int KNOB_RADIUS = 18;
    private static final int KNOB_SPACING = 60;
    private static final float KNOB_MIN_DEG = 20.0f;
    private static final float KNOB_MAX_DEG = 340.0f;
    private static final int FREQ_MIN = 1;
    private static final int FREQ_MAX = 80;

    // Drag state
    private int dragIndex = -1; // -1=none, 0=slider, 1=knobAmp, 2=knobPhase, 3=knobHarm
    private int dragStartX;
    private int dragStartValue;

    // Local values (updated from ContainerData, modified by drag)
    private int localFreq = 20;
    private int localAmp = 70;
    private int localPhase = 0;
    private int localHarm = 0;

    private BlockPos blockPos = BlockPos.ZERO;

    // Debug panel: target values (generated once on open)
    private int targetFreq;
    private int targetAmp;
    private int targetPhase;
    private int targetHarm;
    private boolean targetsGenerated = false;

    public ResonatorScreen(ResonatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        // Extract block pos from menu if available
        if (menu instanceof ResonatorMenu rm) {
            localFreq = rm.getFrequency();
            localAmp = rm.getAmplitude();
            localPhase = rm.getPhase();
            localHarm = rm.getHarmonics();
        }

        // Generate random target values once per GUI open
        if (!targetsGenerated) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            targetFreq = rng.nextInt(FREQ_MIN, FREQ_MAX + 1);
            targetAmp = rng.nextInt(10, 101);
            targetPhase = rng.nextInt(0, 361);
            targetHarm = rng.nextInt(0, 101);
            targetsGenerated = true;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // Sync from server if not dragging
        if (dragIndex < 0) {
            localFreq = menu.getFrequency();
            localAmp = menu.getAmplitude();
            localPhase = menu.getPhase();
            localHarm = menu.getHarmonics();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Debug panel (left of main GUI)
        renderDebugPanel(g, x - PANEL_GAP - PANEL_W, y);

        // Background
        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, GUI_W, GUI_H);

        // Title
        g.drawString(font, Component.translatable("block.apica.resonator"),
                x + 8, y + 6, 0x404040, false);

        // Waveform display
        float freqF = localFreq;
        float ampF = localAmp / 100.0f;
        float phaseF = localPhase;
        float harmF = localHarm / 100.0f;
        WaveformRenderer.render(g, x + WAVE_X, y + WAVE_Y, WAVE_W, WAVE_H,
                freqF, ampF, phaseF, harmF);

        // Frequency slider
        renderFreqSlider(g, x, y, mouseX, mouseY);

        // 3 Knobs
        int knobBaseX = x + GUI_W / 2;
        renderKnob(g, knobBaseX - KNOB_SPACING, y + KNOB_Y, KNOB_RADIUS,
                localAmp / 100.0f, "AMP", String.format("%.1f", localAmp / 100.0f), mouseX, mouseY);
        renderKnob(g, knobBaseX, y + KNOB_Y, KNOB_RADIUS,
                localPhase / 360.0f, "PHASE", localPhase + "\u00B0", mouseX, mouseY);
        renderKnob(g, knobBaseX + KNOB_SPACING, y + KNOB_Y, KNOB_RADIUS,
                localHarm / 100.0f, "HARM", String.format("%.1f", localHarm / 100.0f), mouseX, mouseY);
    }

    private void renderDebugPanel(GuiGraphics g, int px, int py) {
        int panelH = GUI_H;

        // Panel background (dark)
        g.fill(px, py, px + PANEL_W, py + panelH, 0xCC222222);
        // Border
        g.fill(px, py, px + PANEL_W, py + 1, 0xFF444444);
        g.fill(px, py + panelH - 1, px + PANEL_W, py + panelH, 0xFF444444);
        g.fill(px, py, px + 1, py + panelH, 0xFF444444);
        g.fill(px + PANEL_W - 1, py, px + PANEL_W, py + panelH, 0xFF444444);

        // Title
        g.drawString(font, "TARGET", px + 4, py + 4, 0xFFFF8844, false);

        // Target values
        int lineY = py + 18;
        int lineH = 14;

        g.drawString(font, "Freq:", px + 4, lineY, 0xFF888888, false);
        g.drawString(font, targetFreq + " Hz", px + 4, lineY + 9, 0xFFAABBDD, false);
        lineY += lineH + 8;

        g.drawString(font, "Amp:", px + 4, lineY, 0xFF888888, false);
        g.drawString(font, String.format("%.1f", targetAmp / 100.0f), px + 4, lineY + 9, 0xFFAABBDD, false);
        lineY += lineH + 8;

        g.drawString(font, "Phase:", px + 4, lineY, 0xFF888888, false);
        g.drawString(font, targetPhase + "\u00B0", px + 4, lineY + 9, 0xFFAABBDD, false);
        lineY += lineH + 8;

        g.drawString(font, "Harm:", px + 4, lineY, 0xFF888888, false);
        g.drawString(font, String.format("%.1f", targetHarm / 100.0f), px + 4, lineY + 9, 0xFFAABBDD, false);
        lineY += lineH + 14;

        // Separator
        g.fill(px + 4, lineY - 4, px + PANEL_W - 4, lineY - 3, 0xFF444444);

        // Similarity percentage
        float similarity = WaveformRenderer.computeSimilarity(
                localFreq, localAmp / 100.0f, localPhase, localHarm / 100.0f,
                targetFreq, targetAmp / 100.0f, targetPhase, targetHarm / 100.0f);
        similarity = Math.max(0, Math.min(100, similarity));

        // Color: red → yellow → green based on percentage
        int simColor = getSimilarityColor(similarity);

        g.drawString(font, "Match:", px + 4, lineY, 0xFF888888, false);
        String pctText = String.format("%.0f%%", similarity);
        g.drawString(font, pctText, px + 4, lineY + 12, simColor, false);
    }

    /** Retourne une couleur rouge → jaune → vert selon le pourcentage 0-100. */
    private static int getSimilarityColor(float pct) {
        if (pct < 50) {
            // Rouge → Jaune (0-50%)
            int r = 255;
            int gr = (int) (pct / 50.0f * 255);
            return 0xFF000000 | (r << 16) | (gr << 8);
        } else {
            // Jaune → Vert (50-100%)
            int r = (int) ((1.0f - (pct - 50) / 50.0f) * 255);
            int gr = 255;
            return 0xFF000000 | (r << 16) | (gr << 8);
        }
    }

    private void renderFreqSlider(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int sx = x + SLIDER_X;
        int sy = y + SLIDER_Y;

        // Groove background
        g.fill(sx, sy, sx + SLIDER_W, sy + SLIDER_H, 0xFF1A1A1A);
        g.fill(sx, sy, sx + SLIDER_W, sy + 1, 0xFF373737);
        g.fill(sx, sy, sx + 1, sy + SLIDER_H, 0xFF373737);
        g.fill(sx + 1, sy + SLIDER_H - 1, sx + SLIDER_W, sy + SLIDER_H, 0xFFFFFFFF);
        g.fill(sx + SLIDER_W - 1, sy + 1, sx + SLIDER_W, sy + SLIDER_H, 0xFFFFFFFF);

        // Fill
        float ratio = (localFreq - FREQ_MIN) / (float) (FREQ_MAX - FREQ_MIN);
        int fillW = (int) ((SLIDER_W - 2) * ratio);
        if (fillW > 0) {
            g.fill(sx + 1, sy + 1, sx + 1 + fillW, sy + SLIDER_H - 1, 0xFF5588DD);
            g.fill(sx + 1, sy + 1, sx + 1 + fillW, sy + 2, 0xFF77AAFF);
        }

        // Cursor handle
        int handleX = sx + 1 + fillW;
        g.fill(handleX - 2, sy - 1, handleX + 2, sy + SLIDER_H + 1, 0xFFDDDDDD);
        g.fill(handleX - 2, sy - 1, handleX + 2, sy, 0xFFFFFFFF);

        // Hz label
        String hzText = localFreq + " Hz";
        g.drawString(font, hzText, x + 12, y + SLIDER_Y + 1, 0xFF88BBFF, false);
    }

    private void renderKnob(GuiGraphics g, int cx, int cy, int radius,
                              float ratio, String label, String valueText,
                              int mouseX, int mouseY) {
        // Outer ring (dark background)
        fillCircle(g, cx, cy, radius, 0xFF222222);
        fillCircle(g, cx, cy, radius - 2, 0xFF444444);
        fillCircle(g, cx, cy, radius - 3, 0xFF333333);

        // Arc used range indicator (subtle)
        float startAngle = KNOB_MIN_DEG;
        float endAngle = KNOB_MAX_DEG;
        float currentAngle = startAngle + ratio * (endAngle - startAngle);
        renderArc(g, cx, cy, radius - 1, startAngle, currentAngle, 0xFF5588DD);

        // Pointer line
        double angleRad = Math.toRadians(currentAngle - 90);
        int px = cx + (int) (Math.cos(angleRad) * (radius - 5));
        int py = cy + (int) (Math.sin(angleRad) * (radius - 5));
        renderLine(g, cx, cy, px, py, 0xFFFFFFFF);

        // Center dot
        fillCircle(g, cx, cy, 3, 0xFF666666);

        // Label below
        int labelW = font.width(label);
        g.drawString(font, label, cx - labelW / 2, cy + radius + 4, 0xFF888888, false);

        // Value above
        int valW = font.width(valueText);
        g.drawString(font, valueText, cx - valW / 2, cy - radius - 12, 0xFFAABBDD, false);
    }

    /** Remplissage d'un cercle plein par lignes horizontales. */
    private void fillCircle(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** Dessine un arc colore autour d'un cercle (par points). */
    private void renderArc(GuiGraphics g, int cx, int cy, int radius,
                            float startDeg, float endDeg, int color) {
        for (float deg = startDeg; deg <= endDeg; deg += 2.0f) {
            double rad = Math.toRadians(deg - 90);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    /** Bresenham-style line. */
    private void renderLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int steps = Math.max(dx, dy) + 1;
        for (int i = 0; i < steps; i++) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    // =========================================================================
    // INTERACTION
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Check slider
        int sx = x + SLIDER_X;
        int sy = y + SLIDER_Y;
        if (mouseX >= sx && mouseX <= sx + SLIDER_W && mouseY >= sy - 2 && mouseY <= sy + SLIDER_H + 2) {
            dragIndex = 0;
            updateSliderFromMouse(mouseX, x);
            return true;
        }

        // Check knobs
        int knobBaseX = x + GUI_W / 2;
        int ky = y + KNOB_Y;

        if (isInKnob(mouseX, mouseY, knobBaseX - KNOB_SPACING, ky)) {
            dragIndex = 1;
            dragStartX = (int) mouseX;
            dragStartValue = localAmp;
            return true;
        }
        if (isInKnob(mouseX, mouseY, knobBaseX, ky)) {
            dragIndex = 2;
            dragStartX = (int) mouseX;
            dragStartValue = localPhase;
            return true;
        }
        if (isInKnob(mouseX, mouseY, knobBaseX + KNOB_SPACING, ky)) {
            dragIndex = 3;
            dragStartX = (int) mouseX;
            dragStartValue = localHarm;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || dragIndex < 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        int x = (width - imageWidth) / 2;

        if (dragIndex == 0) {
            updateSliderFromMouse(mouseX, x);
        } else {
            // Knob: horizontal drag, left = increase, right = decrease
            int deltaX = dragStartX - (int) mouseX;
            int sensitivity = 2; // pixels per unit

            switch (dragIndex) {
                case 1 -> { // Amplitude (0-100)
                    localAmp = clamp(dragStartValue + deltaX / sensitivity, 0, 100);
                    sendUpdate(1, localAmp);
                }
                case 2 -> { // Phase (0-360)
                    localPhase = clamp(dragStartValue + deltaX / sensitivity * 3, 0, 360);
                    sendUpdate(2, localPhase);
                }
                case 3 -> { // Harmonics (0-100)
                    localHarm = clamp(dragStartValue + deltaX / sensitivity, 0, 100);
                    sendUpdate(3, localHarm);
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragIndex >= 0) {
            dragIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int knobBaseX = x + GUI_W / 2;
        int ky = y + KNOB_Y;

        // Scroll on knobs
        int scroll = (int) scrollY;
        if (scroll == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (isInKnob(mouseX, mouseY, knobBaseX - KNOB_SPACING, ky)) {
            localAmp = clamp(localAmp + scroll * 2, 0, 100);
            sendUpdate(1, localAmp);
            return true;
        }
        if (isInKnob(mouseX, mouseY, knobBaseX, ky)) {
            localPhase = clamp(localPhase + scroll * 5, 0, 360);
            sendUpdate(2, localPhase);
            return true;
        }
        if (isInKnob(mouseX, mouseY, knobBaseX + KNOB_SPACING, ky)) {
            localHarm = clamp(localHarm + scroll * 2, 0, 100);
            sendUpdate(3, localHarm);
            return true;
        }

        // Scroll on slider area
        int sx = x + SLIDER_X;
        int sy = y + SLIDER_Y;
        if (mouseX >= sx && mouseX <= sx + SLIDER_W && mouseY >= sy - 5 && mouseY <= sy + SLIDER_H + 5) {
            localFreq = clamp(localFreq + scroll, FREQ_MIN, FREQ_MAX);
            sendUpdate(0, localFreq);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateSliderFromMouse(double mouseX, int guiX) {
        int sx = guiX + SLIDER_X + 1;
        float ratio = (float) (mouseX - sx) / (SLIDER_W - 2);
        ratio = Math.max(0, Math.min(1, ratio));
        localFreq = FREQ_MIN + (int) (ratio * (FREQ_MAX - FREQ_MIN));
        sendUpdate(0, localFreq);
    }

    private boolean isInKnob(double mx, double my, int cx, int cy) {
        double dx = mx - cx;
        double dy = my - cy;
        return dx * dx + dy * dy <= KNOB_RADIUS * KNOB_RADIUS;
    }

    private void sendUpdate(int paramIndex, int value) {
        // Get block pos from the block entity position stored during openMenu
        if (minecraft != null && minecraft.player != null) {
            // The pos was written in openMenu(resonator, pos), read from the buf in the menu client constructor
            // We need to find the block pos — stored in the menu's extraData
            // Since we don't have direct access, use the player's block interaction range
            BlockPos pos = findResonatorPos();
            if (pos != null) {
                PacketDistributor.sendToServer(new ResonatorUpdatePacket(pos, paramIndex, value));
            }
        }
    }

    private BlockPos findResonatorPos() {
        // The block pos was passed to openMenu. In NeoForge, for IMenuTypeExtension,
        // the buf contains the data written in openMenu callback.
        // We stored it in init() — let's use a field instead.
        // Actually, the pos is available from the FriendlyByteBuf in the client menu constructor.
        // Let me store it in the menu instead.
        return menu.getBlockPos();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // No inventory tooltips needed (no slots)
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Override to prevent default title/inventory label rendering
    }
}
