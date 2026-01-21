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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
