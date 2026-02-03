/**
 * ============================================================
 * [PollenTextures.java]
 * Description: Mapping pollen item → texture de remplissage pour le Pollen Pot
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeemancerItems      | Items pollen         | Mapping item -> texture        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PollenPotRenderer.java (récupération texture atlas sprite)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.color;

import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Associe chaque type de pollen à une texture de bloc (concrete powder).
 * Si un pollen n'a pas de texture définie, la texture par défaut est utilisée.
 */
public class PollenTextures {

    private static final Map<Item, ResourceLocation> POLLEN_TEXTURES = new HashMap<>();

    public static final ResourceLocation DEFAULT_TEXTURE =
        ResourceLocation.withDefaultNamespace("block/light_gray_concrete_powder");

    static {
        // Pollens naturels
        register(BeemancerItems.FLOWER_POLLEN.get(),
            ResourceLocation.withDefaultNamespace("block/pink_concrete_powder"));
        register(BeemancerItems.MUSHROOM_SPORE.get(),
            ResourceLocation.withDefaultNamespace("block/brown_concrete_powder"));
        register(BeemancerItems.TREE_POLLEN.get(),
            ResourceLocation.withDefaultNamespace("block/green_concrete_powder"));
        register(BeemancerItems.CRYSTAL_POLLEN.get(),
            ResourceLocation.withDefaultNamespace("block/magenta_concrete_powder"));

        // Pollens élémentaires
        register(BeemancerItems.POLLEN_OF_WIND.get(),
            ResourceLocation.withDefaultNamespace("block/light_gray_concrete_powder"));
        register(BeemancerItems.POLLEN_OF_THUNDER.get(),
            ResourceLocation.withDefaultNamespace("block/yellow_concrete_powder"));
        register(BeemancerItems.POLLEN_OF_WATER.get(),
            ResourceLocation.withDefaultNamespace("block/blue_concrete_powder"));
        register(BeemancerItems.POLLEN_OF_FIRE.get(),
            ResourceLocation.withDefaultNamespace("block/orange_concrete_powder"));
        register(BeemancerItems.POLLEN_OF_LIGHT.get(),
            ResourceLocation.withDefaultNamespace("block/white_concrete_powder"));
        register(BeemancerItems.POLLEN_OF_DARKNESS.get(),
            ResourceLocation.withDefaultNamespace("block/purple_concrete_powder"));

        // Pollen spécial
        register(BeemancerItems.VOID_POLLEN.get(),
            ResourceLocation.withDefaultNamespace("block/black_concrete_powder"));
    }

    private static void register(Item item, ResourceLocation texture) {
        POLLEN_TEXTURES.put(item, texture);
    }

    /**
     * Retourne la texture associée à un item pollen.
     * @param item L'item pollen
     * @return Le ResourceLocation de la texture, ou DEFAULT_TEXTURE si non trouvé
     */
    public static ResourceLocation getTexture(Item item) {
        return POLLEN_TEXTURES.getOrDefault(item, DEFAULT_TEXTURE);
    }
}
