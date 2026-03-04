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
package com.chapeau.apica.client.gui;

import com.chapeau.apica.Apica;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class GuiRenderHelper {

    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/machin-slot-0.png");
    private static final ResourceLocation SLOTS_2X2_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/machin-slots-0.png");

    // Texture-based honey bars (16x50 each)
    private static final ResourceLocation LEFT_HONEYBAR_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/left_honeybar_bg.png");
    private static final ResourceLocation LEFT_HONEYBAR = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/left_honeybar.png");
    private static final ResourceLocation RIGHT_HONEYBAR_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/right_honeybar_bg.png");
    private static final ResourceLocation RIGHT_HONEYBAR = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/right_honeybar.png");

    // Texture-based right bar (16x50, neutral tintable)
    private static final ResourceLocation RIGHT_BAR = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/right_bar.png");
    private static final int BAR_W = 16;
    private static final int BAR_H = 50;

    // Texture-based progress bar (bg: 54x10, fill: 50x6)
    private static final ResourceLocation PROGRESSBAR_BG = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/progressbar_bg.png");
    private static final ResourceLocation PROGRESSBAR = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/progressbar.png");

    private static final int HONEYBAR_W = 16;
    private static final int HONEYBAR_H = 50;
    private static final int PROGRESSBAR_BG_W = 54;
    private static final int PROGRESSBAR_BG_H = 10;
    private static final int PROGRESSBAR_FILL_W = 50;
    private static final int PROGRESSBAR_FILL_H = 6;

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
     * Fond de slot via texture (18x18 drawn from 19x19 source).
     */
    public static void renderSlot(GuiGraphics g, int x, int y) {
        g.blit(SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18);
    }

    /**
     * Grille de slots 2x2 via texture (36x36).
     */
    public static void renderSlots2x2(GuiGraphics g, int x, int y) {
        g.blit(SLOTS_2X2_TEXTURE, x, y, 0, 0, 36, 36, 36, 36);
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
        int arrowHeight = 18;

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
     * Barre de progression simple (sans fleche). Couleur doree par defaut.
     */
    public static void renderProgressBar(GuiGraphics g, int x, int y, int w, int h, float ratio) {
        renderProgressBar(g, x, y, w, h, ratio, 0xFFD4A017, 0xFFE8B830);
    }

    /**
     * Barre de progression simple (sans fleche) avec couleur personnalisee.
     *
     * @param fillColor      couleur principale de remplissage (ARGB)
     * @param highlightColor couleur de la ligne de highlight en haut (ARGB)
     */
    public static void renderProgressBar(GuiGraphics g, int x, int y, int w, int h, float ratio,
                                          int fillColor, int highlightColor) {
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
            g.fill(x, y, x + fillWidth, y + h, fillColor);
            g.fill(x, y, x + fillWidth, y + 1, highlightColor);
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
     * Onglet vertical avec bordure 3D.
     * L'onglet actif fusionne avec le panneau à droite (pas de bordure droite).
     * L'onglet inactif est enfoncé.
     */
    public static void renderVerticalTab(GuiGraphics g, Font font,
                                          int x, int y, int w, int h,
                                          boolean active, String label) {
        if (active) {
            g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
            // Bordures top/left/bottom (light top/left, dark bottom)
            g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
            g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
            g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        } else {
            g.fill(x + 2, y, x + w, y + h, 0xFFAAAAAA);
            g.fill(x + 2, y, x + w, y + 1, 0xFFDBDBDB);
            g.fill(x + 2, y, x + 3, y + h, 0xFFDBDBDB);
            g.fill(x + 2, y + h - 1, x + w, y + h, 0xFF555555);
            g.fill(x + w - 1, y, x + w, y + h, 0xFF7B7B7B);
        }

        // Label centré (single char)
        int textWidth = font.width(label);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        g.drawString(font, label, textX, textY, active ? 0x404040 : 0x606060, false);
    }

    // === Texture-based rendering ===

    /**
     * Rend une barre de miel (gauche) via texture. Remplit de bas en haut.
     */
    public static void renderLeftHoneyBar(GuiGraphics g, int x, int y, float ratio) {
        g.blit(LEFT_HONEYBAR_BG, x, y, 0, 0, HONEYBAR_W, HONEYBAR_H, HONEYBAR_W, HONEYBAR_H);
        int fillH = (int) (HONEYBAR_H * Math.min(1f, ratio));
        if (fillH > 0) {
            int srcY = HONEYBAR_H - fillH;
            g.blit(LEFT_HONEYBAR, x, y + srcY, 0, srcY, HONEYBAR_W, fillH, HONEYBAR_W, HONEYBAR_H);
        }
    }

    /**
     * Rend une barre de miel (droite) via texture. Remplit de bas en haut.
     */
    public static void renderRightHoneyBar(GuiGraphics g, int x, int y, float ratio) {
        g.blit(RIGHT_HONEYBAR_BG, x, y, 0, 0, HONEYBAR_W, HONEYBAR_H, HONEYBAR_W, HONEYBAR_H);
        int fillH = (int) (HONEYBAR_H * Math.min(1f, ratio));
        if (fillH > 0) {
            int srcY = HONEYBAR_H - fillH;
            g.blit(RIGHT_HONEYBAR, x, y + srcY, 0, srcY, HONEYBAR_W, fillH, HONEYBAR_W, HONEYBAR_H);
        }
    }

    /**
     * Rend une barre de progression via texture. Remplit de gauche a droite.
     */
    public static void renderTextureProgressBar(GuiGraphics g, int x, int y, float ratio) {
        g.blit(PROGRESSBAR_BG, x, y, 0, 0, PROGRESSBAR_BG_W, PROGRESSBAR_BG_H,
               PROGRESSBAR_BG_W, PROGRESSBAR_BG_H);
        int fillW = (int) (PROGRESSBAR_FILL_W * Math.min(1f, ratio));
        if (fillW > 0) {
            g.blit(PROGRESSBAR, x + 2, y + 2, 0, 0, fillW, PROGRESSBAR_FILL_H,
                   PROGRESSBAR_FILL_W, PROGRESSBAR_FILL_H);
        }
    }

    /**
     * Rend une barre de progression via texture, orientee verticalement (haut vers bas).
     * Utilise les memes textures que la version horizontale, pivotees de 90 degres.
     * Largeur resultante: 10px. Hauteur parametrable via scale.
     *
     * @param height hauteur souhaitee en pixels (la texture native fait 54px)
     */
    public static void renderVerticalTextureProgressBar(GuiGraphics g, int x, int y, float ratio, int height) {
        float scale = (float) height / PROGRESSBAR_BG_W;
        g.pose().pushPose();
        g.pose().translate(x + PROGRESSBAR_BG_H, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(90));
        g.pose().scale(scale, 1, 1);
        g.blit(PROGRESSBAR_BG, 0, 0, 0, 0, PROGRESSBAR_BG_W, PROGRESSBAR_BG_H,
               PROGRESSBAR_BG_W, PROGRESSBAR_BG_H);
        int fillW = (int) (PROGRESSBAR_FILL_W * Math.min(1f, ratio));
        if (fillW > 0) {
            g.blit(PROGRESSBAR, 2, 2, 0, 0, fillW, PROGRESSBAR_FILL_H,
                   PROGRESSBAR_FILL_W, PROGRESSBAR_FILL_H);
        }
        g.pose().popPose();
    }

    /**
     * Rend une barre via texture right_bar.png avec teinte de couleur.
     * Le fond utilise right_honeybar_bg, le remplissage teinte avec la couleur donnee.
     * Remplit de bas en haut selon le ratio. Sans marqueurs d'etapes.
     *
     * @param g     contexte de rendu
     * @param x     position X ecran
     * @param y     position Y ecran
     * @param ratio remplissage 0.0 a 1.0
     * @param color couleur de teinte (RGB sans alpha, ex: 0xE8A317)
     */
    public static void renderTintedBar(GuiGraphics g, int x, int y, float ratio, int color) {
        renderTintedBar(g, x, y, ratio, color, 0);
    }

    /**
     * Rend une barre via texture right_bar.png avec teinte de couleur et marqueurs d'etapes.
     * Le fond utilise right_honeybar_bg, le remplissage teinte avec la couleur donnee.
     * Remplit de bas en haut selon le ratio.
     * Des lignes horizontales semi-transparentes sont dessinees par-dessus pour indiquer les niveaux.
     *
     * @param g       contexte de rendu
     * @param x       position X ecran
     * @param y       position Y ecran
     * @param ratio   remplissage 0.0 a 1.0
     * @param color   couleur de teinte (RGB sans alpha, ex: 0xE8A317)
     * @param notches nombre de niveaux/etapes (0 = pas de marqueurs)
     */
    public static void renderTintedBar(GuiGraphics g, int x, int y, float ratio, int color, int notches) {
        // Fond via right_honeybar_bg (barre vide)
        g.blit(RIGHT_HONEYBAR_BG, x, y, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);

        // Remplissage teinte (de bas en haut)
        int fillH = (int) (BAR_H * Math.min(1f, ratio));
        if (fillH > 0) {
            float r = ((color >> 16) & 0xFF) / 255f;
            float green = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            RenderSystem.setShaderColor(r, green, b, 1.0f);
            int srcY = BAR_H - fillH;
            g.blit(RIGHT_BAR, x, y + srcY, 0, srcY, BAR_W, fillH, BAR_W, BAR_H);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // Marqueurs d'etapes (lignes horizontales par-dessus)
        if (notches > 1) {
            int innerH = BAR_H - 4;
            for (int i = 1; i < notches; i++) {
                int notchY = y + BAR_H - 2 - (innerH * i / notches);
                g.fill(x + 2, notchY, x + BAR_W - 2, notchY + 1, 0xAA000000);
            }
        }
    }

    /**
     * Verifie si la souris survole une barre tintee (16x50).
     */
    public static boolean isTintedBarHovered(int barX, int barY, int screenX, int screenY,
                                              int mouseX, int mouseY) {
        int ax = screenX + barX;
        int ay = screenY + barY;
        return mouseX >= ax && mouseX < ax + BAR_W && mouseY >= ay && mouseY < ay + BAR_H;
    }

    /**
     * Verifie si la souris survole une barre de miel (16x50).
     */
    public static boolean isHoneyBarHovered(int barX, int barY, int screenX, int screenY,
                                              int mouseX, int mouseY) {
        int ax = screenX + barX;
        int ay = screenY + barY;
        return mouseX >= ax && mouseX < ax + HONEYBAR_W && mouseY >= ay && mouseY < ay + HONEYBAR_H;
    }

    /**
     * Rendu d'un petit item en badge dans le coin bas-droit d'une zone.
     * Utilise par: IItemDecorator (backpack, magazine), CodexNodeWidget (badge node).
     * @param g       GuiGraphics
     * @param icon    Item a rendre en badge
     * @param x       Position X du coin haut-gauche de la zone parente
     * @param y       Position Y du coin haut-gauche de la zone parente
     * @param size    Taille de la zone parente (ex: 16 pour un slot, 24 pour un node)
     * @param scale   Echelle du badge (ex: 0.5f pour demi-taille)
     * @param zOffset Z effectif cible (200 = devant les items, sous les tooltips).
     *                GuiGraphics.renderItem ajoute z=150 en interne, on compense ici.
     */
    public static void renderBadgeIcon(GuiGraphics g, ItemStack icon, int x, int y,
                                        int size, float scale, float zOffset) {
        PoseStack pose = g.pose();
        pose.pushPose();
        // renderItem pousse z+150 en interne ; compenser pour atteindre le zOffset demande
        pose.translate(x + size - 16 * scale, y + size - 16 * scale, Math.max(0, zOffset - 150));
        pose.scale(scale, scale, 1.0f);
        g.renderItem(icon, 0, 0);
        pose.popPose();
    }

    /**
     * Rendu d'une texture en badge dans le coin bas-droit d'une zone.
     * Variante texture de renderBadgeIcon pour les cas sans ItemStack.
     * Utilise par: IItemDecorator (magnet sur bee_magnet).
     */
    public static void renderBadgeTexture(GuiGraphics g, ResourceLocation texture, int x, int y,
                                           int size, float scale, float zOffset) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + size - 16 * scale, y + size - 16 * scale, zOffset);
        pose.scale(scale, scale, 1.0f);
        g.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        pose.popPose();
    }

    /**
     * Nom lisible d'un fluide Apica.
     */
    public static String getFluidName(FluidStack fluid) {
        if (fluid.isEmpty()) return "";
        String path = fluid.getFluid().builtInRegistryHolder().key().location().getPath();
        if (path.contains("honey")) return "Honey";
        if (path.contains("royal_jelly")) return "Royal Jelly";
        if (path.contains("nectar")) return "Nectar";
        return "Fluid";
    }
}
