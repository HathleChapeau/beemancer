/**
 * ============================================================
 * [Beemancer.java]
 * Description: Point d'entrée principal du mod Beemancer
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation              |
 * |--------------------------|-------------------------|--------------------------|
 * | BeemancerBlocks          | Registre blocs          | Enregistrement au bus    |
 * | BeemancerItems           | Registre items          | Enregistrement au bus    |
 * | BeemancerBlockEntities   | Registre block entities | Enregistrement au bus    |
 * | BeemancerMenus           | Registre menus          | Enregistrement au bus    |
 * | BeemancerCreativeTabs    | Onglets créatifs        | Enregistrement au bus    |
 * | BeemancerEntities        | Registre entités        | Enregistrement au bus    |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Aucun (point d'entrée)
 * 
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.common.entity.bee.DebugBeeEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerCreativeTabs;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(Beemancer.MOD_ID)
public class Beemancer {
    public static final String MOD_ID = "beemancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Beemancer(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Beemancer...");
        
        // Enregistrement des registres au mod event bus
        BeemancerBlocks.register(modEventBus);
        BeemancerItems.register(modEventBus);
        BeemancerBlockEntities.register(modEventBus);
        BeemancerMenus.register(modEventBus);
        BeemancerCreativeTabs.register(modEventBus);
        BeemancerEntities.register(modEventBus);
        
        LOGGER.info("Beemancer initialized!");
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(BeemancerEntities.DEBUG_BEE.get(), DebugBeeEntity.createAttributes().build());
        }
    }
}
