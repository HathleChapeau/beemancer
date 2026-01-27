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

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    // Valeurs par défaut
    private static final float DEFAULT_1 = 0f;   // Pos X
    private static final float DEFAULT_2 = 0f;   // Pos Y
    private static final float DEFAULT_3 = 0f;   // Pos Z
    private static final float DEFAULT_4 = 0f; // Rot X
    private static final float DEFAULT_5 = 0f; // Rot Y
    private static final float DEFAULT_6 = 0.0f;
    private static final float DEFAULT_7 = 0f;   // Scale
    private static final float DEFAULT_8 = 0.0f;
    private static final float DEFAULT_9 = 0.0f;

    // Les 9 valeurs ajustables
    public static float value1 = DEFAULT_1;
    public static float value2 = DEFAULT_2;
    public static float value3 = DEFAULT_3;
    public static float value4 = DEFAULT_4;
    public static float value5 = DEFAULT_5;
    public static float value6 = DEFAULT_6;
    public static float value7 = DEFAULT_7;
    public static float value8 = DEFAULT_8;
    public static float value9 = DEFAULT_9;

    // Index de la valeur sélectionnée (1-9)
    public static int selectedIndex = 1;

    // Mode d'affichage debug (toggle avec clic droit)
    public static boolean displayDebug = false;

    public DebugWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            displayDebug = !displayDebug;
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /**
     * Reset displayDebug quand la baguette n'est plus tenue.
     * Appelé par DebugPanelRenderer.
     */
    public static void onNotHolding() {
        if (displayDebug) {
            displayDebug = false;
        }
    }

    /**
     * Récupère la valeur à l'index donné (1-9)
     */
    public static float getValue(int index) {
        return switch (index) {
            case 1 -> value1;
            case 2 -> value2;
            case 3 -> value3;
            case 4 -> value4;
            case 5 -> value5;
            case 6 -> value6;
            case 7 -> value7;
            case 8 -> value8;
            case 9 -> value9;
            default -> 0f;
        };
    }

    /**
     * Définit la valeur à l'index donné (1-9)
     */
    public static void setValue(int index, float value) {
        switch (index) {
            case 1 -> value1 = value;
            case 2 -> value2 = value;
            case 3 -> value3 = value;
            case 4 -> value4 = value;
            case 5 -> value5 = value;
            case 6 -> value6 = value;
            case 7 -> value7 = value;
            case 8 -> value8 = value;
            case 9 -> value9 = value;
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
        value1 = DEFAULT_1;
        value2 = DEFAULT_2;
        value3 = DEFAULT_3;
        value4 = DEFAULT_4;
        value5 = DEFAULT_5;
        value6 = DEFAULT_6;
        value7 = DEFAULT_7;
        value8 = DEFAULT_8;
        value9 = DEFAULT_9;
    }
}
