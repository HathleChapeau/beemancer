/**
 * ============================================================
 * [GuiRenderHelper.java]
 * Description: Methodes utilitaires pour le rendu programmatique des GUI
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | API de rendu         | Dessin fill, string            |
 * | Font                | Police texte         | Labels                         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les screens d'alchimie et de hive (rendu programmatique)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class GuiRenderHelper {

    /**
     * Fond de container Minecraft avec bordures 3D, labels et separateur.
     */
    public static void renderContainerBackground(GuiGraphics g, Font font, int x, int y,
                                                   int w, int h, String titleKey, int separatorY) {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);

        // 3D border - top/left (light)
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFDBDBDB);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFDBDBDB);

        // 3D border - bottom/right (dark)
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF7B7B7B);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF7B7B7B);

        // Separator line
        g.fill(x + 7, y + separatorY, x + w - 7, y + separatorY + 1, 0xFF8B8B8B);

        // Labels
        g.drawString(font, Component.translatable(titleKey), x + 8, y + 6, 0x404040, false);
        g.drawString(font, Component.translatable("container.inventory"),
                     x + 8, y + separatorY + 3, 0x404040, false);
    }

    /**
     * Fond de slot Minecraft standard (18x18 avec inset 3D).
     */
    public static void renderSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 18, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    /**
     * Grille slots de l'inventaire joueur (27 + 9 hotbar).
     */
    public static void renderPlayerInventory(GuiGraphics g, int x, int y, int invY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderSlot(g, x + 7 + col * 18, y + invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            renderSlot(g, x + 7 + col * 18, y + hotbarY);
        }
    }

    /**
     * Fleche de progression avec remplissage dore et pointe decorative.
     */
    public static void renderProgressArrow(GuiGraphics g, int x, int y, float ratio) {
        int arrowWidth = 36;
        int arrowHeight = 17;

        // Background groove
        g.fill(x, y, x + arrowWidth, y + arrowHeight, 0xFF8B8B8B);
        g.fill(x, y, x + arrowWidth, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + arrowHeight, 0xFF373737);
        g.fill(x + 1, y + arrowHeight - 1, x + arrowWidth, y + arrowHeight, 0xFFFFFFFF);
        g.fill(x + arrowWidth - 1, y + 1, x + arrowWidth, y + arrowHeight, 0xFFFFFFFF);

        // Fill (honey gold)
        int fillWidth = (int) (arrowWidth * ratio);
        if (fillWidth > 0) {
            g.fill(x + 1, y + 1, x + 1 + fillWidth, y + arrowHeight - 1, 0xFFD4A017);
            g.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0xFFE8B830);
        }

        // Arrow tip
        int tipX = x + arrowWidth + 2;
        int midY = y + arrowHeight / 2;
        for (int i = 0; i < 5; i++) {
            g.fill(tipX + i, midY - 4 + i, tipX + i + 1, midY + 5 - i, 0xFF8B8B8B);
        }
    }

    /**
     * Barre de progression simple (sans fleche).
     */
    public static void renderProgressBar(GuiGraphics g, int x, int y, int w, int h, float ratio) {
        // Background
        g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
        // Border
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFF3A3A3A);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF3A3A3A);
        g.fill(x - 1, y, x, y + h, 0xFF3A3A3A);
        g.fill(x + w, y, x + w + 1, y + h, 0xFF3A3A3A);

        // Fill
        int fillWidth = (int) (w * ratio);
        if (fillWidth > 0) {
            g.fill(x, y, x + fillWidth, y + h, 0xFFD4A017);
            g.fill(x, y, x + fillWidth, y + 1, 0xFFE8B830);
        }
    }

    /**
     * Onglet individuel avec bordure 3D style Minecraft.
     * L'onglet actif est surélevé (fond clair, pas de bordure basse).
     * L'onglet inactif est enfoncé (fond plus sombre).
     */
    public static void renderTab(GuiGraphics g, Font font, int x, int y, int w, int h,
                                  boolean active, String label) {
        if (active) {
            // Onglet actif: fond container, pas de bordure basse (fusionné avec le contenu)
            g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
            // Bordure top/left (light)
            g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
            g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
            // Bordure right (dark)
            g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        } else {
            // Onglet inactif: fond plus sombre
            g.fill(x, y + 2, x + w, y + h, 0xFFAAAAAA);
            // Bordure top/left (dark light)
            g.fill(x, y + 2, x + w, y + 3, 0xFFDBDBDB);
            g.fill(x, y + 2, x + 1, y + h, 0xFFDBDBDB);
            // Bordure right/bottom (dark)
            g.fill(x + w - 1, y + 2, x + w, y + h, 0xFF555555);
            g.fill(x, y + h - 1, x + w, y + h, 0xFF7B7B7B);
        }

        // Label centré
        int textWidth = font.width(label);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (active ? (h - 8) / 2 : (h - 8) / 2 + 1);
        g.drawString(font, label, textX, textY, active ? 0x404040 : 0x606060, false);
    }

    /**
     * Barre d'onglets complète. Rend N onglets côte à côte.
     * Dessine aussi la ligne de séparation sous les onglets inactifs.
     *
     * @param labels les labels traduits des onglets
     * @param activeIndex l'index de l'onglet actif (0-based)
     * @return les positions X de début de chaque onglet (pour la détection de clic)
     */
    public static int[] renderTabBar(GuiGraphics g, Font font, int x, int y,
                                      int totalWidth, String[] labels, int activeIndex) {
        int tabCount = labels.length;
        int tabWidth = totalWidth / tabCount;
        int tabHeight = 16;
        int[] tabPositions = new int[tabCount];

        for (int i = 0; i < tabCount; i++) {
            int tabX = x + i * tabWidth;
            tabPositions[i] = tabX;
            renderTab(g, font, tabX, y, tabWidth, tabHeight, i == activeIndex, labels[i]);
        }

        return tabPositions;
    }

    /**
     * Fond de container sans titre ni séparateur (pour usage avec onglets).
     */
    public static void renderContainerBackgroundNoTitle(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);

        // 3D border - top/left (light)
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFDBDBDB);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFDBDBDB);

        // 3D border - bottom/right (dark)
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF7B7B7B);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF7B7B7B);
    }

    /**
     * Grille de slots NxM.
     */
    public static void renderSlotGrid(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                renderSlot(g, x + col * 18, y + row * 18);
            }
        }
    }

    /**
     * Bouton cliquable simple avec texte centré et survol.
     */
    public static void renderButton(GuiGraphics g, Font font, int x, int y, int w, int h,
                                     String label, boolean hovered) {
        int bg = hovered ? 0xFFDBDBDB : 0xFFC6C6C6;
        g.fill(x, y, x + w, y + h, bg);
        // Bordures 3D
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
        // Label centré
        int textWidth = font.width(label);
        g.drawString(font, label, x + (w - textWidth) / 2, y + (h - 8) / 2, 0x404040, false);
    }

    /**
     * Nom lisible d'un fluide Beemancer.
     */
    public static String getFluidName(FluidStack fluid) {
        if (fluid.isEmpty()) return "Empty";
        String path = fluid.getFluid().builtInRegistryHolder().key().location().getPath();
        if (path.contains("honey")) return "Honey";
        if (path.contains("royal_jelly")) return "Royal Jelly";
        if (path.contains("nectar")) return "Nectar";
        return "Fluid";
    }
}
