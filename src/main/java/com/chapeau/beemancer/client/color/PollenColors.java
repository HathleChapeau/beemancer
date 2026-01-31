/**
 * ============================================================
 * [PollenColors.java]
 * Description: Définition des couleurs pour chaque type de pollen
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeemancerItems      | Items pollen         | Mapping item -> couleur        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java (BlockColor registration)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.color;

import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class PollenColors {

    private static final Map<Item, Integer> POLLEN_COLORS = new HashMap<>();

    // Couleur par défaut (jaune pollen classique)
    public static final int DEFAULT_COLOR = 0xFFD700;

    // Couleur quand le pot est vide (gris transparent)
    public static final int EMPTY_COLOR = 0x808080;

    static {
        // Pollens naturels
        register(BeemancerItems.FLOWER_POLLEN.get(), 0xFF69B4);    // Rose vif
        register(BeemancerItems.MUSHROOM_SPORE.get(), 0x8B4513);   // Marron champignon
        register(BeemancerItems.TREE_POLLEN.get(), 0x228B22);      // Vert forêt
        register(BeemancerItems.CRYSTAL_POLLEN.get(), 0x00FFFF);   // Cyan cristal

        // Pollens élémentaires
        register(BeemancerItems.POLLEN_OF_WIND.get(), 0xADD8E6);   // Bleu ciel clair
        register(BeemancerItems.POLLEN_OF_THUNDER.get(), 0xFFFF00); // Jaune électrique
        register(BeemancerItems.POLLEN_OF_WATER.get(), 0x1E90FF);  // Bleu océan
        register(BeemancerItems.POLLEN_OF_FIRE.get(), 0xFF4500);   // Orange rouge feu
        register(BeemancerItems.POLLEN_OF_LIGHT.get(), 0xFFFACD);  // Jaune pâle lumineux
        register(BeemancerItems.POLLEN_OF_DARKNESS.get(), 0x4B0082); // Indigo sombre

        // Pollen spécial
        register(BeemancerItems.VOID_POLLEN.get(), 0x1A0033);      // Violet très sombre
    }

    private static void register(Item item, int color) {
        POLLEN_COLORS.put(item, color);
    }

    /**
     * Retourne la couleur associée à un item pollen.
     * @param item L'item pollen
     * @return La couleur en format 0xRRGGBB, ou DEFAULT_COLOR si non trouvé
     */
    public static int getColor(Item item) {
        return POLLEN_COLORS.getOrDefault(item, DEFAULT_COLOR);
    }

    /**
     * Vérifie si un item a une couleur définie.
     */
    public static boolean hasColor(Item item) {
        return POLLEN_COLORS.containsKey(item);
    }
}
