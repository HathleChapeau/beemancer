/**
 * ============================================================
 * [ResonationNoteRenderer.java]
 * Description: Rendu du contenu d'une sticky note Resonation dans le Codex Book
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorConfigManager  | Waveforms par stat   | Lecture freq/amp/phase         |
 * | BeeSpeciesManager       | Stats espece         | Niveaux de base des traits     |
 * | CodexPlayerData         | Knowledge joueur     | Verification traits connus     |
 * | ApicaAttachments        | Attachement data     | Acces CodexPlayerData          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexBookScreen (rendu overlay sticky note resonation)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen.codex;

import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.config.ResonatorConfigManager;
import com.chapeau.apica.core.registry.ApicaAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ResonationNoteRenderer {

    private static final int LABEL_COLOR = 0xFF3B2A1A;
    private static final int VALUE_COLOR = 0xFF1A5A8A;
    private static final int UNKNOWN_COLOR = 0xFF888888;
    private static final int SPECIES_COLOR = 0xFF2A6B3A;

    private static final String[] STAT_NAMES = {"drop", "speed", "foraging", "tolerance", "activity"};
    private static final String[] STAT_LABELS = {"Drop", "Speed", "Foraging", "Tolerance", "Activity"};

    /**
     * Rend le contenu de la note Resonation a l'interieur de l'overlay sticky note.
     * @param contentX X du debut du contenu (apres marge gauche)
     * @param contentY Y du debut du contenu (sous le titre + separateur)
     * @param contentW Largeur disponible
     */
    public static void render(GuiGraphics g, Font font, String speciesId,
                               int contentX, int contentY, int contentW) {
        ResonatorConfigManager.ensureClientLoaded();
        BeeSpeciesManager.ensureClientLoaded();

        CodexPlayerData knowledge = getPlayerKnowledge();
        BeeSpeciesManager.BeeSpeciesData data = speciesId != null
                ? BeeSpeciesManager.getSpecies(speciesId) : null;

        int y = contentY;
        int lineH = font.lineHeight + 3;

        // Species name + combined wave
        y = renderSpeciesLine(g, font, speciesId, data, knowledge, contentX, y, lineH);

        // Separator
        y += 2;
        g.fill(contentX, y, contentX + contentW, y + 1, 0x40000000);
        y += 4;

        // 5 traits
        int[] levels = getStatLevels(data);
        for (int i = 0; i < STAT_NAMES.length; i++) {
            y = renderTraitLine(g, font, STAT_NAMES[i], STAT_LABELS[i], levels[i],
                    data, knowledge, contentX, y, lineH);
        }
    }

    private static int renderSpeciesLine(GuiGraphics g, Font font, String speciesId,
                                          BeeSpeciesManager.BeeSpeciesData data,
                                          CodexPlayerData knowledge,
                                          int x, int y, int lineH) {
        boolean speciesKnown = speciesId != null && knowledge.isSpeciesKnown(speciesId);

        if (!speciesKnown) {
            g.drawString(font, "???: ???", x, y, UNKNOWN_COLOR, false);
            return y + lineH;
        }

        // Species name
        String name = capitalize(speciesId);
        g.drawString(font, name, x, y, SPECIES_COLOR, false);
        y += lineH;

        // Combined wave
        String combinedWave = computeCombinedWave(data);
        g.drawString(font, combinedWave, x + 4, y, VALUE_COLOR, false);
        return y + lineH;
    }

    private static int renderTraitLine(GuiGraphics g, Font font, String statName, String label,
                                        int level, BeeSpeciesManager.BeeSpeciesData data,
                                        CodexPlayerData knowledge,
                                        int x, int y, int lineH) {
        String traitKey = statName + ":" + level;
        boolean known = knowledge.isTraitKnown(traitKey);

        if (!known) {
            g.drawString(font, "???: ???", x, y, UNKNOWN_COLOR, false);
            return y + lineH;
        }

        // Activity uses special display (Day/Night/Both)
        if ("activity".equals(statName)) {
            String activityName = getActivityName(data);
            g.drawString(font, label + ": " + activityName, x, y, LABEL_COLOR, false);
        } else {
            g.drawString(font, label + " " + level, x, y, LABEL_COLOR, false);
        }

        // Wave values
        ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(statName, level);
        if (wf != null) {
            String waveText = formatWave(wf.frequency, wf.amplitude, wf.phase);
            g.drawString(font, waveText, x + 4, y + lineH, VALUE_COLOR, false);
            return y + lineH * 2;
        }

        // Activity level 0 (Day) has no waveform
        return y + lineH;
    }

    private static String computeCombinedWave(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return "---";

        int[] levels = getStatLevels(data);
        float totalWeight = 0;
        float weightedFreq = 0;
        float weightedAmp = 0;
        float weightedPhase = 0;

        for (int i = 0; i < STAT_NAMES.length; i++) {
            ResonatorConfigManager.StatWaveform wf =
                    ResonatorConfigManager.getStatWaveform(STAT_NAMES[i], levels[i]);
            if (wf == null) continue;
            float weight = levels[i];
            totalWeight += weight;
            weightedFreq += wf.frequency * weight;
            weightedAmp += wf.amplitude * weight;
            weightedPhase += wf.phase * weight;
        }

        if (totalWeight <= 0) return "---";

        int freq = Math.round(weightedFreq / totalWeight);
        int amp = Math.round(weightedAmp / totalWeight);
        int phase = Math.round(weightedPhase / totalWeight) % 360;
        return formatWave(freq, amp, phase);
    }

    private static int[] getStatLevels(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return new int[]{1, 1, 1, 1, 0};
        return new int[]{
                data.dropLevel,
                data.flyingSpeedLevel,
                data.foragingDurationLevel,
                data.toleranceLevel,
                getActivityLevel(data)
        };
    }

    private static int getActivityLevel(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return 0;
        return switch (data.dayNight) {
            case "night" -> 1;
            case "both" -> 2;
            default -> 0;
        };
    }

    private static String getActivityName(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return "Day";
        return switch (data.dayNight) {
            case "night" -> "Night";
            case "both" -> "Both";
            default -> "Day";
        };
    }

    private static String formatWave(int freq, int amp, int phase) {
        return freq + "Hz / " + String.format("%.1f", amp / 100.0f) + "A / " + phase + "\u00B0";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static CodexPlayerData getPlayerKnowledge() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getData(ApicaAttachments.CODEX_DATA);
        }
        return new CodexPlayerData();
    }
}
