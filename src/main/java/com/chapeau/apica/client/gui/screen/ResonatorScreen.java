/**
 * ============================================================
 * [ResonatorScreen.java]
 * Description: GUI du resonateur avec onde, slider Hz, 3 potards et slot abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorMenu           | Menu associe         | Lecture ContainerData + bee     |
 * | WaveformRenderer        | Rendu onde           | Affichage waveform             |
 * | GuiRenderHelper         | Rendu GUI            | Background, bordures, slot     |
 * | ResonatorUpdatePacket   | Sync C2S             | Envoi parametres au serveur    |
 * | ResonatorConfigManager  | Config waveforms     | Target values depuis stats bee |
 * | BeeSpeciesManager       | Stats espece         | Lecture niveaux stats          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup (registerScreens)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.menu.ResonatorMenu;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.config.ResonatorConfigManager;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.core.network.packets.ResonatorFinishPacket;
import com.chapeau.apica.core.network.packets.ResonatorUpdatePacket;
import com.chapeau.apica.core.registry.ApicaAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
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

    // Bee slot position (right side)
    private static final int BEE_SLOT_X = 178;
    private static final int BEE_SLOT_Y = 6;

    // Analysis mode layout
    private static final int ANALYSIS_W = 140;
    private static final int ANALYSIS_H = 120;
    private static final int ANALYSIS_BAR_W = 100;
    private static final int ANALYSIS_BAR_H = 12;
    private static final int ANALYSIS_BTN_W = 60;
    private static final int ANALYSIS_BTN_H = 18;

    // Drag state
    private int dragIndex = -1;
    private int dragStartX;
    private int dragStartValue;

    // Local values
    private int localFreq = 20;
    private int localAmp = 70;
    private int localPhase = 0;
    private int localHarm = 0;

    // Target values
    private int targetFreq;
    private int targetAmp;
    private int targetPhase;
    private int targetHarm;
    private boolean targetsGenerated = false;

    public ResonatorScreen(ResonatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        if (menu.isAnalysisMode()) {
            this.imageWidth = ANALYSIS_W;
            this.imageHeight = ANALYSIS_H;
        } else {
            this.imageWidth = GUI_W;
            this.imageHeight = GUI_H;
        }
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.isAnalysisMode()) {
            localFreq = menu.getFrequency();
            localAmp = menu.getAmplitude();
            localPhase = menu.getPhase();
            localHarm = menu.getHarmonics();

            if (!targetsGenerated) {
                generateTargets();
                targetsGenerated = true;
            }
        }
    }

    /**
     * Genere les valeurs cibles. Si une abeille est posee, utilise ses stats + config waveforms.
     * Sinon, valeurs aleatoires.
     */
    private void generateTargets() {
        ItemStack bee = menu.getStoredBee();
        if (!bee.isEmpty()) {
            generateTargetsFromBee(bee);
        } else {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            targetFreq = rng.nextInt(FREQ_MIN, FREQ_MAX + 1);
            targetAmp = rng.nextInt(10, 101);
            targetPhase = rng.nextInt(0, 361);
            targetHarm = rng.nextInt(0, 101);
        }
    }

    /**
     * Calcule les target values en combinant les waveforms de chaque stat de l'abeille,
     * ponderees par le niveau de la stat.
     */
    private void generateTargetsFromBee(ItemStack bee) {
        ResonatorConfigManager.ensureClientLoaded();
        BeeSpeciesManager.ensureClientLoaded();

        // Lire l'espece depuis le CustomData de l'abeille
        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData speciesData = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        // Niveaux de base des 5 stats
        int dropLvl = speciesData != null ? speciesData.dropLevel : 1;
        int speedLvl = speciesData != null ? speciesData.flyingSpeedLevel : 1;
        int forageLvl = speciesData != null ? speciesData.foragingDurationLevel : 1;
        int toleranceLvl = speciesData != null ? speciesData.toleranceLevel : 1;
        int activityLvl = getActivityLevel(speciesData);

        // Combinaison des waveforms par stat (chaque stat a sa courbe selon son niveau)
        String[] statNames = {"drop", "speed", "foraging", "tolerance", "activity"};
        int[] levels = {dropLvl, speedLvl, forageLvl, toleranceLvl, activityLvl};

        float totalWeight = 0;
        float weightedFreq = 0;
        float weightedAmp = 0;
        float weightedPhase = 0;
        float weightedHarm = 0;

        for (int i = 0; i < statNames.length; i++) {
            ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(statNames[i], levels[i]);
            if (wf == null) continue;
            float weight = levels[i];
            totalWeight += weight;
            weightedFreq += wf.frequency * weight;
            weightedAmp += wf.amplitude * weight;
            weightedPhase += wf.phase * weight;
            weightedHarm += wf.harmonics * weight;
        }

        if (totalWeight > 0) {
            targetFreq = clamp(Math.round(weightedFreq / totalWeight), FREQ_MIN, FREQ_MAX);
            targetAmp = clamp(Math.round(weightedAmp / totalWeight), 0, 100);
            targetPhase = clamp(Math.round(weightedPhase / totalWeight) % 360, 0, 360);
            targetHarm = clamp(Math.round(weightedHarm / totalWeight), 0, 100);
        } else {
            targetFreq = 20;
            targetAmp = 50;
            targetPhase = 0;
            targetHarm = 0;
        }
    }

    private static String getSpeciesFromBee(ItemStack bee) {
        return MagicBeeItem.getSpeciesId(bee);
    }

    private static int getActivityLevel(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return 0;
        return switch (data.dayNight) {
            case "night" -> 1;
            case "both" -> 2;
            default -> 0;
        };
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.isAnalysisMode()) return;
        if (dragIndex < 0) {
            localFreq = menu.getFrequency();
            localAmp = menu.getAmplitude();
            localPhase = menu.getPhase();
            localHarm = menu.getHarmonics();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (menu.isAnalysisMode()) {
            renderAnalysisMode(g, mouseX, mouseY);
            return;
        }

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Info panel (left of main GUI)
        renderInfoPanel(g, x - PANEL_GAP - PANEL_W, y);

        // Background
        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, GUI_W, GUI_H);

        // Title
        g.drawString(font, Component.translatable("block.apica.resonator"),
                x + 8, y + 6, 0x404040, false);

        // Bee slot (right side)
        GuiRenderHelper.renderSlot(g, x + BEE_SLOT_X, y + BEE_SLOT_Y);

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

    // =========================================================================
    // ANALYSIS MODE
    // =========================================================================

    private void renderAnalysisMode(GuiGraphics g, int mouseX, int mouseY) {
        int x = (width - ANALYSIS_W) / 2;
        int y = (height - ANALYSIS_H) / 2;

        // Background panel
        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, ANALYSIS_W, ANALYSIS_H);

        // Title
        g.drawString(font, "Analysis", x + 8, y + 6, 0x404040, false);

        // Bee slot (centered top)
        int slotX = x + ANALYSIS_W / 2 - 8;
        int slotY = y + 22;
        GuiRenderHelper.renderSlot(g, slotX, slotY);

        // Render the bee item in the slot
        ItemStack bee = menu.getStoredBee();
        if (!bee.isEmpty()) {
            g.renderItem(bee, slotX + 1, slotY + 1);
        }

        // Progress bar
        int barX = x + (ANALYSIS_W - ANALYSIS_BAR_W) / 2;
        int barY = y + 50;
        int progress = menu.getAnalysisProgress();
        int duration = menu.getAnalysisDuration();

        // Bar background
        g.fill(barX, barY, barX + ANALYSIS_BAR_W, barY + ANALYSIS_BAR_H, 0xFF1A1A1A);
        g.fill(barX, barY, barX + ANALYSIS_BAR_W, barY + 1, 0xFF373737);
        g.fill(barX, barY, barX + 1, barY + ANALYSIS_BAR_H, 0xFF373737);
        g.fill(barX + 1, barY + ANALYSIS_BAR_H - 1, barX + ANALYSIS_BAR_W, barY + ANALYSIS_BAR_H, 0xFF555555);
        g.fill(barX + ANALYSIS_BAR_W - 1, barY + 1, barX + ANALYSIS_BAR_W, barY + ANALYSIS_BAR_H, 0xFF555555);

        // Bar fill
        if (duration > 0) {
            int fillW = (int) ((ANALYSIS_BAR_W - 2) * ((float) Math.min(progress, duration) / duration));
            if (fillW > 0) {
                g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + ANALYSIS_BAR_H - 1, 0xFF5588DD);
                g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + 2, 0xFF77AAFF);
            }
        }

        // Percentage text
        int pct = duration > 0 ? (int) (100f * Math.min(progress, duration) / duration) : 0;
        String pctText = pct + "%";
        int pctW = font.width(pctText);
        g.drawString(font, pctText, x + ANALYSIS_W / 2 - pctW / 2, barY + 2, 0xFFFFFFFF, false);

        // "Analyzing..." text
        String statusText = menu.isAnalysisComplete() ? "Complete!" : "Analyzing...";
        int statusW = font.width(statusText);
        int statusColor = menu.isAnalysisComplete() ? 0xFF44FF44 : 0xFF888888;
        g.drawString(font, statusText, x + ANALYSIS_W / 2 - statusW / 2, barY + ANALYSIS_BAR_H + 4, statusColor, false);

        // Finish button
        boolean complete = menu.isAnalysisComplete();
        int btnX = x + (ANALYSIS_W - ANALYSIS_BTN_W) / 2;
        int btnY = y + 88;

        int btnColor = complete ? 0xFF336633 : 0xFF333333;
        int btnBorder = complete ? 0xFF44AA44 : 0xFF555555;
        boolean btnHovered = complete && mouseX >= btnX && mouseX <= btnX + ANALYSIS_BTN_W
                && mouseY >= btnY && mouseY <= btnY + ANALYSIS_BTN_H;

        if (btnHovered) {
            btnColor = 0xFF448844;
            btnBorder = 0xFF55CC55;
        }

        // Button border + fill
        g.fill(btnX - 1, btnY - 1, btnX + ANALYSIS_BTN_W + 1, btnY + ANALYSIS_BTN_H + 1, btnBorder);
        g.fill(btnX, btnY, btnX + ANALYSIS_BTN_W, btnY + ANALYSIS_BTN_H, btnColor);

        // Button text
        String btnText = "Finish";
        int textColor = complete ? 0xFFFFFFFF : 0xFF666666;
        int btnTextW = font.width(btnText);
        g.drawString(font, btnText, btnX + ANALYSIS_BTN_W / 2 - btnTextW / 2,
                btnY + (ANALYSIS_BTN_H - font.lineHeight) / 2 + 1, textColor, false);
    }

    private void renderInfoPanel(GuiGraphics g, int px, int py) {
        int panelH = GUI_H;

        // Panel background
        g.fill(px, py, px + PANEL_W, py + panelH, 0xCC222222);
        g.fill(px, py, px + PANEL_W, py + 1, 0xFF444444);
        g.fill(px, py + panelH - 1, px + PANEL_W, py + panelH, 0xFF444444);
        g.fill(px, py, px + 1, py + panelH, 0xFF444444);
        g.fill(px + PANEL_W - 1, py, px + PANEL_W, py + panelH, 0xFF444444);

        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        int lineY = py + 4;
        int lineH = font.lineHeight + 2;

        // Bee slot visual
        GuiRenderHelper.renderSlot(g, px + PANEL_W / 2 - 8, lineY);
        if (!bee.isEmpty()) {
            g.renderItem(bee, px + PANEL_W / 2 - 7, lineY + 1);
        }
        lineY += 22;

        if (bee.isEmpty()) return;

        CodexPlayerData knowledge = getPlayerKnowledge();
        int[] levels = getStatLevels(data);
        List<CompatSpecies> compatibles = findCompatibleSpecies(speciesId);

        // Known Frequencies
        g.drawString(font, "Known:", px + 4, lineY, 0xFF88FF88, false);
        lineY += lineH;

        for (int i = 0; i < STAT_NAMES.length; i++) {
            String traitKey = STAT_NAMES[i] + ":" + levels[i];
            if (!knowledge.isTraitKnown(traitKey)) continue;
            ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
            if (wf == null) continue;
            String label = STAT_SHORT_LABELS[i] + " " + levels[i] + ": " + wf.frequency + "Hz";
            g.drawString(font, label, px + 4, lineY, STAT_COLORS[i], false);
            lineY += lineH;
        }

        for (CompatSpecies cs : compatibles) {
            if (!knowledge.isSpeciesKnown(cs.id)) continue;
            String label = capitalize(cs.id) + ": " + cs.freq + "Hz";
            g.drawString(font, label, px + 4, lineY, 0xFFDDAA88, false);
            lineY += lineH;
        }

        // Separator
        lineY += 2;
        g.fill(px + 4, lineY, px + PANEL_W - 4, lineY + 1, 0xFF444444);
        lineY += 4;

        // Unknown Frequencies
        g.drawString(font, "Unknown:", px + 4, lineY, 0xFFFF8888, false);
        lineY += lineH;

        for (int i = 0; i < STAT_NAMES.length; i++) {
            String traitKey = STAT_NAMES[i] + ":" + levels[i];
            if (knowledge.isTraitKnown(traitKey)) continue;
            g.drawString(font, "???: ???", px + 4, lineY, 0xFF666666, false);
            lineY += lineH;
        }

        for (CompatSpecies cs : compatibles) {
            if (knowledge.isSpeciesKnown(cs.id)) continue;
            g.drawString(font, "???: ???", px + 4, lineY, 0xFF666666, false);
            lineY += lineH;
        }
    }

    private static final String[] STAT_SHORT_LABELS = {"Drp", "Spd", "Frg", "Tol", "Act"};

    private record CompatSpecies(String id, int freq) {}

    private static List<CompatSpecies> findCompatibleSpecies(String speciesId) {
        if (speciesId == null) return List.of();
        BeeSpeciesManager.ensureClientLoaded();
        List<CompatSpecies> result = new ArrayList<>();
        for (BeeSpeciesManager.BeeSpeciesData sp : BeeSpeciesManager.getAllSpecies()) {
            if (sp.parents != null && sp.parents.contains(speciesId)) {
                result.add(new CompatSpecies(sp.id, computeSpeciesFrequency(sp)));
            }
        }
        return result;
    }

    private static int computeSpeciesFrequency(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return 0;
        int[] levels = getStatLevels(data);
        float totalWeight = 0;
        float weightedFreq = 0;
        for (int i = 0; i < STAT_NAMES.length; i++) {
            ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
            if (wf == null) continue;
            float weight = levels[i];
            totalWeight += weight;
            weightedFreq += wf.frequency * weight;
        }
        return totalWeight > 0 ? Math.round(weightedFreq / totalWeight) : 0;
    }

    private static CodexPlayerData getPlayerKnowledge() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getData(ApicaAttachments.CODEX_DATA);
        }
        return new CodexPlayerData();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void renderFreqSlider(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int sx = x + SLIDER_X;
        int sy = y + SLIDER_Y;

        g.fill(sx, sy, sx + SLIDER_W, sy + SLIDER_H, 0xFF1A1A1A);
        g.fill(sx, sy, sx + SLIDER_W, sy + 1, 0xFF373737);
        g.fill(sx, sy, sx + 1, sy + SLIDER_H, 0xFF373737);
        g.fill(sx + 1, sy + SLIDER_H - 1, sx + SLIDER_W, sy + SLIDER_H, 0xFFFFFFFF);
        g.fill(sx + SLIDER_W - 1, sy + 1, sx + SLIDER_W, sy + SLIDER_H, 0xFFFFFFFF);

        float ratio = (localFreq - FREQ_MIN) / (float) (FREQ_MAX - FREQ_MIN);
        int fillW = (int) ((SLIDER_W - 2) * ratio);
        if (fillW > 0) {
            g.fill(sx + 1, sy + 1, sx + 1 + fillW, sy + SLIDER_H - 1, 0xFF5588DD);
            g.fill(sx + 1, sy + 1, sx + 1 + fillW, sy + 2, 0xFF77AAFF);
        }

        // Tirets des traits de l'abeille (au-dessus du fill pour qu'ils soient visibles)
        if (menu.hasBee()) {
            renderStatTicks(g, sx, sy);
            renderSpeciesTicks(g, sx, sy);
        }

        int handleX = sx + 1 + fillW;
        g.fill(handleX - 2, sy - 1, handleX + 2, sy + SLIDER_H + 1, 0xFFDDDDDD);
        g.fill(handleX - 2, sy - 1, handleX + 2, sy, 0xFFFFFFFF);

        String hzText = localFreq + " Hz";
        g.drawString(font, hzText, x + 12, y + SLIDER_Y + 1, 0xFF88BBFF, false);
    }

    private static final String[] STAT_NAMES = {"drop", "speed", "foraging", "tolerance", "activity"};
    private static final int[] STAT_COLORS = {
            0xFFFF8844, // drop - orange
            0xFF44DDFF, // speed - cyan
            0xFF44FF66, // foraging - vert
            0xFFFFDD44, // tolerance - jaune
            0xFFDD44FF  // activity - violet
    };

    /**
     * Dessine un tiret vertical sur la barre Hz pour chaque trait de l'abeille.
     * La frequence est lue du JSON par stat ET par niveau du trait.
     */
    private void renderStatTicks(GuiGraphics g, int sx, int sy) {
        ResonatorConfigManager.ensureClientLoaded();
        BeeSpeciesManager.ensureClientLoaded();

        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        int[] levels = getStatLevels(data);

        for (int i = 0; i < STAT_NAMES.length; i++) {
            ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
            if (wf == null) continue;
            float tickRatio = (wf.frequency - FREQ_MIN) / (float) (FREQ_MAX - FREQ_MIN);
            int tickX = sx + 1 + (int) ((SLIDER_W - 2) * tickRatio);
            g.fill(tickX, sy - 2, tickX + 1, sy + SLIDER_H + 2, STAT_COLORS[i]);
        }
    }

    /**
     * Dessine un tiret blanc sur la barre Hz pour chaque espece compatible connue.
     */
    private void renderSpeciesTicks(GuiGraphics g, int sx, int sy) {
        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        if (speciesId == null) return;

        CodexPlayerData knowledge = getPlayerKnowledge();
        List<CompatSpecies> compatibles = findCompatibleSpecies(speciesId);

        for (CompatSpecies cs : compatibles) {
            if (!knowledge.isSpeciesKnown(cs.id)) continue;
            if (cs.freq < FREQ_MIN || cs.freq > FREQ_MAX) continue;
            float tickRatio = (cs.freq - FREQ_MIN) / (float) (FREQ_MAX - FREQ_MIN);
            int tickX = sx + 1 + (int) ((SLIDER_W - 2) * tickRatio);
            g.fill(tickX, sy - 3, tickX + 1, sy + SLIDER_H + 3, 0xFFDDAA88);
        }
    }

    private static int[] getStatLevels(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return new int[]{1, 1, 1, 1, 1};
        return new int[]{
                data.dropLevel,
                data.flyingSpeedLevel,
                data.foragingDurationLevel,
                data.toleranceLevel,
                getActivityLevel(data)
        };
    }

    private void renderKnob(GuiGraphics g, int cx, int cy, int radius,
                              float ratio, String label, String valueText,
                              int mouseX, int mouseY) {
        fillCircle(g, cx, cy, radius, 0xFF222222);
        fillCircle(g, cx, cy, radius - 2, 0xFF444444);
        fillCircle(g, cx, cy, radius - 3, 0xFF333333);

        float startAngle = KNOB_MIN_DEG;
        float endAngle = KNOB_MAX_DEG;
        float currentAngle = startAngle + ratio * (endAngle - startAngle);
        renderArc(g, cx, cy, radius - 1, startAngle, currentAngle, 0xFF5588DD);

        double angleRad = Math.toRadians(currentAngle - 90);
        int px = cx + (int) (Math.cos(angleRad) * (radius - 5));
        int py = cy + (int) (Math.sin(angleRad) * (radius - 5));
        renderLine(g, cx, cy, px, py, 0xFFFFFFFF);

        fillCircle(g, cx, cy, 3, 0xFF666666);

        int labelW = font.width(label);
        g.drawString(font, label, cx - labelW / 2, cy + radius + 4, 0xFF888888, false);

        int valW = font.width(valueText);
        g.drawString(font, valueText, cx - valW / 2, cy - radius - 12, 0xFFAABBDD, false);
    }

    private void fillCircle(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private void renderArc(GuiGraphics g, int cx, int cy, int radius,
                            float startDeg, float endDeg, int color) {
        for (float deg = startDeg; deg <= endDeg; deg += 2.0f) {
            double rad = Math.toRadians(deg - 90);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

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

        // Analysis mode: only handle Finish button
        if (menu.isAnalysisMode()) {
            if (menu.isAnalysisComplete()) {
                int x = (width - ANALYSIS_W) / 2;
                int y = (height - ANALYSIS_H) / 2;
                int btnX = x + (ANALYSIS_W - ANALYSIS_BTN_W) / 2;
                int btnY = y + 88;
                if (mouseX >= btnX && mouseX <= btnX + ANALYSIS_BTN_W
                        && mouseY >= btnY && mouseY <= btnY + ANALYSIS_BTN_H) {
                    PacketDistributor.sendToServer(new ResonatorFinishPacket(menu.getBlockPos()));
                    onClose();
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

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
        if (menu.isAnalysisMode()) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (button != 0 || dragIndex < 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        int x = (width - imageWidth) / 2;

        if (dragIndex == 0) {
            updateSliderFromMouse(mouseX, x);
        } else {
            int deltaX = dragStartX - (int) mouseX;
            int sensitivity = 2;

            switch (dragIndex) {
                case 1 -> {
                    localAmp = clamp(dragStartValue + deltaX / sensitivity, 0, 100);
                    sendUpdate(1, localAmp);
                }
                case 2 -> {
                    localPhase = clamp(dragStartValue + deltaX / sensitivity * 3, 0, 360);
                    sendUpdate(2, localPhase);
                }
                case 3 -> {
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
        if (menu.isAnalysisMode()) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int knobBaseX = x + GUI_W / 2;
        int ky = y + KNOB_Y;

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
        BlockPos pos = menu.getBlockPos();
        if (pos != null) {
            PacketDistributor.sendToServer(new ResonatorUpdatePacket(pos, paramIndex, value));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Override to prevent default title/inventory label rendering
    }
}
