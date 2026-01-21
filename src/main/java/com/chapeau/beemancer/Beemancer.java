/**
 * ============================================================
 * [Beemancer.java]
 * Description: Point d'entrée principal du mod Beemancer
 * ============================================================
 * 
 * Pattern: Create mod style - utilise modEventBus.addListener()
 * au lieu de @EventBusSubscriber(bus = MOD) qui est deprecated
 * 
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.client.ClientSetup;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.GeneInit;
import com.chapeau.beemancer.core.network.BeemancerNetwork;
import com.chapeau.beemancer.core.registry.*;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(Beemancer.MOD_ID)
public class Beemancer {
    public static final String MOD_ID = "beemancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Beemancer(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Beemancer...");
        
        // Register all deferred registers to mod event bus
        BeemancerBlocks.register(modEventBus);
        BeemancerItems.register(modEventBus);
        BeemancerBlockEntities.register(modEventBus);
        BeemancerMenus.register(modEventBus);
        BeemancerCreativeTabs.register(modEventBus);
        BeemancerEntities.register(modEventBus);
        
        // Register network packets
        BeemancerNetwork.register(modEventBus);
        
        // Add event listeners (Create pattern - no deprecated @EventBusSubscriber(bus=MOD))
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        
        // Client-side registration
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register(modEventBus);
        }
        
        LOGGER.info("Beemancer initialized!");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize gene system
            GeneInit.registerAllGenes();
            LOGGER.info("Gene system initialized with {} genes", 
                    com.chapeau.beemancer.core.gene.GeneRegistry.getAllGenes().size());
        });
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(BeemancerEntities.MAGIC_BEE.get(), MagicBeeEntity.createAttributes().build());
    }
}
