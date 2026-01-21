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
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Aucun (point d'entrée)
 * 
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerCreativeTabs;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
        
        LOGGER.info("Beemancer initialized!");
    }
}
