/**
 * ============================================================
 * [LineDrawingHelper.java]
 * Description: Utilitaire pour dessiner des lignes et flèches avec différents styles
 * ============================================================
 *
 * RÉFÉRENCES:
 * - Rough.js Algorithms: https://shihn.ca/posts/2020/roughjs-algorithms/
 * - Bresenham's Algorithm: https://en.wikipedia.org/wiki/Bresenham's_line_algorithm
 * - Bézier Curves: https://ciechanow.ski/drawing-bezier-curves/
 * - Arrow Math: https://serdarkuzucu.com/2025/07/20/drawing-arrow-pillow-python/
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | Rendu Minecraft      | Dessin des pixels              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StandardPageRenderer (liens du Codex)
 * - BeeTreePageRenderer (liens des abeilles)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.util;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Classe utilitaire pour dessiner des lignes et flèches avec différents styles visuels.
 * Basé sur les algorithmes de Rough.js et les mathématiques vectorielles standard.
 */
public class LineDrawingHelper {

    // ============================================================
    // STYLES DE LIGNE
    // ============================================================

    public enum LineStyle {
        /** Ligne droite simple */
        STRAIGHT,
        /** Ligne avec léger effet manuscrit (wobble) */
        HAND_DRAWN,
        /** Ligne avec effet très sketchy (comme Rough.js) */
        SKETCHY,
        /** Ligne pointillée */
        DASHED,
        /** Ligne en pointillés */
        DOTTED,
        /** Courbe de Bézier quadratique */
        BEZIER_QUAD,
        /** Double ligne pour effet plus prononcé */
        DOUBLE
    }

    // ============================================================
    // STYLES DE FLÈCHE
    // ============================================================

    public enum ArrowStyle {
        /** Pas de flèche */
        NONE,
        /** Flèche simple (deux lignes) */
        SIMPLE,
        /** Flèche triangulaire pleine */
        FILLED,
        /** Flèche en forme de losange */
        DIAMOND,
        /** Flèche ouverte (chevron) */
        CHEVRON,
        /** Flèche manuscrite avec léger wobble */
        HAND_DRAWN,
        /** Point/cercle au lieu d'une flèche */
        DOT
    }

    // ============================================================
    // PARAMÈTRES DE CONFIGURATION
    // ============================================================

    /** Épaisseur par défaut des lignes */
    public static final int DEFAULT_THICKNESS = 2;

    /** Longueur par défaut des têtes de flèche */
    public static final int DEFAULT_ARROW_LENGTH = 8;

    /** Angle par défaut des têtes de flèche (en radians, ~30°) */
    public static final float DEFAULT_ARROW_ANGLE = 0.5f;

    /** Roughness par défaut pour l'effet manuscrit (0-1) */
    public static final float DEFAULT_ROUGHNESS = 0.3f;

    /** Seed pour la génération aléatoire déterministe */
    private static long globalSeed = 12345;

    // ============================================================
    // MÉTHODES PRINCIPALES - LIGNES AVEC FLÈCHES
    // ============================================================

    /**
     * Dessine une ligne avec flèche du point A vers le point B.
     * Méthode principale avec tous les paramètres.
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color,
                                  int thickness,
                                  LineStyle lineStyle,
                                  ArrowStyle arrowStyle,
                                  int arrowLength,
                                  float arrowAngle,
                                  float roughness) {

        // Calcul du vecteur direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        // Décaler la fin pour laisser place à la flèche
        int arrowOffset = (arrowStyle != ArrowStyle.NONE) ? arrowLength : 0;
        int lineEndX = (int) (x2 - nx * arrowOffset);
        int lineEndY = (int) (y2 - ny * arrowOffset);

        // Dessiner la ligne
        drawLine(graphics, x1, y1, lineEndX, lineEndY, color, thickness, lineStyle, roughness);

        // Dessiner la flèche
        if (arrowStyle != ArrowStyle.NONE) {
            drawArrowHead(graphics, x2, y2, nx, ny, color, thickness, arrowStyle, arrowLength, arrowAngle, roughness);
        }
    }

    /**
     * Version simplifiée avec paramètres par défaut.
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color) {
        drawArrow(graphics, x1, y1, x2, y2, color,
                DEFAULT_THICKNESS, LineStyle.HAND_DRAWN, ArrowStyle.SIMPLE,
                DEFAULT_ARROW_LENGTH, DEFAULT_ARROW_ANGLE, DEFAULT_ROUGHNESS);
    }

    /**
     * Version avec style de ligne et flèche personnalisés.
     */
    public static void drawArrow(GuiGraphics graphics,
                                  int x1, int y1, int x2, int y2,
                                  int color, int thickness,
                                  LineStyle lineStyle, ArrowStyle arrowStyle) {
        drawArrow(graphics, x1, y1, x2, y2, color,
                thickness, lineStyle, arrowStyle,
                DEFAULT_ARROW_LENGTH, DEFAULT_ARROW_ANGLE, DEFAULT_ROUGHNESS);
    }

    // ============================================================
    // MÉTHODES DE DESSIN DE LIGNES
    // ============================================================

    /**
     * Dessine une ligne avec le style spécifié.
     */
    public static void drawLine(GuiGraphics graphics,
                                 int x1, int y1, int x2, int y2,
                                 int color, int thickness,
                                 LineStyle style, float roughness) {
        switch (style) {
            case STRAIGHT -> drawStraightLine(graphics, x1, y1, x2, y2, color, thickness);
            case HAND_DRAWN -> drawHandDrawnLine(graphics, x1, y1, x2, y2, color, thickness, roughness);
            case SKETCHY -> drawSketchyLine(graphics, x1, y1, x2, y2, color, thickness, roughness);
            case DASHED -> drawDashedLine(graphics, x1, y1, x2, y2, color, thickness, 8, 4);
            case DOTTED -> drawDottedLine(graphics, x1, y1, x2, y2, color, thickness, 4);
            case BEZIER_QUAD -> drawBezierLine(graphics, x1, y1, x2, y2, color, thickness, roughness);
            case DOUBLE -> drawDoubleLine(graphics, x1, y1, x2, y2, color, thickness, roughness);
        }
    }

    /**
     * Ligne droite simple (Bresenham).
     */
    public static void drawStraightLine(GuiGraphics graphics,
                                         int x1, int y1, int x2, int y2,
                                         int color, int thickness) {
        drawThickLineBresenham(graphics, x1, y1, x2, y2, color, thickness);
    }

    /**
     * Ligne avec léger effet manuscrit.
     * Basé sur Rough.js: ajoute un wobble perpendiculaire.
     */
    public static void drawHandDrawnLine(GuiGraphics graphics,
                                          int x1, int y1, int x2, int y2,
                                          int color, int thickness, float roughness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) {
            drawPixel(graphics, x1, y1, color, thickness);
            return;
        }

        // Vecteurs normalisés
        float nx = dx / length;
        float ny = dy / length;
        // Perpendiculaire
        float px = -ny;
        float py = nx;

        // Nombre de segments
        int segments = Math.max(3, (int) (length / 6));

        // Random déterministe basé sur les coordonnées
        Random rand = createDeterministicRandom(x1, y1, x2, y2);

        int prevX = x1;
        int prevY = y1;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            int baseX = (int) (x1 + dx * t);
            int baseY = (int) (y1 + dy * t);

            // Wobble perpendiculaire (pas aux extrémités)
            float wobble = 0;
            if (i > 0 && i < segments) {
                float maxWobble = roughness * Math.min(length * 0.1f, 4f);
                wobble = (rand.nextFloat() - 0.5f) * 2 * maxWobble;
            }

            int curX = (int) (baseX + px * wobble);
            int curY = (int) (baseY + py * wobble);

            drawThickLineBresenham(graphics, prevX, prevY, curX, curY, color, thickness);

            prevX = curX;
            prevY = curY;
        }
    }

    /**
     * Ligne très sketchy avec double tracé et bowing effect.
     * Algorithme inspiré de Rough.js.
     */
    public static void drawSketchyLine(GuiGraphics graphics,
                                        int x1, int y1, int x2, int y2,
                                        int color, int thickness, float roughness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        Random rand = createDeterministicRandom(x1, y1, x2, y2);

        // Rough.js: randomize les endpoints
        float endpointOffset = roughness * Math.min(length * 0.05f, 3f);
        int rx1 = x1 + (int) ((rand.nextFloat() - 0.5f) * endpointOffset);
        int ry1 = y1 + (int) ((rand.nextFloat() - 0.5f) * endpointOffset);
        int rx2 = x2 + (int) ((rand.nextFloat() - 0.5f) * endpointOffset);
        int ry2 = y2 + (int) ((rand.nextFloat() - 0.5f) * endpointOffset);

        // Premier tracé avec bowing
        drawBowingLine(graphics, rx1, ry1, rx2, ry2, color, thickness, roughness, rand);

        // Deuxième tracé légèrement décalé pour effet sketchy
        if (roughness > 0.2f) {
            rand = createDeterministicRandom(x2, y2, x1, y1); // Seed différent
            int offset = Math.max(1, thickness / 2);
            drawBowingLine(graphics,
                    rx1 + (int) ((rand.nextFloat() - 0.5f) * offset),
                    ry1 + (int) ((rand.nextFloat() - 0.5f) * offset),
                    rx2 + (int) ((rand.nextFloat() - 0.5f) * offset),
                    ry2 + (int) ((rand.nextFloat() - 0.5f) * offset),
                    color, Math.max(1, thickness - 1), roughness * 0.7f, rand);
        }
    }

    /**
     * Ligne avec effet de courbure (bowing) - sous-routine de sketchy.
     */
    private static void drawBowingLine(GuiGraphics graphics,
                                        int x1, int y1, int x2, int y2,
                                        int color, int thickness, float roughness,
                                        Random rand) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        // Perpendiculaire normalisée
        float px = -dy / length;
        float py = dx / length;

        // Points de contrôle pour le bowing (50% et 75% de la ligne)
        float bowAmount = roughness * Math.min(length * 0.15f, 8f);

        float bow1 = (rand.nextFloat() - 0.5f) * bowAmount;
        float bow2 = (rand.nextFloat() - 0.5f) * bowAmount * 0.7f;

        // 4 points: start, 50%, 75%, end
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = x1;
        yPoints[0] = y1;

        xPoints[1] = (int) (x1 + dx * 0.5f + px * bow1);
        yPoints[1] = (int) (y1 + dy * 0.5f + py * bow1);

        xPoints[2] = (int) (x1 + dx * 0.75f + px * bow2);
        yPoints[2] = (int) (y1 + dy * 0.75f + py * bow2);

        xPoints[3] = x2;
        yPoints[3] = y2;

        // Dessiner les segments
        for (int i = 0; i < 3; i++) {
            drawThickLineBresenham(graphics, xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1], color, thickness);
        }
    }

    /**
     * Ligne pointillée (tirets).
     */
    public static void drawDashedLine(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       int color, int thickness,
                                       int dashLength, int gapLength) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        float patternLength = dashLength + gapLength;
        float pos = 0;
        boolean drawing = true;

        while (pos < length) {
            float segmentLength = drawing ? dashLength : gapLength;
            float endPos = Math.min(pos + segmentLength, length);

            if (drawing) {
                int sx = (int) (x1 + nx * pos);
                int sy = (int) (y1 + ny * pos);
                int ex = (int) (x1 + nx * endPos);
                int ey = (int) (y1 + ny * endPos);
                drawThickLineBresenham(graphics, sx, sy, ex, ey, color, thickness);
            }

            pos = endPos;
            drawing = !drawing;
        }
    }

    /**
     * Ligne en pointillés (dots).
     */
    public static void drawDottedLine(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       int color, int thickness, int spacing) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        int numDots = (int) (length / spacing) + 1;
        for (int i = 0; i <= numDots; i++) {
            float t = (float) i / numDots;
            int px = (int) (x1 + dx * t);
            int py = (int) (y1 + dy * t);
            drawPixel(graphics, px, py, color, thickness);
        }
    }

    /**
     * Ligne courbe (Bézier quadratique avec point de contrôle aléatoire).
     */
    public static void drawBezierLine(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       int color, int thickness, float curvature) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        // Perpendiculaire
        float px = -dy / length;
        float py = dx / length;

        // Point de contrôle au milieu avec offset perpendiculaire
        Random rand = createDeterministicRandom(x1, y1, x2, y2);
        float curveAmount = curvature * length * 0.3f * (rand.nextFloat() - 0.5f) * 2;

        float cx = (x1 + x2) / 2f + px * curveAmount;
        float cy = (y1 + y2) / 2f + py * curveAmount;

        // Échantillonner la courbe de Bézier quadratique
        int segments = Math.max(8, (int) (length / 4));
        int prevX = x1;
        int prevY = y1;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            float u = 1 - t;

            // B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
            float bx = u * u * x1 + 2 * u * t * cx + t * t * x2;
            float by = u * u * y1 + 2 * u * t * cy + t * t * y2;

            drawThickLineBresenham(graphics, prevX, prevY, (int) bx, (int) by, color, thickness);
            prevX = (int) bx;
            prevY = (int) by;
        }
    }

    /**
     * Double ligne pour effet plus visible.
     */
    public static void drawDoubleLine(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       int color, int thickness, float spacing) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float px = -dy / length;
        float py = dx / length;

        float offset = Math.max(2, spacing * 3);

        // Deux lignes parallèles
        drawHandDrawnLine(graphics,
                (int) (x1 + px * offset), (int) (y1 + py * offset),
                (int) (x2 + px * offset), (int) (y2 + py * offset),
                color, thickness, spacing);

        drawHandDrawnLine(graphics,
                (int) (x1 - px * offset), (int) (y1 - py * offset),
                (int) (x2 - px * offset), (int) (y2 - py * offset),
                color, thickness, spacing);
    }

    // ============================================================
    // MÉTHODES DE DESSIN DE FLÈCHES
    // ============================================================

    /**
     * Dessine une tête de flèche au point spécifié.
     */
    public static void drawArrowHead(GuiGraphics graphics,
                                      int tipX, int tipY,
                                      float dirX, float dirY,
                                      int color, int thickness,
                                      ArrowStyle style,
                                      int length, float angle, float roughness) {
        switch (style) {
            case SIMPLE -> drawSimpleArrowHead(graphics, tipX, tipY, dirX, dirY, color, thickness, length, angle);
            case FILLED -> drawFilledArrowHead(graphics, tipX, tipY, dirX, dirY, color, length, angle);
            case DIAMOND -> drawDiamondArrowHead(graphics, tipX, tipY, dirX, dirY, color, length);
            case CHEVRON -> drawChevronArrowHead(graphics, tipX, tipY, dirX, dirY, color, thickness, length, angle);
            case HAND_DRAWN -> drawHandDrawnArrowHead(graphics, tipX, tipY, dirX, dirY, color, thickness, length, angle, roughness);
            case DOT -> drawDotArrowHead(graphics, tipX, tipY, color, length);
            case NONE -> {}
        }
    }

    /**
     * Flèche simple (deux lignes).
     */
    public static void drawSimpleArrowHead(GuiGraphics graphics,
                                            int tipX, int tipY,
                                            float dirX, float dirY,
                                            int color, int thickness,
                                            int length, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Branche gauche
        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length);
        int leftY = (int) (tipY - leftDirY * length);

        // Branche droite
        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length);
        int rightY = (int) (tipY - rightDirY * length);

        drawThickLineBresenham(graphics, tipX, tipY, leftX, leftY, color, thickness);
        drawThickLineBresenham(graphics, tipX, tipY, rightX, rightY, color, thickness);
    }

    /**
     * Flèche triangulaire pleine.
     */
    public static void drawFilledArrowHead(GuiGraphics graphics,
                                            int tipX, int tipY,
                                            float dirX, float dirY,
                                            int color,
                                            int length, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Points du triangle
        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length);
        int leftY = (int) (tipY - leftDirY * length);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length);
        int rightY = (int) (tipY - rightDirY * length);

        // Remplir le triangle
        fillTriangle(graphics, tipX, tipY, leftX, leftY, rightX, rightY, color);
    }

    /**
     * Flèche en losange.
     */
    public static void drawDiamondArrowHead(GuiGraphics graphics,
                                             int tipX, int tipY,
                                             float dirX, float dirY,
                                             int color, int size) {
        // 4 points du losange
        int backX = (int) (tipX - dirX * size);
        int backY = (int) (tipY - dirY * size);

        float px = -dirY;
        float py = dirX;
        int halfSize = size / 2;

        int midX = (tipX + backX) / 2;
        int midY = (tipY + backY) / 2;

        int leftX = (int) (midX + px * halfSize);
        int leftY = (int) (midY + py * halfSize);
        int rightX = (int) (midX - px * halfSize);
        int rightY = (int) (midY - py * halfSize);

        // Remplir le losange (deux triangles)
        fillTriangle(graphics, tipX, tipY, leftX, leftY, rightX, rightY, color);
        fillTriangle(graphics, backX, backY, leftX, leftY, rightX, rightY, color);
    }

    /**
     * Flèche chevron (ouverte).
     */
    public static void drawChevronArrowHead(GuiGraphics graphics,
                                             int tipX, int tipY,
                                             float dirX, float dirY,
                                             int color, int thickness,
                                             int length, float angle) {
        // Comme simple mais les lignes ne se rejoignent pas à la pointe
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Point de la pointe légèrement en arrière
        int innerTipX = (int) (tipX - dirX * (length * 0.3f));
        int innerTipY = (int) (tipY - dirY * (length * 0.3f));

        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length);
        int leftY = (int) (tipY - leftDirY * length);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length);
        int rightY = (int) (tipY - rightDirY * length);

        drawThickLineBresenham(graphics, innerTipX, innerTipY, leftX, leftY, color, thickness);
        drawThickLineBresenham(graphics, innerTipX, innerTipY, rightX, rightY, color, thickness);
    }

    /**
     * Flèche manuscrite avec wobble.
     */
    public static void drawHandDrawnArrowHead(GuiGraphics graphics,
                                               int tipX, int tipY,
                                               float dirX, float dirY,
                                               int color, int thickness,
                                               int length, float angle, float roughness) {
        Random rand = createDeterministicRandom(tipX, tipY, (int) (dirX * 100), (int) (dirY * 100));

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        // Ajouter du wobble
        float wobble = roughness * 2;

        float leftDirX = dirX * cos + dirY * sin;
        float leftDirY = -dirX * sin + dirY * cos;
        int leftX = (int) (tipX - leftDirX * length + (rand.nextFloat() - 0.5f) * wobble);
        int leftY = (int) (tipY - leftDirY * length + (rand.nextFloat() - 0.5f) * wobble);

        float rightDirX = dirX * cos - dirY * sin;
        float rightDirY = dirX * sin + dirY * cos;
        int rightX = (int) (tipX - rightDirX * length + (rand.nextFloat() - 0.5f) * wobble);
        int rightY = (int) (tipY - rightDirY * length + (rand.nextFloat() - 0.5f) * wobble);

        // Dessiner avec légère variation
        drawHandDrawnLine(graphics, tipX, tipY, leftX, leftY, color, thickness, roughness * 0.5f);
        drawHandDrawnLine(graphics, tipX, tipY, rightX, rightY, color, thickness, roughness * 0.5f);
    }

    /**
     * Point/cercle au lieu d'une flèche.
     */
    public static void drawDotArrowHead(GuiGraphics graphics,
                                         int x, int y,
                                         int color, int radius) {
        fillCircle(graphics, x, y, radius / 2, color);
    }

    // ============================================================
    // MÉTHODES UTILITAIRES DE BAS NIVEAU
    // ============================================================

    /**
     * Dessine une ligne épaisse avec l'algorithme de Bresenham.
     */
    public static void drawThickLineBresenham(GuiGraphics graphics,
                                               int x1, int y1, int x2, int y2,
                                               int color, int thickness) {
        if (thickness <= 1) {
            drawLineBresenham(graphics, x1, y1, x2, y2, color);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.5f) {
            drawPixel(graphics, x1, y1, color, thickness);
            return;
        }

        // Perpendiculaire normalisée
        float px = -dy / length;
        float py = dx / length;

        // Dessiner plusieurs lignes parallèles
        int halfThick = thickness / 2;
        for (int t = -halfThick; t <= halfThick; t++) {
            int ox = (int) (px * t);
            int oy = (int) (py * t);
            drawLineBresenham(graphics, x1 + ox, y1 + oy, x2 + ox, y2 + oy, color);
        }
    }

    /**
     * Algorithme de Bresenham pour ligne de 1px.
     */
    public static void drawLineBresenham(GuiGraphics graphics,
                                          int x1, int y1, int x2, int y2,
                                          int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    /**
     * Dessine un pixel/carré.
     */
    public static void drawPixel(GuiGraphics graphics, int x, int y, int color, int size) {
        int half = size / 2;
        graphics.fill(x - half, y - half, x - half + size, y - half + size, color);
    }

    /**
     * Remplit un triangle.
     */
    public static void fillTriangle(GuiGraphics graphics,
                                     int x1, int y1, int x2, int y2, int x3, int y3,
                                     int color) {
        // Tri par Y
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
        if (y1 > y3) { int t = y1; y1 = y3; y3 = t; t = x1; x1 = x3; x3 = t; }
        if (y2 > y3) { int t = y2; y2 = y3; y3 = t; t = x2; x2 = x3; x3 = t; }

        // Scanline fill
        for (int y = y1; y <= y3; y++) {
            int xa, xb;

            if (y < y2) {
                xa = interpolate(y, y1, x1, y2, x2);
                xb = interpolate(y, y1, x1, y3, x3);
            } else {
                xa = interpolate(y, y2, x2, y3, x3);
                xb = interpolate(y, y1, x1, y3, x3);
            }

            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            graphics.fill(xa, y, xb + 1, y + 1, color);
        }
    }

    /**
     * Remplit un cercle.
     */
    public static void fillCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
    }

    /**
     * Interpolation linéaire pour scanline.
     */
    private static int interpolate(int y, int y1, int x1, int y2, int x2) {
        if (y1 == y2) return x1;
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
    }

    /**
     * Crée un Random déterministe basé sur les coordonnées.
     */
    private static Random createDeterministicRandom(int x1, int y1, int x2, int y2) {
        long seed = globalSeed + (long) x1 * 73856093L ^ (long) y1 * 19349663L ^ (long) x2 * 83492791L ^ (long) y2 * 47302837L;
        return new Random(seed);
    }

    /**
     * Change le seed global pour la génération aléatoire.
     */
    public static void setGlobalSeed(long seed) {
        globalSeed = seed;
    }

    // ============================================================
    // MÉTHODES SPÉCIALISÉES POUR LE CODEX
    // ============================================================

    /**
     * Dessine une connexion de style Codex entre deux nodes.
     * Combine ligne manuscrite + flèche simple.
     */
    public static void drawCodexConnection(GuiGraphics graphics,
                                            int fromX, int fromY, int fromSize,
                                            int toX, int toY, int toSize,
                                            int color, boolean unlocked) {
        // Calculer les points de départ/arrivée aux bords des nodes
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        float nx = dx / length;
        float ny = dy / length;

        int startX = (int) (fromX + nx * (fromSize / 2 + 2));
        int startY = (int) (fromY + ny * (fromSize / 2 + 2));
        int endX = (int) (toX - nx * (toSize / 2 + 8));
        int endY = (int) (toY - ny * (toSize / 2 + 8));

        // Style selon état
        LineStyle lineStyle = unlocked ? LineStyle.HAND_DRAWN : LineStyle.DASHED;
        ArrowStyle arrowStyle = unlocked ? ArrowStyle.HAND_DRAWN : ArrowStyle.SIMPLE;
        float roughness = unlocked ? 0.4f : 0.2f;

        drawArrow(graphics, startX, startY, endX, endY, color,
                2, lineStyle, arrowStyle, 8, 0.5f, roughness);
    }
}
