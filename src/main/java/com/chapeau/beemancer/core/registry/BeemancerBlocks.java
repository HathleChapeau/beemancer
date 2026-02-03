/**
 * ============================================================
 * [BeemancerBlocks.java]
 * Description: Registre centralisé de tous les blocs du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.alchemy.*;
import com.chapeau.beemancer.common.block.altar.*;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlock;
import com.chapeau.beemancer.common.block.crystal.MagicBreedingCrystalBlock;
import com.chapeau.beemancer.common.block.extractor.ExtractorHeartBlock;
import com.chapeau.beemancer.common.block.hive.HiveMultiblockBlock;
import com.chapeau.beemancer.common.block.statue.BeeStatueBlock;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlock;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlock;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotBlock;
import com.chapeau.beemancer.common.block.storage.ControlledHiveBlock;
import com.chapeau.beemancer.common.block.storage.ControllerPipeBlock;
import com.chapeau.beemancer.common.block.storage.ExportInterfaceBlock;
import com.chapeau.beemancer.common.block.storage.ImportInterfaceBlock;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.common.block.storage.StorageCrateBlock;
import com.chapeau.beemancer.common.block.storage.StorageRelayBlock;
import com.chapeau.beemancer.common.block.storage.StorageTerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
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

public class BeemancerBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Beemancer.MOD_ID);

    // --- STORAGE ---
    public static final DeferredBlock<Block> STORAGE_CRATE = BLOCKS.register("storage_crate",
            () -> new StorageCrateBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageControllerBlock> STORAGE_CONTROLLER = BLOCKS.register("storage_controller",
            () -> new StorageControllerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageRelayBlock> STORAGE_RELAY = BLOCKS.register("storage_relay",
            () -> new StorageRelayBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
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

    public static final DeferredBlock<ControllerPipeBlock> CONTROLLER_PIPE = BLOCKS.register("controller_pipe",
            () -> new ControllerPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    // --- BEE MACHINES ---
    public static final DeferredBlock<Block> BEE_CREATOR = BLOCKS.register("bee_creator",
            () -> new BeeCreatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

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

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE_TIER3 = BLOCKS.register("powered_centrifuge_tier3",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(4.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 3));

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

    public static final DeferredBlock<AlembicBlock> ALEMBIC = BLOCKS.register("alembic",
            () -> new AlembicBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<InfuserBlock> INFUSER = BLOCKS.register("infuser",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0)));

    public static final DeferredBlock<InfuserBlock> INFUSER_TIER2 = BLOCKS.register("infuser_tier2",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0), 2));

    public static final DeferredBlock<InfuserBlock> INFUSER_TIER3 = BLOCKS.register("infuser_tier3",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0), 3));

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
                    .lightLevel(state -> state.getValue(HoneyCrystalBlock.FORMED) ? 15 : 10)
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
                    .lightLevel(state -> state.getValue(AltarHeartBlock.FORMED) ? 15 : 8)
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
                    .lightLevel(state -> state.getValue(ExtractorHeartBlock.FORMED) ? 12 : 6)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- BEE STATUE ---
    public static final DeferredBlock<BeeStatueBlock> BEE_STATUE = BLOCKS.register("bee_statue",
            () -> new BeeStatueBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

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

    // --- BUILDING BLOCKS: HONEYED STONE ---
    public static final DeferredBlock<WallBlock> HONEYED_STONE_WALL = BLOCKS.register("honeyed_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // --- BUILDING BLOCKS: HONEYED GLASS ---
    public static final DeferredBlock<TransparentBlock> HONEYED_GLASS = BLOCKS.register("honeyed_glass",
            () -> new TransparentBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_GLASS_PANE = BLOCKS.register("honeyed_glass_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    // --- BUILDING BLOCKS: ROYAL WOOD ---
    public static final DeferredBlock<Block> ROYAL_PLANKS = BLOCKS.register("royal_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<StairBlock> ROYAL_WOOD_STAIR = BLOCKS.register("royal_wood_stair",
            () -> new StairBlock(ROYAL_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<SlabBlock> ROYAL_WOOD_SLAB = BLOCKS.register("royal_wood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceBlock> ROYAL_FENCE = BLOCKS.register("royal_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceGateBlock> ROYAL_FENCE_GATE = BLOCKS.register("royal_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<TrapDoorBlock> ROYAL_TRAPDOOR = BLOCKS.register("royal_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<DoorBlock> ROYAL_DOOR = BLOCKS.register("royal_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    // --- BUILDING BLOCKS: ROYAL STONE ---
    public static final DeferredBlock<Block> ROYAL_STONE = BLOCKS.register("royal_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<StairBlock> ROYAL_STONE_STAIR = BLOCKS.register("royal_stone_stair",
            () -> new StairBlock(ROYAL_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<SlabBlock> ROYAL_STONE_SLAB = BLOCKS.register("royal_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<WallBlock> ROYAL_STONE_WALL = BLOCKS.register("royal_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // --- BUILDING BLOCKS: NECTAR WOOD ---
    public static final DeferredBlock<Block> NECTAR_PLANKS = BLOCKS.register("nectar_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<StairBlock> NECTAR_WOOD_STAIR = BLOCKS.register("nectar_wood_stair",
            () -> new StairBlock(NECTAR_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<SlabBlock> NECTAR_WOOD_SLAB = BLOCKS.register("nectar_wood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceBlock> NECTAR_FENCE = BLOCKS.register("nectar_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceGateBlock> NECTAR_FENCE_GATE = BLOCKS.register("nectar_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<TrapDoorBlock> NECTAR_TRAPDOOR = BLOCKS.register("nectar_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<DoorBlock> NECTAR_DOOR = BLOCKS.register("nectar_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    // --- BUILDING BLOCKS: NECTAR STONE ---
    public static final DeferredBlock<Block> NECTAR_STONE = BLOCKS.register("nectar_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<StairBlock> NECTAR_STONE_STAIR = BLOCKS.register("nectar_stone_stair",
            () -> new StairBlock(NECTAR_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<SlabBlock> NECTAR_STONE_SLAB = BLOCKS.register("nectar_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<WallBlock> NECTAR_STONE_WALL = BLOCKS.register("nectar_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // --- FLUID BLOCKS ---
    public static final DeferredBlock<LiquidBlock> HONEY_FLUID_BLOCK = BLOCKS.register("honey",
            () -> new LiquidBlock(BeemancerFluids.HONEY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> ROYAL_JELLY_FLUID_BLOCK = BLOCKS.register("royal_jelly",
            () -> new LiquidBlock(BeemancerFluids.ROYAL_JELLY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> NECTAR_FLUID_BLOCK = BLOCKS.register("nectar",
            () -> new LiquidBlock(BeemancerFluids.NECTAR_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .lightLevel(state -> 8)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
