/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.beemancer.client;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.color.PollenColors;
import com.chapeau.beemancer.client.gui.hud.DebugPanelRenderer;
import com.chapeau.beemancer.client.gui.hud.WandOverlayRenderer;
import com.chapeau.beemancer.client.gui.screen.BeeCreatorScreen;
import com.chapeau.beemancer.client.input.DebugKeyHandler;
import com.chapeau.beemancer.client.gui.screen.IncubatorScreen;
import com.chapeau.beemancer.client.gui.screen.MagicHiveScreen;
import com.chapeau.beemancer.client.gui.screen.StorageCrateScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.*;
import com.chapeau.beemancer.client.gui.screen.storage.StorageTerminalScreen;
import com.chapeau.beemancer.client.renderer.BuildingWandPreviewRenderer;
import com.chapeau.beemancer.client.renderer.block.AltarHeartRenderer;
import com.chapeau.beemancer.client.renderer.block.BeeStatueRenderer;
import com.chapeau.beemancer.client.renderer.block.HoneyReservoirRenderer;
import com.chapeau.beemancer.client.renderer.block.StorageControllerRenderer;
import com.chapeau.beemancer.client.renderer.debug.BeeDebugRenderer;
import com.chapeau.beemancer.client.renderer.entity.MagicBeeRenderer;
import com.chapeau.beemancer.client.renderer.item.MagicBeeItemRenderer;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Supplier;

public class ClientSetup {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerScreens);
        modEventBus.addListener(ClientSetup::registerEntityRenderers);
        modEventBus.addListener(ClientSetup::registerClientExtensions);
        modEventBus.addListener(ClientSetup::registerBlockColors);
        modEventBus.addListener(ClientSetup::registerAdditionalModels);

        NeoForge.EVENT_BUS.register(WandOverlayRenderer.class);
        NeoForge.EVENT_BUS.register(BuildingWandPreviewRenderer.class);
        NeoForge.EVENT_BUS.register(DebugPanelRenderer.class);
        NeoForge.EVENT_BUS.register(DebugKeyHandler.class);
        NeoForge.EVENT_BUS.register(BeeDebugRenderer.class);
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        // Core screens
        event.register(BeemancerMenus.STORAGE_CRATE.get(), StorageCrateScreen::new);
        event.register(BeemancerMenus.STORAGE_TERMINAL.get(), StorageTerminalScreen::new);
        event.register(BeemancerMenus.BEE_CREATOR.get(), BeeCreatorScreen::new);
        event.register(BeemancerMenus.MAGIC_HIVE.get(), MagicHiveScreen::new);
        event.register(BeemancerMenus.INCUBATOR.get(), IncubatorScreen::new);
        
        // Alchemy screens
        event.register(BeemancerMenus.MANUAL_CENTRIFUGE.get(), ManualCentrifugeScreen::new);
        event.register(BeemancerMenus.POWERED_CENTRIFUGE.get(), PoweredCentrifugeScreen::new);
        event.register(BeemancerMenus.HONEY_TANK.get(), HoneyTankScreen::new);
        event.register(BeemancerMenus.CREATIVE_TANK.get(), CreativeTankScreen::new);
        event.register(BeemancerMenus.CRYSTALLIZER.get(), CrystallizerScreen::new);
        event.register(BeemancerMenus.ALEMBIC.get(), AlembicScreen::new);
        event.register(BeemancerMenus.INFUSER.get(), InfuserScreen::new);
        event.register(BeemancerMenus.MULTIBLOCK_TANK.get(), MultiblockTankScreen::new);
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeemancerEntities.MAGIC_BEE.get(), MagicBeeRenderer::new);

        // Block Entity Renderers
        event.registerBlockEntityRenderer(BeemancerBlockEntities.STORAGE_CONTROLLER.get(),
            StorageControllerRenderer::new);
        event.registerBlockEntityRenderer(BeemancerBlockEntities.BEE_STATUE.get(),
            BeeStatueRenderer::new);
        // HoneyReservoirRenderer - rendu dynamique du fluide avec scale
        event.registerBlockEntityRenderer(BeemancerBlockEntities.HONEY_RESERVOIR.get(),
            HoneyReservoirRenderer::new);
        // AltarHeartRenderer - rendu des conduits animés
        event.registerBlockEntityRenderer(BeemancerBlockEntities.ALTAR_HEART.get(),
            AltarHeartRenderer::new);
    }

    private static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        // --- Item Extensions ---
        // MagicBee item - render 3D bee model in inventory
        event.registerItem(new IClientItemExtensions() {
            private MagicBeeItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MagicBeeItemRenderer();
                }
                return renderer;
            }
        }, BeemancerItems.MAGIC_BEE.get());

        // --- Fluid Extensions ---
        // Honey - blanc (pas de tint, la texture contient déjà la couleur)
        registerFluidExtension(event, BeemancerFluids.HONEY_FLUID_TYPE,
            "block/fluid/honey_still", "block/fluid/honey_flow", 0xFFFFFFFF);

        // Royal Jelly - blanc (pas de tint, la texture contient déjà la couleur)
        registerFluidExtension(event, BeemancerFluids.ROYAL_JELLY_FLUID_TYPE,
            "block/fluid/royal_jelly_still", "block/fluid/royal_jelly_flow", 0xFFFFFFFF);

        // Nectar - blanc (pas de tint, la texture contient déjà la couleur)
        registerFluidExtension(event, BeemancerFluids.NECTAR_FLUID_TYPE,
            "block/fluid/nectar_still", "block/fluid/nectar_flow", 0xFFFFFFFF);
    }

    private static void registerFluidExtension(RegisterClientExtensionsEvent event,
            Supplier<FluidType> fluidType, String stillPath, String flowPath, int tintColor) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private final ResourceLocation stillTexture =
                ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, stillPath);
            private final ResourceLocation flowingTexture =
                ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, flowPath);

            @Override
            public ResourceLocation getStillTexture() { return stillTexture; }

            @Override
            public ResourceLocation getFlowingTexture() { return flowingTexture; }

            @Override
            public int getTintColor() { return tintColor; }
        }, fluidType.get());
    }

    // =========================================================================
    // BLOCK COLORS
    // =========================================================================

    private static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        // Pollen Pot - colore l'intérieur selon le type de pollen stocké
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFFFF;
            }

            if (level.getBlockEntity(pos) instanceof PollenPotBlockEntity pot) {
                if (pot.isEmpty()) {
                    return PollenColors.EMPTY_COLOR;
                }
                return PollenColors.getColor(pot.getPollenItem());
            }

            return PollenColors.EMPTY_COLOR;
        }, BeemancerBlocks.POLLEN_POT.get());
    }

    // =========================================================================
    // ADDITIONAL MODELS (pour le rendu dynamique)
    // =========================================================================

    private static void registerAdditionalModels(final ModelEvent.RegisterAdditional event) {
        // Modèles des anneaux du Altar Heart (rendus dynamiquement)
        event.register(ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_heart_big_ring")));
        event.register(ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "block/altar/altar_heart_small_ring")));
    }
}
