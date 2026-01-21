/**
 * ============================================================
 * [BeemancerEntities.java]
 * Description: Registre centralisé de toutes les entités du mod
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | Beemancer           | MOD_ID               | Clé du registre     |
 * | DebugBeeEntity      | Entité à enregistrer | Création du type    |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement)
 * - BeeDebugItem.java (spawn)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.core.registry;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.DebugBeeEntity;
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
    public static final Supplier<EntityType<DebugBeeEntity>> DEBUG_BEE = ENTITIES.register("debug_bee",
            () -> EntityType.Builder.of(DebugBeeEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.6F) // Même taille qu'une abeille vanilla
                    .clientTrackingRange(8)
                    .build("debug_bee"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
