/**
 * ============================================================
 * [BeemancerBlockEntities.java]
 * Description: Registre centralisé de tous les BlockEntities
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.beecreator.BeeCreatorBlockEntity;
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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
