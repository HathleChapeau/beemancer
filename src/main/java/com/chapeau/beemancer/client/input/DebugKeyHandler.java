/**
 * ============================================================
 * [DebugKeyHandler.java]
 * Description: Gestionnaire des touches pour le panneau de debug
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DebugWandItem       | Valeurs à modifier   | Ajustement des valeurs         |
 * | DebugPanelRenderer  | Vérification wand    | isHoldingDebugWand()           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement de l'event
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.input;

import com.chapeau.beemancer.client.gui.hud.DebugPanelRenderer;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Gestionnaire des touches pour le debug panel.
 *
 * Touches:
 * - Numpad 1-9: Sélectionner une valeur
 * - Flèche gauche: Diminuer de 0.1 (Shift: 1.0, Ctrl: 0.01)
 * - Flèche droite: Augmenter de 0.1 (Shift: 1.0, Ctrl: 0.01)
 */
@OnlyIn(Dist.CLIENT)
public class DebugKeyHandler {

    private static final float FINE_STEP = 0.01f;
    private static final float SMALL_STEP = 0.1f;
    private static final float LARGE_STEP = 1.0f;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Seulement sur appui (pas relâchement)
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Ne pas traiter si un écran est ouvert (chat, inventaire, etc.)
        if (mc.screen != null) return;

        // Vérifier si le joueur tient la debug wand (pour le reste des touches)
        if (!DebugPanelRenderer.isHoldingDebugWand(player)) return;

        int key = event.getKey();
        boolean shift = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        // Touches du pavé numérique 1-9 uniquement (pas les touches normales pour éviter conflit hotbar)
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            int index = key - GLFW.GLFW_KEY_KP_1 + 1;
            DebugWandItem.selectValue(index);
            return;
        }

        // Flèche haut - sélectionner valeur précédente (avec boucle)
        if (key == GLFW.GLFW_KEY_UP) {
            int newIndex = DebugWandItem.selectedIndex - 1;
            if (newIndex < 1) newIndex = 9;
            DebugWandItem.selectValue(newIndex);
            return;
        }

        // Flèche bas - sélectionner valeur suivante (avec boucle)
        if (key == GLFW.GLFW_KEY_DOWN) {
            int newIndex = DebugWandItem.selectedIndex + 1;
            if (newIndex > 9) newIndex = 1;
            DebugWandItem.selectValue(newIndex);
            return;
        }

        // Flèche gauche - diminuer
        if (key == GLFW.GLFW_KEY_LEFT) {
            float delta = shift ? -LARGE_STEP : ctrl ? -FINE_STEP : -SMALL_STEP;
            DebugWandItem.adjustSelectedValue(delta);
            return;
        }

        // Flèche droite - augmenter
        if (key == GLFW.GLFW_KEY_RIGHT) {
            float delta = shift ? LARGE_STEP : ctrl ? FINE_STEP : SMALL_STEP;
            DebugWandItem.adjustSelectedValue(delta);
            return;
        }

        // R - Reset toutes les valeurs
        if (key == GLFW.GLFW_KEY_R && shift) {
            DebugWandItem.resetAll();
        }
    }
}
