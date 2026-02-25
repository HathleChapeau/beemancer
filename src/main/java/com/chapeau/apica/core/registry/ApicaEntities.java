/**
 * ============================================================
 * [ApicaEntities.java]
 * Description: Registre centralisé de toutes les entités du mod
 * ============================================================
 */
package com.chapeau.apica.core.registry;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import com.chapeau.apica.core.entity.InteractionMarkerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ApicaEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(Registries.ENTITY_TYPE, Apica.MOD_ID);

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
    public static final Supplier<EntityType<HoverbikeEntity>> HOVERBIKE = ENTITIES.register("hoverbike",
            () -> EntityType.Builder.of(HoverbikeEntity::new, MobCategory.MISC)
                    .sized(1.2F, 1.0F)
                    .clientTrackingRange(10)
                    .build("hoverbike"));

    // --- PROJECTILES ---
    public static final Supplier<EntityType<LeafBlowerProjectileEntity>> LEAF_BLOWER_ORB =
            ENTITIES.register("leaf_blower_orb",
                    () -> EntityType.Builder.<LeafBlowerProjectileEntity>of(LeafBlowerProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("leaf_blower_orb"));

    // --- INTERACTION MARKERS ---
    public static final Supplier<EntityType<InteractionMarkerEntity>> INTERACTION_MARKER =
            ENTITIES.register("interaction_marker",
                    () -> EntityType.Builder.of(InteractionMarkerEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .noSave()
                            .fireImmune()
                            .clientTrackingRange(8)
                            .build("interaction_marker"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
