/**
 * ============================================================
 * [WaveformRenderer.java]
 * Description: Moteur de rendu d'onde par synthese additive (Fourier)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | API de rendu         | fill() pour dessiner l'onde    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorScreen (affichage de l'onde)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Rendu d'une onde par synthese additive.
 * Supporte la modulation de frequence, amplitude, phase et harmoniques.
 * L'onde scrolle en temps reel pour un effet oscilloscope vivant.
 */
public class WaveformRenderer {

    private static final int BG_COLOR = 0xFF111111;
    private static final int GRID_COLOR = 0xFF1A3A1A;
    private static final int GRID_CENTER_COLOR = 0xFF2A5A2A;
    private static final int WAVE_COLOR = 0xFF00FF88;
    private static final int WAVE_GLOW_COLOR = 0x4000FF88;
    private static final int BORDER_COLOR = 0xFF333333;
    private static final int MAX_HARMONICS = 15;

    /**
     * Dessine l'onde complete dans la zone specifiee.
     *
     * @param g         graphics context
     * @param x         coin superieur gauche X
     * @param y         coin superieur gauche Y
     * @param w         largeur de la zone
     * @param h         hauteur de la zone
     * @param freqHz    frequence en Hz (1-80)
     * @param amplitude amplitude 0.0-1.0
     * @param phaseDeg  phase en degres (0-360)
     * @param harmRatio ratio d'harmoniques 0.0-1.0 (0=sine pure, 1=quasi-carre)
     */
    public static void render(GuiGraphics g, int x, int y, int w, int h,
                               float freqHz, float amplitude, float phaseDeg, float harmRatio) {
        renderBackground(g, x, y, w, h);
        renderGrid(g, x, y, w, h);
        renderWave(g, x, y, w, h, freqHz, amplitude, phaseDeg, harmRatio);
        renderBorder(g, x, y, w, h);
    }

    private static void renderBackground(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_COLOR);
    }

    private static void renderGrid(GuiGraphics g, int x, int y, int w, int h) {
        int centerY = y + h / 2;

        // Ligne centrale (axe 0)
        g.fill(x, centerY, x + w, centerY + 1, GRID_CENTER_COLOR);

        // Lignes quart
        int quarterH = h / 4;
        g.fill(x, centerY - quarterH, x + w, centerY - quarterH + 1, GRID_COLOR);
        g.fill(x, centerY + quarterH, x + w, centerY + quarterH + 1, GRID_COLOR);

        // Lignes verticales espacees de 20px
        for (int gx = x + 20; gx < x + w; gx += 20) {
            g.fill(gx, y, gx + 1, y + h, GRID_COLOR);
        }
    }

    private static void renderWave(GuiGraphics g, int x, int y, int w, int h,
                                    float freqHz, float amplitude, float phaseDeg, float harmRatio) {
        if (amplitude < 0.01f) return;

        int centerY = y + h / 2;
        float halfH = (h / 2.0f) - 2.0f;
        float phaseRad = (float) Math.toRadians(phaseDeg);

        // Temps pour le scrolling en temps reel
        float timeOffset = (System.currentTimeMillis() % 100000L) / 1000.0f;

        // Nombre d'harmoniques actives (0 a MAX_HARMONICS)
        int numHarmonics = (int) (harmRatio * MAX_HARMONICS);
        float harmonicFrac = (harmRatio * MAX_HARMONICS) - numHarmonics;

        int prevPixelY = -1;

        for (int px = 0; px < w; px++) {
            // Position temporelle pour ce pixel
            float t = (px / (float) w) * 4.0f + timeOffset * freqHz * 0.05f;

            // Synthese additive
            float sample = computeSample(t, freqHz, phaseRad, numHarmonics, harmonicFrac);
            sample *= amplitude;
            sample = Math.max(-1.0f, Math.min(1.0f, sample));

            int pixelY = centerY - (int) (sample * halfH);

            // Glow (colonne large, semi-transparent)
            int glowTop = Math.min(pixelY, centerY) - 1;
            int glowBot = Math.max(pixelY, centerY) + 2;
            glowTop = Math.max(glowTop, y);
            glowBot = Math.min(glowBot, y + h);
            g.fill(x + px, glowTop, x + px + 1, glowBot, WAVE_GLOW_COLOR);

            // Ligne principale (2px de haut)
            int lineTop = Math.max(pixelY - 1, y);
            int lineBot = Math.min(pixelY + 1, y + h);

            // Connecter les pixels pour eviter les trous
            if (prevPixelY >= 0 && Math.abs(pixelY - prevPixelY) > 2) {
                int connectTop = Math.max(Math.min(pixelY, prevPixelY), y);
                int connectBot = Math.min(Math.max(pixelY, prevPixelY) + 1, y + h);
                g.fill(x + px, connectTop, x + px + 1, connectBot, WAVE_COLOR);
            }

            g.fill(x + px, lineTop, x + px + 1, lineBot, WAVE_COLOR);
            prevPixelY = pixelY;
        }
    }

    /**
     * Calcule un echantillon par synthese additive (serie de Fourier approximation onde carree).
     * Fondamentale + harmoniques impaires (3e, 5e, 7e...) avec amplitude decroissante 1/n.
     */
    private static float computeSample(float t, float freqHz, float phaseRad,
                                        int numHarmonics, float harmonicFrac) {
        float omega = (float) (2.0 * Math.PI * freqHz * t * 0.01);
        float sample = (float) Math.sin(omega + phaseRad);

        // Harmoniques impaires
        for (int n = 1; n <= numHarmonics + 1; n++) {
            int harmonic = 2 * n + 1; // 3, 5, 7, 9, ...
            float harmAmplitude = 1.0f / harmonic;

            // Interpolation douce pour la derniere harmonique
            if (n == numHarmonics + 1) {
                harmAmplitude *= harmonicFrac;
            }

            if (harmAmplitude < 0.001f) continue;
            sample += harmAmplitude * (float) Math.sin(harmonic * omega + phaseRad * harmonic);
        }

        // Normaliser: la somme totale de la serie de Fourier pour onde carree = ~1.27 * fondamentale
        float normFactor = 1.0f;
        if (numHarmonics > 0) {
            normFactor = 1.0f / (1.0f + computeHarmonicSum(numHarmonics, harmonicFrac));
        }
        return sample * normFactor;
    }

    /** Somme des amplitudes des harmoniques (pour normalisation). */
    private static float computeHarmonicSum(int numHarmonics, float harmonicFrac) {
        float sum = 0;
        for (int n = 1; n <= numHarmonics + 1; n++) {
            int harmonic = 2 * n + 1;
            float amp = 1.0f / harmonic;
            if (n == numHarmonics + 1) amp *= harmonicFrac;
            sum += amp;
        }
        return sum;
    }

    private static void renderBorder(GuiGraphics g, int x, int y, int w, int h) {
        // Top
        g.fill(x, y, x + w, y + 1, BORDER_COLOR);
        // Bottom
        g.fill(x, y + h - 1, x + w, y + h, BORDER_COLOR);
        // Left
        g.fill(x, y, x + 1, y + h, BORDER_COLOR);
        // Right
        g.fill(x + w - 1, y, x + w, y + h, BORDER_COLOR);
    }
}
