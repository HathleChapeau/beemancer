/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (screens, renderers, etc.)
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation          |
 * |---------------------|----------------------|----------------------|
 * | BeemancerMenus      | Types de menus       | Liaison menu-screen  |
 * | BeemancerEntities   | Types d'entités      | Liaison entity-render|
 * | StorageCrateScreen  | Screen à enregistrer | Affichage GUI        |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Beemancer.java (événement client)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.client;

import com.chapeau.beemancer.Beemancer;
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
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Utiliser le renderer vanilla de Bee pour notre DebugBee
        event.registerEntityRenderer(BeemancerEntities.DEBUG_BEE.get(), BeeRenderer::new);
    }
}
