/**
 * ============================================================
 * [ApicaBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.alchemy.AlembicHeartBlock;
import com.chapeau.apica.common.block.alchemy.CentrifugeHeartBlock;
import com.chapeau.apica.common.block.alchemy.CrankBlock;
import com.chapeau.apica.common.block.alchemy.CreativeTankBlock;
import com.chapeau.apica.common.block.alchemy.CrystallizerBlock;
import com.chapeau.apica.common.block.alchemy.HoneyPipeBlock;
import com.chapeau.apica.common.block.alchemy.HoneyTankBlock;
import com.chapeau.apica.common.block.alchemy.InfuserBlock;
import com.chapeau.apica.common.block.alchemy.InfuserHeartBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import com.chapeau.apica.common.block.alchemy.ManualCentrifugeBlock;
import com.chapeau.apica.common.block.alchemy.MultiblockTankBlock;
import com.chapeau.apica.common.block.alchemy.PoweredCentrifugeBlock;
import com.chapeau.apica.common.block.altar.AltarHeartBlock;
import com.chapeau.apica.common.block.altar.HoneyCrystalBlock;
import com.chapeau.apica.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.apica.common.block.altar.HoneyPedestalBlock;
import com.chapeau.apica.common.block.altar.HoneyReservoirBlock;
import com.chapeau.apica.common.block.altar.HoneyedSlabBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneStairBlock;
import com.chapeau.apica.content.flywheeltest.BeeSpawnerBlock;
import com.chapeau.apica.common.block.building.HoneyedGlassBlock;
import com.chapeau.apica.common.block.building.HoneyedStoneWallBlock;
import com.chapeau.apica.common.block.building.IronFoundationBlock;
import com.chapeau.apica.common.block.building.RoyalGoldBlock;
import com.chapeau.apica.common.block.hive.BeeNestBlock;
import com.chapeau.apica.common.block.mount.AssemblyTableBlock;
import com.chapeau.apica.common.block.crystal.MagicBreedingCrystalBlock;
import com.chapeau.apica.common.block.extractor.ExtractorHeartBlock;
import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.common.block.hive.HiveMultiblockBlock;
import com.chapeau.apica.common.block.statue.BeeStatueBlock;
import com.chapeau.apica.common.block.hive.MagicHiveBlock;
import com.chapeau.apica.common.block.incubator.IncubatorBlock;
import com.chapeau.apica.common.block.pollenpot.PollenPotBlock;
import com.chapeau.apica.common.block.storage.ControlledHiveBlock;
import com.chapeau.apica.common.block.storage.ExportInterfaceBlock;
import com.chapeau.apica.common.block.storage.ImportInterfaceBlock;
import com.chapeau.apica.common.block.storage.StorageControllerBlock;
import com.chapeau.apica.common.block.storage.StorageHiveBlock;
import com.chapeau.apica.common.block.storage.StorageRelayBlock;
import com.chapeau.apica.common.block.storage.StorageTerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ApicaBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Apica.MOD_ID);

    // --- STORAGE ---
    public static final DeferredBlock<StorageControllerBlock> STORAGE_CONTROLLER = BLOCKS.register("storage_controller",
            () -> new StorageControllerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageRelayBlock> STORAGE_RELAY = BLOCKS.register("storage_relay",
            () -> new StorageRelayBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL = BLOCKS.register("storage_terminal",
            () -> new StorageTerminalBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ImportInterfaceBlock> IMPORT_INTERFACE = BLOCKS.register("import_interface",
            () -> new ImportInterfaceBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ExportInterfaceBlock> EXPORT_INTERFACE = BLOCKS.register("export_interface",
            () -> new ExportInterfaceBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ControlledHiveBlock> CONTROLLED_HIVE = BLOCKS.register("controlled_hive",
            () -> new ControlledHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));

    // --- STORAGE HIVES ---
    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE = BLOCKS.register("storage_hive",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE_TIER2 = BLOCKS.register("storage_hive_tier2",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD), 2));

    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE_TIER3 = BLOCKS.register("storage_hive_tier3",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD), 3));

    // --- BEE MACHINES ---
    public static final DeferredBlock<Block> MAGIC_HIVE = BLOCKS.register("magic_hive",
            () -> new MagicHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)));

    public static final DeferredBlock<Block> INCUBATOR = BLOCKS.register("incubator",
            () -> new IncubatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

    // --- ANTIBREEDING ---
    public static final DeferredBlock<Block> ANTIBREEDING_CRYSTAL = BLOCKS.register("antibreeding_crystal",
            () -> new MagicBreedingCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7)
                    .noOcclusion()));

    // Legacy alias
    public static final DeferredBlock<Block> BREEDING_CRYSTAL = ANTIBREEDING_CRYSTAL;

    // --- ALCHEMY MACHINES ---
    public static final DeferredBlock<ManualCentrifugeBlock> MANUAL_CENTRIFUGE = BLOCKS.register("manual_centrifuge",
            () -> new ManualCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<CrankBlock> CRANK = BLOCKS.register("crank",
            () -> new CrankBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE = BLOCKS.register("powered_centrifuge",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE_TIER2 = BLOCKS.register("powered_centrifuge_tier2",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 2));

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK = BLOCKS.register("honey_tank",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<CreativeTankBlock> CREATIVE_TANK = BLOCKS.register("creative_tank",
            () -> new CreativeTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE = BLOCKS.register("honey_pipe",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER2 = BLOCKS.register("honey_pipe_tier2",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER3 = BLOCKS.register("honey_pipe_tier3",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<HoneyPipeBlock> HONEY_PIPE_TIER4 = BLOCKS.register("honey_pipe_tier4",
            () -> new HoneyPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = BLOCKS.register("item_pipe",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER2 = BLOCKS.register("item_pipe_tier2",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER3 = BLOCKS.register("item_pipe_tier3",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_TIER4 = BLOCKS.register("item_pipe_tier4",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<CrystallizerBlock> CRYSTALLIZER = BLOCKS.register("crystallizer",
            () -> new CrystallizerBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(CrystallizerBlock.ACTIVE) ? 10 : 4)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<InfuserBlock> INFUSER = BLOCKS.register("infuser",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0)));

    // public static final DeferredBlock<InfuserBlock> INFUSER_TIER2 = BLOCKS.register("infuser_tier2",
    //         () -> new InfuserBlock(BlockBehaviour.Properties.of()
    //                 .strength(3.5f)
    //                 .sound(SoundType.WOOD)
    //                 .requiresCorrectToolForDrops()
    //                 .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0), 2));

    public static final DeferredBlock<MultiblockTankBlock> MULTIBLOCK_TANK = BLOCKS.register("multiblock_tank",
            () -> new MultiblockTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    // --- HONEY ALTAR ---
    public static final DeferredBlock<HoneyedStoneBlock> HONEYED_STONE = BLOCKS.register("honeyed_stone",
            () -> new HoneyedStoneBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedStoneStairBlock> HONEYED_STONE_STAIR = BLOCKS.register("honeyed_stone_stair",
            () -> new HoneyedStoneStairBlock(
                    HONEYED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyPedestalBlock> HONEY_PEDESTAL = BLOCKS.register("honey_pedestal",
            () -> new HoneyPedestalBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalConduitBlock> HONEY_CRYSTAL_CONDUIT = BLOCKS.register("honey_crystal_conduit",
            () -> new HoneyCrystalConduitBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalBlock> HONEY_CRYSTAL = BLOCKS.register("honey_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 11)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedSlabBlock> HONEYED_SLAB = BLOCKS.register("honeyed_slab",
            () -> new HoneyedSlabBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyReservoirBlock> HONEY_RESERVOIR = BLOCKS.register("honey_reservoir",
            () -> new HoneyReservoirBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<AltarHeartBlock> ALTAR_HEART = BLOCKS.register("altar_heart",
            () -> new AltarHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- HIVE MULTIBLOCK ---
    public static final DeferredBlock<HiveMultiblockBlock> HIVE_MULTIBLOCK = BLOCKS.register("hive_multiblock",
            () -> new HiveMultiblockBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // --- POLLEN POT ---
    public static final DeferredBlock<PollenPotBlock> POLLEN_POT = BLOCKS.register("pollen_pot",
            () -> new PollenPotBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()));

    // --- ESSENCE EXTRACTOR ---
    public static final DeferredBlock<ExtractorHeartBlock> EXTRACTOR_HEART = BLOCKS.register("extractor_heart",
            () -> new ExtractorHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- ALCHEMY HEARTS ---
    public static final DeferredBlock<AlembicHeartBlock> ALEMBIC_HEART = BLOCKS.register("alembic_heart",
            () -> new AlembicHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // public static final DeferredBlock<InfuserHeartBlock> INFUSER_HEART = BLOCKS.register("infuser_heart",
    //         () -> new InfuserHeartBlock(BlockBehaviour.Properties.of()
    //                 .strength(3.0f)
    //                 .sound(SoundType.AMETHYST)
    //                 .lightLevel(state -> 4)
    //                 .noOcclusion()
    //                 .requiresCorrectToolForDrops()));

    public static final DeferredBlock<CentrifugeHeartBlock> CENTRIFUGE_HEART = BLOCKS.register("centrifuge_heart",
            () -> new CentrifugeHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- BEE STATUE ---
    public static final DeferredBlock<BeeStatueBlock> BEE_STATUE = BLOCKS.register("bee_statue",
            () -> new BeeStatueBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    // --- BUILDING BLOCKS: HONEYED LOGS ---
    public static final DeferredBlock<RotatedPillarBlock> HONEYED_LOG = BLOCKS.register("honeyed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HONEYED_LOG = BLOCKS.register("stripped_honeyed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> HONEYED_WOOD = BLOCKS.register("honeyed_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HONEYED_WOOD = BLOCKS.register("stripped_honeyed_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    // --- BUILDING BLOCKS: HONEYED WOOD ---
    public static final DeferredBlock<Block> HONEYED_PLANKS = BLOCKS.register("honeyed_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<StairBlock> HONEYED_WOOD_STAIR = BLOCKS.register("honeyed_wood_stair",
            () -> new StairBlock(HONEYED_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<SlabBlock> HONEYED_WOOD_SLAB = BLOCKS.register("honeyed_wood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceBlock> HONEYED_FENCE = BLOCKS.register("honeyed_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceGateBlock> HONEYED_FENCE_GATE = BLOCKS.register("honeyed_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<TrapDoorBlock> HONEYED_TRAPDOOR = BLOCKS.register("honeyed_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<DoorBlock> HONEYED_DOOR = BLOCKS.register("honeyed_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<ButtonBlock> HONEYED_BUTTON = BLOCKS.register("honeyed_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of()
                    .strength(0.5f).sound(SoundType.WOOD).noCollission()));

    // --- BUILDING BLOCKS: HONEYED STONE ---
    public static final DeferredBlock<HoneyedStoneWallBlock> HONEYED_STONE_WALL = BLOCKS.register("honeyed_stone_wall",
            () -> new HoneyedStoneWallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // --- BUILDING BLOCKS: HONEYED GLASS ---
    public static final DeferredBlock<HoneyedGlassBlock> HONEYED_GLASS = BLOCKS.register("honeyed_glass",
            () -> new HoneyedGlassBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_GLASS_PANE = BLOCKS.register("honeyed_glass_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    // --- IRON FOUNDATION ---
    public static final DeferredBlock<IronFoundationBlock> IRON_FOUNDATION = BLOCKS.register("iron_foundation",
            () -> new IronFoundationBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<WallBlock> IRON_FOUNDATION_WALL = BLOCKS.register("iron_foundation_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StairBlock> IRON_FOUNDATION_STAIR = BLOCKS.register("iron_foundation_stair",
            () -> new StairBlock(IRON_FOUNDATION.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<SlabBlock> IRON_FOUNDATION_SLAB = BLOCKS.register("iron_foundation_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<TrapDoorBlock> IRON_FOUNDATION_TRAPDOOR = BLOCKS.register("iron_foundation_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DoorBlock> IRON_FOUNDATION_DOOR = BLOCKS.register("iron_foundation_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- MATERIAL STORAGE BLOCKS ---
    public static final DeferredBlock<Block> HONEYED_IRON_BLOCK = BLOCKS.register("honeyed_iron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<RoyalGoldBlock> ROYAL_GOLD_BLOCK = BLOCKS.register("royal_gold_block",
            () -> new RoyalGoldBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> NECTAR_DIAMOND_BLOCK = BLOCKS.register("nectar_diamond_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalBlock> ROYAL_CRYSTAL = BLOCKS.register("royal_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 13)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    Block.box(3, 0, 3, 13, 12, 13)));

    public static final DeferredBlock<HoneyCrystalBlock> NECTAR_CRYSTAL = BLOCKS.register("nectar_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    Block.box(3, 0, 3, 13, 14, 13)));

    // --- ASSEMBLY TABLE ---
    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE = BLOCKS.register("assembly_table",
            () -> new AssemblyTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // --- FLYWHEEL TEST ---
    public static final DeferredBlock<BeeSpawnerBlock> BEE_SPAWNER = BLOCKS.register("bee_spawner",
            () -> new BeeSpawnerBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    // --- BEE NEST ---
    public static final DeferredBlock<BeeNestBlock> BEE_NEST = BLOCKS.register("bee_nest",
            () -> new BeeNestBlock(BlockBehaviour.Properties.of()
                    .strength(0.6f)
                    .sound(SoundType.WOOD)));

    // --- FLUID BLOCKS ---
    public static final DeferredBlock<LiquidBlock> HONEY_FLUID_BLOCK = BLOCKS.register("honey",
            () -> new LiquidBlock(ApicaFluids.HONEY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> ROYAL_JELLY_FLUID_BLOCK = BLOCKS.register("royal_jelly",
            () -> new LiquidBlock(ApicaFluids.ROYAL_JELLY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> NECTAR_FLUID_BLOCK = BLOCKS.register("nectar",
            () -> new LiquidBlock(ApicaFluids.NECTAR_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .lightLevel(state -> 8)));

    // --- CODEX PLACEHOLDER (rendu uniquement, non placable en jeu) ---
    public static final DeferredBlock<Block> AIR_PLACEHOLDER = BLOCKS.register("air_placeholder",
            () -> new Block(BlockBehaviour.Properties.of()
                    .noCollission().noLootTable().noOcclusion()));

    public static final DeferredBlock<Block> TANK_PLACEHOLDER = BLOCKS.register("tank_placeholder",
            () -> new Block(BlockBehaviour.Properties.of()
                    .noCollission().noLootTable().noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
