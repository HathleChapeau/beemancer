/**
 * ============================================================
 * [ApicaBlockEntities.java]
 * Description: Registre centralisé de tous les BlockEntities
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.hive.BeeNestBlockEntity;
import com.chapeau.apica.common.block.hive.HiveMultiblockBlockEntity;
import com.chapeau.apica.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.apica.common.block.incubator.IncubatorBlockEntity;
import com.chapeau.apica.common.block.pollenpot.PollenPotBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.AlembicHeartBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.CrankBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.CreativeTankBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.CrystallizerBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.HoneyLampBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.LiquidPipeBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.InfuserBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.InfuserHeartBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.ApicaFurnaceBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.apica.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.apica.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.chapeau.apica.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.apica.common.block.resonator.ResonatorBlockEntity;
import com.chapeau.apica.common.block.api.ApiBlockEntity;
import com.chapeau.apica.common.block.statue.BeeStatueBlockEntity;
import com.chapeau.apica.common.blockentity.mount.AssemblyTableBlockEntity;
import com.chapeau.apica.common.blockentity.extractor.ExtractorHeartBlockEntity;
import com.chapeau.apica.common.blockentity.storage.ExportInterfaceBlockEntity;
import com.chapeau.apica.common.blockentity.storage.ImportInterfaceBlockEntity;
import com.chapeau.apica.common.blockentity.storage.LiquidTrashCanBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageBarrelBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.apica.common.blockentity.storage.TrashCanBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageHiveBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageRelayBlockEntity;
import com.chapeau.apica.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Apica.MOD_ID);

    public static final Supplier<BlockEntityType<StorageControllerBlockEntity>> STORAGE_CONTROLLER =
            BLOCK_ENTITIES.register("storage_controller",
                    () -> BlockEntityType.Builder.of(
                            StorageControllerBlockEntity::new,
                            ApicaBlocks.STORAGE_CONTROLLER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageRelayBlockEntity>> STORAGE_RELAY =
            BLOCK_ENTITIES.register("storage_relay",
                    () -> BlockEntityType.Builder.of(
                            StorageRelayBlockEntity::new,
                            ApicaBlocks.STORAGE_RELAY.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL =
            BLOCK_ENTITIES.register("storage_terminal",
                    () -> BlockEntityType.Builder.of(
                            StorageTerminalBlockEntity::new,
                            ApicaBlocks.STORAGE_TERMINAL.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageHiveBlockEntity>> STORAGE_HIVE =
            BLOCK_ENTITIES.register("storage_hive",
                    () -> BlockEntityType.Builder.of(
                            StorageHiveBlockEntity::new,
                            ApicaBlocks.STORAGE_HIVE.get(),
                            ApicaBlocks.STORAGE_HIVE_TIER2.get(),
                            ApicaBlocks.STORAGE_HIVE_TIER3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ImportInterfaceBlockEntity>> IMPORT_INTERFACE =
            BLOCK_ENTITIES.register("import_interface",
                    () -> BlockEntityType.Builder.of(
                            ImportInterfaceBlockEntity::new,
                            ApicaBlocks.IMPORT_INTERFACE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ExportInterfaceBlockEntity>> EXPORT_INTERFACE =
            BLOCK_ENTITIES.register("export_interface",
                    () -> BlockEntityType.Builder.of(
                            ExportInterfaceBlockEntity::new,
                            ApicaBlocks.EXPORT_INTERFACE.get()
                    ).build(null));

    // --- STORAGE BARRELS ---
    public static final Supplier<BlockEntityType<StorageBarrelBlockEntity>> STORAGE_BARREL =
            BLOCK_ENTITIES.register("storage_barrel",
                    () -> BlockEntityType.Builder.of(
                            StorageBarrelBlockEntity::new,
                            ApicaBlocks.STORAGE_BARREL_MK1.get(),
                            ApicaBlocks.STORAGE_BARREL_MK2.get(),
                            ApicaBlocks.STORAGE_BARREL_MK3.get(),
                            ApicaBlocks.STORAGE_BARREL_MK4.get()
                    ).build(null));

    // --- TRASH CANS ---
    public static final Supplier<BlockEntityType<TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITIES.register("trash_can",
                    () -> BlockEntityType.Builder.of(
                            TrashCanBlockEntity::new,
                            ApicaBlocks.TRASH_CAN.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<LiquidTrashCanBlockEntity>> LIQUID_TRASH_CAN =
            BLOCK_ENTITIES.register("liquid_trash_can",
                    () -> BlockEntityType.Builder.of(
                            LiquidTrashCanBlockEntity::new,
                            ApicaBlocks.LIQUID_TRASH_CAN.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<MagicHiveBlockEntity>> MAGIC_HIVE =
            BLOCK_ENTITIES.register("magic_hive",
                    () -> BlockEntityType.Builder.of(
                            MagicHiveBlockEntity::new,
                            ApicaBlocks.MAGIC_HIVE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HiveMultiblockBlockEntity>> HIVE_MULTIBLOCK =
            BLOCK_ENTITIES.register("hive_multiblock",
                    () -> BlockEntityType.Builder.of(
                            HiveMultiblockBlockEntity::new,
                            ApicaBlocks.HIVE_MULTIBLOCK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<IncubatorBlockEntity>> INCUBATOR = 
            BLOCK_ENTITIES.register("incubator",
                    () -> BlockEntityType.Builder.of(
                            IncubatorBlockEntity::new,
                            ApicaBlocks.INCUBATOR.get()
                    ).build(null));

    // --- ALCHEMY MACHINES ---
    public static final Supplier<BlockEntityType<ManualCentrifugeBlockEntity>> MANUAL_CENTRIFUGE =
            BLOCK_ENTITIES.register("manual_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            ManualCentrifugeBlockEntity::new,
                            ApicaBlocks.MANUAL_CENTRIFUGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CrankBlockEntity>> CRANK =
            BLOCK_ENTITIES.register("crank",
                    () -> BlockEntityType.Builder.of(
                            CrankBlockEntity::new,
                            ApicaBlocks.CRANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE =
            BLOCK_ENTITIES.register("powered_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::new,
                            ApicaBlocks.POWERED_CENTRIFUGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE_TIER2 =
            BLOCK_ENTITIES.register("powered_centrifuge_tier2",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::createTier2,
                            ApicaBlocks.POWERED_CENTRIFUGE_TIER2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyTankBlockEntity>> HONEY_TANK =
            BLOCK_ENTITIES.register("honey_tank",
                    () -> BlockEntityType.Builder.of(
                            HoneyTankBlockEntity::new,
                            ApicaBlocks.HONEY_TANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CreativeTankBlockEntity>> CREATIVE_TANK =
            BLOCK_ENTITIES.register("creative_tank",
                    () -> BlockEntityType.Builder.of(
                            CreativeTankBlockEntity::new,
                            ApicaBlocks.CREATIVE_TANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<LiquidPipeBlockEntity>> LIQUID_PIPE =
            BLOCK_ENTITIES.register("liquid_pipe",
                    () -> BlockEntityType.Builder.of(
                            LiquidPipeBlockEntity::new,
                            ApicaBlocks.LIQUID_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<LiquidPipeBlockEntity>> LIQUID_PIPE_MK2 =
            BLOCK_ENTITIES.register("liquid_pipe_mk2",
                    () -> BlockEntityType.Builder.of(
                            LiquidPipeBlockEntity::createMk2,
                            ApicaBlocks.LIQUID_PIPE_MK2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<LiquidPipeBlockEntity>> LIQUID_PIPE_MK3 =
            BLOCK_ENTITIES.register("liquid_pipe_mk3",
                    () -> BlockEntityType.Builder.of(
                            LiquidPipeBlockEntity::createMk3,
                            ApicaBlocks.LIQUID_PIPE_MK3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<LiquidPipeBlockEntity>> LIQUID_PIPE_MK4 =
            BLOCK_ENTITIES.register("liquid_pipe_mk4",
                    () -> BlockEntityType.Builder.of(
                            LiquidPipeBlockEntity::createMk4,
                            ApicaBlocks.LIQUID_PIPE_MK4.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::new,
                            ApicaBlocks.ITEM_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_MK2 =
            BLOCK_ENTITIES.register("item_pipe_mk2",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createMk2,
                            ApicaBlocks.ITEM_PIPE_MK2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_MK3 =
            BLOCK_ENTITIES.register("item_pipe_mk3",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createMk3,
                            ApicaBlocks.ITEM_PIPE_MK3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_MK4 =
            BLOCK_ENTITIES.register("item_pipe_mk4",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createMk4,
                            ApicaBlocks.ITEM_PIPE_MK4.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER = 
            BLOCK_ENTITIES.register("crystallizer",
                    () -> BlockEntityType.Builder.of(
                            CrystallizerBlockEntity::new,
                            ApicaBlocks.CRYSTALLIZER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<InfuserBlockEntity>> INFUSER =
            BLOCK_ENTITIES.register("infuser",
                    () -> BlockEntityType.Builder.of(
                            InfuserBlockEntity::new,
                            ApicaBlocks.INFUSER.get()
                    ).build(null));

    // public static final Supplier<BlockEntityType<InfuserBlockEntity>> INFUSER_TIER2 =
    //         BLOCK_ENTITIES.register("infuser_tier2",
    //                 () -> BlockEntityType.Builder.of(
    //                         InfuserBlockEntity::createTier2,
    //                         ApicaBlocks.INFUSER_TIER2.get()
    //                 ).build(null));

    public static final Supplier<BlockEntityType<HoneyLampBlockEntity>> HONEY_LAMP =
            BLOCK_ENTITIES.register("honey_lamp",
                    () -> BlockEntityType.Builder.of(
                            HoneyLampBlockEntity::new,
                            ApicaBlocks.HONEY_LAMP.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<MultiblockTankBlockEntity>> MULTIBLOCK_TANK =
            BLOCK_ENTITIES.register("multiblock_tank",
                    () -> BlockEntityType.Builder.of(
                            MultiblockTankBlockEntity::new,
                            ApicaBlocks.MULTIBLOCK_TANK.get()
                    ).build(null));

    // --- HONEY ALTAR ---
    public static final Supplier<BlockEntityType<HoneyReservoirBlockEntity>> HONEY_RESERVOIR =
            BLOCK_ENTITIES.register("honey_reservoir",
                    () -> BlockEntityType.Builder.of(
                            HoneyReservoirBlockEntity::new,
                            ApicaBlocks.HONEY_RESERVOIR.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<AltarHeartBlockEntity>> ALTAR_HEART =
            BLOCK_ENTITIES.register("altar_heart",
                    () -> BlockEntityType.Builder.of(
                            AltarHeartBlockEntity::new,
                            ApicaBlocks.ALTAR_HEART.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPedestalBlockEntity>> HONEY_PEDESTAL =
            BLOCK_ENTITIES.register("honey_pedestal",
                    () -> BlockEntityType.Builder.of(
                            HoneyPedestalBlockEntity::new,
                            ApicaBlocks.HONEY_PEDESTAL.get()
                    ).build(null));

    // --- POLLEN POT ---
    public static final Supplier<BlockEntityType<PollenPotBlockEntity>> POLLEN_POT =
            BLOCK_ENTITIES.register("pollen_pot",
                    () -> BlockEntityType.Builder.of(
                            PollenPotBlockEntity::new,
                            ApicaBlocks.POLLEN_POT.get()
                    ).build(null));

    // --- ESSENCE INJECTOR ---
    public static final Supplier<BlockEntityType<com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity>> INJECTOR =
            BLOCK_ENTITIES.register("injector",
                    () -> BlockEntityType.Builder.of(
                            com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity::new,
                            ApicaBlocks.INJECTOR.get()
                    ).build(null));

    // --- ESSENCE EXTRACTOR ---
    public static final Supplier<BlockEntityType<ExtractorHeartBlockEntity>> EXTRACTOR_HEART =
            BLOCK_ENTITIES.register("extractor_heart",
                    () -> BlockEntityType.Builder.of(
                            ExtractorHeartBlockEntity::new,
                            ApicaBlocks.EXTRACTOR_HEART.get()
                    ).build(null));

    // --- ALCHEMY HEARTS ---
    public static final Supplier<BlockEntityType<AlembicHeartBlockEntity>> ALEMBIC_HEART =
            BLOCK_ENTITIES.register("alembic_heart",
                    () -> BlockEntityType.Builder.of(
                            AlembicHeartBlockEntity::new,
                            ApicaBlocks.ALEMBIC_HEART.get()
                    ).build(null));

    // public static final Supplier<BlockEntityType<InfuserHeartBlockEntity>> INFUSER_HEART =
    //         BLOCK_ENTITIES.register("infuser_heart",
    //                 () -> BlockEntityType.Builder.of(
    //                         InfuserHeartBlockEntity::new,
    //                         ApicaBlocks.INFUSER_HEART.get()
    //                 ).build(null));

    public static final Supplier<BlockEntityType<CentrifugeHeartBlockEntity>> CENTRIFUGE_HEART =
            BLOCK_ENTITIES.register("centrifuge_heart",
                    () -> BlockEntityType.Builder.of(
                            CentrifugeHeartBlockEntity::new,
                            ApicaBlocks.CENTRIFUGE_HEART.get()
                    ).build(null));

    // --- APICA FURNACES ---
    public static final Supplier<BlockEntityType<ApicaFurnaceBlockEntity>> HONEY_FURNACE =
            BLOCK_ENTITIES.register("honey_furnace",
                    () -> BlockEntityType.Builder.of(
                            ApicaFurnaceBlockEntity::new,
                            ApicaBlocks.HONEY_FURNACE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ApicaFurnaceBlockEntity>> ROYAL_FURNACE =
            BLOCK_ENTITIES.register("royal_furnace",
                    () -> BlockEntityType.Builder.of(
                            ApicaFurnaceBlockEntity::createRoyal,
                            ApicaBlocks.ROYAL_FURNACE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ApicaFurnaceBlockEntity>> NECTAR_FURNACE =
            BLOCK_ENTITIES.register("nectar_furnace",
                    () -> BlockEntityType.Builder.of(
                            ApicaFurnaceBlockEntity::createNectar,
                            ApicaBlocks.NECTAR_FURNACE.get()
                    ).build(null));

    // --- BEE NEST ---
    public static final Supplier<BlockEntityType<BeeNestBlockEntity>> BEE_NEST =
            BLOCK_ENTITIES.register("bee_nest",
                    () -> BlockEntityType.Builder.of(
                            BeeNestBlockEntity::new,
                            ApicaBlocks.BEE_NEST.get()
                    ).build(null));

    // --- ASSEMBLY TABLE ---
    public static final Supplier<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE =
            BLOCK_ENTITIES.register("assembly_table",
                    () -> BlockEntityType.Builder.of(
                            AssemblyTableBlockEntity::new,
                            ApicaBlocks.ASSEMBLY_TABLE.get()
                    ).build(null));

    // --- RESONATOR ---
    public static final Supplier<BlockEntityType<ResonatorBlockEntity>> RESONATOR =
            BLOCK_ENTITIES.register("resonator",
                    () -> BlockEntityType.Builder.of(
                            ResonatorBlockEntity::new,
                            ApicaBlocks.RESONATOR.get()
                    ).build(null));

    // --- BEE STATUE ---
    public static final Supplier<BlockEntityType<BeeStatueBlockEntity>> BEE_STATUE =
            BLOCK_ENTITIES.register("bee_statue",
                    () -> BlockEntityType.Builder.of(
                            BeeStatueBlockEntity::new,
                            ApicaBlocks.BEE_STATUE.get()
                    ).build(null));

    // --- API ---
    public static final Supplier<BlockEntityType<ApiBlockEntity>> API =
            BLOCK_ENTITIES.register("api",
                    () -> BlockEntityType.Builder.of(
                            ApiBlockEntity::new,
                            ApicaBlocks.API.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
