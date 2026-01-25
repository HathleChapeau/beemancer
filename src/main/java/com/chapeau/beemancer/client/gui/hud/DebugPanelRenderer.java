/**
 * ============================================================
 * [DebugPanelRenderer.java]
 * Description: Renderer HUD pour le panneau de debug
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Valeurs à afficher   | Lecture des 9 valeurs          |
 * | BeemancerItems      | Item baguette        | Vérification item en main      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.hud;

import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renderer pour le panneau de debug.
 * Affiche les 9 valeurs et indique laquelle est sélectionnée.
 */
@OnlyIn(Dist.CLIENT)
public class DebugPanelRenderer {

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int BOX_MARGIN = 10;

    // Couleurs
    private static final int BG_COLOR = 0xDD000000;        // Noir quasi-opaque
    private static final int BORDER_COLOR = 0xFF555555;    // Gris
    private static final int TITLE_COLOR = 0xFFFFAA00;     // Orange
    private static final int LABEL_COLOR = 0xFFAAAAAA;     // Gris clair
    private static final int VALUE_COLOR = 0xFFFFFFFF;     // Blanc
    private static final int SELECTED_BG = 0xFF333355;     // Bleu foncé
    private static final int SELECTED_COLOR = 0xFF55FF55;  // Vert clair
    private static final int HINT_COLOR = 0xFF888888;      // Gris

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (event.getName() != VanillaGuiLayers.HOTBAR) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Vérifier si le joueur tient la debug wand
        if (!isHoldingDebugWand(player)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        renderPanel(graphics, font);
    }

    /**
     * Vérifie si le joueur tient la debug wand
     */
    public static boolean isHoldingDebugWand(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.is(BeemancerItems.DEBUG_WAND.get())
            || offHand.is(BeemancerItems.DEBUG_WAND.get());
    }

    private static void renderPanel(GuiGraphics graphics, Font font) {
        String title = "Debug Panel";
        String hint1 = "[Numpad 1-9 / ↑↓] Select";
        String hint2 = "[←→] ±0.1 | [Shift] ±1.0";
        String hint3 = "[Shift+R] Reset";

        // Calculer dimensions
        int maxWidth = font.width(title);
        maxWidth = Math.max(maxWidth, font.width(hint1));
        maxWidth = Math.max(maxWidth, font.width(hint2));
        maxWidth = Math.max(maxWidth, font.width(hint3));

        for (int i = 1; i <= 9; i++) {
            String line = "Value " + i + ": " + formatValue(DebugWandItem.getValue(i));
            maxWidth = Math.max(maxWidth, font.width(line) + 10); // +10 pour indicateur sélection
        }

        int boxWidth = maxWidth + PADDING * 2;
        int boxHeight = PADDING * 2 + LINE_HEIGHT // Titre
                + LINE_HEIGHT * 9 + PADDING       // 9 valeurs
                + LINE_HEIGHT * 3 + PADDING;      // 3 lignes de hints

        int x = BOX_MARGIN;
        int y = BOX_MARGIN;

        // Fond
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);

        // Bordure
        drawBorder(graphics, x, y, boxWidth, boxHeight, BORDER_COLOR);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Titre
        graphics.drawString(font, title, textX, textY, TITLE_COLOR, false);
        textY += LINE_HEIGHT + 2;

        // Ligne séparatrice
        graphics.fill(x + 2, textY - 1, x + boxWidth - 2, textY, BORDER_COLOR);
        textY += 3;

        // Les 9 valeurs
        for (int i = 1; i <= 9; i++) {
            boolean isSelected = (i == DebugWandItem.selectedIndex);

            // Fond de sélection
            if (isSelected) {
                graphics.fill(x + 2, textY - 1, x + boxWidth - 2, textY + LINE_HEIGHT - 1, SELECTED_BG);
            }

            // Indicateur de sélection
            String prefix = isSelected ? "> " : "  ";
            String label = prefix + "Value " + i + ": ";
            String value = formatValue(DebugWandItem.getValue(i));

            int labelColor = isSelected ? SELECTED_COLOR : LABEL_COLOR;
            int valueColor = isSelected ? SELECTED_COLOR : VALUE_COLOR;

            graphics.drawString(font, label, textX, textY, labelColor, false);
            graphics.drawString(font, value, textX + font.width(label), textY, valueColor, false);

            textY += LINE_HEIGHT;
        }

        // Ligne séparatrice
        textY += 2;
        graphics.fill(x + 2, textY - 1, x + boxWidth - 2, textY, BORDER_COLOR);
        textY += 3;

        // Hints (3 lignes)
        graphics.drawString(font, hint1, textX, textY, HINT_COLOR, false);
        textY += LINE_HEIGHT;
        graphics.drawString(font, hint2, textX, textY, HINT_COLOR, false);
        textY += LINE_HEIGHT;
        graphics.drawString(font, hint3, textX, textY, HINT_COLOR, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);                    // Top
        graphics.fill(x, y + height - 1, x + width, y + height, color);  // Bottom
        graphics.fill(x, y, x + 1, y + height, color);                   // Left
        graphics.fill(x + width - 1, y, x + width, y + height, color);   // Right
    }

    private static String formatValue(float value) {
        // Format avec 2 décimales, signe explicite pour positifs
        if (value >= 0) {
            return String.format("+%.2f", value);
        }
        return String.format("%.2f", value);
    }
}
