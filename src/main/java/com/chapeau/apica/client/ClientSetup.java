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
import com.chapeau.apica.client.gui.hud.MagazineGaugeHud;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
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
import com.chapeau.apica.client.gui.screen.alchemy.UncraftingTableScreen;
import com.chapeau.apica.client.gui.screen.storage.NetworkInterfaceScreen;
import com.chapeau.apica.client.gui.screen.storage.StorageTerminalScreen;
import com.chapeau.apica.client.renderer.BuildingWandPreviewRenderer;
import com.chapeau.apica.client.renderer.ChopperHivePreviewRenderer;
import com.chapeau.apica.client.renderer.MiningLaserBeamRenderer;
import com.chapeau.apica.client.renderer.block.AssemblyTableRenderer;
import com.chapeau.apica.client.renderer.block.AssemblyTableStatsRenderer;
import com.chapeau.apica.client.renderer.block.ApiRenderer;
import com.chapeau.apica.client.renderer.block.AltarHeartRenderer;
import com.chapeau.apica.client.renderer.block.BeeStatueRenderer;
import com.chapeau.apica.client.renderer.block.HoneyPedestalRenderer;
import com.chapeau.apica.client.renderer.block.HoneyReservoirRenderer;
import com.chapeau.apica.client.renderer.block.CentrifugeHeartRenderer;
import com.chapeau.apica.client.renderer.block.CrankRenderer;
import com.chapeau.apica.client.renderer.block.LaunchpadRenderer;
import com.chapeau.apica.client.renderer.block.InjectorRenderer;
import com.chapeau.apica.client.renderer.block.InfuserHeartRenderer;
import com.chapeau.apica.client.renderer.block.PipeExtractRenderer;
import com.chapeau.apica.client.renderer.block.CrystallizerRenderer;
import com.chapeau.apica.client.renderer.block.InfuserRenderer;
import com.chapeau.apica.client.renderer.block.ResonatorRenderer;
import com.chapeau.apica.client.renderer.block.UncraftingTableRenderer;
import com.chapeau.apica.client.renderer.block.HoneyTankRenderer;
import com.chapeau.apica.client.renderer.block.IncubatorRenderer;
import com.chapeau.apica.client.particle.HoneyPixelParticle;
import com.chapeau.apica.client.particle.LaserHaloParticle;
import com.chapeau.apica.client.particle.LaserRingParticle;
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
import com.chapeau.apica.client.model.entity.LeafBlowerProjectileModel;
import com.chapeau.apica.client.renderer.entity.LeafBlowerProjectileRenderer;
import com.chapeau.apica.client.renderer.entity.MagicBeeRenderer;
import com.chapeau.apica.client.renderer.entity.CompanionBeeRenderer;
import com.chapeau.apica.client.renderer.entity.DeliveryBeeRenderer;
import com.chapeau.apica.client.renderer.item.BuildingStaffItemRenderer;
import com.chapeau.apica.client.renderer.item.LeafBlowerItemRenderer;
import com.chapeau.apica.client.renderer.item.MiningLaserItemRenderer;
import com.chapeau.apica.client.renderer.item.ChopperHiveItemRenderer;
import com.chapeau.apica.client.renderer.item.MagicBeeItemRenderer;
import com.chapeau.apica.common.blockentity.alchemy.LiquidPipeBlockEntity;
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
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Supplier;

public class ClientSetup {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterMenuScreensEvent e)         -> timed("registerScreens",          () -> registerScreens(e)));
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> timed("registerEntityRenderers", () -> registerEntityRenderers(e)));
        modEventBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> timed("registerLayerDefinitions", () -> registerLayerDefinitions(e)));
        modEventBus.addListener((RegisterClientExtensionsEvent e)    -> timed("registerClientExtensions", () -> registerClientExtensions(e)));
        modEventBus.addListener((RegisterColorHandlersEvent.Block e)  -> timed("registerBlockColors",     () -> registerBlockColors(e)));
        modEventBus.addListener((RegisterColorHandlersEvent.Item e)   -> timed("registerItemColors",      () -> registerItemColors(e)));
        modEventBus.addListener((ModelEvent.RegisterAdditional e)     -> timed("registerAdditionalModels",() -> registerAdditionalModels(e)));
        modEventBus.addListener((RegisterParticleProvidersEvent e)    -> timed("registerParticleProviders",() -> registerParticleProviders(e)));
        modEventBus.addListener((FMLClientSetupEvent e)              -> e.enqueueWork(ClientSetup::registerItemProperties));
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent e) -> timed("registerTooltipComponents", () -> registerTooltipComponents(e)));
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent e) -> timed("registerItemDecorations", () -> registerItemDecorations(e)));

        // AnimationTimer: compteur client-side pour animations sans stutter (pattern Create)
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AnimationTimer.tick());

        NeoForge.EVENT_BUS.register(BuildingWandPreviewRenderer.class);
        NeoForge.EVENT_BUS.register(ChopperHivePreviewRenderer.class);
        NeoForge.EVENT_BUS.register(MiningLaserBeamRenderer.class);
        NeoForge.EVENT_BUS.register(DebugPanelRenderer.class);
        NeoForge.EVENT_BUS.register(DebugKeyHandler.class);
        NeoForge.EVENT_BUS.register(BeeDebugRenderer.class);

        // Debug renderers (quand displayDebug = true)
        NeoForge.EVENT_BUS.register(CustomDebugDisplayRenderer.class);

        // Assembly Table stats billboard
        NeoForge.EVENT_BUS.register(AssemblyTableStatsRenderer.class);

        // Hoverbike system
        NeoForge.EVENT_BUS.register(HoverbikeDebugRenderer.class);
        NeoForge.EVENT_BUS.register(HoverbikeDebugHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeGaugeHud.class);
        NeoForge.EVENT_BUS.register(HoverbikeCameraController.class);
        NeoForge.EVENT_BUS.register(HoverbikeEditModeEffect.class);
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.hud.HoverbikeEditModeHandler.class);
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.hud.HoverbikeEditStatsHud.class);
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.hud.HoverbikeInfoHud.class);

        // Overlay quand on ouvre un GUI adjacent depuis le bouton Debug de l'interface
        NeoForge.EVENT_BUS.register(com.chapeau.apica.client.gui.screen.storage.AdjacentGuiOverlayRenderer.class);

        // Magazine gauge HUD
        NeoForge.EVENT_BUS.register(MagazineGaugeHud.class);

        // Fix outline translucent pour blocs comme le Crystallizer
        NeoForge.EVENT_BUS.register(TranslucentOutlineRenderer.class);


        // Debug Opti
        NeoForge.EVENT_BUS.register(RenderCostVisualizer.class);
        NeoForge.EVENT_BUS.register(LogicCostVisualizer.class);
    }

    private static void timed(String label, Runnable action) {
        long t = System.currentTimeMillis();
        action.run();
        Apica.LOGGER.info("[TIMING] ClientSetup.{}: {}ms", label, System.currentTimeMillis() - t);
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

        // Uncrafting Table
        event.register(ApicaMenus.UNCRAFTING_TABLE.get(), UncraftingTableScreen::new);

        // Resonator
        event.register(ApicaMenus.RESONATOR.get(), ResonatorScreen::new);

        // Trash Cans
        event.register(ApicaMenus.TRASH_CAN.get(), com.chapeau.apica.client.gui.screen.TrashCanScreen::new);
        event.register(ApicaMenus.LIQUID_TRASH_CAN.get(), com.chapeau.apica.client.gui.screen.LiquidTrashCanScreen::new);

        // Item Filter
        event.register(ApicaMenus.ITEM_FILTER.get(), com.chapeau.apica.client.gui.screen.ItemFilterScreen::new);

        // Dubstep Radio
        event.register(ApicaMenus.DUBSTEP_RADIO.get(), com.chapeau.apica.client.gui.screen.DubstepRadioScreen::new);

        // Backpack
        event.register(ApicaMenus.BACKPACK.get(), com.chapeau.apica.client.gui.screen.BackpackScreen::new);
    }

    private static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ApicaEntities.MAGIC_BEE.get(), MagicBeeRenderer::new);
        event.registerEntityRenderer(ApicaEntities.DELIVERY_BEE.get(), DeliveryBeeRenderer::new);
        event.registerEntityRenderer(ApicaEntities.COMPANION_BEE.get(), CompanionBeeRenderer::new);
        event.registerEntityRenderer(ApicaEntities.HOVERBIKE.get(), HoverbikeRenderer::new);
        // Leaf Blower orb projectile
        event.registerEntityRenderer(ApicaEntities.LEAF_BLOWER_ORB.get(), LeafBlowerProjectileRenderer::new);

        // Interaction marker - invisible entity, no rendering
        event.registerEntityRenderer(ApicaEntities.INTERACTION_MARKER.get(),
            com.chapeau.apica.client.renderer.entity.InteractionMarkerRenderer::new);

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
        // ApiRenderer - bloc vivant qui scale dynamiquement
        event.registerBlockEntityRenderer(ApicaBlockEntities.API.get(),
            ApiRenderer::new);
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
        // UncraftingTableRenderer - item flottant au-dessus de la table
        event.registerBlockEntityRenderer(ApicaBlockEntities.UNCRAFTING_TABLE.get(),
            UncraftingTableRenderer::new);
        // InjectorRenderer - abeille flottante + essence a plat sur l'injecteur
        event.registerBlockEntityRenderer(ApicaBlockEntities.INJECTOR.get(),
            InjectorRenderer::new);
        // StorageBarrelRenderer - item + quantite sur la face avant
        event.registerBlockEntityRenderer(ApicaBlockEntities.STORAGE_BARREL.get(),
            com.chapeau.apica.client.renderer.block.StorageBarrelRenderer::new);
        // HoneyLampRenderer - halo translucide cross planes
        event.registerBlockEntityRenderer(ApicaBlockEntities.HONEY_LAMP.get(),
            com.chapeau.apica.client.renderer.block.HoneyLampRenderer::new);

        // LaunchpadRenderer - plaque inclinable animee
        event.registerBlockEntityRenderer(ApicaBlockEntities.LAUNCHPAD.get(),
            LaunchpadRenderer::new);

        // PipeExtractRenderer - indicateur d'extraction sur les pipes (item et liquide)
        event.registerBlockEntityRenderer(ApicaBlockEntities.ITEM_PIPE.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.ITEM_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.ITEM_PIPE_MK2.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.ITEM_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.ITEM_PIPE_MK3.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.ITEM_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.ITEM_PIPE_MK4.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.ITEM_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.LIQUID_PIPE.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.LIQUID_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.LIQUID_PIPE_MK2.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.LIQUID_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.LIQUID_PIPE_MK3.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.LIQUID_EXTRACT_MODEL_LOC));
        event.registerBlockEntityRenderer(ApicaBlockEntities.LIQUID_PIPE_MK4.get(),
            ctx -> new PipeExtractRenderer<>(ctx, PipeExtractRenderer.LIQUID_EXTRACT_MODEL_LOC));
    }

    private static void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Hoverbike — modele de base
        event.registerLayerDefinition(HoverbikeRenderer.LAYER_LOCATION, HoverbikeModel::createBodyLayer);

        // Leaf Blower orb model
        event.registerLayerDefinition(LeafBlowerProjectileModel.LAYER_LOCATION, LeafBlowerProjectileModel::createBodyLayer);

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

        // Building Staff - render 3D model + mini block in crystal
        event.registerItem(new IClientItemExtensions() {
            private BuildingStaffItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new BuildingStaffItemRenderer();
                }
                return renderer;
            }
        }, ApicaItems.BUILDING_STAFF.get());

        // Leaf Blower - render 3D model + charging animation overlay + no equip bob
        event.registerItem(new IClientItemExtensions() {
            private LeafBlowerItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new LeafBlowerItemRenderer();
                }
                return renderer;
            }

            @Override
            public boolean applyForgeHandTransform(
                    com.mojang.blaze3d.vertex.PoseStack poseStack,
                    net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                int side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                } else {
                    float sqrtSwing = net.minecraft.util.Mth.sqrt(swingProcess);
                    float f5 = -0.4F * net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    float f6 = 0.2F * net.minecraft.util.Mth.sin(sqrtSwing * (float) (Math.PI * 2));
                    float f10 = -0.2F * net.minecraft.util.Mth.sin(swingProcess * (float) Math.PI);
                    poseStack.translate((float) side * f5, f6, f10);
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                    float f = net.minecraft.util.Mth.sin(swingProcess * swingProcess * (float) Math.PI);
                    float f1 = net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) side * f1 * 70.0F));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) side * f * -20.0F));
                }
                return true;
            }
        }, ApicaItems.LEAF_BLOWER.get());

        // Mining Laser - render 3D model + charging animation overlay + rings + beam + no equip bob
        event.registerItem(new IClientItemExtensions() {
            private MiningLaserItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MiningLaserItemRenderer();
                }
                return renderer;
            }

            @Override
            public boolean applyForgeHandTransform(
                    com.mojang.blaze3d.vertex.PoseStack poseStack,
                    net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                int side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                } else {
                    float sqrtSwing = net.minecraft.util.Mth.sqrt(swingProcess);
                    float f5 = -0.4F * net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    float f6 = 0.2F * net.minecraft.util.Mth.sin(sqrtSwing * (float) (Math.PI * 2));
                    float f10 = -0.2F * net.minecraft.util.Mth.sin(swingProcess * (float) Math.PI);
                    poseStack.translate((float) side * f5, f6, f10);
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                    float f = net.minecraft.util.Mth.sin(swingProcess * swingProcess * (float) Math.PI);
                    float f1 = net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) side * f1 * 70.0F));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) side * f * -20.0F));
                }
                return true;
            }
        }, ApicaItems.MINING_LASER.get());

        // Chopper Hive - render 3D model with animated slabs + orbiting bees in first person
        // + hand transform freeze during chopping (no arm swing)
        event.registerItem(new IClientItemExtensions() {
            private ChopperHiveItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ChopperHiveItemRenderer();
                }
                return renderer;
            }

            @Override
            public boolean applyForgeHandTransform(
                    com.mojang.blaze3d.vertex.PoseStack poseStack,
                    net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                int side = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
                if (com.chapeau.apica.common.item.tool.ChopperHiveLockHelper.isLocked()) {
                    // Main figee pendant le chopping (pas de swing)
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                } else {
                    // Animation normale de swing
                    float sqrtSwing = net.minecraft.util.Mth.sqrt(swingProcess);
                    float f5 = -0.4F * net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    float f6 = 0.2F * net.minecraft.util.Mth.sin(sqrtSwing * (float) (Math.PI * 2));
                    float f10 = -0.2F * net.minecraft.util.Mth.sin(swingProcess * (float) Math.PI);
                    poseStack.translate((float) side * f5, f6, f10);
                    poseStack.translate((float) side * 0.56F, -0.52F, -0.72F);
                    float f = net.minecraft.util.Mth.sin(swingProcess * swingProcess * (float) Math.PI);
                    float f1 = net.minecraft.util.Mth.sin(sqrtSwing * (float) Math.PI);
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) side * f1 * 70.0F));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) side * f * -20.0F));
                }
                return true;
            }
        }, ApicaItems.CHOPPER_HIVE.get());

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
    // RENDER LAYERS
    // =========================================================================

    // =========================================================================
    // PARTICLES
    // =========================================================================

    private static void registerParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ApicaParticles.HONEY_PIXEL.get(), HoneyPixelParticle.Provider::new);
        event.registerSpriteSet(ApicaParticles.RUNE.get(), RuneParticle.Provider::new);
        event.registerSpriteSet(ApicaParticles.LASER_RING.get(), LaserRingParticle.Provider::new);
        event.registerSpriteSet(ApicaParticles.LASER_HALO.get(), LaserHaloParticle.Provider::new);
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
            ApicaBlocks.ITEM_PIPE_MK2.get(),
            ApicaBlocks.ITEM_PIPE_MK3.get(),
            ApicaBlocks.ITEM_PIPE_MK4.get()
        );

        // Liquid Pipes - teinte du core
        // Sans teinte = blanc (texture de base visible), avec teinte = couleur du colorant sur pipe_core_white
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || level == null || pos == null) {
                return 0xFFFFFFFF;
            }
            if (level.getBlockEntity(pos) instanceof LiquidPipeBlockEntity pipe) {
                if (pipe.hasTint()) {
                    return pipe.getTintColor() | 0xFF000000;
                }
            }
            return 0xFFFFFFFF; // Blanc — pas de teinte appliquée
        },
            ApicaBlocks.LIQUID_PIPE.get(),
            ApicaBlocks.LIQUID_PIPE_MK2.get(),
            ApicaBlocks.LIQUID_PIPE_MK3.get(),
            ApicaBlocks.LIQUID_PIPE_MK4.get()
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

        // Building Staff 3D model (rendu par BEWLR)
        event.register(BuildingStaffItemRenderer.STAFF_MODEL_LOC);

        // Leaf Blower 3D body model (rendu par BEWLR)
        event.register(LeafBlowerItemRenderer.BODY_MODEL_LOC);

        // Mining Laser 3D body model (rendu par BEWLR)
        event.register(MiningLaserItemRenderer.BODY_MODEL_LOC);

        // Chopper Hive 3D part models (rendu par BEWLR)
        event.register(ChopperHiveItemRenderer.BOTTOM_MODEL_LOC);
        event.register(ChopperHiveItemRenderer.CENTER_MODEL_LOC);
        event.register(ChopperHiveItemRenderer.TOP_MODEL_LOC);

        // Modèles multiblock tank
        event.register(MultiblockTankRenderer.FORMED_MODEL_LOC);  // Formé (scalé)
        event.register(MultiblockTankRenderer.SINGLE_MODEL_LOC);  // Non formé (bloc simple)

        // Modèles extract pour les pipes (rendu BER de l'indicateur d'extraction)
        event.register(PipeExtractRenderer.ITEM_EXTRACT_MODEL_LOC);
        event.register(PipeExtractRenderer.LIQUID_EXTRACT_MODEL_LOC);

        // Modèle Api (bloc vivant scalé dynamiquement)
        event.register(ApiRenderer.API_MODEL_LOC);

        // Modèle plaque du Launchpad (rendu BER avec rotation dynamique)
        event.register(LaunchpadRenderer.PLATE_MODEL_LOC);
    }

    // =========================================================================
    // ITEM PROPERTIES (model overrides)
    // =========================================================================

    private static void registerTooltipComponents(final net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(com.chapeau.apica.common.item.BackpackTooltip.class,
                com.chapeau.apica.client.gui.tooltip.ClientBackpackTooltip::new);
    }

    private static void registerItemDecorations(final net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent event) {
        net.minecraft.world.item.ItemStack chestIcon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);
        ResourceLocation magnetTex = ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/item/artifacts/magnet.png");

        // Backpack: icone coffre en bas a droite (toujours visible)
        event.register(ApicaItems.BACKPACK.get(), (graphics, font, stack, xOffset, yOffset) -> {
            com.mojang.blaze3d.vertex.PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(xOffset + 8, yOffset + 8, 200);
            pose.scale(0.5f, 0.5f, 1.0f);
            graphics.renderItem(chestIcon, 0, 0);
            pose.popPose();
            return false;
        });

        // Bee Magnet: icone magnet en bas a droite (toujours visible)
        event.register(ApicaItems.BEE_MAGNET.get(), (graphics, font, stack, xOffset, yOffset) -> {
            com.mojang.blaze3d.vertex.PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(xOffset + 8, yOffset + 8, 200);
            pose.scale(0.5f, 0.5f, 1.0f);
            graphics.blit(magnetTex, 0, 0, 0, 0, 16, 16, 16, 16);
            pose.popPose();
            return false;
        });

        // IMagazineHolder items: icone magazine en bas a droite (equipee ou vide)
        net.minecraft.world.item.ItemStack emptyMag = new net.minecraft.world.item.ItemStack(ApicaItems.MAGAZINE.get());
        net.neoforged.neoforge.client.IItemDecorator magazineDecorator = (graphics, font, stack, xOffset, yOffset) -> {
            net.minecraft.world.item.ItemStack displayMag;
            if (com.chapeau.apica.common.item.magazine.MagazineData.hasMagazine(stack)) {
                String fluidId = com.chapeau.apica.common.item.magazine.MagazineData.getFluidId(stack);
                int amount = com.chapeau.apica.common.item.magazine.MagazineData.getFluidAmount(stack);
                displayMag = com.chapeau.apica.common.item.magazine.MagazineItem.createFilled(fluidId, amount);
            } else {
                displayMag = emptyMag;
            }
            com.mojang.blaze3d.vertex.PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(xOffset + 8, yOffset + 8, 200);
            pose.scale(0.5f, 0.5f, 1.0f);
            graphics.renderItem(displayMag, 0, 0);
            pose.popPose();
            return false;
        };
        event.register(ApicaItems.LEAF_BLOWER.get(), magazineDecorator);
        event.register(ApicaItems.MINING_LASER.get(), magazineDecorator);
        event.register(ApicaItems.CHOPPER_HIVE.get(), magazineDecorator);
        event.register(ApicaItems.BUILDING_STAFF.get(), magazineDecorator);
    }

    private static void registerItemProperties() {
        ItemProperties.register(ApicaItems.MAGAZINE.get(),
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_fluid"),
                (stack, level, entity, seed) -> {
                    String fluidId = MagazineFluidData.getFluidId(stack);
                    if (fluidId.contains("honey")) return 0.25f;
                    if (fluidId.contains("royal_jelly")) return 0.5f;
                    if (fluidId.contains("nectar")) return 0.75f;
                    return 0.0f;
                });
    }

}
