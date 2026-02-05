/**
 * ============================================================
 * [LineDrawingHelper.java]
 * Description: Utilitaire OPTIMISE pour dessiner des lignes et fleches
 * ============================================================
 *
 * REFERENCES D'OPTIMISATION:
 * - Fabric Docs: https://docs.fabricmc.net/develop/rendering/draw-context
 *   -> Utiliser hLine/vLine pour lignes horizontales/verticales
 *   -> Utiliser fill() avec UN SEUL appel par rectangle
 * - JVM Gaming Forum: https://jvm-gaming.org/t/fastest-way-to-do-2d-line-drawing/27137
 *   -> Eviter anti-aliasing (cout x1000)
 *   -> Minimiser les draw calls
 * - RenderUtils Gist: https://gist.github.com/ItziSpyder/c06c34f28e406c04be63d593f0a0d0c1
 *   -> Utiliser fillRect pour lignes simples
 *   -> Batch rendering avec beginRendering/finishRendering
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | Rendu Minecraft      | Dessin des rectangles          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StandardPageRenderer (liens du Codex)
 * - BeeTreePageRenderer (liens des abeilles)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.util;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Classe utilitaire OPTIMISEE pour dessiner des lignes et fleches.
 *
 * PRINCIPE D'OPTIMISATION:
 * - UNE ligne = UN ou DEUX appels graphics.fill() maximum
 * - Pas de boucle pixel par pixel (Bresenham est trop lent pour GUI)
 * - Les lignes diagonales sont dessinees comme des rectangles fins
 */
public class LineDrawingHelper {

    // ============================================================
    // STYLES DE LIGNE (simplifies pour performance)
    // ============================================================

    public enum LineStyle {
        /** Ligne droite simple - LE PLUS RAPIDE */
        STRAIGHT,
        /** Ligne pointillee */
        DASHED,
        /** Ligne en pointilles */
        DOTTED
    }

    // ============================================================
    // STYLES DE FLECHE (simplifies pour performance)
    // ============================================================

    public enum ArrowStyle {
        /** Pas de fleche */
        NONE,
        /** Fleche simple (deux lignes) - RAPIDE */
        SIMPLE,
        /** Fleche triangulaire pleine */
        FILLED
    }

    // ============================================================
    // CONSTANTES
    // ============================================================

    public static final int DEFAULT_THICKNESS = 2;
    public static final int DEFAULT_ARROW_LENGTH = 6;
    public static final float DEFAULT_ARROW_ANGLE = 0.5f;

    // ============================================================
    // METHODE PRINCIPALE - LIGNE AVEC FLECHE
    // ============================================================

    /**
     * Dessine une ligne avec fleche - VERSION OPTIMISEE.
     * Maximum 3-4 appels fill() au total.
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color, int thickness,
                                  LineStyle lineStyle, ArrowStyle arrowStyle,
                                  int arrowLength, float arrowAngle, float unused) {

        // Calcul direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        // Ajuster fin pour la fleche
        int lineEndX = x2;
        int lineEndY = y2;
        if (arrowStyle != ArrowStyle.NONE) {
            lineEndX = (int) (x2 - nx * arrowLength);
            lineEndY = (int) (y2 - ny * arrowLength);
        }

        // Dessiner la ligne
        switch (lineStyle) {
            case STRAIGHT -> drawLineOptimized(graphics, x1, y1, lineEndX, lineEndY, color, thickness);
            case DASHED -> drawDashedLineOptimized(graphics, x1, y1, lineEndX, lineEndY, color, thickness);
            case DOTTED -> drawDottedLineOptimized(graphics, x1, y1, lineEndX, lineEndY, color, thickness);
        }

        // Dessiner la fleche
        if (arrowStyle != ArrowStyle.NONE) {
            drawArrowHeadOptimized(graphics, x2, y2, nx, ny, color, thickness, arrowStyle, arrowLength, arrowAngle);
        }
    }

    /**
     * Version simplifiee avec parametres par defaut.
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color) {
        drawArrow(graphics, x1, y1, x2, y2, color,
                DEFAULT_THICKNESS, LineStyle.STRAIGHT, ArrowStyle.SIMPLE,
                DEFAULT_ARROW_LENGTH, DEFAULT_ARROW_ANGLE, 0);
    }

    // ============================================================
    // DESSIN DE LIGNES OPTIMISE
    // ============================================================

    /**
     * Dessine une ligne droite - OPTIMISE.
     * Utilise UN SEUL appel fill() pour les lignes H/V,
     * et approximation rectangulaire pour les diagonales.
     */
    public static void drawLineOptimized(GuiGraphics graphics,
                                          int x1, int y1, int x2, int y2,
                                          int color, int thickness) {
        // Cas special: ligne horizontale - UN SEUL fill()
        if (y1 == y2) {
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int halfT = thickness / 2;
            graphics.fill(minX, y1 - halfT, maxX, y1 - halfT + thickness, color);
            return;
        }

        // Cas special: ligne verticale - UN SEUL fill()
        if (x1 == x2) {
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int halfT = thickness / 2;
            graphics.fill(x1 - halfT, minY, x1 - halfT + thickness, maxY, color);
            return;
        }

        // Ligne diagonale: dessiner comme une serie de petits rectangles
        // Beaucoup plus rapide que Bresenham pixel par pixel
        drawDiagonalLineAsStepped(graphics, x1, y1, x2, y2, color, thickness);
    }

    /**
     * Dessine une ligne diagonale en "escalier" - RAPIDE.
     * Au lieu de dessiner pixel par pixel, on dessine des segments
     * horizontaux ou verticaux selon la pente.
     */
    private static void drawDiagonalLineAsStepped(GuiGraphics graphics,
                                                   int x1, int y1, int x2, int y2,
                                                   int color, int thickness) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int halfT = thickness / 2;

        // Choisir la direction principale (moins de segments = plus rapide)
        if (dx >= dy) {
            // Principalement horizontal - dessiner des segments horizontaux
            int steps = Math.max(1, dx / 4); // ~4 pixels par segment
            float stepX = (float)(x2 - x1) / steps;
            float stepY = (float)(y2 - y1) / steps;

            for (int i = 0; i < steps; i++) {
                int segX1 = (int)(x1 + stepX * i);
                int segY1 = (int)(y1 + stepY * i);
                int segX2 = (int)(x1 + stepX * (i + 1));
                int segY2 = (int)(y1 + stepY * (i + 1));

                // Rectangle couvrant ce segment
                int minX = Math.min(segX1, segX2);
                int maxX = Math.max(segX1, segX2) + 1;
                int minY = Math.min(segY1, segY2) - halfT;
                int maxY = Math.max(segY1, segY2) + halfT + 1;

                graphics.fill(minX, minY, maxX, maxY, color);
            }
        } else {
            // Principalement vertical - dessiner des segments verticaux
            int steps = Math.max(1, dy / 4);
            float stepX = (float)(x2 - x1) / steps;
            float stepY = (float)(y2 - y1) / steps;

            for (int i = 0; i < steps; i++) {
                int segX1 = (int)(x1 + stepX * i);
                int segY1 = (int)(y1 + stepY * i);
                int segX2 = (int)(x1 + stepX * (i + 1));
                int segY2 = (int)(y1 + stepY * (i + 1));

                int minX = Math.min(segX1, segX2) - halfT;
                int maxX = Math.max(segX1, segX2) + halfT + 1;
                int minY = Math.min(segY1, segY2);
                int maxY = Math.max(segY1, segY2) + 1;

                graphics.fill(minX, minY, maxX, maxY, color);
            }
        }
    }

    /**
     * Ligne pointillee - OPTIMISE.
     */
    public static void drawDashedLineOptimized(GuiGraphics graphics,
                                                int x1, int y1, int x2, int y2,
                                                int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        int dashLen = 8;
        int gapLen = 4;
        int halfT = thickness / 2;

        float pos = 0;
        boolean drawing = true;

        while (pos < length) {
            float segLen = drawing ? dashLen : gapLen;
            float endPos = Math.min(pos + segLen, length);

            if (drawing) {
                int sx = (int) (x1 + nx * pos);
                int sy = (int) (y1 + ny * pos);
                int ex = (int) (x1 + nx * endPos);
                int ey = (int) (y1 + ny * endPos);

                // Un seul rectangle par tiret
                int minX = Math.min(sx, ex) - halfT;
                int maxX = Math.max(sx, ex) + halfT;
                int minY = Math.min(sy, ey) - halfT;
                int maxY = Math.max(sy, ey) + halfT;
                graphics.fill(minX, minY, maxX, maxY, color);
            }

            pos = endPos;
            drawing = !drawing;
        }
    }

    /**
     * Ligne en pointilles - OPTIMISE.
     */
    public static void drawDottedLineOptimized(GuiGraphics graphics,
                                                int x1, int y1, int x2, int y2,
                                                int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        int spacing = 6;
        int dotSize = thickness;
        int numDots = (int) (length / spacing);

        for (int i = 0; i <= numDots; i++) {
            float t = (float) i / Math.max(1, numDots);
            int px = (int) (x1 + dx * t);
            int py = (int) (y1 + dy * t);
            // Un carre par point
            graphics.fill(px - dotSize/2, py - dotSize/2,
                         px + dotSize/2 + 1, py + dotSize/2 + 1, color);
        }
    }

    // ============================================================
    // DESSIN DE FLECHES OPTIMISE
    // ============================================================

    /**
     * Dessine une tete de fleche - OPTIMISE.
     * Maximum 2-3 appels fill().
     */
    private static void drawArrowHeadOptimized(GuiGraphics graphics,
                                                int tipX, int tipY,
                                                float dirX, float dirY,
                                                int color, int thickness,
                                                ArrowStyle style,
                                                int length, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Calcul des branches
        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length);
        int leftY = (int) (tipY - leftDirY * length);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length);
        int rightY = (int) (tipY - rightDirY * length);

        if (style == ArrowStyle.SIMPLE) {
            // Deux lignes simples
            drawLineOptimized(graphics, tipX, tipY, leftX, leftY, color, thickness);
            drawLineOptimized(graphics, tipX, tipY, rightX, rightY, color, thickness);
        } else if (style == ArrowStyle.FILLED) {
            // Triangle rempli - 3 rectangles pour approximer
            fillTriangleFast(graphics, tipX, tipY, leftX, leftY, rightX, rightY, color);
        }
    }

    /**
     * Remplit un triangle de maniere RAPIDE.
     * Utilise une approximation avec quelques rectangles au lieu de scanline.
     */
    private static void fillTriangleFast(GuiGraphics graphics,
                                          int x1, int y1, int x2, int y2, int x3, int y3,
                                          int color) {
        // Centre du triangle
        int cx = (x1 + x2 + x3) / 3;
        int cy = (y1 + y2 + y3) / 3;

        // Dessiner 3 lignes epaisses du centre vers chaque sommet
        // C'est une approximation mais tres rapide
        drawLineOptimized(graphics, cx, cy, x1, y1, color, 3);
        drawLineOptimized(graphics, cx, cy, x2, y2, color, 3);
        drawLineOptimized(graphics, cx, cy, x3, y3, color, 3);

        // Petit carre au centre pour remplir
        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
    }

    // ============================================================
    // METHODES LEGACY (pour compatibilite)
    // ============================================================

    public static void drawLine(GuiGraphics graphics,
                                 int x1, int y1, int x2, int y2,
                                 int color, int thickness,
                                 LineStyle style, float unused) {
        switch (style) {
            case STRAIGHT -> drawLineOptimized(graphics, x1, y1, x2, y2, color, thickness);
            case DASHED -> drawDashedLineOptimized(graphics, x1, y1, x2, y2, color, thickness);
            case DOTTED -> drawDottedLineOptimized(graphics, x1, y1, x2, y2, color, thickness);
        }
    }
}
