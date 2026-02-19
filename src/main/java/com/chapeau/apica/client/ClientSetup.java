/**
 * ============================================================
 * [ClientSetup.java]
 * Description: Configuration client (NeoForge 1.21.1)
 * ============================================================
 */
package com.chapeau.apica.client;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.camera.HoverbikeCameraController;
import com.chapeau.apica.client.gui.hud.DebugPanelRenderer;
import com.chapeau.apica.client.gui.hud.HoverbikeDebugHud;
import com.chapeau.apica.client.gui.hud.HoverbikeGaugeHud;
import com.chapeau.apica.client.model.HoverbikeModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.client.renderer.entity.HoverbikeRenderer;
import com.chapeau.apica.client.input.DebugKeyHandler;
import com.chapeau.apica.client.gui.screen.IncubatorScreen;
import com.chapeau.apica.client.gui.screen.MagicHiveScreen;
import com.chapeau.apica.client.gui.screen.alchemy.AlembicScreen;
import com.chapeau.apica.client.gui.screen.alchemy.CreativeTankScreen;
import com.chapeau.apica.client.gui.screen.alchemy.CrystallizerScreen;
import com.chapeau.apica.client.gui.screen.alchemy.HoneyTankScreen;
import com.chapeau.apica.client.gui.screen.alchemy.InfuserScreen;
import com.chapeau.apica.client.gui.screen.alchemy.ManualCentrifugeScreen;
import com.chapeau.apica.client.gui.screen.ResonatorScreen;
import com.chapeau.apica.client.gui.screen.alchemy.MultiblockTankScreen;
import com.chapeau.apica.client.gui.screen.alchemy.ApicaFurnaceScreen;
import com.chapeau.apica.client.gui.screen.alchemy.PoweredCentrifugeScreen;
import com.chapeau.apica.client.gui.screen.storage.NetworkInterfaceScreen;
import com.chapeau.apica.client.gui.screen.storage.StorageTerminalScreen;
import com.chapeau.apica.client.renderer.BuildingWandPreviewRenderer;
import com.chapeau.apica.client.renderer.block.AssemblyTableRenderer;
import com.chapeau.apica.client.renderer.block.AltarHeartRenderer;
import com.chapeau.apica.client.renderer.block.BeeStatueRenderer;
import com.chapeau.apica.client.renderer.block.HoneyPedestalRenderer;
import com.chapeau.apica.client.renderer.block.HoneyReservoirRenderer;
import com.chapeau.apica.client.renderer.block.CentrifugeHeartRenderer;
import com.chapeau.apica.client.renderer.block.CrankRenderer;
import com.chapeau.apica.client.renderer.block.InfuserHeartRenderer;
import com.chapeau.apica.client.renderer.block.CrystallizerRenderer;
import com.chapeau.apica.client.renderer.block.InfuserRenderer;
import com.chapeau.apica.client.renderer.block.ResonatorRenderer;
import com.chapeau.apica.client.renderer.block.HoneyTankRenderer;
import com.chapeau.apica.client.renderer.block.IncubatorRenderer;
import com.chapeau.apica.client.particle.HoneyPixelParticle;
import com.chapeau.apica.client.particle.RuneParticle;
import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.client.renderer.block.MultiblockTankRenderer;
import com.chapeau.apica.client.renderer.block.PollenPotRenderer;
import com.chapeau.apica.client.renderer.block.StorageControllerRenderer;
import com.chapeau.apica.client.renderer.block.StorageHiveRenderer;
import com.chapeau.apica.client.renderer.block.StorageTerminalRenderer;
import com.chapeau.apica.client.renderer.block.TranslucentOutlineRenderer;
import com.chapeau.apica.client.renderer.debug.BeeDebugRenderer;
import com.chapeau.apica.client.renderer.debug.CustomDebugDisplayRenderer;
import com.chapeau.apica.client.renderer.debug.HoverbikeDebugRenderer;
import com.chapeau.apica.client.visual.FlywheelTestBeeVisualizer;
import com.chapeau.apica.client.renderer.entity.MagicBeeRenderer;
import com.chapeau.apica.client.renderer.entity.DeliveryBeeRenderer;
import com.chapeau.apica.client.renderer.item.MagicBeeItemRenderer;
import com.chapeau.apica.common.blockentity.alchemy.HoneyPipeBlockEntity;
import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import com.chapeau.apica.client.animation.AnimationTimer;
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
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
        modEventBus.addListener(ClientSetup::registerItemColors);
        modEventBus.addListener(ClientSetup::registerAdditionalModels);
        modEventBus.addListener(ClientSetup::registerParticleProviders);
        modEventBus.addListener(ClientSetup::onClientSetup);

        // AnimationTimer: compteur client-side pour animations sans stutter (pattern Create)
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AnimationTimer.tick());

        NeoForge.EVENT_BUS.register(BuildingWandPreviewRenderer.class);
        NeoForge.EVENT_BUS.register(DebugPanelRenderer.class);
        NeoForge.EVENT_BUS.register(DebugKeyHandler.class);
        NeoForge.EVENT_BUS.register(BeeDebugRenderer.class);

        // Debug renderers (quand displayDebug = true)
        NeoForge.EVENT_BUS.register(CustomDebugDisplayRenderer.class);

        // Hoverbike system
        NeoForge.EVENT_BUS.register(HoverbikeDebugRenderer.class);
        NeoForge.EVENT_BUS.register(HoverbikeDebugHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeGaugeHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeCameraController.class);
        NeoForge.EVENT_BUS.register(HoverbikeEditModeEffect.class);
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.hud.HoverbikeEditModeHandler.class);
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.hud.HoverbikeEditStatsHud.class);

        // Overlay quand on ouvre un GUI adjacent depuis le bouton Debug de l'interface
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.screen.storage.AdjacentGuiOverlayRenderer.class);

        // Fix outline translucent pour blocs comme le Crystallizer
        NeoForge.EVENT_BUS.register(TranslucentOutlineRenderer.class);


        // Debug Opti
        NeoForge.EVENT_BUS.register(RenderCostVisualizer.class);
        NeoForge.EVENT_BUS.register(LogicCostVisualizer.class);
    }

    private static void registerScreens(final RegisterMenuScreensEvent event) {
        // Core screens
        event.register(ApicaMenus.STORAGE_TERMINAL.get(), StorageTerminalScreen::new);
        event.register(ApicaMenus.NETWORK_INTERFACE.get(), NetworkInterfaceScreen::new);
        event.register(ApicaMenus.MAGIC_HIVE.get(), MagicHiveScreen::new);
        event.register(ApicaMenus.INCUBATOR.get(), IncubatorScreen::new);
        event.register(ApicaMenus.INJECTOR.get(), com.chapeau.apica.client.gui.screen.InjectorScreen::new);

        // Alchemy screens
        event.register(ApicaMenus.MANUAL_CENTRIFUGE.get(), ManualCentrifugeScreen::new);
        event.register(ApicaMenus.POWERED_CENTRIFUGE.get(), PoweredCentrifugeScreen::new);
        event.register(ApicaMenus.HONEY_TANK.get(), HoneyTankScreen::new);
        event.register(ApicaMenus.CREATIVE_TANK.get(), CreativeTankScreen::new);
        event.register(ApicaMenus.CRYSTALLIZER.get(), CrystallizerScreen::new);
        event.register(ApicaMenus.ALEMBIC.get(), AlembicScreen::new);
        event.register(ApicaMenus.INFUSER.get(), InfuserScreen::new);
        event.register(ApicaMenus.MULTIBLOCK_TANK.get(), MultiblockTankScreen::new);

        // Apica Furnaces
        event.register(ApicaMenus.APICA_FURNACE.get(), ApicaFurnaceScreen::new);

        // Resonator
        event.register(ApicaMenus.RESONATOR.get(), ResonatorScreen::new);
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ApicaEntities.MAGIC_BEE.get(), MagicBeeRenderer::new);
        event.registerEntityRenderer(ApicaEntities.DELIVERY_BEE.get(), DeliveryBeeRenderer::new);
        event.registerEntityRenderer(ApicaEntities.HOVERBIKE.get(), HoverbikeRenderer::new);
        // Flywheel test bee - noop renderer (Flywheel handles rendering)
        event.registerEntityRenderer(ApicaEntities.FLYWHEEL_TEST_BEE.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);

        // Block Entity Renderers
        event.registerBlockEntityRenderer(ApicaBlockEntities.STORAGE_CONTROLLER.get(),
            StorageControllerRenderer::new);
        event.registerBlockEntityRenderer(ApicaBlockEntities.STORAGE_RELAY.get(),
            com.chapeau.apica.client.renderer.block.StorageRelayRenderer::new);
        event.registerBlockEntityRenderer(ApicaBlockEntities.STORAGE_TERMINAL.get(),
            StorageTerminalRenderer::new);
        // StorageHiveRenderer - oscillation verticale quand linkee
        event.registerBlockEntityRenderer(ApicaBlockEntities.STORAGE_HIVE.get(),
            StorageHiveRenderer::new);
        event.registerBlockEntityRenderer(ApicaBlockEntities.BEE_STATUE.get(),
            BeeStatueRenderer::new);
        // HoneyReservoirRenderer - rendu dynamique du fluide avec scale
        event.registerBlockEntityRenderer(ApicaBlockEntities.HONEY_RESERVOIR.get(),
            HoneyReservoirRenderer::new);
        // AltarHeartRenderer - rendu des conduits animés
        event.registerBlockEntityRenderer(ApicaBlockEntities.ALTAR_HEART.get(),
            AltarHeartRenderer::new);
        // HoneyPedestalRenderer - item flottant au-dessus du pedestal
        event.registerBlockEntityRenderer(ApicaBlockEntities.HONEY_PEDESTAL.get(),
            HoneyPedestalRenderer::new);
        // CrankRenderer - rotation quand la centrifugeuse tourne
        event.registerBlockEntityRenderer(ApicaBlockEntities.CRANK.get(),
            CrankRenderer::new);
        // CrystallizerRenderer - cores animés (rotation + scale)
        event.registerBlockEntityRenderer(ApicaBlockEntities.CRYSTALLIZER.get(),
            CrystallizerRenderer::new);
        // InfuserRenderer - item flottant + particules miel
        event.registerBlockEntityRenderer(ApicaBlockEntities.INFUSER.get(),
            InfuserRenderer::new);
        // event.registerBlockEntityRenderer(ApicaBlockEntities.INFUSER_TIER2.get(),
        //     InfuserRenderer::new);
        // HoneyTankRenderer - fluide dynamique
        event.registerBlockEntityRenderer(ApicaBlockEntities.HONEY_TANK.get(),
            HoneyTankRenderer::new);
        // MultiblockTankRenderer - fluide dynamique multi-bloc
        event.registerBlockEntityRenderer(ApicaBlockEntities.MULTIBLOCK_TANK.get(),
            MultiblockTankRenderer::new);
        // PollenPotRenderer - cube texturé avec hauteur dynamique selon remplissage
        event.registerBlockEntityRenderer(ApicaBlockEntities.POLLEN_POT.get(),
            PollenPotRenderer::new);
        // IncubatorRenderer - item flottant au centre de l'incubateur
        event.registerBlockEntityRenderer(ApicaBlockEntities.INCUBATOR.get(),
            IncubatorRenderer::new);
        // AssemblyTableRenderer - piece de moto sur la table
        event.registerBlockEntityRenderer(ApicaBlockEntities.ASSEMBLY_TABLE.get(),
            AssemblyTableRenderer::new);
        // CentrifugeHeartRenderer - cubes centraux animés de la centrifuge multibloc
        event.registerBlockEntityRenderer(ApicaBlockEntities.CENTRIFUGE_HEART.get(),
            CentrifugeHeartRenderer::new);
        // InfuserHeartRenderer - cubes centraux statiques de l'infuser multibloc
        // event.registerBlockEntityRenderer(ApicaBlockEntities.INFUSER_HEART.get(),
        //     InfuserHeartRenderer::new);
        // ResonatorRenderer - abeille flottante au-dessus du resonateur
        event.registerBlockEntityRenderer(ApicaBlockEntities.RESONATOR.get(),
            ResonatorRenderer::new);
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
        }, ApicaItems.MAGIC_BEE.get());

        // --- Fluid Extensions ---
        registerFluidExtension(event, ApicaFluids.HONEY_FLUID_TYPE,
            //"block/fluid/honey_still", "block/fluid/honey_flow", 0xFFFFFFFF);
            "block/t", "block/t2", 0xFFE8A317);

        registerFluidExtension(event, ApicaFluids.ROYAL_JELLY_FLUID_TYPE,
            //"block/fluid/royal_jelly_still", "block/fluid/royal_jelly_flow", 0xFFFFFFFF);
            "block/t", "block/t2", 0xFFF5F0E0);

        registerFluidExtension(event, ApicaFluids.NECTAR_FLUID_TYPE,
            "block/fluid/nectar_still", "block/fluid/nectar_flow", 0xFFFFFFFF);
    }

    private static void registerFluidExtension(RegisterClientExtensionsEvent event,
            Supplier<FluidType> fluidType, String stillPath, String flowPath, int tintColor) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private final ResourceLocation stillTexture =
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, stillPath);
            private final ResourceLocation flowingTexture =
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, flowPath);

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
        event.registerSpriteSet(ApicaParticles.HONEY_PIXEL.get(), HoneyPixelParticle.Provider::new);
        event.registerSpriteSet(ApicaParticles.RUNE.get(), RuneParticle.Provider::new);
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
            ApicaBlocks.ITEM_PIPE.get(),
            ApicaBlocks.ITEM_PIPE_TIER2.get(),
            ApicaBlocks.ITEM_PIPE_TIER3.get(),
            ApicaBlocks.ITEM_PIPE_TIER4.get()
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
            ApicaBlocks.HONEY_PIPE.get(),
            ApicaBlocks.HONEY_PIPE_TIER2.get(),
            ApicaBlocks.HONEY_PIPE_TIER3.get(),
            ApicaBlocks.HONEY_PIPE_TIER4.get()
        );

        // Creative Tank - teinte rose creative
        event.register((state, level, pos, tintIndex) -> 0xFFFF69B4,
            ApicaBlocks.CREATIVE_TANK.get()
        );

        // Creative Breeding Crystal - teinte rose creative
        event.register((state, level, pos, tintIndex) -> 0xFFFF69B4,
            ApicaBlocks.CREATIVE_BREEDING_CRYSTAL.get()
        );
    }

    private static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
        // Creative Tank item - teinte rose creative
        event.register((stack, tintIndex) -> 0xFFFF69B4,
            ApicaBlocks.CREATIVE_TANK.get().asItem()
        );

        // Creative Breeding Crystal item - teinte rose creative
        event.register((stack, tintIndex) -> 0xFFFF69B4,
            ApicaBlocks.CREATIVE_BREEDING_CRYSTAL.get().asItem()
        );

        // Species Essence - teinte selon la couleur de l'espece
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            String speciesId = com.chapeau.apica.common.item.essence.SpeciesEssenceItem.getSpeciesId(stack);
            if (speciesId == null) return 0xFFFFFFFF;
            com.chapeau.apica.core.bee.BeeSpeciesManager.ensureClientLoaded();
            return 0xFF000000 | com.chapeau.apica.core.bee.BeeSpeciesManager.getSpeciesColor(speciesId);
        }, ApicaItems.SPECIES_ESSENCE.get());
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
