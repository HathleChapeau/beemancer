/**
 * ============================================================
 * [BeemancerBlockEntities.java]
 * Description: Registre centralisé de tous les BlockEntities
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.beemancer.common.block.hive.MagicHiveBlockEntity;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.*;
import com.chapeau.beemancer.common.blockentity.storage.StorageCrateBlockEntity;
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

    public static final Supplier<BlockEntityType<PoweredCentrifugeBlockEntity>> POWERED_CENTRIFUGE = 
            BLOCK_ENTITIES.register("powered_centrifuge",
                    () -> BlockEntityType.Builder.of(
                            PoweredCentrifugeBlockEntity::new,
                            BeemancerBlocks.POWERED_CENTRIFUGE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyTankBlockEntity>> HONEY_TANK = 
            BLOCK_ENTITIES.register("honey_tank",
                    () -> BlockEntityType.Builder.of(
                            HoneyTankBlockEntity::new,
                            BeemancerBlocks.HONEY_TANK.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<HoneyPipeBlockEntity>> HONEY_PIPE = 
            BLOCK_ENTITIES.register("honey_pipe",
                    () -> BlockEntityType.Builder.of(
                            HoneyPipeBlockEntity::new,
                            BeemancerBlocks.HONEY_PIPE.get()
                    ).build(null));

    public static final Supplier<BlockEntityType<NectarFilterBlockEntity>> NECTAR_FILTER = 
            BLOCK_ENTITIES.register("nectar_filter",
                    () -> BlockEntityType.Builder.of(
                            NectarFilterBlockEntity::new,
                            BeemancerBlocks.NECTAR_FILTER.get()
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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
