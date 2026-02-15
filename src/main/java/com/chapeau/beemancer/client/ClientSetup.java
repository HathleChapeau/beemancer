/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.beemancer.client;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.client.camera.HoverbikeCameraController;
import com.chapeau.beemancer.client.gui.hud.DebugPanelRenderer;
import com.chapeau.beemancer.client.gui.hud.HoverbikeDebugHud;
import com.chapeau.beemancer.client.gui.hud.HoverbikeGaugeHud;
import com.chapeau.beemancer.client.model.HoverbikeModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.beemancer.client.renderer.entity.HoverbikeRenderer;
import com.chapeau.beemancer.client.input.DebugKeyHandler;
import com.chapeau.beemancer.client.gui.screen.IncubatorScreen;
import com.chapeau.beemancer.client.gui.screen.MagicHiveScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.AlembicScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.CreativeTankScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.CrystallizerScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.HoneyTankScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.InfuserScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.ManualCentrifugeScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.MultiblockTankScreen;
import com.chapeau.beemancer.client.gui.screen.alchemy.PoweredCentrifugeScreen;
import com.chapeau.beemancer.client.gui.screen.storage.NetworkInterfaceScreen;
import com.chapeau.beemancer.client.gui.screen.storage.StorageTerminalScreen;
import com.chapeau.beemancer.client.renderer.BuildingWandPreviewRenderer;
import com.chapeau.beemancer.client.renderer.block.AssemblyTableRenderer;
import com.chapeau.beemancer.client.renderer.block.AltarHeartRenderer;
import com.chapeau.beemancer.client.renderer.block.BeeStatueRenderer;
import com.chapeau.beemancer.client.renderer.block.HoneyPedestalRenderer;
import com.chapeau.beemancer.client.renderer.block.HoneyReservoirRenderer;
import com.chapeau.beemancer.client.renderer.block.CentrifugeHeartRenderer;
import com.chapeau.beemancer.client.renderer.block.CrankRenderer;
import com.chapeau.beemancer.client.renderer.block.InfuserHeartRenderer;
import com.chapeau.beemancer.client.renderer.block.CrystallizerRenderer;
import com.chapeau.beemancer.client.renderer.block.InfuserRenderer;
import com.chapeau.beemancer.client.renderer.block.HoneyTankRenderer;
import com.chapeau.beemancer.client.renderer.block.IncubatorRenderer;
import com.chapeau.beemancer.client.particle.HoneyPixelParticle;
import com.chapeau.beemancer.client.particle.RuneParticle;
import com.chapeau.beemancer.core.registry.BeemancerParticles;
import com.chapeau.beemancer.client.renderer.block.MultiblockTankRenderer;
import com.chapeau.beemancer.client.renderer.block.PollenPotRenderer;
import com.chapeau.beemancer.client.renderer.block.StorageControllerRenderer;
import com.chapeau.beemancer.client.renderer.block.StorageTerminalRenderer;
import com.chapeau.beemancer.client.renderer.block.TranslucentOutlineRenderer;
import com.chapeau.beemancer.client.renderer.debug.BeeDebugRenderer;
import com.chapeau.beemancer.client.renderer.debug.CustomDebugDisplayRenderer;
import com.chapeau.beemancer.client.visual.FlywheelTestBeeVisualizer;
import com.chapeau.beemancer.client.renderer.entity.MagicBeeRenderer;
import com.chapeau.beemancer.client.renderer.entity.DeliveryBeeRenderer;
import com.chapeau.beemancer.client.renderer.item.MagicBeeItemRenderer;
import com.chapeau.beemancer.common.blockentity.alchemy.HoneyPipeBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.ItemPipeBlockEntity;
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
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
        modEventBus.addListener(ClientSetup::registerLayerDefinitions);
        modEventBus.addListener(ClientSetup::registerClientExtensions);
        modEventBus.addListener(ClientSetup::registerBlockColors);
        modEventBus.addListener(ClientSetup::registerAdditionalModels);
        modEventBus.addListener(ClientSetup::registerParticleProviders);
        modEventBus.addListener(ClientSetup::onClientSetup);

        NeoForge.EVENT_BUS.register(BuildingWandPreviewRenderer.class);
        NeoForge.EVENT_BUS.register(DebugPanelRenderer.class);
        NeoForge.EVENT_BUS.register(DebugKeyHandler.class);
        NeoForge.EVENT_BUS.register(BeeDebugRenderer.class);

        // Debug renderers (quand displayDebug = true)
        NeoForge.EVENT_BUS.register(CustomDebugDisplayRenderer.class);

        // Hoverbike system
        NeoForge.EVENT_BUS.register(HoverbikeDebugHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeGaugeHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeCameraController.class);
        NeoForge.EVENT_BUS.register(HoverbikeEditModeEffect.class);
        NeoForge.EVENT_BUS.register(com.chapeau.beemancer.client.gui.hud.HoverbikeEditModeHandler.class);
        NeoForge.EVENT_BUS.register(com.chapeau.beemancer.client.gui.hud.HoverbikeEditStatsHud.class);

        // Overlay quand on ouvre un GUI adjacent depuis le bouton Debug de l'interface
        NeoForge.EVENT_BUS.register(com.chapeau.beemancer.client.gui.screen.storage.AdjacentGuiOverlayRenderer.class);

        // Fix outline translucent pour blocs comme le Crystallizer
        NeoForge.EVENT_BUS.register(TranslucentOutlineRenderer.class);


        // Debug Opti
        NeoForge.EVENT_BUS.register(RenderCostVisualizer.class);
        NeoForge.EVENT_BUS.register(LogicCostVisualizer.class);
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        // Core screens
        event.register(BeemancerMenus.STORAGE_TERMINAL.get(), StorageTerminalScreen::new);
        event.register(BeemancerMenus.NETWORK_INTERFACE.get(), NetworkInterfaceScreen::new);
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
        event.registerEntityRenderer(BeemancerEntities.DELIVERY_BEE.get(), DeliveryBeeRenderer::new);
        event.registerEntityRenderer(BeemancerEntities.HOVERBIKE.get(), HoverbikeRenderer::new);
        // Flywheel test bee - noop renderer (Flywheel handles rendering)
        event.registerEntityRenderer(BeemancerEntities.FLYWHEEL_TEST_BEE.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);

        // Block Entity Renderers
        event.registerBlockEntityRenderer(BeemancerBlockEntities.STORAGE_CONTROLLER.get(),
            StorageControllerRenderer::new);
        event.registerBlockEntityRenderer(BeemancerBlockEntities.STORAGE_RELAY.get(),
            com.chapeau.beemancer.client.renderer.block.StorageRelayRenderer::new);
        event.registerBlockEntityRenderer(BeemancerBlockEntities.STORAGE_TERMINAL.get(),
            StorageTerminalRenderer::new);
        event.registerBlockEntityRenderer(BeemancerBlockEntities.BEE_STATUE.get(),
            BeeStatueRenderer::new);
        // HoneyReservoirRenderer - rendu dynamique du fluide avec scale
        event.registerBlockEntityRenderer(BeemancerBlockEntities.HONEY_RESERVOIR.get(),
            HoneyReservoirRenderer::new);
        // AltarHeartRenderer - rendu des conduits animés
        event.registerBlockEntityRenderer(BeemancerBlockEntities.ALTAR_HEART.get(),
            AltarHeartRenderer::new);
        // HoneyPedestalRenderer - item flottant au-dessus du pedestal
        event.registerBlockEntityRenderer(BeemancerBlockEntities.HONEY_PEDESTAL.get(),
            HoneyPedestalRenderer::new);
        // CrankRenderer - rotation quand la centrifugeuse tourne
        event.registerBlockEntityRenderer(BeemancerBlockEntities.CRANK.get(),
            CrankRenderer::new);
        // CrystallizerRenderer - cores animés (rotation + scale)
        event.registerBlockEntityRenderer(BeemancerBlockEntities.CRYSTALLIZER.get(),
            CrystallizerRenderer::new);
        // InfuserRenderer - item flottant + particules miel
        event.registerBlockEntityRenderer(BeemancerBlockEntities.INFUSER.get(),
            InfuserRenderer::new);
        event.registerBlockEntityRenderer(BeemancerBlockEntities.INFUSER_TIER2.get(),
            InfuserRenderer::new);
        // HoneyTankRenderer - fluide dynamique
        event.registerBlockEntityRenderer(BeemancerBlockEntities.HONEY_TANK.get(),
            HoneyTankRenderer::new);
        // MultiblockTankRenderer - fluide dynamique multi-bloc
        event.registerBlockEntityRenderer(BeemancerBlockEntities.MULTIBLOCK_TANK.get(),
            MultiblockTankRenderer::new);
        // PollenPotRenderer - cube texturé avec hauteur dynamique selon remplissage
        event.registerBlockEntityRenderer(BeemancerBlockEntities.POLLEN_POT.get(),
            PollenPotRenderer::new);
        // IncubatorRenderer - item flottant au centre de l'incubateur
        event.registerBlockEntityRenderer(BeemancerBlockEntities.INCUBATOR.get(),
            IncubatorRenderer::new);
        // AssemblyTableRenderer - piece de moto sur la table
        event.registerBlockEntityRenderer(BeemancerBlockEntities.ASSEMBLY_TABLE.get(),
            AssemblyTableRenderer::new);
        // CentrifugeHeartRenderer - cubes centraux animés de la centrifuge multibloc
        event.registerBlockEntityRenderer(BeemancerBlockEntities.CENTRIFUGE_HEART.get(),
            CentrifugeHeartRenderer::new);
        // InfuserHeartRenderer - cubes centraux statiques de l'infuser multibloc
        event.registerBlockEntityRenderer(BeemancerBlockEntities.INFUSER_HEART.get(),
            InfuserHeartRenderer::new);
    }

    private static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Hoverbike — modele de base
        event.registerLayerDefinition(HoverbikeRenderer.LAYER_LOCATION, HoverbikeModel::createBodyLayer);

        // Hoverbike — parties modulaires (toutes variantes, enregistrees dynamiquement)
        for (HoverbikePartVariants.ModelFactory factory : HoverbikePartVariants.getAllModelFactories().values()) {
            event.registerLayerDefinition(factory.layerLocation(), factory.layerDefinition());
        }
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
        registerFluidExtension(event, BeemancerFluids.HONEY_FLUID_TYPE,
            //"block/fluid/honey_still", "block/fluid/honey_flow", 0xFFFFFFFF);
            "block/t", "block/t2", 0xFFE8A317);

        registerFluidExtension(event, BeemancerFluids.ROYAL_JELLY_FLUID_TYPE,
            //"block/fluid/royal_jelly_still", "block/fluid/royal_jelly_flow", 0xFFFFFFFF);
            "block/t", "block/t2", 0xFFF5F0E0);

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
    // PARTICLES
    // =========================================================================

    private static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BeemancerParticles.HONEY_PIXEL.get(), HoneyPixelParticle.Provider::new);
        event.registerSpriteSet(BeemancerParticles.RUNE.get(), RuneParticle.Provider::new);
    }

    // =========================================================================
    // BLOCK COLORS
    // =========================================================================

    private static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        // Item Pipes - teinte du core
        // Sans teinte = blanc (texture de base visible), avec teinte = couleur du colorant sur pipe_core_white
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFFFF;
            }
            if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
                if (pipe.hasTint()) {
                    return pipe.getTintColor() | 0xFF000000;
                }
            }
            return 0xFFFFFFFF; // Blanc — pas de teinte appliquée
        },
            BeemancerBlocks.ITEM_PIPE.get(),
            BeemancerBlocks.ITEM_PIPE_TIER2.get(),
            BeemancerBlocks.ITEM_PIPE_TIER3.get(),
            BeemancerBlocks.ITEM_PIPE_TIER4.get()
        );

        // Honey Pipes - teinte du core
        // Sans teinte = blanc (texture de base visible), avec teinte = couleur du colorant sur pipe_core_white
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFFFF;
            }
            if (level.getBlockEntity(pos) instanceof HoneyPipeBlockEntity pipe) {
                if (pipe.hasTint()) {
                    return pipe.getTintColor() | 0xFF000000;
                }
            }
            return 0xFFFFFFFF; // Blanc — pas de teinte appliquée
        },
            BeemancerBlocks.HONEY_PIPE.get(),
            BeemancerBlocks.HONEY_PIPE_TIER2.get(),
            BeemancerBlocks.HONEY_PIPE_TIER3.get(),
            BeemancerBlocks.HONEY_PIPE_TIER4.get()
        );
    }

    // =========================================================================
    // ADDITIONAL MODELS (pour le rendu dynamique)
    // =========================================================================

    private static void registerAdditionalModels(final ModelEvent.RegisterAdditional event) {
        // Modèles du Altar Heart formé (3 parties de structure + conduits, rendus dynamiquement)
        event.register(AltarHeartRenderer.PEDESTAL_MODEL_LOC);
        event.register(AltarHeartRenderer.CORE_MODEL_LOC);
        event.register(AltarHeartRenderer.TOP_MODEL_LOC);
        event.register(AltarHeartRenderer.CONDUIT_MODEL_LOC);

        // Modele core du Storage Controller forme (coeur anime)
        event.register(StorageControllerRenderer.CORE_MODEL_LOC);

        // Modèle formed du honey reservoir (rendu par BER avec spread offset)
        event.register(HoneyReservoirRenderer.FORMED_MODEL_LOC);

        // Modèle core du crystallizer (cubes animés)
        event.register(CrystallizerRenderer.CORE_MODEL_LOC);

        // Modèle crank (rendu par BER avec rotation)
        event.register(CrankRenderer.CRANK_MODEL_LOC);

        // Modèles core du centrifuge heart (cubes animés)
        event.register(CentrifugeHeartRenderer.CORE_MODEL_LOC);
        event.register(CentrifugeHeartRenderer.CORE_WORKING_MODEL_LOC);

        // Modèle core de l'infuser heart (cubes statiques)
        event.register(InfuserHeartRenderer.CORE_MODEL_LOC);

        // Modèles multiblock tank
        event.register(MultiblockTankRenderer.FORMED_MODEL_LOC);  // Formé (scalé)
        event.register(MultiblockTankRenderer.SINGLE_MODEL_LOC);  // Non formé (bloc simple)
    }

    // =========================================================================
    // FLYWHEEL VISUALIZER REGISTRATION
    // =========================================================================

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(FlywheelTestBeeVisualizer::register);
    }
}
