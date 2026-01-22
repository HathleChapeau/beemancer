/**
 * ============================================================
 * [BeemancerItems.java]
 * Description: Registre centralisé de tous les items du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.BeeWandItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
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

    public static final DeferredItem<BlockItem> BEE_CREATOR = ITEMS.register("bee_creator",
            () -> new BlockItem(BeemancerBlocks.BEE_CREATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MAGIC_HIVE = ITEMS.register("magic_hive",
            () -> new BlockItem(BeemancerBlocks.MAGIC_HIVE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INCUBATOR = ITEMS.register("incubator",
            () -> new BlockItem(BeemancerBlocks.INCUBATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BREEDING_CRYSTAL = ITEMS.register("breeding_crystal",
            () -> new BlockItem(BeemancerBlocks.BREEDING_CRYSTAL.get(), new Item.Properties()));

    // --- BEE ITEMS ---
    public static final DeferredItem<MagicBeeItem> MAGIC_BEE = ITEMS.register("magic_bee",
            () -> new MagicBeeItem(new Item.Properties()));

    public static final DeferredItem<BeeLarvaItem> BEE_LARVA = ITEMS.register("bee_larva",
            () -> new BeeLarvaItem(new Item.Properties()));

    // --- TOOLS ---
    public static final DeferredItem<BeeWandItem> BEE_WAND = ITEMS.register("bee_wand",
            () -> new BeeWandItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
