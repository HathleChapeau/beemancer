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
 * | (rendu inline)          | Rendu GUI            | Background, bordures, slot     |
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
    private static final int FREQ_MAX = 130;

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

    // Info panel pagination
    private static final int PAGE_SIZE = 8;
    private int infoPage = 0;
    private int infoPageCount = 1;

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

    // Log throttle for undiscovered trait hint
    private static final int DETECTION_RANGE = 20;
    private int lastLoggedFreq = -1;
    private int lastLoggedAmp = -1;
    private int lastLoggedPhase = -1;
    private int lastLoggedHarm = -1;

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
     * Lit les target values directement depuis la waveform de l'espece.
     */
    private void generateTargetsFromBee(ItemStack bee) {
        BeeSpeciesManager.ensureClientLoaded();

        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData speciesData = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        if (speciesData != null) {
            targetFreq = clamp(speciesData.waveformFreq, FREQ_MIN, FREQ_MAX);
            targetAmp = clamp(speciesData.waveformAmp, 0, 100);
            targetPhase = clamp(speciesData.waveformPhase % 360, 0, 360);
            targetHarm = clamp(speciesData.waveformHarm, 0, 100);
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
        if (data == null) return 1;
        return switch (data.dayNight) {
            case "night" -> 2;
            case "both" -> 3;
            default -> 1;
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
        logClosestUndiscoveredTrait();
    }

    private void logClosestUndiscoveredTrait() {
        if (localFreq == lastLoggedFreq && localAmp == lastLoggedAmp
                && localPhase == lastLoggedPhase && localHarm == lastLoggedHarm) return;
        lastLoggedFreq = localFreq;
        lastLoggedAmp = localAmp;
        lastLoggedPhase = localPhase;
        lastLoggedHarm = localHarm;

        if (!menu.hasBee()) return;

        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;
        if (data == null) return;

        ResonatorConfigManager.ensureClientLoaded();
        BeeSpeciesManager.ensureClientLoaded();
        CodexPlayerData knowledge = getPlayerKnowledge();
        int[] levels = getStatLevels(data);

        String closestName = null;
        int closestHz = 0;
        int closestGap = Integer.MAX_VALUE;
        int closestAmp = 0;
        int closestPhase = 0;
        int closestHarm = 0;

        // Stat traits (drop, speed, foraging, tolerance, activity)
        for (int i = 0; i < STAT_NAMES.length; i++) {
            String traitKey = STAT_NAMES[i] + ":" + levels[i];
            if (knowledge.isTraitKnown(traitKey)) continue;
            ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
            if (wf == null) continue;
            int gap = Math.abs(wf.frequency - localFreq);
            if (gap < closestGap) {
                closestGap = gap;
                closestHz = wf.frequency;
                closestName = capitalize(STAT_NAMES[i]) + " Lv" + levels[i];
                closestAmp = wf.amplitude;
                closestPhase = wf.phase;
                closestHarm = wf.harmonics;
            }
        }

        // Bee's own species frequency (if not yet discovered)
        if (!knowledge.isFrequencyKnown(speciesId) && !knowledge.isSpeciesKnown(speciesId)) {
            int gap = Math.abs(data.waveformFreq - localFreq);
            if (gap < closestGap) {
                closestGap = gap;
                closestHz = data.waveformFreq;
                closestName = "Species " + capitalize(speciesId);
                closestAmp = data.waveformAmp;
                closestPhase = data.waveformPhase;
                closestHarm = data.waveformHarm;
            }
        }

        // Compatible species (if frequency not yet discovered)
        List<CompatSpecies> compatibles = findCompatibleSpecies(speciesId);
        for (CompatSpecies cs : compatibles) {
            if (knowledge.isSpeciesKnown(cs.id) || knowledge.isFrequencyKnown(cs.id)) continue;
            int gap = Math.abs(cs.freq - localFreq);
            if (gap < closestGap) {
                closestGap = gap;
                closestHz = cs.freq;
                closestName = "Compat " + capitalize(cs.id);
                closestAmp = cs.amp;
                closestPhase = cs.phase;
                closestHarm = cs.harm;
            }
        }

        if (closestName == null) return;

        // Proximity: 0 at edge of DETECTION_RANGE, ±1 at exact match, sign = side
        float proximity;
        if (closestGap >= DETECTION_RANGE) {
            proximity = 0f;
        } else {
            float magnitude = 1f - (float) closestGap / DETECTION_RANGE;
            int sign = localFreq >= closestHz ? 1 : -1;
            if (closestGap == 0) sign = 1;
            proximity = magnitude * sign;
        }

        System.out.println("[Apica Resonator] Closest undiscovered: " + closestName
                + " at " + closestHz + "Hz | Cursor: " + localFreq + "Hz | Gap: " + closestGap + "Hz"
                + " | Proximity: " + String.format("%+.0f%%", proximity * 100));

        // Per-parameter difference: current potard value vs trait target value + % diff
        float freqDiff = (localFreq - closestHz) / (float) (FREQ_MAX - FREQ_MIN) * 100f;
        float ampDiff = (localAmp - closestAmp);
        float phaseDiff = (localPhase - closestPhase) / 360f * 100f;
        float harmDiff = (localHarm - closestHarm);

        System.out.println("[Apica Resonator]   FREQ: " + localFreq + " vs " + closestHz
                + " (" + String.format("%+.1f%%", freqDiff) + ")"
                + " | AMP: " + localAmp + " vs " + closestAmp
                + " (" + String.format("%+.1f%%", ampDiff) + ")"
                + " | PHASE: " + localPhase + "\u00B0 vs " + closestPhase + "\u00B0"
                + " (" + String.format("%+.1f%%", phaseDiff) + ")"
                + " | HARM: " + localHarm + " vs " + closestHarm
                + " (" + String.format("%+.1f%%", harmDiff) + ")");
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

        // Background — dark semi-transparent panel
        g.fill(x, y, x + GUI_W, y + GUI_H, 0xCC1A1A2E);
        g.fill(x, y, x + GUI_W, y + 1, 0xFF555555);
        g.fill(x, y + GUI_H - 1, x + GUI_W, y + GUI_H, 0xFF555555);
        g.fill(x, y, x + 1, y + GUI_H, 0xFF555555);
        g.fill(x + GUI_W - 1, y, x + GUI_W, y + GUI_H, 0xFF555555);

        // Title (above panel)
        g.drawCenteredString(font, Component.translatable("block.apica.resonator"),
                x + GUI_W / 2, y - 10, 0xDDDDDD);

        // Bee slot (right side) — vanilla style
        renderVanillaSlot(g, x + BEE_SLOT_X, y + BEE_SLOT_Y);

        // Waveform display
        float freqF = localFreq;
        float ampF = localAmp / 100.0f;
        float phaseF = localPhase;
        float harmF = localHarm / 100.0f;
        WaveformRenderer.render(g, x + WAVE_X, y + WAVE_Y, WAVE_W, WAVE_H,
                freqF, ampF, phaseF, harmF);

        // Frequency slider
        renderFreqSlider(g, x, y, mouseX, mouseY);

        // 3 spinning cursors (decorative, between slider and knobs)
        long time = System.currentTimeMillis() % 360000L;
        int cursorY = y + 105;
        int cursorBaseX = x + GUI_W / 2;
        float[] speeds = {1.0f, 1.7f, 2.3f};
        for (int i = 0; i < 3; i++) {
            int cx = cursorBaseX + (i - 1) * 50;
            float angle = (time * speeds[i] * 0.1f) % 360f;
            renderSpinningCursor(g, cx, cursorY, 10, angle);
        }

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

        // Background panel — dark semi-transparent
        g.fill(x, y, x + ANALYSIS_W, y + ANALYSIS_H, 0xCC1A1A2E);
        g.fill(x, y, x + ANALYSIS_W, y + 1, 0xFF555555);
        g.fill(x, y + ANALYSIS_H - 1, x + ANALYSIS_W, y + ANALYSIS_H, 0xFF555555);
        g.fill(x, y, x + 1, y + ANALYSIS_H, 0xFF555555);
        g.fill(x + ANALYSIS_W - 1, y, x + ANALYSIS_W, y + ANALYSIS_H, 0xFF555555);

        // Title (above panel)
        g.drawCenteredString(font, "Analysis", x + ANALYSIS_W / 2, y - 10, 0xDDDDDD);

        // Bee slot (centered top) — vanilla style
        int slotX = x + ANALYSIS_W / 2 - 8;
        int slotY = y + 22;
        renderVanillaSlot(g, slotX, slotY);

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

        // Panel background — dark semi-transparent (same as main panel)
        g.fill(px, py, px + PANEL_W, py + panelH, 0xCC1A1A2E);
        g.fill(px, py, px + PANEL_W, py + 1, 0xFF555555);
        g.fill(px, py + panelH - 1, px + PANEL_W, py + panelH, 0xFF555555);
        g.fill(px, py, px + 1, py + panelH, 0xFF555555);
        g.fill(px + PANEL_W - 1, py, px + PANEL_W, py + panelH, 0xFF555555);

        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        int lineY = py + 4;
        int lineH = font.lineHeight + 2;

        // Bee slot visual — vanilla style
        renderVanillaSlot(g, px + PANEL_W / 2 - 8, lineY);
        if (!bee.isEmpty()) {
            g.renderItem(bee, px + PANEL_W / 2 - 7, lineY + 1);
        }
        lineY += 22;

        if (bee.isEmpty()) return;

        CodexPlayerData knowledge = getPlayerKnowledge();
        ResonatorConfigManager.ensureClientLoaded();
        BeeSpeciesManager.ensureClientLoaded();

        // Bee species + its frequency (3 states)
        String beeHz = data != null ? data.waveformFreq + "Hz" : "---";
        if (knowledge.isSpeciesKnown(speciesId)) {
            g.drawString(font, capitalize(speciesId) + ": " + beeHz, px + 4, lineY, 0xFF88BBFF, false);
        } else if (knowledge.isFrequencyKnown(speciesId)) {
            g.drawString(font, "???: " + beeHz, px + 4, lineY, 0xFF88BBFF, false);
        } else {
            g.drawString(font, "???: ???", px + 4, lineY, 0xFF666666, false);
        }
        lineY += lineH + 2;
        g.fill(px + 4, lineY, px + PANEL_W - 4, lineY + 1, 0xFF555555);
        lineY += 4;

        // Build entry lists
        int[] levels = getStatLevels(data);
        List<CompatSpecies> compatibles = findCompatibleSpecies(speciesId);
        List<FreqEntry> allEntries = buildFreqEntries(knowledge, levels, compatibles);

        // Paginate (PAGE_SIZE entries per page)
        infoPageCount = Math.max(1, (allEntries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        infoPage = clamp(infoPage, 0, infoPageCount - 1);

        int start = infoPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allEntries.size());

        for (int i = start; i < end; i++) {
            FreqEntry entry = allEntries.get(i);
            g.drawString(font, entry.text, px + 4, lineY, entry.color, false);
            lineY += lineH;
        }

        // Page navigation (bottom of panel)
        if (infoPageCount > 1) {
            int navY = py + panelH - 14;
            String pageText = (infoPage + 1) + "/" + infoPageCount;
            int pageW = font.width(pageText);
            g.drawString(font, pageText, px + PANEL_W / 2 - pageW / 2, navY, 0xFFAAAAAA, false);

            // Left arrow
            if (infoPage > 0) {
                g.drawString(font, "<", px + 4, navY, 0xFFDDDDDD, false);
            }
            // Right arrow
            if (infoPage < infoPageCount - 1) {
                g.drawString(font, ">", px + PANEL_W - 10, navY, 0xFFDDDDDD, false);
            }
        }
    }

    private record FreqEntry(String text, int color) {}

    private static final int HARMONIZED_COLOR = 0xFF8B5CF6;
    private static final int COMPAT_COLOR = 0xFFDDAA88;

    private List<FreqEntry> buildFreqEntries(CodexPlayerData knowledge, int[] levels,
                                              List<CompatSpecies> compatibles) {
        List<FreqEntry> entries = new ArrayList<>();

        // Traits (known = details, unknown = ???)
        for (int i = 0; i < STAT_NAMES.length; i++) {
            String traitKey = STAT_NAMES[i] + ":" + levels[i];
            if (knowledge.isTraitKnown(traitKey)) {
                ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
                if (wf != null) {
                    entries.add(new FreqEntry(STAT_SHORT_LABELS[i] + " " + levels[i] + ": "
                            + wf.frequency + "Hz", STAT_COLORS[i]));
                } else {
                    entries.add(new FreqEntry(STAT_SHORT_LABELS[i] + " " + levels[i] + ": ---", STAT_COLORS[i]));
                }
            } else {
                entries.add(new FreqEntry("???: ???", 0xFF666666));
            }
        }

        // Normal compatible species (non-harmonized links)
        List<CompatSpecies> normal = compatibles.stream().filter(cs -> !cs.harmonized).toList();
        if (!normal.isEmpty()) {
            entries.add(new FreqEntry("", 0));
            for (CompatSpecies cs : normal) {
                entries.add(buildCompatEntry(knowledge, cs, COMPAT_COLOR));
            }
        }

        // Harmonized compatible species
        List<CompatSpecies> harmonized = compatibles.stream().filter(cs -> cs.harmonized).toList();
        if (!harmonized.isEmpty()) {
            entries.add(new FreqEntry("", 0));
            entries.add(new FreqEntry("Harmonized:", HARMONIZED_COLOR));
            for (CompatSpecies cs : harmonized) {
                entries.add(buildCompatEntry(knowledge, cs, HARMONIZED_COLOR));
            }
        }

        return entries;
    }

    private FreqEntry buildCompatEntry(CodexPlayerData knowledge, CompatSpecies cs, int color) {
        String hz = cs.freq + "Hz";
        if (knowledge.isSpeciesKnown(cs.id)) {
            return new FreqEntry(capitalize(cs.id) + ": " + hz, color);
        } else if (knowledge.isFrequencyKnown(cs.id)) {
            return new FreqEntry("???: " + hz, color);
        } else {
            return new FreqEntry("???: ???", 0xFF666666);
        }
    }

    private static final String[] STAT_SHORT_LABELS = {"Drp", "Spd", "Frg", "Tol", "Act"};

    private record CompatSpecies(String id, int freq, int amp, int phase, int harm, boolean harmonized) {}

    /**
     * Pour chaque espece enfant ayant speciesId comme parent,
     * retourne l'AUTRE parent avec sa courbe et si l'enfant est harmonized.
     */
    private static List<CompatSpecies> findCompatibleSpecies(String speciesId) {
        if (speciesId == null) return List.of();
        BeeSpeciesManager.ensureClientLoaded();
        List<CompatSpecies> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (BeeSpeciesManager.BeeSpeciesData child : BeeSpeciesManager.getAllSpecies()) {
            if (child.parents != null && child.parents.contains(speciesId)) {
                for (String parentId : child.parents) {
                    if (!parentId.equals(speciesId) && seen.add(parentId)) {
                        BeeSpeciesManager.BeeSpeciesData parentData = BeeSpeciesManager.getSpecies(parentId);
                        if (parentData != null) {
                            result.add(new CompatSpecies(parentId,
                                    parentData.waveformFreq, parentData.waveformAmp,
                                    parentData.waveformPhase, parentData.waveformHarm,
                                    child.harmonized));
                        }
                    }
                }
            }
        }
        return result;
    }

    private static String formatWave(int freq, int amp, int phase, int harm) {
        return freq + "Hz " + String.format("%.1f", amp / 100.0f) + "A "
                + phase + "\u00B0 " + String.format("%.1f", harm / 100.0f) + "H";
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
     * Dessine un tiret sur la barre Hz pour chaque espece compatible dont la frequence est connue.
     */
    private void renderSpeciesTicks(GuiGraphics g, int sx, int sy) {
        ItemStack bee = menu.getStoredBee();
        String speciesId = getSpeciesFromBee(bee);
        if (speciesId == null) return;

        CodexPlayerData knowledge = getPlayerKnowledge();
        List<CompatSpecies> compatibles = findCompatibleSpecies(speciesId);

        for (CompatSpecies cs : compatibles) {
            if (!knowledge.isSpeciesKnown(cs.id) && !knowledge.isFrequencyKnown(cs.id)) continue;
            if (cs.freq < FREQ_MIN || cs.freq > FREQ_MAX) continue;
            float tickRatio = (cs.freq - FREQ_MIN) / (float) (FREQ_MAX - FREQ_MIN);
            int tickX = sx + 1 + (int) ((SLIDER_W - 2) * tickRatio);
            int tickColor = cs.harmonized ? HARMONIZED_COLOR : COMPAT_COLOR;
            g.fill(tickX, sy - 3, tickX + 1, sy + SLIDER_H + 3, tickColor);
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

    private void renderSpinningCursor(GuiGraphics g, int cx, int cy, int radius, float angleDeg) {
        fillCircle(g, cx, cy, radius, 0xFF222222);
        fillCircle(g, cx, cy, radius - 2, 0xFF444444);
        fillCircle(g, cx, cy, radius - 3, 0xFF333333);

        renderArc(g, cx, cy, radius - 1, 0f, 360f, 0xFF5588DD);

        double angleRad = Math.toRadians(angleDeg - 90);
        int px = cx + (int) (Math.cos(angleRad) * (radius - 4));
        int py = cy + (int) (Math.sin(angleRad) * (radius - 4));
        renderLine(g, cx, cy, px, py, 0xFFFFFFFF);

        fillCircle(g, cx, cy, 2, 0xFF666666);
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

        // Check info panel page arrows
        if (infoPageCount > 1) {
            int px = x - PANEL_GAP - PANEL_W;
            int py = y;
            int navY = py + GUI_H - 14;
            // Left arrow "<"
            if (mouseX >= px + 2 && mouseX <= px + 14 && mouseY >= navY - 2 && mouseY <= navY + font.lineHeight + 2) {
                if (infoPage > 0) {
                    infoPage--;
                    return true;
                }
            }
            // Right arrow ">"
            if (mouseX >= px + PANEL_W - 14 && mouseX <= px + PANEL_W - 2 && mouseY >= navY - 2 && mouseY <= navY + font.lineHeight + 2) {
                if (infoPage < infoPageCount - 1) {
                    infoPage++;
                    return true;
                }
            }
        }

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

    /**
     * Rend un slot 18x18 avec le style vanilla Minecraft (bords 3D biseautes).
     */
    private static void renderVanillaSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFC6C6C6);
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
