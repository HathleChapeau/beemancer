/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (pattern Create)
 * ============================================================
 */
package com.chapeau.beemancer.client;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.screen.BeeCreatorScreen;
import com.chapeau.beemancer.client.gui.screen.StorageCrateScreen;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientSetup {

    /**
     * Register client setup to mod event bus (called from main mod class)
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerScreens);
        modEventBus.addListener(ClientSetup::registerEntityRenderers);
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(BeemancerMenus.STORAGE_CRATE.get(), StorageCrateScreen::new);
        event.register(BeemancerMenus.BEE_CREATOR.get(), BeeCreatorScreen::new);
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeemancerEntities.MAGIC_BEE.get(), BeeRenderer::new);
    }
}
