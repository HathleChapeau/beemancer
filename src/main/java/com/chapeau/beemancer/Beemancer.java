/**
 * ============================================================
 * [Beemancer.java]
 * Description: Point d'entrée principal du mod Beemancer
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                         |
 * |-------------------------|--------------------------------|
 * | BeemancerRegistries     | Enregistrement des registres   |
 * | BeemancerEventHandlers  | Gestion des événements         |
 * | ClientSetup             | Configuration client           |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.client.ClientSetup;
import com.chapeau.beemancer.common.block.pollenpot.PollenPotEvents;
import com.chapeau.beemancer.common.block.storage.StorageEvents;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.core.command.BeemancerCommands;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import com.chapeau.beemancer.common.entity.mount.RidingSettingsLoader;
import com.chapeau.beemancer.content.gene.GeneInit;
import com.chapeau.beemancer.core.bee.BeeSpeciesManager;
import com.chapeau.beemancer.core.bee.BiomeTemperatureManager;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.network.BeemancerNetwork;
import com.chapeau.beemancer.core.network.packets.CodexSyncPacket;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeSerializers;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

@Mod(Beemancer.MOD_ID)
public class Beemancer {

    public static final String MOD_ID = "beemancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public Beemancer(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Beemancer...");

        registerAllRegistries(modEventBus);
        registerModEventListeners(modEventBus);
        registerForgeEventListeners();
        registerClientSetup(modEventBus);

        LOGGER.info("Beemancer initialized!");
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    // =========================================================================
    // REGISTRATION - REGISTRIES
    // =========================================================================

    private void registerAllRegistries(IEventBus modEventBus) {
        // Core registries
        BeemancerBlocks.register(modEventBus);
        BeemancerItems.register(modEventBus);
        BeemancerBlockEntities.register(modEventBus);
        BeemancerEntities.register(modEventBus);

        // UI and creative tabs
        BeemancerMenus.register(modEventBus);
        BeemancerCreativeTabs.register(modEventBus);

        // Attachments and sounds
        BeemancerAttachments.register(modEventBus);
        BeemancerSounds.register(modEventBus);

        // Fluids and recipes
        BeemancerFluids.register(modEventBus);
        BeemancerRecipeTypes.register(modEventBus);
        BeemancerRecipeSerializers.register(modEventBus);

        // Network
        BeemancerNetwork.register(modEventBus);
    }

    // =========================================================================
    // REGISTRATION - MOD EVENT LISTENERS
    // =========================================================================

    private void registerModEventListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::onRegisterCapabilities);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GeneInit.registerAllGenes();
            LOGGER.info("Gene system initialized with {} genes", GeneRegistry.getAllGenes().size());
        });
    }

    private void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(BeemancerEntities.MAGIC_BEE.get(), MagicBeeEntity.createAttributes().build());
        event.put(BeemancerEntities.RIDEABLE_BEE.get(), RideableBeeEntity.createAttributes().build());
    }

    private void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        registerFluidCapabilities(event);
        registerItemCapabilities(event);
    }

    // =========================================================================
    // REGISTRATION - FORGE EVENT LISTENERS
    // =========================================================================

    private void registerForgeEventListeners() {
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.register(StorageEvents.class);
        NeoForge.EVENT_BUS.register(MultiblockEvents.class);
        NeoForge.EVENT_BUS.register(PollenPotEvents.class);
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Loading Beemancer data configurations...");
        loadDataConfigurations(event);
        LOGGER.info("Beemancer data configurations loaded!");
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        MultiblockEvents.clearAll();
        LOGGER.info("Beemancer server caches cleared");
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncCodexDataToPlayer(player);
        }
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        BeemancerCommands.register(event.getDispatcher());
        LOGGER.info("Beemancer commands registered");
    }

    // =========================================================================
    // REGISTRATION - CLIENT
    // =========================================================================

    private void registerClientSetup(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register(modEventBus);
        }
    }

    // =========================================================================
    // CAPABILITIES REGISTRATION
    // =========================================================================

    private void registerFluidCapabilities(RegisterCapabilitiesEvent event) {
        // Simple fluid handlers
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.MANUAL_CENTRIFUGE.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.HONEY_TANK.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.CREATIVE_TANK.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.HONEY_PIPE.get(),
                (be, side) -> be.getBuffer()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.CRYSTALLIZER.get(),
                (be, side) -> be.getInputTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.INFUSER.get(),
                (be, side) -> be.getHoneyTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.HONEY_RESERVOIR.get(),
                (be, side) -> be
        );

        // Conditional fluid handlers
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.MULTIBLOCK_TANK.get(),
                (be, side) -> be.isValidCuboid() ? be.getFluidTank() : null
        );

        // Directional fluid handlers
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.POWERED_CENTRIFUGE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputTank() : be.getFuelTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.ALEMBIC.get(),
                (be, side) -> {
                    if (side == Direction.DOWN) return be.getNectarTank();
                    if (side == Direction.UP) return be.getRoyalJellyTank();
                    return be.getHoneyTank();
                }
        );
    }

    private void registerItemCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BeemancerBlockEntities.ITEM_PIPE.get(),
                (be, side) -> be.getBuffer()
        );
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private void loadDataConfigurations(ServerStartingEvent event) {
        var server = event.getServer();

        BeeSpeciesManager.load(server);
        BiomeTemperatureManager.load(server);
        BreedingManager.loadCombinations(server);
        BeeBehaviorManager.load(server);
        RidingSettingsLoader.load(server);
        CodexManager.load(server);
    }

    // =========================================================================
    // PLAYER SYNC
    // =========================================================================

    private void syncCodexDataToPlayer(ServerPlayer player) {
        CodexPlayerData data = player.getData(BeemancerAttachments.CODEX_DATA);
        PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
        LOGGER.debug("Synced codex data to player {}", player.getName().getString());
    }
}
