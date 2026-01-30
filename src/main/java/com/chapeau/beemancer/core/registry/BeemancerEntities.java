/**
 * ============================================================
 * [BeemancerEntities.java]
 * Description: Registre centralisé de toutes les entités du mod
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
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

    // --- DELIVERY ---
    public static final Supplier<EntityType<DeliveryBeeEntity>> DELIVERY_BEE = ENTITIES.register("delivery_bee",
            () -> EntityType.Builder.of(DeliveryBeeEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.6F)
                    .clientTrackingRange(8)
                    .build("delivery_bee"));

    // --- MOUNTS ---
    // Abeille chevauchable (scale 1.5x visuellement)
    // Hitbox: ~1.05 largeur, ~0.9 hauteur
    public static final Supplier<EntityType<RideableBeeEntity>> RIDEABLE_BEE = ENTITIES.register("rideable_bee",
            () -> EntityType.Builder.of(RideableBeeEntity::new, MobCategory.CREATURE)
                    .sized(1.05F, 0.9F)
                    .clientTrackingRange(10)
                    .build("rideable_bee"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
