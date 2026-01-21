/**
 * ============================================================
 * [Beemancer.java]
 * Description: Point d'entrée principal du mod Beemancer
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.GeneInit;
import com.chapeau.beemancer.core.registry.*;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(Beemancer.MOD_ID)
public class Beemancer {
    public static final String MOD_ID = "beemancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Beemancer(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Beemancer...");
        
        // Register all registries
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
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                // Initialize gene system
                GeneInit.registerAllGenes();
                Beemancer.LOGGER.info("Gene system initialized with {} genes", 
                        com.chapeau.beemancer.core.gene.GeneRegistry.getAllGenes().size());
            });
        }

        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(BeemancerEntities.MAGIC_BEE.get(), MagicBeeEntity.createAttributes().build());
        }
    }
}
