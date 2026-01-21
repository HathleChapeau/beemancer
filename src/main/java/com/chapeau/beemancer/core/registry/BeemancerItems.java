/**
 * ============================================================
 * [BeemancerItems.java]
 * Description: Registre centralisé de tous les items du mod
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | Beemancer           | MOD_ID               | Clé du registre     |
 * | BeemancerBlocks     | Blocs enregistrés    | Création BlockItems |
 * | BeeDebugItem        | Item abeille debug   | Enregistrement      |
 * | BeeWandItem         | Baguette contrôle    | Enregistrement      |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement)
 * - BeemancerCreativeTabs.java (ajout aux tabs)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.item.bee.BeeDebugItem;
import com.chapeau.beemancer.common.item.bee.BeeWandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BeemancerItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Beemancer.MOD_ID);

    // --- BLOCK ITEMS ---
    public static final DeferredItem<BlockItem> STORAGE_CRATE = ITEMS.register("storage_crate",
            () -> new BlockItem(BeemancerBlocks.STORAGE_CRATE.get(), new Item.Properties()));

    // --- BEE ITEMS ---
    public static final DeferredItem<BeeDebugItem> BEE_DEBUG = ITEMS.register("bee_debug",
            () -> new BeeDebugItem(new Item.Properties()));

    // --- TOOLS ---
    public static final DeferredItem<BeeWandItem> BEE_WAND = ITEMS.register("bee_wand",
            () -> new BeeWandItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
