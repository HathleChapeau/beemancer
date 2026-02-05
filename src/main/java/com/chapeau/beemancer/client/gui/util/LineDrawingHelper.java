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
 *
 * STYLE VISUEL:
 * - Lignes en L (orthogonales) pour les connexions diagonales
 * - Effet d'ombre subtil pour la profondeur
 * - Fleches chevron elegantes
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
 * - Lignes orthogonales (H+V) = 2 appels fill() maximum
 * - Effet d'ombre = +2 appels fill() (optionnel)
 * - Fleche chevron = 2 appels fill()
 */
public class LineDrawingHelper {

    // ============================================================
    // STYLES DE LIGNE
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
    // STYLES DE FLECHE
    // ============================================================

    public enum ArrowStyle {
        /** Pas de fleche */
        NONE,
        /** Fleche chevron (V) - ELEGANT */
        SIMPLE,
        /** Fleche triangulaire pleine */
        FILLED
    }

    // ============================================================
    // CONSTANTES
    // ============================================================

    public static final int DEFAULT_THICKNESS = 2;
    public static final int DEFAULT_ARROW_LENGTH = 8;
    public static final float DEFAULT_ARROW_ANGLE = 0.45f;

    // Couleur d'ombre (noir semi-transparent)
    private static final int SHADOW_COLOR = 0x40000000;
    private static final int SHADOW_OFFSET = 1;

    // ============================================================
    // METHODE PRINCIPALE - LIGNE AVEC FLECHE
    // ============================================================

    /**
     * Dessine une ligne avec fleche - VERSION OPTIMISEE.
     * Utilise un routage en L pour les lignes diagonales (plus propre visuellement).
     * Maximum 6-8 appels fill() au total (avec ombre).
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color, int thickness,
                                  LineStyle lineStyle, ArrowStyle arrowStyle,
                                  int arrowLength, float arrowAngle, float unused) {

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

        // Dessiner la ligne avec routage intelligent
        boolean isHorizontal = y1 == y2;
        boolean isVertical = x1 == x2;

        if (isHorizontal || isVertical) {
            // Ligne droite H ou V - simple et rapide
            drawStraightLineWithShadow(graphics, x1, y1, lineEndX, lineEndY, color, thickness, lineStyle);
        } else {
            // Ligne diagonale - utiliser routage en L
            drawLShapedLine(graphics, x1, y1, lineEndX, lineEndY, color, thickness, lineStyle);
        }

        // Dessiner la fleche
        if (arrowStyle != ArrowStyle.NONE) {
            drawChevronArrow(graphics, x2, y2, nx, ny, color, thickness, arrowLength, arrowAngle);
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
    // DESSIN DE LIGNES - NOUVELLES METHODES VISUELLES
    // ============================================================

    /**
     * Dessine une ligne droite (H ou V) avec effet d'ombre subtil.
     */
    private static void drawStraightLineWithShadow(GuiGraphics graphics,
                                                    int x1, int y1, int x2, int y2,
                                                    int color, int thickness, LineStyle style) {
        int halfT = thickness / 2;

        if (y1 == y2) {
            // Ligne horizontale
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            // Ombre
            graphics.fill(minX + SHADOW_OFFSET, y1 - halfT + SHADOW_OFFSET,
                         maxX + SHADOW_OFFSET, y1 - halfT + thickness + SHADOW_OFFSET, SHADOW_COLOR);
            // Ligne principale
            graphics.fill(minX, y1 - halfT, maxX, y1 - halfT + thickness, color);
        } else {
            // Ligne verticale
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            // Ombre
            graphics.fill(x1 - halfT + SHADOW_OFFSET, minY + SHADOW_OFFSET,
                         x1 - halfT + thickness + SHADOW_OFFSET, maxY + SHADOW_OFFSET, SHADOW_COLOR);
            // Ligne principale
            graphics.fill(x1 - halfT, minY, x1 - halfT + thickness, maxY, color);
        }
    }

    /**
     * Dessine une ligne en forme de L (routage orthogonal).
     * Le coude est place au milieu du trajet vertical.
     * Beaucoup plus elegant que les diagonales en escalier.
     */
    private static void drawLShapedLine(GuiGraphics graphics,
                                         int x1, int y1, int x2, int y2,
                                         int color, int thickness, LineStyle style) {
        int halfT = thickness / 2;

        // Point de coude: vertical d'abord, puis horizontal
        // Le coude est au niveau Y du point de depart, X du point d'arrivee
        int midX = x1;
        int midY = y2;

        // Segment 1: vertical (de y1 vers y2)
        int minY = Math.min(y1, midY);
        int maxY = Math.max(y1, midY);

        // Ombre segment vertical
        graphics.fill(x1 - halfT + SHADOW_OFFSET, minY + SHADOW_OFFSET,
                     x1 - halfT + thickness + SHADOW_OFFSET, maxY + halfT + SHADOW_OFFSET, SHADOW_COLOR);
        // Segment vertical
        graphics.fill(x1 - halfT, minY, x1 - halfT + thickness, maxY + halfT, color);

        // Segment 2: horizontal (de x1 vers x2)
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);

        // Ombre segment horizontal
        graphics.fill(minX - halfT + SHADOW_OFFSET, midY - halfT + SHADOW_OFFSET,
                     maxX + SHADOW_OFFSET, midY - halfT + thickness + SHADOW_OFFSET, SHADOW_COLOR);
        // Segment horizontal
        graphics.fill(minX - halfT, midY - halfT, maxX, midY - halfT + thickness, color);

        // Petit carre au coude pour lisser la jonction
        graphics.fill(x1 - halfT, midY - halfT, x1 + halfT, midY + halfT, color);
    }

    /**
     * Dessine une fleche chevron elegante (forme V).
     */
    private static void drawChevronArrow(GuiGraphics graphics,
                                          int tipX, int tipY,
                                          float dirX, float dirY,
                                          int color, int thickness,
                                          int length, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Calcul des branches du chevron
        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length);
        int leftY = (int) (tipY - leftDirY * length);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length);
        int rightY = (int) (tipY - rightDirY * length);

        int halfT = thickness / 2;

        // Ombre branche gauche
        drawSimpleLine(graphics, tipX + SHADOW_OFFSET, tipY + SHADOW_OFFSET,
                       leftX + SHADOW_OFFSET, leftY + SHADOW_OFFSET, SHADOW_COLOR, thickness);
        // Ombre branche droite
        drawSimpleLine(graphics, tipX + SHADOW_OFFSET, tipY + SHADOW_OFFSET,
                       rightX + SHADOW_OFFSET, rightY + SHADOW_OFFSET, SHADOW_COLOR, thickness);

        // Branche gauche
        drawSimpleLine(graphics, tipX, tipY, leftX, leftY, color, thickness);
        // Branche droite
        drawSimpleLine(graphics, tipX, tipY, rightX, rightY, color, thickness);

        // Point central pour fermer le chevron
        graphics.fill(tipX - halfT, tipY - halfT, tipX + halfT + 1, tipY + halfT + 1, color);
    }

    /**
     * Dessine une ligne simple (utilisee pour les branches de fleche).
     * Pas d'ombre, juste un rectangle.
     */
    private static void drawSimpleLine(GuiGraphics graphics,
                                        int x1, int y1, int x2, int y2,
                                        int color, int thickness) {
        int halfT = thickness / 2;

        // Ligne horizontale
        if (y1 == y2) {
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            graphics.fill(minX, y1 - halfT, maxX, y1 + halfT + 1, color);
            return;
        }

        // Ligne verticale
        if (x1 == x2) {
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            graphics.fill(x1 - halfT, minY, x1 + halfT + 1, maxY, color);
            return;
        }

        // Diagonale: dessiner comme rectangle incline (approximation simple)
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy)) / 2;
        steps = Math.max(2, Math.min(steps, 8)); // Entre 2 et 8 segments

        for (int i = 0; i < steps; i++) {
            float t1 = (float) i / steps;
            float t2 = (float) (i + 1) / steps;
            int sx = (int) (x1 + dx * t1);
            int sy = (int) (y1 + dy * t1);
            int ex = (int) (x1 + dx * t2);
            int ey = (int) (y1 + dy * t2);

            int minX = Math.min(sx, ex) - halfT;
            int maxX = Math.max(sx, ex) + halfT + 1;
            int minY = Math.min(sy, ey) - halfT;
            int maxY = Math.max(sy, ey) + halfT + 1;
            graphics.fill(minX, minY, maxX, maxY, color);
        }
    }

    // ============================================================
    // METHODES LEGACY (compatibilite)
    // ============================================================

    /**
     * Dessine une ligne droite - VERSION LEGACY.
     */
    public static void drawLineOptimized(GuiGraphics graphics,
                                          int x1, int y1, int x2, int y2,
                                          int color, int thickness) {
        if (y1 == y2 || x1 == x2) {
            drawStraightLineWithShadow(graphics, x1, y1, x2, y2, color, thickness, LineStyle.STRAIGHT);
        } else {
            drawLShapedLine(graphics, x1, y1, x2, y2, color, thickness, LineStyle.STRAIGHT);
        }
    }

    /**
     * Ligne pointillee - pour compatibilite.
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

        int dashLen = 6;
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

                int minX = Math.min(sx, ex) - halfT;
                int maxX = Math.max(sx, ex) + halfT + 1;
                int minY = Math.min(sy, ey) - halfT;
                int maxY = Math.max(sy, ey) + halfT + 1;
                graphics.fill(minX, minY, maxX, maxY, color);
            }

            pos = endPos;
            drawing = !drawing;
        }
    }

    /**
     * Ligne en pointilles - pour compatibilite.
     */
    public static void drawDottedLineOptimized(GuiGraphics graphics,
                                                int x1, int y1, int x2, int y2,
                                                int color, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        int spacing = 5;
        int dotSize = thickness + 1;
        int numDots = (int) (length / spacing);

        for (int i = 0; i <= numDots; i++) {
            float t = (float) i / Math.max(1, numDots);
            int px = (int) (x1 + dx * t);
            int py = (int) (y1 + dy * t);
            graphics.fill(px - dotSize/2, py - dotSize/2,
                         px + dotSize/2 + 1, py + dotSize/2 + 1, color);
        }
    }

    /**
     * Methode legacy pour compatibilite.
     */
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
