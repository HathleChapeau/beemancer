/**
 * ============================================================
 * [BeemancerEntities.java]
 * Description: Registre centralisé de toutes les entités du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BeemancerEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(Registries.ENTITY_TYPE, Beemancer.MOD_ID);

    // --- BEES ---
    public static final Supplier<EntityType<MagicBeeEntity>> MAGIC_BEE = ENTITIES.register("magic_bee",
            () -> EntityType.Builder.of(MagicBeeEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.6F)
                    .clientTrackingRange(8)
                    .build("magic_bee"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
