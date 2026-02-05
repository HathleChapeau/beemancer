/**
 * ============================================================
 * [LineDrawingHelper.java]
 * Description: Utilitaire pour dessiner des lignes en 3 segments
 * ============================================================
 *
 * SYSTEME DE TRAITS:
 * - 3 segments: droit -> virage -> droit
 * - Mode HORIZONTAL: part en X, tourne, finit en Y
 * - Mode VERTICAL: part en Y, tourne, finit en X
 * - Pourcentage: position du virage sur l'axe principal
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
 * Dessine des connexions en 3 segments entre nodes du Codex.
 */
public class LineDrawingHelper {

    // ============================================================
    // MODE DE CONNEXION
    // ============================================================

    public enum ConnectionMode {
        /** Part horizontalement, tourne, finit verticalement */
        HORIZONTAL,
        /** Part verticalement, tourne, finit horizontalement */
        VERTICAL
    }

    // ============================================================
    // COULEURS
    // ============================================================

    public static final int COLOR_LOCKED = 0xFF9E9E9E;    // Gris
    public static final int COLOR_UNLOCKED = 0xFFE6A700;  // Ambre dore

    // ============================================================
    // CONSTANTES
    // ============================================================

    public static final int DEFAULT_THICKNESS = 2;

    // ============================================================
    // METHODE PRINCIPALE
    // ============================================================

    /**
     * Dessine une connexion en 3 segments entre deux points.
     *
     * @param graphics Le contexte graphique
     * @param x1 Position X du centre du node 1
     * @param y1 Position Y du centre du node 1
     * @param x2 Position X du centre du node 2
     * @param y2 Position Y du centre du node 2
     * @param mode HORIZONTAL ou VERTICAL (direction du premier segment)
     * @param turnPercent Pourcentage (0.0 a 1.0) ou se fait le virage
     * @param unlocked true = ambre, false = gris
     * @param thickness Epaisseur du trait
     */
    public static void drawConnection(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       ConnectionMode mode, float turnPercent,
                                       boolean unlocked, int thickness) {
        int color = unlocked ? COLOR_UNLOCKED : COLOR_LOCKED;
        int halfT = thickness / 2;

        // Clamp le pourcentage entre 0 et 1
        turnPercent = Math.max(0.0f, Math.min(1.0f, turnPercent));

        if (mode == ConnectionMode.HORIZONTAL) {
            // Mode horizontal: X d'abord, puis Y
            // Position X du virage
            int turnX = x1 + (int)((x2 - x1) * turnPercent);

            // Segment 1: horizontal de (x1, y1) a (turnX, y1)
            drawHorizontalLine(graphics, x1, turnX, y1, color, halfT);

            // Segment 2: vertical de (turnX, y1) a (turnX, y2)
            drawVerticalLine(graphics, turnX, y1, y2, color, halfT);

            // Segment 3: horizontal de (turnX, y2) a (x2, y2)
            drawHorizontalLine(graphics, turnX, x2, y2, color, halfT);

        } else {
            // Mode vertical: Y d'abord, puis X
            // Position Y du virage
            int turnY = y1 + (int)((y2 - y1) * turnPercent);

            // Segment 1: vertical de (x1, y1) a (x1, turnY)
            drawVerticalLine(graphics, x1, y1, turnY, color, halfT);

            // Segment 2: horizontal de (x1, turnY) a (x2, turnY)
            drawHorizontalLine(graphics, x1, x2, turnY, color, halfT);

            // Segment 3: vertical de (x2, turnY) a (x2, y2)
            drawVerticalLine(graphics, x2, turnY, y2, color, halfT);
        }
    }

    /**
     * Version simplifiee avec epaisseur par defaut.
     */
    public static void drawConnection(GuiGraphics graphics,
                                       int x1, int y1, int x2, int y2,
                                       ConnectionMode mode, float turnPercent,
                                       boolean unlocked) {
        drawConnection(graphics, x1, y1, x2, y2, mode, turnPercent, unlocked, DEFAULT_THICKNESS);
    }

    // ============================================================
    // METHODES DE DESSIN PRIMITIVES
    // ============================================================

    /**
     * Dessine une ligne horizontale.
     */
    private static void drawHorizontalLine(GuiGraphics graphics,
                                            int x1, int x2, int y,
                                            int color, int halfThickness) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        graphics.fill(minX, y - halfThickness, maxX, y + halfThickness, color);
    }

    /**
     * Dessine une ligne verticale.
     */
    private static void drawVerticalLine(GuiGraphics graphics,
                                          int x, int y1, int y2,
                                          int color, int halfThickness) {
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        graphics.fill(x - halfThickness, minY, x + halfThickness, maxY, color);
    }
}
