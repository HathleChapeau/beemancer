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
import com.chapeau.beemancer.common.item.codex.CodexItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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

    // --- ALCHEMY MACHINES ---
    public static final DeferredItem<BlockItem> MANUAL_CENTRIFUGE = ITEMS.register("manual_centrifuge",
            () -> new BlockItem(BeemancerBlocks.MANUAL_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE = ITEMS.register("powered_centrifuge",
            () -> new BlockItem(BeemancerBlocks.POWERED_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_TANK = ITEMS.register("honey_tank",
            () -> new BlockItem(BeemancerBlocks.HONEY_TANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PIPE = ITEMS.register("honey_pipe",
            () -> new BlockItem(BeemancerBlocks.HONEY_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> NECTAR_FILTER = ITEMS.register("nectar_filter",
            () -> new BlockItem(BeemancerBlocks.NECTAR_FILTER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CRYSTALLIZER = ITEMS.register("crystallizer",
            () -> new BlockItem(BeemancerBlocks.CRYSTALLIZER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ALEMBIC = ITEMS.register("alembic",
            () -> new BlockItem(BeemancerBlocks.ALEMBIC.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSER = ITEMS.register("infuser",
            () -> new BlockItem(BeemancerBlocks.INFUSER.get(), new Item.Properties()));

    // --- BEE ITEMS ---
    public static final DeferredItem<MagicBeeItem> MAGIC_BEE = ITEMS.register("magic_bee",
            () -> new MagicBeeItem(new Item.Properties()));

    public static final DeferredItem<BeeLarvaItem> BEE_LARVA = ITEMS.register("bee_larva",
            () -> new BeeLarvaItem(new Item.Properties()));

    // --- TOOLS ---
    public static final DeferredItem<BeeWandItem> BEE_WAND = ITEMS.register("bee_wand",
            () -> new BeeWandItem(new Item.Properties()));

    // --- CODEX ---
    public static final DeferredItem<CodexItem> CODEX = ITEMS.register("codex",
            () -> new CodexItem(new Item.Properties()));

    // --- FLUID BUCKETS ---
    public static final DeferredItem<BucketItem> HONEY_BUCKET = ITEMS.register("honey_bucket",
            () -> new BucketItem(BeemancerFluids.HONEY_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> ROYAL_JELLY_BUCKET = ITEMS.register("royal_jelly_bucket",
            () -> new BucketItem(BeemancerFluids.ROYAL_JELLY_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> NECTAR_BUCKET = ITEMS.register("nectar_bucket",
            () -> new BucketItem(BeemancerFluids.NECTAR_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> FIRE_NECTAR_BUCKET = ITEMS.register("fire_nectar_bucket",
            () -> new BucketItem(BeemancerFluids.FIRE_NECTAR_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> FROST_NECTAR_BUCKET = ITEMS.register("frost_nectar_bucket",
            () -> new BucketItem(BeemancerFluids.FROST_NECTAR_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> STORM_NECTAR_BUCKET = ITEMS.register("storm_nectar_bucket",
            () -> new BucketItem(BeemancerFluids.STORM_NECTAR_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // --- ALCHEMY INGREDIENTS ---
    public static final DeferredItem<Item> BEESWAX = ITEMS.register("beeswax",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PROPOLIS = ITEMS.register("propolis",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROYAL_COMB = ITEMS.register("royal_comb",
            () -> new Item(new Item.Properties()));

    // --- CRYSTAL SHARDS (for Crystallizer) ---
    public static final DeferredItem<Item> RAW_CRYSTAL_SHARD = ITEMS.register("raw_crystal_shard",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENRICHED_CRYSTAL_SHARD = ITEMS.register("enriched_crystal_shard",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RADIANT_CRYSTAL_SHARD = ITEMS.register("radiant_crystal_shard",
            () -> new Item(new Item.Properties()));

    // --- HONEY CRYSTALS (for Crystallizer machine) ---
    public static final DeferredItem<Item> HONEY_CRYSTAL = ITEMS.register("honey_crystal",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENRICHED_HONEY_CRYSTAL = ITEMS.register("enriched_honey_crystal",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RADIANT_HONEY_CRYSTAL = ITEMS.register("radiant_honey_crystal",
            () -> new Item(new Item.Properties()));

    // --- POLLEN (for Infuser elemental nectars) ---
    public static final DeferredItem<Item> FIRE_POLLEN = ITEMS.register("fire_pollen",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> FROST_POLLEN = ITEMS.register("frost_pollen",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STORM_POLLEN = ITEMS.register("storm_pollen",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
