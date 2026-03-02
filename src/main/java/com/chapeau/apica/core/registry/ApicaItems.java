/**
 * ============================================================
 * [ApicaItems.java]
 * Description: Registre centralisé de tous les items du mod
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.bee.BeeLarvaItem;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.common.item.codex.CodexItem;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.item.essence.EssenceItem;
import com.chapeau.apica.common.item.essence.SpeciesEssenceItem;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.item.mount.CreativeFocusItem;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.chapeau.apica.common.item.mount.HoverbikeSpawnItem;
import com.chapeau.apica.common.item.BarrelUpgradeItem;
import com.chapeau.apica.common.item.VoidUpgradeItem;
import com.chapeau.apica.common.item.ItemFilterItem;
import com.chapeau.apica.common.item.BeeElytraItem;
import com.chapeau.apica.common.item.NectarBreadItem;
import com.chapeau.apica.common.item.tool.BuildingWandItem;
import com.chapeau.apica.common.item.tool.LeafBlowerItem;
import com.chapeau.apica.common.item.tool.ChopperCubeItem;
import com.chapeau.apica.common.item.tool.MiningLaserItem;
import net.minecraft.world.food.FoodProperties;
import com.chapeau.apica.common.item.tool.ScoopItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ApicaItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Apica.MOD_ID);

    // --- BLOCK ITEMS ---
    public static final DeferredItem<BlockItem> STORAGE_CONTROLLER = ITEMS.register("storage_controller",
            () -> new BlockItem(ApicaBlocks.STORAGE_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> STORAGE_RELAY = ITEMS.register("storage_relay",
            () -> new BlockItem(ApicaBlocks.STORAGE_RELAY.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> STORAGE_TERMINAL = ITEMS.register("storage_terminal",
            () -> new BlockItem(ApicaBlocks.STORAGE_TERMINAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> IMPORT_INTERFACE = ITEMS.register("import_interface",
            () -> new BlockItem(ApicaBlocks.IMPORT_INTERFACE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> EXPORT_INTERFACE = ITEMS.register("export_interface",
            () -> new BlockItem(ApicaBlocks.EXPORT_INTERFACE.get(), new Item.Properties()));

    // --- Storage Barrels ---
    public static final DeferredItem<BlockItem> STORAGE_BARREL_MK1 = ITEMS.register("storage_barrel_mk1",
            () -> new BlockItem(ApicaBlocks.STORAGE_BARREL_MK1.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STORAGE_BARREL_MK2 = ITEMS.register("storage_barrel_mk2",
            () -> new BlockItem(ApicaBlocks.STORAGE_BARREL_MK2.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STORAGE_BARREL_MK3 = ITEMS.register("storage_barrel_mk3",
            () -> new BlockItem(ApicaBlocks.STORAGE_BARREL_MK3.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STORAGE_BARREL_MK4 = ITEMS.register("storage_barrel_mk4",
            () -> new BlockItem(ApicaBlocks.STORAGE_BARREL_MK4.get(), new Item.Properties()));

    // --- Trash Cans ---
    public static final DeferredItem<BlockItem> TRASH_CAN = ITEMS.register("trash_can",
            () -> new BlockItem(ApicaBlocks.TRASH_CAN.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LIQUID_TRASH_CAN = ITEMS.register("liquid_trash_can",
            () -> new BlockItem(ApicaBlocks.LIQUID_TRASH_CAN.get(), new Item.Properties()));

    // --- Barrel Upgrades ---
    public static final DeferredItem<BarrelUpgradeItem> BARREL_MK2_UPGRADE = ITEMS.register("barrel_mk2_upgrade",
            () -> new BarrelUpgradeItem(new Item.Properties().stacksTo(16), 2));
    public static final DeferredItem<BarrelUpgradeItem> BARREL_MK3_UPGRADE = ITEMS.register("barrel_mk3_upgrade",
            () -> new BarrelUpgradeItem(new Item.Properties().stacksTo(16), 3));
    public static final DeferredItem<BarrelUpgradeItem> BARREL_MK4_UPGRADE = ITEMS.register("barrel_mk4_upgrade",
            () -> new BarrelUpgradeItem(new Item.Properties().stacksTo(16), 4));

    // --- Void Upgrade ---
    public static final DeferredItem<VoidUpgradeItem> VOID_UPGRADE = ITEMS.register("void_upgrade",
            () -> new VoidUpgradeItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> CONTROLLED_HIVE = ITEMS.register("controlled_hive",
            () -> new BlockItem(ApicaBlocks.CONTROLLED_HIVE.get(), new Item.Properties()));

    // --- Storage Hives ---
    public static final DeferredItem<BlockItem> STORAGE_HIVE = ITEMS.register("storage_hive",
            () -> new BlockItem(ApicaBlocks.STORAGE_HIVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STORAGE_HIVE_TIER2 = ITEMS.register("storage_hive_tier2",
            () -> new BlockItem(ApicaBlocks.STORAGE_HIVE_TIER2.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STORAGE_HIVE_TIER3 = ITEMS.register("storage_hive_tier3",
            () -> new BlockItem(ApicaBlocks.STORAGE_HIVE_TIER3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MAGIC_HIVE = ITEMS.register("magic_hive",
            () -> new BlockItem(ApicaBlocks.MAGIC_HIVE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INCUBATOR = ITEMS.register("incubator",
            () -> new BlockItem(ApicaBlocks.INCUBATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ANTIBREEDING_CRYSTAL = ITEMS.register("antibreeding_crystal",
            () -> new BlockItem(ApicaBlocks.ANTIBREEDING_CRYSTAL.get(), new Item.Properties()));

    // Legacy alias
    public static final DeferredItem<BlockItem> BREEDING_CRYSTAL = ANTIBREEDING_CRYSTAL;

    // --- CREATIVE BREEDING CRYSTAL (debug) ---
    public static final DeferredItem<BlockItem> CREATIVE_BREEDING_CRYSTAL = ITEMS.register("creative_breeding_crystal",
            () -> new BlockItem(ApicaBlocks.CREATIVE_BREEDING_CRYSTAL.get(), new Item.Properties()));

    // --- CREATIVE TOLERANCE CRYSTAL (debug) ---
    public static final DeferredItem<BlockItem> CREATIVE_TOLERANCE_CRYSTAL = ITEMS.register("creative_tolerance_crystal",
            () -> new BlockItem(ApicaBlocks.CREATIVE_TOLERANCE_CRYSTAL.get(), new Item.Properties()));

    // --- ALCHEMY MACHINES ---
    public static final DeferredItem<BlockItem> MANUAL_CENTRIFUGE = ITEMS.register("manual_centrifuge",
            () -> new BlockItem(ApicaBlocks.MANUAL_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CRANK = ITEMS.register("crank",
            () -> new BlockItem(ApicaBlocks.CRANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE = ITEMS.register("powered_centrifuge",
            () -> new BlockItem(ApicaBlocks.POWERED_CENTRIFUGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POWERED_CENTRIFUGE_TIER2 = ITEMS.register("powered_centrifuge_tier2",
            () -> new BlockItem(ApicaBlocks.POWERED_CENTRIFUGE_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_TANK = ITEMS.register("honey_tank",
            () -> new BlockItem(ApicaBlocks.HONEY_TANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_LAMP = ITEMS.register("honey_lamp",
            () -> new BlockItem(ApicaBlocks.HONEY_LAMP.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CREATIVE_TANK = ITEMS.register("creative_tank",
            () -> new BlockItem(ApicaBlocks.CREATIVE_TANK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LIQUID_PIPE = ITEMS.register("liquid_pipe",
            () -> new BlockItem(ApicaBlocks.LIQUID_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LIQUID_PIPE_MK2 = ITEMS.register("liquid_pipe_mk2",
            () -> new BlockItem(ApicaBlocks.LIQUID_PIPE_MK2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LIQUID_PIPE_MK3 = ITEMS.register("liquid_pipe_mk3",
            () -> new BlockItem(ApicaBlocks.LIQUID_PIPE_MK3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LIQUID_PIPE_MK4 = ITEMS.register("liquid_pipe_mk4",
            () -> new BlockItem(ApicaBlocks.LIQUID_PIPE_MK4.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE = ITEMS.register("item_pipe",
            () -> new BlockItem(ApicaBlocks.ITEM_PIPE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_MK2 = ITEMS.register("item_pipe_mk2",
            () -> new BlockItem(ApicaBlocks.ITEM_PIPE_MK2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_MK3 = ITEMS.register("item_pipe_mk3",
            () -> new BlockItem(ApicaBlocks.ITEM_PIPE_MK3.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ITEM_PIPE_MK4 = ITEMS.register("item_pipe_mk4",
            () -> new BlockItem(ApicaBlocks.ITEM_PIPE_MK4.get(), new Item.Properties()));

    // --- ITEM FILTER ---
    public static final DeferredItem<ItemFilterItem> ITEM_FILTER = ITEMS.register("item_filter",
            () -> new ItemFilterItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> CRYSTALLIZER = ITEMS.register("crystallizer",
            () -> new BlockItem(ApicaBlocks.CRYSTALLIZER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> INFUSER = ITEMS.register("infuser",
            () -> new BlockItem(ApicaBlocks.INFUSER.get(), new Item.Properties()));

    // public static final DeferredItem<BlockItem> INFUSER_TIER2 = ITEMS.register("infuser_tier2",
    //         () -> new BlockItem(ApicaBlocks.INFUSER_TIER2.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MULTIBLOCK_TANK = ITEMS.register("multiblock_tank",
            () -> new BlockItem(ApicaBlocks.MULTIBLOCK_TANK.get(), new Item.Properties()));

    // --- HONEY ALTAR ---
    public static final DeferredItem<BlockItem> HONEYED_STONE = ITEMS.register("honeyed_stone",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEYED_STONE_STAIR = ITEMS.register("honeyed_stone_stair",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_STAIR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_PEDESTAL = ITEMS.register("honey_pedestal",
            () -> new BlockItem(ApicaBlocks.HONEY_PEDESTAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_CRYSTAL_CONDUIT = ITEMS.register("honey_crystal_conduit",
            () -> new BlockItem(ApicaBlocks.HONEY_CRYSTAL_CONDUIT.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_CRYSTAL = ITEMS.register("honey_crystal",
            () -> new BlockItem(ApicaBlocks.HONEY_CRYSTAL.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEYED_SLAB = ITEMS.register("honeyed_slab",
            () -> new BlockItem(ApicaBlocks.HONEYED_SLAB.get(), new Item.Properties()));

    // --- HONEYED STONE BRICK ---
    public static final DeferredItem<BlockItem> HONEYED_STONE_BRICK = ITEMS.register("honeyed_stone_brick",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_BRICK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_STONE_BRICK_STAIR = ITEMS.register("honeyed_stone_brick_stair",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_BRICK_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_STONE_BRICK_SLAB = ITEMS.register("honeyed_stone_brick_slab",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_BRICK_SLAB.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HONEY_RESERVOIR = ITEMS.register("honey_reservoir",
            () -> new BlockItem(ApicaBlocks.HONEY_RESERVOIR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ALTAR_HEART = ITEMS.register("altar_heart",
            () -> new BlockItem(ApicaBlocks.ALTAR_HEART.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> HIVE_MULTIBLOCK = ITEMS.register("hive_multiblock",
            () -> new BlockItem(ApicaBlocks.HIVE_MULTIBLOCK.get(), new Item.Properties()));

    // --- POLLEN POT ---
    public static final DeferredItem<BlockItem> POLLEN_POT = ITEMS.register("pollen_pot",
            () -> new BlockItem(ApicaBlocks.POLLEN_POT.get(), new Item.Properties()));

    // --- ESSENCE EXTRACTOR ---
    public static final DeferredItem<BlockItem> EXTRACTOR_HEART = ITEMS.register("extractor_heart",
            () -> new BlockItem(ApicaBlocks.EXTRACTOR_HEART.get(), new Item.Properties()));

    // --- ALCHEMY HEARTS ---
    public static final DeferredItem<BlockItem> ALEMBIC_HEART = ITEMS.register("alembic_heart",
            () -> new BlockItem(ApicaBlocks.ALEMBIC_HEART.get(), new Item.Properties()));
    // public static final DeferredItem<BlockItem> INFUSER_HEART = ITEMS.register("infuser_heart",
    //         () -> new BlockItem(ApicaBlocks.INFUSER_HEART.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CENTRIFUGE_HEART = ITEMS.register("centrifuge_heart",
            () -> new BlockItem(ApicaBlocks.CENTRIFUGE_HEART.get(), new Item.Properties()));

    // --- APICA FURNACES ---
    public static final DeferredItem<BlockItem> HONEY_FURNACE = ITEMS.register("honey_furnace",
            () -> new BlockItem(ApicaBlocks.HONEY_FURNACE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_FURNACE = ITEMS.register("royal_furnace",
            () -> new BlockItem(ApicaBlocks.ROYAL_FURNACE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_FURNACE = ITEMS.register("nectar_furnace",
            () -> new BlockItem(ApicaBlocks.NECTAR_FURNACE.get(), new Item.Properties()));

    // --- UNCRAFTING TABLE ---
    public static final DeferredItem<BlockItem> UNCRAFTING_TABLE = ITEMS.register("uncrafting_table",
            () -> new BlockItem(ApicaBlocks.UNCRAFTING_TABLE.get(), new Item.Properties()));

    // --- BEE STATUE ---
    public static final DeferredItem<BlockItem> BEE_STATUE = ITEMS.register("bee_statue",
            () -> new BlockItem(ApicaBlocks.BEE_STATUE.get(), new Item.Properties()));

    // --- API ---
    public static final DeferredItem<BlockItem> API = ITEMS.register("api",
            () -> new BlockItem(ApicaBlocks.API.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED LOGS ---
    public static final DeferredItem<BlockItem> HONEYED_LOG = ITEMS.register("honeyed_log",
            () -> new BlockItem(ApicaBlocks.HONEYED_LOG.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_HONEYED_LOG = ITEMS.register("stripped_honeyed_log",
            () -> new BlockItem(ApicaBlocks.STRIPPED_HONEYED_LOG.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_WOOD = ITEMS.register("honeyed_wood",
            () -> new BlockItem(ApicaBlocks.HONEYED_WOOD.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_HONEYED_WOOD = ITEMS.register("stripped_honeyed_wood",
            () -> new BlockItem(ApicaBlocks.STRIPPED_HONEYED_WOOD.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED WOOD ---
    public static final DeferredItem<BlockItem> HONEYED_PLANKS = ITEMS.register("honeyed_planks",
            () -> new BlockItem(ApicaBlocks.HONEYED_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_WOOD_STAIR = ITEMS.register("honeyed_wood_stair",
            () -> new BlockItem(ApicaBlocks.HONEYED_WOOD_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_WOOD_SLAB = ITEMS.register("honeyed_wood_slab",
            () -> new BlockItem(ApicaBlocks.HONEYED_WOOD_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_FENCE = ITEMS.register("honeyed_fence",
            () -> new BlockItem(ApicaBlocks.HONEYED_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_FENCE_GATE = ITEMS.register("honeyed_fence_gate",
            () -> new BlockItem(ApicaBlocks.HONEYED_FENCE_GATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_TRAPDOOR = ITEMS.register("honeyed_trapdoor",
            () -> new BlockItem(ApicaBlocks.HONEYED_TRAPDOOR.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> HONEYED_DOOR = ITEMS.register("honeyed_door",
            () -> new DoubleHighBlockItem(ApicaBlocks.HONEYED_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_BUTTON = ITEMS.register("honeyed_button",
            () -> new BlockItem(ApicaBlocks.HONEYED_BUTTON.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED STONE ---
    public static final DeferredItem<BlockItem> HONEYED_STONE_WALL = ITEMS.register("honeyed_stone_wall",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_WALL.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED STONE BRICK ---
    public static final DeferredItem<BlockItem> HONEYED_STONE_BRICK_WALL = ITEMS.register("honeyed_stone_brick_wall",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_BRICK_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_STONE_BRICK_PANE = ITEMS.register("honeyed_stone_brick_pane",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_BRICK_PANE.get(), new Item.Properties()));

    // --- IRON FOUNDATION ---
    public static final DeferredItem<BlockItem> IRON_FOUNDATION = ITEMS.register("iron_foundation",
            () -> new BlockItem(ApicaBlocks.IRON_FOUNDATION.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_FOUNDATION_WALL = ITEMS.register("iron_foundation_wall",
            () -> new BlockItem(ApicaBlocks.IRON_FOUNDATION_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_FOUNDATION_TRAPDOOR = ITEMS.register("iron_foundation_trapdoor",
            () -> new BlockItem(ApicaBlocks.IRON_FOUNDATION_TRAPDOOR.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> IRON_FOUNDATION_DOOR = ITEMS.register("iron_foundation_door",
            () -> new DoubleHighBlockItem(ApicaBlocks.IRON_FOUNDATION_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_FOUNDATION_STAIR = ITEMS.register("iron_foundation_stair",
            () -> new BlockItem(ApicaBlocks.IRON_FOUNDATION_STAIR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> IRON_FOUNDATION_SLAB = ITEMS.register("iron_foundation_slab",
            () -> new BlockItem(ApicaBlocks.IRON_FOUNDATION_SLAB.get(), new Item.Properties()));

    // --- BUILDING BLOCKS: HONEYED GLASS ---
    public static final DeferredItem<BlockItem> HONEYED_GLASS = ITEMS.register("honeyed_glass",
            () -> new BlockItem(ApicaBlocks.HONEYED_GLASS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_GLASS_PANE = ITEMS.register("honeyed_glass_pane",
            () -> new BlockItem(ApicaBlocks.HONEYED_GLASS_PANE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_PLANK_PANE = ITEMS.register("honeyed_plank_pane",
            () -> new BlockItem(ApicaBlocks.HONEYED_PLANK_PANE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HONEYED_STONE_PANE = ITEMS.register("honeyed_stone_pane",
            () -> new BlockItem(ApicaBlocks.HONEYED_STONE_PANE.get(), new Item.Properties()));

    // --- MATERIAL STORAGE BLOCKS ---
    public static final DeferredItem<BlockItem> HONEYED_IRON_BLOCK = ITEMS.register("honeyed_iron_block",
            () -> new BlockItem(ApicaBlocks.HONEYED_IRON_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_GOLD_BLOCK = ITEMS.register("royal_gold_block",
            () -> new BlockItem(ApicaBlocks.ROYAL_GOLD_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_DIAMOND_BLOCK = ITEMS.register("nectar_diamond_block",
            () -> new BlockItem(ApicaBlocks.NECTAR_DIAMOND_BLOCK.get(), new Item.Properties()));

    // --- WOODEN FRAME ---
    public static final DeferredItem<Item> WOODEN_FRAME = ITEMS.register("wooden_frame",
            () -> new Item(new Item.Properties()));

    // --- CRAFTING MATERIALS ---
    public static final DeferredItem<Item> HONEYED_IRON = ITEMS.register("honeyed_iron",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> ROYAL_CRYSTAL = ITEMS.register("royal_crystal",
            () -> new BlockItem(ApicaBlocks.ROYAL_CRYSTAL.get(), new Item.Properties()));
    public static final DeferredItem<Item> ROYAL_GOLD = ITEMS.register("royal_gold",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NECTAR_DIAMOND = ITEMS.register("nectar_diamond",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> NECTAR_CRYSTAL = ITEMS.register("nectar_crystal",
            () -> new BlockItem(ApicaBlocks.NECTAR_CRYSTAL.get(), new Item.Properties()));
    public static final DeferredItem<Item> BLANK_RUNE = ITEMS.register("blank_rune",
            () -> new Item(new Item.Properties()));

    // --- ARTIFACT CORES ---
    public static final DeferredItem<Item> HONEY_ARTIFACT_CORE = ITEMS.register("honey_artifact_core",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ROYAL_ARTIFACT_CORE = ITEMS.register("royal_artifact_core",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NECTAR_ARTIFACT_CORE = ITEMS.register("nectar_artifact_core",
            () -> new Item(new Item.Properties()));

    // --- ESSENCES ---
    // Production Essences (améliore le niveau de production des abeilles)
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

    // Species Essence (metadata-driven, unique item)
    public static final DeferredItem<SpeciesEssenceItem> SPECIES_ESSENCE = ITEMS.register("species_essence",
            () -> new SpeciesEssenceItem(new Item.Properties()));

    // --- ESSENCE INJECTOR ---
    public static final DeferredItem<BlockItem> INJECTOR = ITEMS.register("injector",
            () -> new BlockItem(ApicaBlocks.INJECTOR.get(), new Item.Properties()));

    // --- BEE ITEMS ---
    public static final DeferredItem<MagicBeeItem> MAGIC_BEE = ITEMS.register("magic_bee",
            () -> new MagicBeeItem(new Item.Properties()));

    public static final DeferredItem<BeeLarvaItem> BEE_LARVA = ITEMS.register("bee_larva",
            () -> new BeeLarvaItem(new Item.Properties()));

    // --- TOOLS ---
    public static final DeferredItem<BuildingWandItem> BUILDING_STAFF = ITEMS.register("building_staff",
            () -> new BuildingWandItem(new Item.Properties()));

    public static final DeferredItem<DebugWandItem> DEBUG_WAND = ITEMS.register("debug_wand",
            () -> new DebugWandItem(new Item.Properties()));

    public static final DeferredItem<LeafBlowerItem> LEAF_BLOWER = ITEMS.register("leaf_blower",
            () -> new LeafBlowerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<MiningLaserItem> MINING_LASER = ITEMS.register("mining_laser",
            () -> new MiningLaserItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<ChopperCubeItem> CHOPPER_CUBE = ITEMS.register("chopper_cube",
            () -> new ChopperCubeItem(new Item.Properties().stacksTo(1)));

    // --- EQUIPMENT ---
    public static final DeferredItem<BeeElytraItem> BEE_ELYTRA = ITEMS.register("bee_elytra",
            () -> new BeeElytraItem(new Item.Properties().durability(432).rarity(Rarity.UNCOMMON)));

    // --- FOOD ---
    public static final DeferredItem<Item> HONEY_BREAD = ITEMS.register("honey_bread",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(7).saturationModifier(0.6f).build())));
    public static final DeferredItem<Item> ROYAL_BREAD = ITEMS.register("royal_bread",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(10).saturationModifier(0.7f).build())));
    public static final DeferredItem<NectarBreadItem> NECTAR_BREAD = ITEMS.register("nectar_bread",
            () -> new NectarBreadItem(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(15).saturationModifier(0.8f).build()).rarity(Rarity.RARE)));

    // --- RESONATOR ---
    public static final DeferredItem<BlockItem> RESONATOR = ITEMS.register("resonator",
            () -> new BlockItem(ApicaBlocks.RESONATOR.get(), new Item.Properties()));

    // --- CODEX ---
    public static final DeferredItem<CodexItem> CODEX = ITEMS.register("codex",
            () -> new CodexItem(new Item.Properties()));

    // --- FLUID BUCKETS ---
    public static final DeferredItem<BucketItem> HONEY_BUCKET = ITEMS.register("honey_bucket",
            () -> new BucketItem(ApicaFluids.HONEY_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> ROYAL_JELLY_BUCKET = ITEMS.register("royal_jelly_bucket",
            () -> new BucketItem(ApicaFluids.ROYAL_JELLY_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> NECTAR_BUCKET = ITEMS.register("nectar_bucket",
            () -> new BucketItem(ApicaFluids.NECTAR_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // --- COMBS (legacy) ---
    public static final DeferredItem<Item> ROYAL_COMB = ITEMS.register("royal_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BIG_ROYAL_COMB = ITEMS.register("big_royal_comb",
            () -> new Item(new Item.Properties()));

    // --- NEW COMBS (from bee species) ---
    public static final DeferredItem<Item> MEADOW_COMB = ITEMS.register("meadow_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FOREST_COMB = ITEMS.register("forest_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RIVER_COMB = ITEMS.register("river_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> END_COMB = ITEMS.register("end_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MONSTY_COMB = ITEMS.register("monsty_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOCILE_COMB = ITEMS.register("docile_comb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPARK_COMB = ITEMS.register("spark_comb",
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

    // --- BEE NEST ---
    public static final DeferredItem<BlockItem> BEE_NEST = ITEMS.register("bee_nest",
            () -> new BlockItem(ApicaBlocks.BEE_NEST.get(), new Item.Properties()));

    // --- SCOOP ---
    public static final DeferredItem<ScoopItem> SCOOP = ITEMS.register("scoop",
            () -> new ScoopItem(new Item.Properties()));

    // --- MOUNT ITEMS ---
    public static final DeferredItem<HoverbikeSpawnItem> HOVERBIKE_SPAWN = ITEMS.register("hoverbike_spawn",
            () -> new HoverbikeSpawnItem(new Item.Properties()));

    // --- ASSEMBLY TABLE ---
    public static final DeferredItem<BlockItem> ASSEMBLY_TABLE = ITEMS.register("assembly_table",
            () -> new BlockItem(ApicaBlocks.ASSEMBLY_TABLE.get(), new Item.Properties()));

    // --- HOVERBIKE PARTS: CHASSIS ---
    public static final DeferredItem<HoverbikePartItem> CHASSIS_STANDARD = ITEMS.register("chassis_standard",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.CHASSIS, 0));
    public static final DeferredItem<HoverbikePartItem> CHASSIS_HEAVY_FRAME = ITEMS.register("chassis_heavy_frame",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.CHASSIS, 1));
    public static final DeferredItem<HoverbikePartItem> CHASSIS_ARMORED = ITEMS.register("chassis_armored",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.CHASSIS, 2));

    // --- HOVERBIKE PARTS: COEUR ---
    public static final DeferredItem<HoverbikePartItem> COEUR_CUBE = ITEMS.register("coeur_cube",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.COEUR, 0));
    public static final DeferredItem<HoverbikePartItem> COEUR_PYRAMID = ITEMS.register("coeur_pyramid",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.COEUR, 1));
    public static final DeferredItem<HoverbikePartItem> COEUR_CRYSTAL = ITEMS.register("coeur_crystal",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.COEUR, 2));

    // --- HOVERBIKE PARTS: PROPULSEUR ---
    public static final DeferredItem<HoverbikePartItem> PROPULSEUR_DUAL_EXHAUST = ITEMS.register("propulseur_dual_exhaust",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.PROPULSEUR, 0));
    public static final DeferredItem<HoverbikePartItem> PROPULSEUR_QUAD_NOZZLES = ITEMS.register("propulseur_quad_nozzles",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.PROPULSEUR, 1));
    public static final DeferredItem<HoverbikePartItem> PROPULSEUR_MEGA_REACTOR = ITEMS.register("propulseur_mega_reactor",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.PROPULSEUR, 2));

    // --- HOVERBIKE PARTS: RADIATEUR ---
    public static final DeferredItem<HoverbikePartItem> RADIATEUR_PANELS = ITEMS.register("radiateur_panels",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.RADIATEUR, 0));
    public static final DeferredItem<HoverbikePartItem> RADIATEUR_FIN_ARRAY = ITEMS.register("radiateur_fin_array",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.RADIATEUR, 1));
    public static final DeferredItem<HoverbikePartItem> RADIATEUR_WIDE_PANELS = ITEMS.register("radiateur_wide_panels",
            () -> new HoverbikePartItem(new Item.Properties(), HoverbikePart.RADIATEUR, 2));

    // --- CREATIVE FOCUS (debug tool for hoverbike stats) ---
    public static final DeferredItem<CreativeFocusItem> CREATIVE_FOCUS = ITEMS.register("creative_focus",
            () -> new CreativeFocusItem(new Item.Properties().stacksTo(1)));

    // --- CODEX PLACEHOLDER ---
    public static final DeferredItem<BlockItem> AIR_PLACEHOLDER = ITEMS.register("air_placeholder",
            () -> new BlockItem(ApicaBlocks.AIR_PLACEHOLDER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> TANK_PLACEHOLDER = ITEMS.register("tank_placeholder",
            () -> new BlockItem(ApicaBlocks.TANK_PLACEHOLDER.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
