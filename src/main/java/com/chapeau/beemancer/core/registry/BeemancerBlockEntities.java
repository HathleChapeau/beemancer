/**
 * ============================================================
 * [BeemancerBlockEntities.java]
 * Description: Registre centralisé de tous les BlockEntities
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.beemancer.common.block.hive.HiveMultiblockBlockEntity;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.*;
import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.common.blockentity.altar.HoneyCrystalBlockEntity;
import com.chapeau.beemancer.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.common.block.statue.BeeStatueBlockEntity;
import com.chapeau.beemancer.common.blockentity.extractor.ExtractorHeartBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.ControllerPipeBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageCrateBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageRelayBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Beemancer.MOD_ID);

    public static final Supplier<BlockEntityType<StorageCrateBlockEntity>> STORAGE_CRATE =
            BLOCK_ENTITIES.register("storage_crate",
                    () -> BlockEntityType.Builder.of(
                            StorageCrateBlockEntity::new,
                            BeemancerBlocks.STORAGE_CRATE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageControllerBlockEntity>> STORAGE_CONTROLLER =
            BLOCK_ENTITIES.register("storage_controller",
                    () -> BlockEntityType.Builder.of(
                            StorageControllerBlockEntity::new,
                            BeemancerBlocks.STORAGE_CONTROLLER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageRelayBlockEntity>> STORAGE_RELAY =
            BLOCK_ENTITIES.register("storage_relay",
                    () -> BlockEntityType.Builder.of(
                            StorageRelayBlockEntity::new,
                            BeemancerBlocks.STORAGE_RELAY.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL =
            BLOCK_ENTITIES.register("storage_terminal",
                    () -> BlockEntityType.Builder.of(
                            StorageTerminalBlockEntity::new,
                            BeemancerBlocks.STORAGE_TERMINAL.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ControllerPipeBlockEntity>> CONTROLLER_PIPE =
            BLOCK_ENTITIES.register("controller_pipe",
                    () -> BlockEntityType.Builder.of(
                            ControllerPipeBlockEntity::new,
                            BeemancerBlocks.CONTROLLER_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<BeeCreatorBlockEntity>> BEE_CREATOR = 
            BLOCK_ENTITIES.register("bee_creator",
                    () -> BlockEntityType.Builder.of(
                            BeeCreatorBlockEntity::new,
                            BeemancerBlocks.BEE_CREATOR.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<MagicHiveBlockEntity>> MAGIC_HIVE =
            BLOCK_ENTITIES.register("magic_hive",
                    () -> BlockEntityType.Builder.of(
                            MagicHiveBlockEntity::new,
                            BeemancerBlocks.MAGIC_HIVE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HiveMultiblockBlockEntity>> HIVE_MULTIBLOCK =
            BLOCK_ENTITIES.register("hive_multiblock",
                    () -> BlockEntityType.Builder.of(
                            HiveMultiblockBlockEntity::new,
                            BeemancerBlocks.HIVE_MULTIBLOCK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<IncubatorBlockEntity>> INCUBATOR = 
            BLOCK_ENTITIES.register("incubator",
                    () -> BlockEntityType.Builder.of(
                            IncubatorBlockEntity::new,
                            BeemancerBlocks.INCUBATOR.get()
                    ).build(null));

    // --- ALCHEMY MACHINES ---
    public static final Supplier<BlockEntityType<ManualCentrifugeBlockEntity>> MANUAL_CENTRIFUGE =
            BLOCK_ENTITIES.register("manual_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            ManualCentrifugeBlockEntity::new,
                            BeemancerBlocks.MANUAL_CENTRIFUGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CrankBlockEntity>> CRANK =
            BLOCK_ENTITIES.register("crank",
                    () -> BlockEntityType.Builder.of(
                            CrankBlockEntity::new,
                            BeemancerBlocks.CRANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE =
            BLOCK_ENTITIES.register("powered_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::new,
                            BeemancerBlocks.POWERED_CENTRIFUGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE_TIER2 =
            BLOCK_ENTITIES.register("powered_centrifuge_tier2",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::createTier2,
                            BeemancerBlocks.POWERED_CENTRIFUGE_TIER2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE_TIER3 =
            BLOCK_ENTITIES.register("powered_centrifuge_tier3",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::createTier3,
                            BeemancerBlocks.POWERED_CENTRIFUGE_TIER3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyTankBlockEntity>> HONEY_TANK =
            BLOCK_ENTITIES.register("honey_tank",
                    () -> BlockEntityType.Builder.of(
                            HoneyTankBlockEntity::new,
                            BeemancerBlocks.HONEY_TANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CreativeTankBlockEntity>> CREATIVE_TANK =
            BLOCK_ENTITIES.register("creative_tank",
                    () -> BlockEntityType.Builder.of(
                            CreativeTankBlockEntity::new,
                            BeemancerBlocks.CREATIVE_TANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPipeBlockEntity>> HONEY_PIPE =
            BLOCK_ENTITIES.register("honey_pipe",
                    () -> BlockEntityType.Builder.of(
                            HoneyPipeBlockEntity::new,
                            BeemancerBlocks.HONEY_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPipeBlockEntity>> HONEY_PIPE_TIER2 =
            BLOCK_ENTITIES.register("honey_pipe_tier2",
                    () -> BlockEntityType.Builder.of(
                            HoneyPipeBlockEntity::createTier2,
                            BeemancerBlocks.HONEY_PIPE_TIER2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPipeBlockEntity>> HONEY_PIPE_TIER3 =
            BLOCK_ENTITIES.register("honey_pipe_tier3",
                    () -> BlockEntityType.Builder.of(
                            HoneyPipeBlockEntity::createTier3,
                            BeemancerBlocks.HONEY_PIPE_TIER3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPipeBlockEntity>> HONEY_PIPE_TIER4 =
            BLOCK_ENTITIES.register("honey_pipe_tier4",
                    () -> BlockEntityType.Builder.of(
                            HoneyPipeBlockEntity::createTier4,
                            BeemancerBlocks.HONEY_PIPE_TIER4.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::new,
                            BeemancerBlocks.ITEM_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_TIER2 =
            BLOCK_ENTITIES.register("item_pipe_tier2",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createTier2,
                            BeemancerBlocks.ITEM_PIPE_TIER2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_TIER3 =
            BLOCK_ENTITIES.register("item_pipe_tier3",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createTier3,
                            BeemancerBlocks.ITEM_PIPE_TIER3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE_TIER4 =
            BLOCK_ENTITIES.register("item_pipe_tier4",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::createTier4,
                            BeemancerBlocks.ITEM_PIPE_TIER4.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER = 
            BLOCK_ENTITIES.register("crystallizer",
                    () -> BlockEntityType.Builder.of(
                            CrystallizerBlockEntity::new,
                            BeemancerBlocks.CRYSTALLIZER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<AlembicBlockEntity>> ALEMBIC = 
            BLOCK_ENTITIES.register("alembic",
                    () -> BlockEntityType.Builder.of(
                            AlembicBlockEntity::new,
                            BeemancerBlocks.ALEMBIC.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<InfuserBlockEntity>> INFUSER =
            BLOCK_ENTITIES.register("infuser",
                    () -> BlockEntityType.Builder.of(
                            InfuserBlockEntity::new,
                            BeemancerBlocks.INFUSER.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<InfuserBlockEntity>> INFUSER_TIER2 =
            BLOCK_ENTITIES.register("infuser_tier2",
                    () -> BlockEntityType.Builder.of(
                            InfuserBlockEntity::createTier2,
                            BeemancerBlocks.INFUSER_TIER2.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<InfuserBlockEntity>> INFUSER_TIER3 =
            BLOCK_ENTITIES.register("infuser_tier3",
                    () -> BlockEntityType.Builder.of(
                            InfuserBlockEntity::createTier3,
                            BeemancerBlocks.INFUSER_TIER3.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<MultiblockTankBlockEntity>> MULTIBLOCK_TANK =
            BLOCK_ENTITIES.register("multiblock_tank",
                    () -> BlockEntityType.Builder.of(
                            MultiblockTankBlockEntity::new,
                            BeemancerBlocks.MULTIBLOCK_TANK.get()
                    ).build(null));

    // --- HONEY ALTAR ---
    public static final Supplier<BlockEntityType<HoneyCrystalBlockEntity>> HONEY_CRYSTAL =
            BLOCK_ENTITIES.register("honey_crystal",
                    () -> BlockEntityType.Builder.of(
                            HoneyCrystalBlockEntity::new,
                            BeemancerBlocks.HONEY_CRYSTAL.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyReservoirBlockEntity>> HONEY_RESERVOIR =
            BLOCK_ENTITIES.register("honey_reservoir",
                    () -> BlockEntityType.Builder.of(
                            HoneyReservoirBlockEntity::new,
                            BeemancerBlocks.HONEY_RESERVOIR.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<AltarHeartBlockEntity>> ALTAR_HEART =
            BLOCK_ENTITIES.register("altar_heart",
                    () -> BlockEntityType.Builder.of(
                            AltarHeartBlockEntity::new,
                            BeemancerBlocks.ALTAR_HEART.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPedestalBlockEntity>> HONEY_PEDESTAL =
            BLOCK_ENTITIES.register("honey_pedestal",
                    () -> BlockEntityType.Builder.of(
                            HoneyPedestalBlockEntity::new,
                            BeemancerBlocks.HONEY_PEDESTAL.get()
                    ).build(null));

    // --- POLLEN POT ---
    public static final Supplier<BlockEntityType<PollenPotBlockEntity>> POLLEN_POT =
            BLOCK_ENTITIES.register("pollen_pot",
                    () -> BlockEntityType.Builder.of(
                            PollenPotBlockEntity::new,
                            BeemancerBlocks.POLLEN_POT.get()
                    ).build(null));

    // --- ESSENCE EXTRACTOR ---
    public static final Supplier<BlockEntityType<ExtractorHeartBlockEntity>> EXTRACTOR_HEART =
            BLOCK_ENTITIES.register("extractor_heart",
                    () -> BlockEntityType.Builder.of(
                            ExtractorHeartBlockEntity::new,
                            BeemancerBlocks.EXTRACTOR_HEART.get()
                    ).build(null));

    // --- BEE STATUE ---
    public static final Supplier<BlockEntityType<BeeStatueBlockEntity>> BEE_STATUE =
            BLOCK_ENTITIES.register("bee_statue",
                    () -> BlockEntityType.Builder.of(
                            BeeStatueBlockEntity::new,
                            BeemancerBlocks.BEE_STATUE.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
