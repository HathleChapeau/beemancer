/**
 * ============================================================
 * [ApicaTags.java]
 * Description: Définition des tags pour items, blocs et entités
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | Apica      | MOD_ID               | Namespace des tags             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PollenPotBlockEntity.java (vérification pollens)
 *
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ApicaTags {

    public static class Items {
        /**
         * Tag pour tous les pollens (flower_pollen, tree_pollen, crystal_pollen, etc.)
         */
        public static final TagKey<Item> POLLENS = tag("pollens");

        /**
         * Tag pour tous les combs
         */
        public static final TagKey<Item> COMBS = tag("combs");

        /**
         * Tag pour toutes les essences (filtre slots du Storage Controller)
         */
        public static final TagKey<Item> ESSENCES = tag("essences");

        /**
         * Tag pour les items que les abeilles compagnon aiment (fleurs, combs, miel, etc.)
         */
        public static final TagKey<Item> BEE_FOOD = tag("bee_food");

        /**
         * Tag pour les items que les abeilles compagnon detestent (magic bee, larva)
         */
        public static final TagKey<Item> BEE_HATED_FOOD = tag("bee_hated_food");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, name));
        }
    }

    public static class Blocks {
        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, name));
        }
    }
}
