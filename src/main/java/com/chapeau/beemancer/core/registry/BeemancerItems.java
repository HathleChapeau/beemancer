/**
 * ============================================================
 * [BeemancerItems.java]
 * Description: Registre centralisé de tous les items du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.item.codex.CodexItem;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.common.item.essence.EssenceItem;
import com.chapeau.beemancer.common.item.mount.RideableBeeSpawnItem;
import com.chapeau.beemancer.common.item.tool.BuildingWandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
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

    public static final DeferredItem<BlockItem> STORAGE_CONTROLLER = ITEMS.register("storage_controller",
            () -> new BlockItem(BeemancerBlocks.STORAGE_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> STORAGE_TERMINAL = ITEMS.register("storage_terminal",
            () -> new BlockItem(BeemancerBlocks.STORAGE_TERMINAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CONTROLLED_HIVE = ITEMS.register("controlled_hive",
            () -> new BlockItem(BeemancerBlocks.CONTROLLED_HIVE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CONTROLLER_PIPE = ITEMS.register("controller_pipe",
            () -> new BlockItem(BeemancerBlocks.CONTROLLER_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BEE_CREATOR = ITEMS.register("bee_creator",
            () -> new BlockItem(BeemancerBlocks.BEE_CREATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MAGIC_HIVE = ITEMS.register("magic_hive",
            () -> new BlockItem(BeemancerBlocks.MAGIC_HIVE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INCUBATOR = ITEMS.register("incubator",
            () -> new BlockItem(BeemancerBlocks.INCUBATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ANTIBREEDING_CRYSTAL = ITEMS.register("antibreeding_crystal",
            () -> new BlockItem(BeemancerBlocks.ANTIBREEDING_CRYSTAL.get(), new Item.Properties()));

    // Legacy alias
    public static final DeferredItem<BlockItem> BREEDING_CRYSTAL = ANTIBREEDING_CRYSTAL;

    // --- ALCHEMY MACHINES ---
    public static final DeferredItem<BlockItem> MANUAL_CENTRIFUGE = ITEMS.register("manual_centrifuge",
            () -> new BlockItem(BeemancerBlocks.MANUAL_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE = ITEMS.register("powered_centrifuge",
            () -> new BlockItem(BeemancerBlocks.POWERED_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE_TIER2 = ITEMS.register("powered_centrifuge_tier2",
            () -> new BlockItem(BeemancerBlocks.POWERED_CENTRIFUGE_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE_TIER3 = ITEMS.register("powered_centrifuge_tier3",
            () -> new BlockItem(BeemancerBlocks.POWERED_CENTRIFUGE_TIER3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_TANK = ITEMS.register("honey_tank",
            () -> new BlockItem(BeemancerBlocks.HONEY_TANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CREATIVE_TANK = ITEMS.register("creative_tank",
            () -> new BlockItem(BeemancerBlocks.CREATIVE_TANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PIPE = ITEMS.register("honey_pipe",
            () -> new BlockItem(BeemancerBlocks.HONEY_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PIPE_TIER2 = ITEMS.register("honey_pipe_tier2",
            () -> new BlockItem(BeemancerBlocks.HONEY_PIPE_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PIPE_TIER3 = ITEMS.register("honey_pipe_tier3",
            () -> new BlockItem(BeemancerBlocks.HONEY_PIPE_TIER3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PIPE_TIER4 = ITEMS.register("honey_pipe_tier4",
            () -> new BlockItem(BeemancerBlocks.HONEY_PIPE_TIER4.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE = ITEMS.register("item_pipe",
            () -> new BlockItem(BeemancerBlocks.ITEM_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_TIER2 = ITEMS.register("item_pipe_tier2",
            () -> new BlockItem(BeemancerBlocks.ITEM_PIPE_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_TIER3 = ITEMS.register("item_pipe_tier3",
            () -> new BlockItem(BeemancerBlocks.ITEM_PIPE_TIER3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_TIER4 = ITEMS.register("item_pipe_tier4",
            () -> new BlockItem(BeemancerBlocks.ITEM_PIPE_TIER4.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CRYSTALLIZER = ITEMS.register("crystallizer",
            () -> new BlockItem(BeemancerBlocks.CRYSTALLIZER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ALEMBIC = ITEMS.register("alembic",
            () -> new BlockItem(BeemancerBlocks.ALEMBIC.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSER = ITEMS.register("infuser",
            () -> new BlockItem(BeemancerBlocks.INFUSER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSER_TIER2 = ITEMS.register("infuser_tier2",
            () -> new BlockItem(BeemancerBlocks.INFUSER_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSER_TIER3 = ITEMS.register("infuser_tier3",
            () -> new BlockItem(BeemancerBlocks.INFUSER_TIER3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MULTIBLOCK_TANK = ITEMS.register("multiblock_tank",
            () -> new BlockItem(BeemancerBlocks.MULTIBLOCK_TANK.get(), new Item.Properties()));

    // --- HONEY ALTAR ---
    public static final DeferredItem<BlockItem> HONEYED_STONE = ITEMS.register("honeyed_stone",
            () -> new BlockItem(BeemancerBlocks.HONEYED_STONE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEYED_STONE_STAIR = ITEMS.register("honeyed_stone_stair",
            () -> new BlockItem(BeemancerBlocks.HONEYED_STONE_STAIR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PEDESTAL = ITEMS.register("honey_pedestal",
            () -> new BlockItem(BeemancerBlocks.HONEY_PEDESTAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_CRYSTAL_CONDUIT = ITEMS.register("honey_crystal_conduit",
            () -> new BlockItem(BeemancerBlocks.HONEY_CRYSTAL_CONDUIT.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_CRYSTAL = ITEMS.register("honey_crystal",
            () -> new BlockItem(BeemancerBlocks.HONEY_CRYSTAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEYED_SLAB = ITEMS.register("honeyed_slab",
            () -> new BlockItem(BeemancerBlocks.HONEYED_SLAB.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_RESERVOIR = ITEMS.register("honey_reservoir",
            () -> new BlockItem(BeemancerBlocks.HONEY_RESERVOIR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ALTAR_HEART = ITEMS.register("altar_heart",
            () -> new BlockItem(BeemancerBlocks.ALTAR_HEART.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HIVE_MULTIBLOCK = ITEMS.register("hive_multiblock",
            () -> new BlockItem(BeemancerBlocks.HIVE_MULTIBLOCK.get(), new Item.Properties()));

    // --- POLLEN POT ---
    public static final DeferredItem<BlockItem> POLLEN_POT = ITEMS.register("pollen_pot",
            () -> new BlockItem(BeemancerBlocks.POLLEN_POT.get(), new Item.Properties()));

    // --- ESSENCE EXTRACTOR ---
    public static final DeferredItem<BlockItem> EXTRACTOR_HEART = ITEMS.register("extractor_heart",
            () -> new BlockItem(BeemancerBlocks.EXTRACTOR_HEART.get(), new Item.Properties()));

    // --- BEE STATUE ---
    public static final DeferredItem<BlockItem> BEE_STATUE = ITEMS.register("bee_statue",
            () -> new BlockItem(BeemancerBlocks.BEE_STATUE.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED WOOD ---
    public static final DeferredItem<BlockItem> HONEYED_PLANKS = ITEMS.register("honeyed_planks",
            () -> new BlockItem(BeemancerBlocks.HONEYED_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_WOOD_STAIR = ITEMS.register("honeyed_wood_stair",
            () -> new BlockItem(BeemancerBlocks.HONEYED_WOOD_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_WOOD_SLAB = ITEMS.register("honeyed_wood_slab",
            () -> new BlockItem(BeemancerBlocks.HONEYED_WOOD_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_FENCE = ITEMS.register("honeyed_fence",
            () -> new BlockItem(BeemancerBlocks.HONEYED_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_FENCE_GATE = ITEMS.register("honeyed_fence_gate",
            () -> new BlockItem(BeemancerBlocks.HONEYED_FENCE_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_TRAPDOOR = ITEMS.register("honeyed_trapdoor",
            () -> new BlockItem(BeemancerBlocks.HONEYED_TRAPDOOR.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> HONEYED_DOOR = ITEMS.register("honeyed_door",
            () -> new DoubleHighBlockItem(BeemancerBlocks.HONEYED_DOOR.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED STONE ---
    public static final DeferredItem<BlockItem> HONEYED_STONE_WALL = ITEMS.register("honeyed_stone_wall",
            () -> new BlockItem(BeemancerBlocks.HONEYED_STONE_WALL.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED GLASS ---
    public static final DeferredItem<BlockItem> HONEYED_GLASS = ITEMS.register("honeyed_glass",
            () -> new BlockItem(BeemancerBlocks.HONEYED_GLASS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_GLASS_PANE = ITEMS.register("honeyed_glass_pane",
            () -> new BlockItem(BeemancerBlocks.HONEYED_GLASS_PANE.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: ROYAL WOOD ---
    public static final DeferredItem<BlockItem> ROYAL_PLANKS = ITEMS.register("royal_planks",
            () -> new BlockItem(BeemancerBlocks.ROYAL_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_WOOD_STAIR = ITEMS.register("royal_wood_stair",
            () -> new BlockItem(BeemancerBlocks.ROYAL_WOOD_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_WOOD_SLAB = ITEMS.register("royal_wood_slab",
            () -> new BlockItem(BeemancerBlocks.ROYAL_WOOD_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_FENCE = ITEMS.register("royal_fence",
            () -> new BlockItem(BeemancerBlocks.ROYAL_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_FENCE_GATE = ITEMS.register("royal_fence_gate",
            () -> new BlockItem(BeemancerBlocks.ROYAL_FENCE_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_TRAPDOOR = ITEMS.register("royal_trapdoor",
            () -> new BlockItem(BeemancerBlocks.ROYAL_TRAPDOOR.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> ROYAL_DOOR = ITEMS.register("royal_door",
            () -> new DoubleHighBlockItem(BeemancerBlocks.ROYAL_DOOR.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: ROYAL STONE ---
    public static final DeferredItem<BlockItem> ROYAL_STONE = ITEMS.register("royal_stone",
            () -> new BlockItem(BeemancerBlocks.ROYAL_STONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_STONE_STAIR = ITEMS.register("royal_stone_stair",
            () -> new BlockItem(BeemancerBlocks.ROYAL_STONE_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_STONE_SLAB = ITEMS.register("royal_stone_slab",
            () -> new BlockItem(BeemancerBlocks.ROYAL_STONE_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_STONE_WALL = ITEMS.register("royal_stone_wall",
            () -> new BlockItem(BeemancerBlocks.ROYAL_STONE_WALL.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: NECTAR WOOD ---
    public static final DeferredItem<BlockItem> NECTAR_PLANKS = ITEMS.register("nectar_planks",
            () -> new BlockItem(BeemancerBlocks.NECTAR_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_WOOD_STAIR = ITEMS.register("nectar_wood_stair",
            () -> new BlockItem(BeemancerBlocks.NECTAR_WOOD_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_WOOD_SLAB = ITEMS.register("nectar_wood_slab",
            () -> new BlockItem(BeemancerBlocks.NECTAR_WOOD_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_FENCE = ITEMS.register("nectar_fence",
            () -> new BlockItem(BeemancerBlocks.NECTAR_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_FENCE_GATE = ITEMS.register("nectar_fence_gate",
            () -> new BlockItem(BeemancerBlocks.NECTAR_FENCE_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_TRAPDOOR = ITEMS.register("nectar_trapdoor",
            () -> new BlockItem(BeemancerBlocks.NECTAR_TRAPDOOR.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> NECTAR_DOOR = ITEMS.register("nectar_door",
            () -> new DoubleHighBlockItem(BeemancerBlocks.NECTAR_DOOR.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: NECTAR STONE ---
    public static final DeferredItem<BlockItem> NECTAR_STONE = ITEMS.register("nectar_stone",
            () -> new BlockItem(BeemancerBlocks.NECTAR_STONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_STONE_STAIR = ITEMS.register("nectar_stone_stair",
            () -> new BlockItem(BeemancerBlocks.NECTAR_STONE_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_STONE_SLAB = ITEMS.register("nectar_stone_slab",
            () -> new BlockItem(BeemancerBlocks.NECTAR_STONE_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_STONE_WALL = ITEMS.register("nectar_stone_wall",
            () -> new BlockItem(BeemancerBlocks.NECTAR_STONE_WALL.get(), new Item.Properties()));

    // --- CRAFTING MATERIALS ---
    public static final DeferredItem<Item> HONEYED_IRON = ITEMS.register("honeyed_iron",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ROYAL_CRYSTAL = ITEMS.register("royal_crystal",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ROYAL_GOLD = ITEMS.register("royal_gold",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NECTAR_DIAMOND = ITEMS.register("nectar_diamond",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NECTAR_CRYSTAL = ITEMS.register("nectar_crystal",
            () -> new Item(new Item.Properties()));

    // --- ESSENCES ---
    // Drop Essences (améliore le drop level des abeilles)
    public static final DeferredItem<EssenceItem> LESSER_DROP_ESSENCE = ITEMS.register("lesser_drop_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.DROP, EssenceItem.EssenceLevel.LESSER));
    public static final DeferredItem<EssenceItem> DROP_ESSENCE = ITEMS.register("drop_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.DROP, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> GREATER_DROP_ESSENCE = ITEMS.register("greater_drop_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.DROP, EssenceItem.EssenceLevel.GREATER));
    public static final DeferredItem<EssenceItem> PERFECT_DROP_ESSENCE = ITEMS.register("perfect_drop_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.DROP, EssenceItem.EssenceLevel.PERFECT));

    // Speed Essences (améliore la vitesse de vol des abeilles)
    public static final DeferredItem<EssenceItem> LESSER_SPEED_ESSENCE = ITEMS.register("lesser_speed_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.SPEED, EssenceItem.EssenceLevel.LESSER));
    public static final DeferredItem<EssenceItem> SPEED_ESSENCE = ITEMS.register("speed_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.SPEED, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> GREATER_SPEED_ESSENCE = ITEMS.register("greater_speed_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.SPEED, EssenceItem.EssenceLevel.GREATER));
    public static final DeferredItem<EssenceItem> PERFECT_SPEED_ESSENCE = ITEMS.register("perfect_speed_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.SPEED, EssenceItem.EssenceLevel.PERFECT));

    // Foraging Essences (améliore la durée de butinage des abeilles)
    public static final DeferredItem<EssenceItem> LESSER_FORAGING_ESSENCE = ITEMS.register("lesser_foraging_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.FORAGING, EssenceItem.EssenceLevel.LESSER));
    public static final DeferredItem<EssenceItem> FORAGING_ESSENCE = ITEMS.register("foraging_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.FORAGING, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> GREATER_FORAGING_ESSENCE = ITEMS.register("greater_foraging_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.FORAGING, EssenceItem.EssenceLevel.GREATER));
    public static final DeferredItem<EssenceItem> PERFECT_FORAGING_ESSENCE = ITEMS.register("perfect_foraging_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.FORAGING, EssenceItem.EssenceLevel.PERFECT));

    // Tolerance Essences (améliore la tolérance des abeilles)
    public static final DeferredItem<EssenceItem> LESSER_TOLERANCE_ESSENCE = ITEMS.register("lesser_tolerance_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.TOLERANCE, EssenceItem.EssenceLevel.LESSER));
    public static final DeferredItem<EssenceItem> TOLERANCE_ESSENCE = ITEMS.register("tolerance_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.TOLERANCE, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> GREATER_TOLERANCE_ESSENCE = ITEMS.register("greater_tolerance_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.TOLERANCE, EssenceItem.EssenceLevel.GREATER));
    public static final DeferredItem<EssenceItem> PERFECT_TOLERANCE_ESSENCE = ITEMS.register("perfect_tolerance_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.TOLERANCE, EssenceItem.EssenceLevel.PERFECT));

    // Day/Night Essences (améliore le cycle jour/nuit des abeilles)
    public static final DeferredItem<EssenceItem> DIURNAL_ESSENCE = ITEMS.register("diurnal_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.DIURNAL, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> NOCTURNAL_ESSENCE = ITEMS.register("nocturnal_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.NOCTURNAL, EssenceItem.EssenceLevel.NORMAL));
    public static final DeferredItem<EssenceItem> INSOMNIA_ESSENCE = ITEMS.register("insomnia_essence",
            () -> new EssenceItem(new Item.Properties(), EssenceItem.EssenceType.INSOMNIA, EssenceItem.EssenceLevel.PERFECT));

    // --- BEE ITEMS ---
    public static final DeferredItem<MagicBeeItem> MAGIC_BEE = ITEMS.register("magic_bee",
            () -> new MagicBeeItem(new Item.Properties()));

    public static final DeferredItem<BeeLarvaItem> BEE_LARVA = ITEMS.register("bee_larva",
            () -> new BeeLarvaItem(new Item.Properties()));

    // --- TOOLS ---
    public static final DeferredItem<BuildingWandItem> BUILDING_WAND = ITEMS.register("building_wand",
            () -> new BuildingWandItem(new Item.Properties()));

    public static final DeferredItem<DebugWandItem> DEBUG_WAND = ITEMS.register("debug_wand",
            () -> new DebugWandItem(new Item.Properties()));

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

    // --- ALCHEMY INGREDIENTS ---
    public static final DeferredItem<Item> BEESWAX = ITEMS.register("beeswax",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PROPOLIS = ITEMS.register("propolis",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> POLLEN = ITEMS.register("pollen",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> HONEYED_WOOD = ITEMS.register("honeyed_wood",
            () -> new Item(new Item.Properties()));

    // --- COMBS (legacy) ---
    public static final DeferredItem<Item> COMMON_COMB = ITEMS.register("common_comb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> NOBLE_COMB = ITEMS.register("noble_comb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DILIGENT_COMB = ITEMS.register("diligent_comb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROYAL_COMB = ITEMS.register("royal_comb",
            () -> new Item(new Item.Properties()));

    // --- NEW COMBS (from bee species) ---
    public static final DeferredItem<Item> MEADOW_COMB = ITEMS.register("meadow_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FOREST_COMB = ITEMS.register("forest_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RIVER_COMB = ITEMS.register("river_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHER_COMB = ITEMS.register("nether_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> END_COMB = ITEMS.register("end_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MONSTY_COMB = ITEMS.register("monsty_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOCILE_COMB = ITEMS.register("docile_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPARK_COMB = ITEMS.register("spark_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CULTURAL_COMB = ITEMS.register("cultural_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARBON_COMB = ITEMS.register("carbon_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CUPRIC_COMB = ITEMS.register("cupric_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FESTERING_COMB = ITEMS.register("festering_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SKELETAL_COMB = ITEMS.register("skeletal_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARACHNID_COMB = ITEMS.register("arachnid_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TANNER_COMB = ITEMS.register("tanner_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUERCUS_COMB = ITEMS.register("quercus_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BOREAL_COMB = ITEMS.register("boreal_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TROPICAL_COMB = ITEMS.register("tropical_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PAPERBARK_COMB = ITEMS.register("paperbark_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REED_COMB = ITEMS.register("reed_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FLOWER_COMB = ITEMS.register("flower_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MUSHROOM_COMB = ITEMS.register("mushroom_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TREE_COMB = ITEMS.register("tree_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRYSTAL_COMB = ITEMS.register("crystal_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARGIL_COMB = ITEMS.register("argil_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> UMBRA_COMB = ITEMS.register("umbra_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SAVANNA_COMB = ITEMS.register("savanna_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FROST_COMB = ITEMS.register("frost_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VOLATILE_COMB = ITEMS.register("volatile_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SWIFT_COMB = ITEMS.register("swift_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FERROUS_COMB = ITEMS.register("ferrous_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FLUX_COMB = ITEMS.register("flux_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAZULI_COMB = ITEMS.register("lazuli_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUARTZOSE_COMB = ITEMS.register("quartzose_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LUMINOUS_COMB = ITEMS.register("luminous_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VISCOUS_COMB = ITEMS.register("viscous_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INKY_COMB = ITEMS.register("inky_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GEODE_COMB = ITEMS.register("geode_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MARSH_COMB = ITEMS.register("marsh_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAJESTIC_COMB = ITEMS.register("majestic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEADY_COMB = ITEMS.register("steady_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> AURIC_COMB = ITEMS.register("auric_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGMATIC_COMB = ITEMS.register("magmatic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRIMSON_COMB = ITEMS.register("crimson_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TREASURE_COMB = ITEMS.register("treasure_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SIPHONING_COMB = ITEMS.register("siphoning_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZEPHYR_COMB = ITEMS.register("zephyr_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PRISMATIC_COMB = ITEMS.register("prismatic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRYSTALLINE_COMB = ITEMS.register("crystalline_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLAZING_COMB = ITEMS.register("blazing_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIAMANTINE_COMB = ITEMS.register("diamantine_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VENERABLE_COMB = ITEMS.register("venerable_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VOLCANIC_COMB = ITEMS.register("volcanic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TRAVELER_COMB = ITEMS.register("traveler_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SORROW_COMB = ITEMS.register("sorrow_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIGHT_COMB = ITEMS.register("light_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DARK_COMB = ITEMS.register("dark_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IMPERIAL_COMB = ITEMS.register("imperial_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEMONIC_COMB = ITEMS.register("demonic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PALADIN_COMB = ITEMS.register("paladin_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ANCIENT_COMB = ITEMS.register("ancient_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VOID_COMB = ITEMS.register("void_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRACONIC_COMB = ITEMS.register("draconic_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STELLAR_COMB = ITEMS.register("stellar_comb",
            () -> new Item(new Item.Properties()));

    // --- POLLENS ---
    public static final DeferredItem<Item> FLOWER_POLLEN = ITEMS.register("flower_pollen",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MUSHROOM_SPORE = ITEMS.register("mushroom_spore",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TREE_POLLEN = ITEMS.register("tree_pollen",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRYSTAL_POLLEN = ITEMS.register("crystal_pollen",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_WIND = ITEMS.register("pollen_of_wind",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_THUNDER = ITEMS.register("pollen_of_thunder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_WATER = ITEMS.register("pollen_of_water",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_FIRE = ITEMS.register("pollen_of_fire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_LIGHT = ITEMS.register("pollen_of_light",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLLEN_OF_DARKNESS = ITEMS.register("pollen_of_darkness",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VOID_POLLEN = ITEMS.register("void_pollen",
            () -> new Item(new Item.Properties()));

    // --- FRAGMENTS (4 fragments = 1 resource) ---
    public static final DeferredItem<Item> COAL_FRAGMENT = ITEMS.register("coal_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEATHER_FRAGMENT = ITEMS.register("leather_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OAK_LOG_FRAGMENT = ITEMS.register("oak_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPRUCE_LOG_FRAGMENT = ITEMS.register("spruce_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> JUNGLE_LOG_FRAGMENT = ITEMS.register("jungle_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BIRCH_LOG_FRAGMENT = ITEMS.register("birch_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DARK_OAK_LOG_FRAGMENT = ITEMS.register("dark_oak_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ACACIA_LOG_FRAGMENT = ITEMS.register("acacia_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANGROVE_LOG_FRAGMENT = ITEMS.register("mangrove_log_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ICE_FRAGMENT = ITEMS.register("ice_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GUNPOWDER_FRAGMENT = ITEMS.register("gunpowder_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLAZE_ROD_FRAGMENT = ITEMS.register("blaze_rod_fragment",
            () -> new Item(new Item.Properties()));

    // --- SHARDS (6 shards = 1 fragment) ---
    public static final DeferredItem<Item> RAW_COPPER_SHARD = ITEMS.register("raw_copper_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_IRON_SHARD = ITEMS.register("raw_iron_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_GOLD_SHARD = ITEMS.register("raw_gold_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPIS_LAZULI_SHARD = ITEMS.register("lapis_lazuli_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUARTZ_SHARD = ITEMS.register("quartz_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SLIME_BALL_SHARD = ITEMS.register("slime_ball_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGMA_CREAM_SHARD = ITEMS.register("magma_cream_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GHAST_TEAR_SHARD = ITEMS.register("ghast_tear_shard",
            () -> new Item(new Item.Properties()));

    // --- SHARD FRAGMENTS (6 shards = 1 fragment, 4 fragments = 1 resource) ---
    public static final DeferredItem<Item> RAW_COPPER_FRAGMENT = ITEMS.register("raw_copper_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_IRON_FRAGMENT = ITEMS.register("raw_iron_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_GOLD_FRAGMENT = ITEMS.register("raw_gold_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LAPIS_LAZULI_FRAGMENT = ITEMS.register("lapis_lazuli_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUARTZ_FRAGMENT = ITEMS.register("quartz_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SLIME_BALL_FRAGMENT = ITEMS.register("slime_ball_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGMA_CREAM_FRAGMENT = ITEMS.register("magma_cream_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GHAST_TEAR_FRAGMENT = ITEMS.register("ghast_tear_fragment",
            () -> new Item(new Item.Properties()));

    // --- DUSTS (9 dusts = 1 shard) ---
    public static final DeferredItem<Item> DIAMOND_DUST = ITEMS.register("diamond_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMERALD_DUST = ITEMS.register("emerald_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_DUST = ITEMS.register("obsidian_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_PEARL_DUST = ITEMS.register("ender_pearl_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHERITE_SCRAP_DUST = ITEMS.register("netherite_scrap_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGON_BREATH_DUST = ITEMS.register("dragon_breath_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHER_STAR_DUST = ITEMS.register("nether_star_dust",
            () -> new Item(new Item.Properties()));

    // --- DUST SHARDS (9 dusts = 1 shard, 6 shards = 1 fragment) ---
    public static final DeferredItem<Item> DIAMOND_SHARD = ITEMS.register("diamond_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMERALD_SHARD = ITEMS.register("emerald_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_SHARD = ITEMS.register("obsidian_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_PEARL_SHARD = ITEMS.register("ender_pearl_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHERITE_SCRAP_SHARD = ITEMS.register("netherite_scrap_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGON_BREATH_SHARD = ITEMS.register("dragon_breath_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHER_STAR_SHARD = ITEMS.register("nether_star_shard",
            () -> new Item(new Item.Properties()));

    // --- DUST FRAGMENTS (6 shards = 1 fragment, 4 fragments = 1 resource) ---
    public static final DeferredItem<Item> DIAMOND_FRAGMENT = ITEMS.register("diamond_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EMERALD_FRAGMENT = ITEMS.register("emerald_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_FRAGMENT = ITEMS.register("obsidian_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_PEARL_FRAGMENT = ITEMS.register("ender_pearl_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHERITE_SCRAP_FRAGMENT = ITEMS.register("netherite_scrap_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGON_BREATH_FRAGMENT = ITEMS.register("dragon_breath_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHER_STAR_FRAGMENT = ITEMS.register("nether_star_fragment",
            () -> new Item(new Item.Properties()));

    // --- CRYSTAL SHARDS ---
    public static final DeferredItem<Item> RAW_CRYSTAL_SHARD = ITEMS.register("raw_crystal_shard",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENRICHED_CRYSTAL_SHARD = ITEMS.register("enriched_crystal_shard",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RADIANT_CRYSTAL_SHARD = ITEMS.register("radiant_crystal_shard",
            () -> new Item(new Item.Properties()));

    // --- HONEY CRYSTALS (higher tiers, item only) ---
    public static final DeferredItem<Item> ENRICHED_HONEY_CRYSTAL = ITEMS.register("enriched_honey_crystal",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RADIANT_HONEY_CRYSTAL = ITEMS.register("radiant_honey_crystal",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROYAL_HONEY_CRYSTAL = ITEMS.register("royal_honey_crystal",
            () -> new Item(new Item.Properties()));

    // --- MOUNT ITEMS ---
    public static final DeferredItem<RideableBeeSpawnItem> RIDEABLE_BEE_SPAWN = ITEMS.register("rideable_bee_spawn",
            () -> new RideableBeeSpawnItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
