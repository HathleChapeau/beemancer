/**
 * ============================================================
 * [NotchedGaugeWidget.java]
 * Description: Widget de jauge verticale crantee pour afficher les niveaux
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | API de rendu         | Dessin de la jauge             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InjectorScreen.java (5 jauges de stats)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Jauge verticale crantee reutilisable.
 * Affiche des crans (notches) pour indiquer les niveaux et le remplissage partiel.
 */
public class NotchedGaugeWidget {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int notchCount;
    private final int pointsPerNotch;
    private final Supplier<Integer> currentPointsSupplier;
    private final int fillColor;
    private final String label;

    /**
     * @param x              position X relative au screen
     * @param y              position Y relative au screen
     * @param width          largeur de la jauge
     * @param height         hauteur de la jauge
     * @param notchCount     nombre de crans (4 pour stats, 2 pour activite)
     * @param pointsPerNotch points necessaires par cran (typiquement 50)
     * @param currentPointsSupplier fournit les points actuels
     * @param fillColor      couleur de remplissage (ARGB)
     * @param label          label court affiche au-dessus
     */
    public NotchedGaugeWidget(int x, int y, int width, int height, int notchCount,
                              int pointsPerNotch, Supplier<Integer> currentPointsSupplier,
                              int fillColor, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.notchCount = notchCount;
        this.pointsPerNotch = pointsPerNotch;
        this.currentPointsSupplier = currentPointsSupplier;
        this.fillColor = fillColor;
        this.label = label;
    }

    public void render(GuiGraphics g, int screenX, int screenY) {
        int gx = screenX + x;
        int gy = screenY + y;

        // Cadre (style enfonce Minecraft)
        g.fill(gx, gy, gx + width, gy + 1, 0xFF373737);
        g.fill(gx, gy, gx + 1, gy + height, 0xFF373737);
        g.fill(gx, gy + height - 1, gx + width, gy + height, 0xFFFFFFFF);
        g.fill(gx + width - 1, gy, gx + width, gy + height, 0xFFFFFFFF);
        g.fill(gx + 1, gy + 1, gx + width - 1, gy + height - 1, 0xFF8B8B8B);

        // Calcul du remplissage
        int currentPoints = currentPointsSupplier.get();
        int maxPoints = notchCount * pointsPerNotch;
        int innerH = height - 2;
        int innerW = width - 2;
        int fillH = maxPoints > 0 ? (int) ((float) Math.min(currentPoints, maxPoints) / maxPoints * innerH) : 0;

        // Barre de remplissage (du bas vers le haut)
        if (fillH > 0) {
            int fillY = gy + height - 1 - fillH;
            g.fill(gx + 1, fillY, gx + 1 + innerW, gy + height - 1, fillColor);

            // Highlight gauche
            int r = (fillColor >> 16) & 0xFF;
            int green = (fillColor >> 8) & 0xFF;
            int b = fillColor & 0xFF;
            int highlight = 0xFF000000 | (Math.min(255, r + 50) << 16) | (Math.min(255, green + 50) << 8) | Math.min(255, b + 50);
            g.fill(gx + 1, fillY, gx + 2, gy + height - 1, highlight);
        }

        // Lignes de crans (separateurs entre niveaux)
        for (int i = 1; i < notchCount; i++) {
            int notchY = gy + height - 1 - (innerH * i / notchCount);
            g.fill(gx + 1, notchY, gx + width - 1, notchY + 1, 0xAA000000);
        }
    }

    public boolean isMouseOver(int screenX, int screenY, int mouseX, int mouseY) {
        int gx = screenX + x;
        int gy = screenY + y;
        return mouseX >= gx && mouseX < gx + width && mouseY >= gy && mouseY < gy + height;
    }

    public List<Component> getTooltip() {
        int currentPoints = currentPointsSupplier.get();
        int filledNotches = currentPoints / pointsPerNotch;
        int partialPoints = currentPoints % pointsPerNotch;
        return List.of(
                Component.literal(label + ": " + filledNotches + "/" + notchCount),
                Component.literal(partialPoints + "/" + pointsPerNotch + " pts")
                        .withStyle(s -> s.withColor(0xAAAAAA))
        );
    }

    public String getLabel() { return label; }
    public int getX() { return x; }
    public int getY() { return y; }
}
