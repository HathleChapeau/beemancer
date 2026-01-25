/**
 * ============================================================
 * [DebugWandItem.java]
 * Description: Baguette de debug avec 9 valeurs ajustables en temps réel
 * ============================================================
 *
 * UTILISÉ PAR:
 * - DebugPanelRenderer.java: Affichage HUD
 * - DebugKeyHandler.java: Gestion des touches
 * - MagicBeeItemRenderer.java: Utilisation des valeurs pour ajustements
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.debug;

import net.minecraft.world.item.Item;

/**
 * Baguette de debug permettant d'ajuster 9 valeurs en temps réel.
 *
 * Utilisation:
 * - Tenir la baguette en main pour voir le panneau
 * - Touches 1-9 du PAVÉ NUMÉRIQUE pour sélectionner une valeur
 * - Flèche gauche/droite pour ajuster de 0.1
 * - Shift + flèche pour ajuster de 1.0
 */
public class DebugWandItem extends Item {

    // Valeurs par défaut (pour reset)
    // 0-2: Position X, Y, Z | 3-4: Rotation X, Y | 5: (libre) | 6: Scale | 7-8: (libre)
    private static final float[] DEFAULTS = {0.5f, 1.1f, 0.5f, -30.0f, 225.0f, 0.0f, 1.4f, 0.0f, 0.0f};

    // Les 9 valeurs ajustables (index 0-8, affichées comme 1-9)
    public static final float[] values = {0.5f, 1.1f, 0.5f, -30.0f, 225.0f, 0.0f, 1.4f, 0.0f, 0.0f};

    // Index de la valeur sélectionnée (1-9 pour l'affichage, 0-8 en interne)
    public static int selectedIndex = 1;

    public DebugWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Récupère la valeur à l'index donné (1-9)
     */
    public static float getValue(int index) {
        if (index >= 1 && index <= 9) {
            return values[index - 1];
        }
        return 0f;
    }

    /**
     * Définit la valeur à l'index donné (1-9)
     */
    public static void setValue(int index, float value) {
        if (index >= 1 && index <= 9) {
            values[index - 1] = value;
        }
    }

    /**
     * Ajuste la valeur sélectionnée
     * @param delta Le montant à ajouter (positif ou négatif)
     */
    public static void adjustSelectedValue(float delta) {
        float current = getValue(selectedIndex);
        setValue(selectedIndex, Math.round((current + delta) * 100f) / 100f);
    }

    /**
     * Sélectionne une valeur (1-9)
     */
    public static void selectValue(int index) {
        if (index >= 1 && index <= 9) {
            selectedIndex = index;
        }
    }

    /**
     * Remet toutes les valeurs aux valeurs par défaut
     */
    public static void resetAll() {
        System.arraycopy(DEFAULTS, 0, values, 0, 9);
    }
}
