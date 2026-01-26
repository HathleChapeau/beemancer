/**
 * ============================================================
 * [BeemancerTags.java]
 * Description: Définition des tags pour items, blocs et entités
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | Beemancer      | MOD_ID               | Namespace des tags             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PollenPotBlockEntity.java (vérification pollens)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class BeemancerTags {

    public static class Items {
        /**
         * Tag pour tous les pollens (flower_pollen, tree_pollen, crystal_pollen, etc.)
         */
        public static final TagKey<Item> POLLENS = tag("pollens");

        /**
         * Tag pour tous les combs
         */
        public static final TagKey<Item> COMBS = tag("combs");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, name));
        }
    }

    public static class Blocks {
        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, name));
        }
    }
}
