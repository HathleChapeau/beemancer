/**
 * ============================================================
 * [WandOverlayRenderer.java]
 * Description: Renderer HUD pour afficher les données trackées par la baguette
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeWandItem         | Données trackées     | Récupération des valeurs       |
 * | BeemancerItems      | Item baguette        | Vérification item en main      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.hud;

import com.chapeau.beemancer.common.item.bee.BeeWandItem;
import com.chapeau.beemancer.common.item.bee.WandClassTracker;
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

import java.util.List;

/**
 * Renderer pour l'overlay HUD de la baguette magique.
 * Affiche un encadré en haut à gauche avec les données trackées.
 */
@OnlyIn(Dist.CLIENT)
public class WandOverlayRenderer {
    
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BOX_MARGIN = 10;
    private static final int NAME_COLOR = 0xFFAAAAAA;  // Gris clair
    private static final int VALUE_COLOR = 0xFFFFFFFF; // Blanc
    private static final int TITLE_COLOR = 0xFF55FF55; // Vert clair
    private static final int BG_COLOR = 0xAA000000;    // Noir semi-transparent
    private static final int BORDER_COLOR = 0xFF444444; // Gris foncé
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        // Seulement après le layer HOTBAR pour être au-dessus
        if (event.getName() != VanillaGuiLayers.HOTBAR) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        
        // Vérifier si le joueur tient la baguette
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        ItemStack wandStack = null;
        if (mainHand.is(BeemancerItems.BEE_WAND.get())) {
            wandStack = mainHand;
        } else if (offHand.is(BeemancerItems.BEE_WAND.get())) {
            wandStack = offHand;
        }
        
        if (wandStack == null) return;
        
        // Mettre à jour la sélection
        BeeWandItem.updateClientSelection(mc.level, wandStack);
        
        // Dessiner l'overlay
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        
        renderOverlay(graphics, font);
    }
    
    private static void renderOverlay(GuiGraphics graphics, Font font) {
        List<WandClassTracker.TrackedValue> values = BeeWandItem.getTrackedValues();
        String className = BeeWandItem.getSelectedClassName();
        boolean hasSelection = BeeWandItem.hasSelection();
        
        // Titre
        String title = hasSelection ? "Wand: " + className : "Wand: No Selection";
        
        // Calculer les dimensions
        int maxWidth = font.width(title);
        for (WandClassTracker.TrackedValue value : values) {
            int lineWidth = font.width(value.name() + ": " + value.value());
            maxWidth = Math.max(maxWidth, lineWidth);
        }
        
        int boxWidth = maxWidth + PADDING * 2;
        int boxHeight = PADDING * 2 + LINE_HEIGHT; // Titre
        if (hasSelection && !values.isEmpty()) {
            boxHeight += LINE_HEIGHT * values.size() + PADDING;
        }
        
        int x = BOX_MARGIN;
        int y = BOX_MARGIN;
        
        // Dessiner le fond
        graphics.fill(x, y, x + boxWidth, y + boxHeight, BG_COLOR);
        
        // Dessiner la bordure
        graphics.fill(x, y, x + boxWidth, y + 1, BORDER_COLOR); // Top
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, BORDER_COLOR); // Bottom
        graphics.fill(x, y, x + 1, y + boxHeight, BORDER_COLOR); // Left
        graphics.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, BORDER_COLOR); // Right
        
        // Dessiner le titre
        int textX = x + PADDING;
        int textY = y + PADDING;
        graphics.drawString(font, title, textX, textY, hasSelection ? TITLE_COLOR : NAME_COLOR, false);
        textY += LINE_HEIGHT;
        
        // Dessiner les valeurs
        if (hasSelection && !values.isEmpty()) {
            textY += PADDING / 2;
            for (WandClassTracker.TrackedValue value : values) {
                String nameStr = value.name() + ": ";
                String valueStr = value.value();
                
                // Nom en gris
                graphics.drawString(font, nameStr, textX, textY, NAME_COLOR, false);
                // Valeur en blanc
                int valueX = textX + font.width(nameStr);
                graphics.drawString(font, valueStr, valueX, textY, VALUE_COLOR, false);
                
                textY += LINE_HEIGHT;
            }
        }
    }
}
