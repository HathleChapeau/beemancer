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
import com.chapeau.apica.common.block.alchemy.HoneyLampBlock;
import com.chapeau.apica.common.block.alchemy.LiquidPipeBlock;
import com.chapeau.apica.common.block.alchemy.HoneyTankBlock;
import com.chapeau.apica.common.block.alchemy.InfuserBlock;
import com.chapeau.apica.common.block.alchemy.InfuserHeartBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import com.chapeau.apica.common.block.alchemy.ManualCentrifugeBlock;
import com.chapeau.apica.common.block.alchemy.MultiblockTankBlock;
import com.chapeau.apica.common.block.alchemy.ApicaFurnaceBlock;
import com.chapeau.apica.common.block.alchemy.PoweredCentrifugeBlock;
import com.chapeau.apica.common.block.altar.AltarHeartBlock;
import com.chapeau.apica.common.block.altar.HoneyCrystalBlock;
import com.chapeau.apica.common.block.altar.HoneyCrystalConduitBlock;
import com.chapeau.apica.common.block.altar.HoneyPedestalBlock;
import com.chapeau.apica.common.block.altar.HoneyReservoirBlock;
import com.chapeau.apica.common.block.altar.HoneyedSlabBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneBrickBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneBrickSlabBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneBrickStairBlock;
import com.chapeau.apica.common.block.altar.HoneyedStoneStairBlock;
import com.chapeau.apica.common.block.building.HoneyedGlassBlock;
import com.chapeau.apica.common.block.building.HoneyedStoneBrickWallBlock;
import com.chapeau.apica.common.block.building.HoneyedStoneWallBlock;
import com.chapeau.apica.common.block.building.IronFoundationBlock;
import com.chapeau.apica.common.block.building.RoyalGoldBlock;
import com.chapeau.apica.common.block.hive.BeeNestBlock;
import com.chapeau.apica.common.block.mount.AssemblyTableBlock;
import com.chapeau.apica.common.block.crystal.AntiBreedingCrystalBlock;
import com.chapeau.apica.common.block.crystal.CreativeBreedingCrystalBlock;
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
import com.chapeau.apica.common.block.storage.LiquidTrashCanBlock;
import com.chapeau.apica.common.block.storage.StorageBarrelBlock;
import com.chapeau.apica.common.block.storage.StorageControllerBlock;
import com.chapeau.apica.common.block.storage.TrashCanBlock;
import com.chapeau.apica.common.block.resonator.ResonatorBlock;
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

    private static <T extends Block> DeferredBlock<T> registerTimed(String name, java.util.function.Supplier<T> factory) {
        return BLOCKS.register(name, () -> {
            long t = System.currentTimeMillis();
            T block = factory.get();
            long ms = System.currentTimeMillis() - t;
            if (ms > 5) Apica.LOGGER.warn("[TIMING] Block '{}' slow: {}ms", name, ms);
            else Apica.LOGGER.debug("[TIMING] Block '{}': {}ms", name, ms);
            return block;
        });
    }

    // --- STORAGE ---
    public static final DeferredBlock<StorageControllerBlock> STORAGE_CONTROLLER = registerTimed("storage_controller",
            () -> new StorageControllerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageRelayBlock> STORAGE_RELAY = registerTimed("storage_relay",
            () -> new StorageRelayBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL = registerTimed("storage_terminal",
            () -> new StorageTerminalBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ImportInterfaceBlock> IMPORT_INTERFACE = registerTimed("import_interface",
            () -> new ImportInterfaceBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ExportInterfaceBlock> EXPORT_INTERFACE = registerTimed("export_interface",
            () -> new ExportInterfaceBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // --- STORAGE BARRELS ---
    public static final DeferredBlock<StorageBarrelBlock> STORAGE_BARREL_MK1 = registerTimed("storage_barrel_mk1",
            () -> new StorageBarrelBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<StorageBarrelBlock> STORAGE_BARREL_MK2 = registerTimed("storage_barrel_mk2",
            () -> new StorageBarrelBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD), 2));

    public static final DeferredBlock<StorageBarrelBlock> STORAGE_BARREL_MK3 = registerTimed("storage_barrel_mk3",
            () -> new StorageBarrelBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.WOOD), 3));

    public static final DeferredBlock<StorageBarrelBlock> STORAGE_BARREL_MK4 = registerTimed("storage_barrel_mk4",
            () -> new StorageBarrelBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .sound(SoundType.WOOD), 4));

    // --- TRASH CANS ---
    public static final DeferredBlock<TrashCanBlock> TRASH_CAN = registerTimed("trash_can",
            () -> new TrashCanBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<LiquidTrashCanBlock> LIQUID_TRASH_CAN = registerTimed("liquid_trash_can",
            () -> new LiquidTrashCanBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<ControlledHiveBlock> CONTROLLED_HIVE = registerTimed("controlled_hive",
            () -> new ControlledHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));

    // --- STORAGE HIVES ---
    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE = registerTimed("storage_hive",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE_TIER2 = registerTimed("storage_hive_tier2",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD), 2));

    public static final DeferredBlock<StorageHiveBlock> STORAGE_HIVE_TIER3 = registerTimed("storage_hive_tier3",
            () -> new StorageHiveBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD), 3));

    // --- BEE MACHINES ---
    public static final DeferredBlock<Block> MAGIC_HIVE = registerTimed("magic_hive",
            () -> new MagicHiveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)));

    public static final DeferredBlock<Block> INCUBATOR = registerTimed("incubator",
            () -> new IncubatorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .requiresCorrectToolForDrops()));

    // --- ANTIBREEDING ---
    public static final DeferredBlock<Block> ANTIBREEDING_CRYSTAL = registerTimed("antibreeding_crystal",
            () -> new AntiBreedingCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7)
                    .noOcclusion()));

    // Legacy alias
    public static final DeferredBlock<Block> BREEDING_CRYSTAL = ANTIBREEDING_CRYSTAL;

    // --- CREATIVE BREEDING CRYSTAL (debug) ---
    public static final DeferredBlock<Block> CREATIVE_BREEDING_CRYSTAL = registerTimed("creative_breeding_crystal",
            () -> new CreativeBreedingCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 10)
                    .noOcclusion()));

    // --- ALCHEMY MACHINES ---
    public static final DeferredBlock<ManualCentrifugeBlock> MANUAL_CENTRIFUGE = registerTimed("manual_centrifuge",
            () -> new ManualCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<CrankBlock> CRANK = registerTimed("crank",
            () -> new CrankBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE = registerTimed("powered_centrifuge",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PoweredCentrifugeBlock> POWERED_CENTRIFUGE_TIER2 = registerTimed("powered_centrifuge_tier2",
            () -> new PoweredCentrifugeBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 2));

    public static final DeferredBlock<HoneyTankBlock> HONEY_TANK = registerTimed("honey_tank",
            () -> new HoneyTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<CreativeTankBlock> CREATIVE_TANK = registerTimed("creative_tank",
            () -> new CreativeTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<LiquidPipeBlock> LIQUID_PIPE = registerTimed("liquid_pipe",
            () -> new LiquidPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<LiquidPipeBlock> LIQUID_PIPE_MK2 = registerTimed("liquid_pipe_mk2",
            () -> new LiquidPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<LiquidPipeBlock> LIQUID_PIPE_MK3 = registerTimed("liquid_pipe_mk3",
            () -> new LiquidPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<LiquidPipeBlock> LIQUID_PIPE_MK4 = registerTimed("liquid_pipe_mk4",
            () -> new LiquidPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = registerTimed("item_pipe",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_MK2 = registerTimed("item_pipe_mk2",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(1.8f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 2));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_MK3 = registerTimed("item_pipe_mk3",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 3));

    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE_MK4 = registerTimed("item_pipe_mk4",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.COPPER)
                    .noOcclusion(), 4));

    public static final DeferredBlock<CrystallizerBlock> CRYSTALLIZER = registerTimed("crystallizer",
            () -> new CrystallizerBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(CrystallizerBlock.ACTIVE) ? 10 : 4)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<InfuserBlock> INFUSER = registerTimed("infuser",
            () -> new InfuserBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0)));

    // public static final DeferredBlock<InfuserBlock> INFUSER_TIER2 = registerTimed("infuser_tier2",
    //         () -> new InfuserBlock(BlockBehaviour.Properties.of()
    //                 .strength(3.5f)
    //                 .sound(SoundType.WOOD)
    //                 .requiresCorrectToolForDrops()
    //                 .lightLevel(state -> state.getValue(InfuserBlock.WORKING) ? 10 : 0), 2));

    public static final DeferredBlock<HoneyLampBlock> HONEY_LAMP = registerTimed("honey_lamp",
            () -> new HoneyLampBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<MultiblockTankBlock> MULTIBLOCK_TANK = registerTimed("multiblock_tank",
            () -> new MultiblockTankBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    // --- HONEY ALTAR ---
    public static final DeferredBlock<HoneyedStoneBlock> HONEYED_STONE = registerTimed("honeyed_stone",
            () -> new HoneyedStoneBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedStoneStairBlock> HONEYED_STONE_STAIR = registerTimed("honeyed_stone_stair",
            () -> new HoneyedStoneStairBlock(
                    HONEYED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyPedestalBlock> HONEY_PEDESTAL = registerTimed("honey_pedestal",
            () -> new HoneyPedestalBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalConduitBlock> HONEY_CRYSTAL_CONDUIT = registerTimed("honey_crystal_conduit",
            () -> new HoneyCrystalConduitBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalBlock> HONEY_CRYSTAL = registerTimed("honey_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 11)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedSlabBlock> HONEYED_SLAB = registerTimed("honeyed_slab",
            () -> new HoneyedSlabBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // --- HONEYED STONE BRICK ---
    public static final DeferredBlock<HoneyedStoneBrickBlock> HONEYED_STONE_BRICK = registerTimed("honeyed_stone_brick",
            () -> new HoneyedStoneBrickBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedStoneBrickStairBlock> HONEYED_STONE_BRICK_STAIR = registerTimed("honeyed_stone_brick_stair",
            () -> new HoneyedStoneBrickStairBlock(
                    HONEYED_STONE_BRICK.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyedStoneBrickSlabBlock> HONEYED_STONE_BRICK_SLAB = registerTimed("honeyed_stone_brick_slab",
            () -> new HoneyedStoneBrickSlabBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyReservoirBlock> HONEY_RESERVOIR = registerTimed("honey_reservoir",
            () -> new HoneyReservoirBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredBlock<AltarHeartBlock> ALTAR_HEART = registerTimed("altar_heart",
            () -> new AltarHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- HIVE MULTIBLOCK ---
    public static final DeferredBlock<HiveMultiblockBlock> HIVE_MULTIBLOCK = registerTimed("hive_multiblock",
            () -> new HiveMultiblockBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // --- POLLEN POT ---
    public static final DeferredBlock<PollenPotBlock> POLLEN_POT = registerTimed("pollen_pot",
            () -> new PollenPotBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()));

    // --- ESSENCE EXTRACTOR ---
    public static final DeferredBlock<ExtractorHeartBlock> EXTRACTOR_HEART = registerTimed("extractor_heart",
            () -> new ExtractorHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- ESSENCE INJECTOR ---
    public static final DeferredBlock<com.chapeau.apica.common.block.injector.InjectorBlock> INJECTOR = registerTimed("injector",
            () -> new com.chapeau.apica.common.block.injector.InjectorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- ALCHEMY HEARTS ---
    public static final DeferredBlock<AlembicHeartBlock> ALEMBIC_HEART = registerTimed("alembic_heart",
            () -> new AlembicHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // public static final DeferredBlock<InfuserHeartBlock> INFUSER_HEART = registerTimed("infuser_heart",
    //         () -> new InfuserHeartBlock(BlockBehaviour.Properties.of()
    //                 .strength(3.0f)
    //                 .sound(SoundType.AMETHYST)
    //                 .lightLevel(state -> 4)
    //                 .noOcclusion()
    //                 .requiresCorrectToolForDrops()));

    public static final DeferredBlock<CentrifugeHeartBlock> CENTRIFUGE_HEART = registerTimed("centrifuge_heart",
            () -> new CentrifugeHeartBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- APICA FURNACES ---
    public static final DeferredBlock<ApicaFurnaceBlock> HONEY_FURNACE = registerTimed("honey_furnace",
            () -> new ApicaFurnaceBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ApicaFurnaceBlock> ROYAL_FURNACE = registerTimed("royal_furnace",
            () -> new ApicaFurnaceBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 2));

    public static final DeferredBlock<ApicaFurnaceBlock> NECTAR_FURNACE = registerTimed("nectar_furnace",
            () -> new ApicaFurnaceBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), 3));

    // --- BEE STATUE ---
    public static final DeferredBlock<BeeStatueBlock> BEE_STATUE = registerTimed("bee_statue",
            () -> new BeeStatueBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    // --- BUILDING BLOCKS: HONEYED LOGS ---
    public static final DeferredBlock<RotatedPillarBlock> HONEYED_LOG = registerTimed("honeyed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HONEYED_LOG = registerTimed("stripped_honeyed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> HONEYED_WOOD = registerTimed("honeyed_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HONEYED_WOOD = registerTimed("stripped_honeyed_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    // --- BUILDING BLOCKS: HONEYED WOOD ---
    public static final DeferredBlock<Block> HONEYED_PLANKS = registerTimed("honeyed_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<StairBlock> HONEYED_WOOD_STAIR = registerTimed("honeyed_wood_stair",
            () -> new StairBlock(HONEYED_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<SlabBlock> HONEYED_WOOD_SLAB = registerTimed("honeyed_wood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceBlock> HONEYED_FENCE = registerTimed("honeyed_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<FenceGateBlock> HONEYED_FENCE_GATE = registerTimed("honeyed_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD)));

    public static final DeferredBlock<TrapDoorBlock> HONEYED_TRAPDOOR = registerTimed("honeyed_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<DoorBlock> HONEYED_DOOR = registerTimed("honeyed_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<ButtonBlock> HONEYED_BUTTON = registerTimed("honeyed_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.of()
                    .strength(0.5f).sound(SoundType.WOOD).noCollission()));

    // --- BUILDING BLOCKS: HONEYED STONE ---
    public static final DeferredBlock<HoneyedStoneWallBlock> HONEYED_STONE_WALL = registerTimed("honeyed_stone_wall",
            () -> new HoneyedStoneWallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // --- BUILDING BLOCKS: HONEYED STONE BRICK ---
    public static final DeferredBlock<HoneyedStoneBrickWallBlock> HONEYED_STONE_BRICK_WALL = registerTimed("honeyed_stone_brick_wall",
            () -> new HoneyedStoneBrickWallBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_STONE_BRICK_PANE = registerTimed("honeyed_stone_brick_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).noOcclusion()));

    // --- BUILDING BLOCKS: HONEYED GLASS ---
    public static final DeferredBlock<HoneyedGlassBlock> HONEYED_GLASS = registerTimed("honeyed_glass",
            () -> new HoneyedGlassBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_GLASS_PANE = registerTimed("honeyed_glass_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f).sound(SoundType.GLASS).noOcclusion()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_PLANK_PANE = registerTimed("honeyed_plank_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<IronBarsBlock> HONEYED_STONE_PANE = registerTimed("honeyed_stone_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f).sound(SoundType.STONE).noOcclusion()));

    // --- IRON FOUNDATION ---
    public static final DeferredBlock<IronFoundationBlock> IRON_FOUNDATION = registerTimed("iron_foundation",
            () -> new IronFoundationBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<WallBlock> IRON_FOUNDATION_WALL = registerTimed("iron_foundation_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StairBlock> IRON_FOUNDATION_STAIR = registerTimed("iron_foundation_stair",
            () -> new StairBlock(IRON_FOUNDATION.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<SlabBlock> IRON_FOUNDATION_SLAB = registerTimed("iron_foundation_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<TrapDoorBlock> IRON_FOUNDATION_TRAPDOOR = registerTimed("iron_foundation_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DoorBlock> IRON_FOUNDATION_DOOR = registerTimed("iron_foundation_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    // --- MATERIAL STORAGE BLOCKS ---
    public static final DeferredBlock<Block> HONEYED_IRON_BLOCK = registerTimed("honeyed_iron_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<RoyalGoldBlock> ROYAL_GOLD_BLOCK = registerTimed("royal_gold_block",
            () -> new RoyalGoldBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> NECTAR_DIAMOND_BLOCK = registerTimed("nectar_diamond_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<HoneyCrystalBlock> ROYAL_CRYSTAL = registerTimed("royal_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 13)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    Block.box(3, 1, 3, 13, 13, 13)));

    public static final DeferredBlock<HoneyCrystalBlock> NECTAR_CRYSTAL = registerTimed("nectar_crystal",
            () -> new HoneyCrystalBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(),
                    Block.box(3, 0, 3, 13, 14, 13)));

    // --- RESONATOR ---
    public static final DeferredBlock<ResonatorBlock> RESONATOR = registerTimed("resonator",
            () -> new ResonatorBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // --- ASSEMBLY TABLE ---
    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE = registerTimed("assembly_table",
            () -> new AssemblyTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    // --- BEE NEST ---
    public static final DeferredBlock<BeeNestBlock> BEE_NEST = registerTimed("bee_nest",
            () -> new BeeNestBlock(BlockBehaviour.Properties.of()
                    .strength(0.6f)
                    .sound(SoundType.WOOD)));

    // --- FLUID BLOCKS ---
    public static final DeferredBlock<LiquidBlock> HONEY_FLUID_BLOCK = registerTimed("honey",
            () -> new LiquidBlock(ApicaFluids.HONEY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> ROYAL_JELLY_FLUID_BLOCK = registerTimed("royal_jelly",
            () -> new LiquidBlock(ApicaFluids.ROYAL_JELLY_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));

    public static final DeferredBlock<LiquidBlock> NECTAR_FLUID_BLOCK = registerTimed("nectar",
            () -> new LiquidBlock(ApicaFluids.NECTAR_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable().noCollission().strength(100.0F)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .lightLevel(state -> 8)));

    // --- CODEX PLACEHOLDER (rendu uniquement, non placable en jeu) ---
    public static final DeferredBlock<Block> AIR_PLACEHOLDER = registerTimed("air_placeholder",
            () -> new Block(BlockBehaviour.Properties.of()
                    .noCollission().noLootTable().noOcclusion()));

    public static final DeferredBlock<Block> TANK_PLACEHOLDER = registerTimed("tank_placeholder",
            () -> new Block(BlockBehaviour.Properties.of()
                    .noCollission().noLootTable().noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
