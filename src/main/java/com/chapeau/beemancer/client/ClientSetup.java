/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (screens, renderers, etc.)
 * ============================================================
 */
package com.chapeau.beemancer.client;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.gui.screen.BeeCreatorScreen;
import com.chapeau.beemancer.client.gui.screen.StorageCrateScreen;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Beemancer.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BeemancerMenus.STORAGE_CRATE.get(), StorageCrateScreen::new);
        event.register(BeemancerMenus.BEE_CREATOR.get(), BeeCreatorScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeemancerEntities.MAGIC_BEE.get(), BeeRenderer::new);
    }
}
