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
 * | BeeSpeciesManager       | Stats + waveform     | Donnees espece et courbe       |
 * | ResonatorConfigManager  | Waveforms par stat   | Lecture freq/amp/phase traits  |
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResonationNoteRenderer {

    private static final int LABEL_COLOR = 0xFF3B2A1A;
    private static final int UNKNOWN_COLOR = 0xFF888888;
    private static final int SPECIES_COLOR = 0xFF2A6B3A;
    private static final int COMPAT_COLOR = 0xFF6B4A2A;
    private static final int HARMONIZED_COLOR = 0xFF8B5CF6;
    private static final int HEADER_COLOR = 0xFF3B2A1A;

    private static final String[] STAT_NAMES = {"drop", "speed", "foraging", "tolerance", "activity"};
    private static final String[] STAT_LABELS = {"Drop", "Speed", "Foraging", "Tolerance", "Activity"};

    /**
     * Calcule la hauteur necessaire pour le contenu de la note resonation.
     * Utilise par CodexBookScreen pour dimensionner l'overlay.
     */
    public static int getContentHeight(Font font, String speciesId) {
        int lineH = font.lineHeight + 3;
        // Species line + separator + 5 traits
        int h = lineH + 5 + (5 * lineH);

        // Compatible section
        List<CompatParent> compatibles = findCompatibleParents(speciesId);
        List<CompatParent> normal = compatibles.stream().filter(cp -> !cp.harmonized).toList();
        List<CompatParent> harmonized = compatibles.stream().filter(cp -> cp.harmonized).toList();

        if (!normal.isEmpty()) {
            h += 5; // separator
            h += lineH; // "Compatible:" header
            h += normal.size() * lineH;
        }
        if (!harmonized.isEmpty()) {
            h += 5; // separator
            h += lineH; // "Harmonized:" header
            h += harmonized.size() * lineH;
        }
        return h;
    }

    /**
     * Rend le contenu de la note Resonation a l'interieur de l'overlay sticky note.
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

        // Species name + combined wave (one line) — 3 states
        boolean speciesKnown = speciesId != null && knowledge.isSpeciesKnown(speciesId);
        boolean freqKnown = speciesId != null && knowledge.isFrequencyKnown(speciesId);
        if (speciesKnown) {
            String combined = getSpeciesWave(data);
            g.drawString(font, capitalize(speciesId) + ": " + combined, contentX, y, SPECIES_COLOR, false);
        } else if (freqKnown) {
            String combined = getSpeciesWave(data);
            g.drawString(font, "???: " + combined, contentX, y, UNKNOWN_COLOR, false);
        } else {
            g.drawString(font, "???: ???", contentX, y, UNKNOWN_COLOR, false);
        }
        y += lineH;

        // Separator
        y += 1;
        g.fill(contentX, y, contentX + contentW, y + 1, 0x40000000);
        y += 4;

        // 5 traits (each on one line)
        int[] levels = getStatLevels(data);
        for (int i = 0; i < STAT_NAMES.length; i++) {
            String traitKey = STAT_NAMES[i] + ":" + levels[i];
            boolean known = knowledge.isTraitKnown(traitKey);

            if (!known) {
                g.drawString(font, "???: ???", contentX, y, UNKNOWN_COLOR, false);
            } else {
                String line = buildTraitLine(STAT_NAMES[i], STAT_LABELS[i], levels[i], data);
                g.drawString(font, line, contentX, y, LABEL_COLOR, false);
            }
            y += lineH;
        }

        // Compatible breeding partners (other parent species)
        List<CompatParent> compatibles = findCompatibleParents(speciesId);
        List<CompatParent> normal = compatibles.stream().filter(cp -> !cp.harmonized).toList();
        List<CompatParent> harmonized = compatibles.stream().filter(cp -> cp.harmonized).toList();

        // Normal compatible species
        if (!normal.isEmpty()) {
            y += 1;
            g.fill(contentX, y, contentX + contentW, y + 1, 0x40000000);
            y += 4;

            g.drawString(font, "Compatible:", contentX, y, HEADER_COLOR, false);
            y += lineH;

            for (CompatParent cp : normal) {
                y = renderCompatEntry(g, font, knowledge, cp, contentX, y, lineH, COMPAT_COLOR);
            }
        }

        // Harmonized compatible species (purple)
        if (!harmonized.isEmpty()) {
            y += 1;
            g.fill(contentX, y, contentX + contentW, y + 1, 0x40000000);
            y += 4;

            g.drawString(font, "Harmonized:", contentX, y, HARMONIZED_COLOR, false);
            y += lineH;

            for (CompatParent cp : harmonized) {
                y = renderCompatEntry(g, font, knowledge, cp, contentX, y, lineH, HARMONIZED_COLOR);
            }
        }
    }

    /**
     * Rend une entree de parent compatible et retourne le y suivant.
     */
    private static int renderCompatEntry(GuiGraphics g, Font font, CodexPlayerData knowledge,
                                          CompatParent cp, int x, int y, int lineH, int color) {
        boolean parentSpeciesKnown = knowledge.isSpeciesKnown(cp.id);
        boolean parentFreqKnown = knowledge.isFrequencyKnown(cp.id);
        String wave = formatWave(cp.freq, cp.amp, cp.phase, cp.harm);

        if (parentSpeciesKnown) {
            g.drawString(font, capitalize(cp.id) + ": " + wave, x, y, color, false);
        } else if (parentFreqKnown) {
            g.drawString(font, "???: " + wave, x, y, UNKNOWN_COLOR, false);
        } else {
            g.drawString(font, "???: ???", x, y, UNKNOWN_COLOR, false);
        }
        return y + lineH;
    }

    private record CompatParent(String id, int freq, int amp, int phase, int harm, boolean harmonized) {}

    /**
     * Pour chaque espece enfant ayant speciesId comme parent,
     * retourne l'AUTRE parent (celui avec lequel on doit croiser l'abeille).
     * Le flag harmonized est true si l'enfant qui lie les deux parents est harmonized.
     */
    private static List<CompatParent> findCompatibleParents(String speciesId) {
        if (speciesId == null) return List.of();
        BeeSpeciesManager.ensureClientLoaded();
        List<CompatParent> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (BeeSpeciesManager.BeeSpeciesData sp : BeeSpeciesManager.getAllSpecies()) {
            if (sp.parents != null && sp.parents.contains(speciesId)) {
                for (String parentId : sp.parents) {
                    if (!parentId.equals(speciesId) && seen.add(parentId)) {
                        BeeSpeciesManager.BeeSpeciesData parentData = BeeSpeciesManager.getSpecies(parentId);
                        if (parentData != null) {
                            result.add(new CompatParent(
                                    parentId,
                                    parentData.waveformFreq,
                                    parentData.waveformAmp,
                                    parentData.waveformPhase,
                                    parentData.waveformHarm,
                                    sp.harmonized
                            ));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Retourne la courbe combinee de l'espece depuis ses donnees pre-calculees.
     */
    private static String getSpeciesWave(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return "---";
        return formatWave(data.waveformFreq, data.waveformAmp, data.waveformPhase, data.waveformHarm);
    }

    private static String buildTraitLine(String statName, String label, int level,
                                          BeeSpeciesManager.BeeSpeciesData data) {
        String prefix;
        if ("activity".equals(statName)) {
            prefix = label + " (" + getActivityName(data) + ")";
        } else {
            prefix = label + " " + level;
        }

        ResonatorConfigManager.StatWaveform wf = ResonatorConfigManager.getStatWaveform(statName, level);
        if (wf == null) {
            return prefix + ": ---";
        }
        return prefix + ": " + formatWave(wf.frequency, wf.amplitude, wf.phase, wf.harmonics);
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

    private static int getActivityLevel(BeeSpeciesManager.BeeSpeciesData data) {
        if (data == null) return 1;
        return switch (data.dayNight) {
            case "night" -> 2;
            case "both" -> 3;
            default -> 1;
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

    private static String formatWave(int freq, int amp, int phase, int harm) {
        return freq + "Hz " + String.format("%.1f", amp / 100.0f) + "A "
                + phase + "\u00B0 " + String.format("%.1f", harm / 100.0f) + "H";
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
