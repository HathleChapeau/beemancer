/**
 * ============================================================
 * [Apica.java]
 * Description: Point d'entrée principal du mod Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                         |
 * |-------------------------|--------------------------------|
 * | ApicaRegistries     | Enregistrement des registres   |
 * | ApicaEventHandlers  | Gestion des événements         |
 * | ClientSetup             | Configuration client           |
 * ------------------------------------------------------------
 *
 * ============================================================
 */
package com.chapeau.apica;

import com.chapeau.apica.client.ClientSetup;
import com.chapeau.apica.common.block.pollenpot.PollenPotEvents;
import com.chapeau.apica.common.block.storage.StorageBarrelBlock;
import com.chapeau.apica.common.block.storage.StorageEvents;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.book.CodexBookManager;
import com.chapeau.apica.common.quest.QuestManager;
import com.chapeau.apica.core.command.ApicaCommands;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.entity.bee.MagicBeeEntity;
import com.chapeau.apica.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeConfigManager;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.core.entity.InteractionMarkerEntity;
import com.chapeau.apica.core.entity.InteractionMarkerTypes;
import com.chapeau.apica.content.gene.GeneInit;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.bee.BiomeTemperatureManager;
import com.chapeau.apica.core.behavior.BeeBehaviorManager;
import com.chapeau.apica.core.breeding.BreedingManager;
import com.chapeau.apica.core.gene.GeneRegistry;
import com.chapeau.apica.core.multiblock.MultiblockEvents;
import com.chapeau.apica.core.network.ApicaNetwork;
import com.chapeau.apica.core.util.SplitItemHandler;
import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.network.packets.QuestSyncPacket;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.recipe.ApicaRecipeSerializers;
import com.chapeau.apica.core.recipe.ApicaRecipeTypes;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.chapeau.apica.core.registry.ApicaCreativeTabs;
import com.chapeau.apica.core.registry.ApicaEntities;
import com.chapeau.apica.core.registry.ApicaFeatures;
import com.chapeau.apica.core.registry.ApicaFluids;
import com.chapeau.apica.core.registry.ApicaItems;
import com.chapeau.apica.core.registry.ApicaMenus;
import com.chapeau.apica.core.registry.ApicaSounds;
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

@Mod(Apica.MOD_ID)
public class Apica {

    public static final String MOD_ID = "apica";
    public static final Logger LOGGER = LogUtils.getLogger();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public Apica(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Apica...");

        registerAllRegistries(modEventBus);
        registerModEventListeners(modEventBus);
        registerForgeEventListeners();
        registerClientSetup(modEventBus);

        // Hoverbike config (fichiers JSON dans config/apica/hoverbike/)
        HoverbikeConfigManager.init();

        LOGGER.info("Apica initialized!");
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
        ApicaBlocks.register(modEventBus);
        ApicaItems.register(modEventBus);
        ApicaBlockEntities.register(modEventBus);
        ApicaEntities.register(modEventBus);

        // UI and creative tabs
        ApicaMenus.register(modEventBus);
        ApicaCreativeTabs.register(modEventBus);

        // Attachments and sounds
        ApicaAttachments.register(modEventBus);
        ApicaSounds.register(modEventBus);

        // Fluids and recipes
        ApicaFluids.register(modEventBus);
        ApicaRecipeTypes.register(modEventBus);
        ApicaRecipeSerializers.register(modEventBus);

        // Worldgen features
        ApicaFeatures.register(modEventBus);

        // Particles
        ApicaParticles.register(modEventBus);

        // Network
        ApicaNetwork.register(modEventBus);
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
            // Gene system
            GeneInit.registerAllGenes();
            LOGGER.info("Gene system initialized with {} genes", GeneRegistry.getAllGenes().size());

            // Interaction marker types
            InteractionMarkerTypes.init();
        });
    }

    private void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {
        event.put(ApicaEntities.MAGIC_BEE.get(), MagicBeeEntity.createAttributes().build());
        event.put(ApicaEntities.DELIVERY_BEE.get(), DeliveryBeeEntity.createAttributes().build());
        event.put(ApicaEntities.HOVERBIKE.get(), HoverbikeEntity.createAttributes().build());
        event.put(ApicaEntities.INTERACTION_MARKER.get(), InteractionMarkerEntity.createAttributes().build());
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
        NeoForge.EVENT_BUS.addListener(StorageBarrelBlock::onLeftClickBlock);
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Loading Apica data configurations...");
        loadDataConfigurations(event);
        LOGGER.info("Apica data configurations loaded!");
    }

    private void onServerStopping(final ServerStoppingEvent event) {
        MultiblockEvents.clearAll();
        LOGGER.info("Apica server caches cleared");
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Vérifier les quêtes OBTAIN côté serveur avant de sync
            // (le joueur peut avoir des items dans l'inventaire depuis la dernière session)
            QuestManager.checkObtainQuests(player);

            syncCodexDataToPlayer(player);
            syncQuestDataToPlayer(player);
        }
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        ApicaCommands.register(event.getDispatcher());
        LOGGER.info("Apica commands registered");
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
                ApicaBlockEntities.MANUAL_CENTRIFUGE.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.HONEY_TANK.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.CREATIVE_TANK.get(),
                (be, side) -> be.getFluidTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.LIQUID_PIPE.get(),
                (be, side) -> be.getBuffer()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.CRYSTALLIZER.get(),
                (be, side) -> (side == Direction.UP || side == Direction.DOWN) ? be.getInputTank() : null
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.INFUSER.get(),
                (be, side) -> be.getHoneyTank()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.HONEY_RESERVOIR.get(),
                (be, side) -> {
                    // Si le reservoir est lié à un controleur multibloc, déléguer entièrement
                    if (be.getControllerPos() != null) {
                        var provider = be.findCapabilityProvider();
                        if (provider != null) {
                            return provider.getFluidHandlerForBlock(be.getBlockPos(), side);
                        }
                        // Lié mais controleur indisponible: ne PAS exposer le tank propre
                        return null;
                    }
                    // Reservoir standalone (altar, etc.): expose son propre tank
                    return be;
                }
        );

        // Conditional fluid handlers
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.MULTIBLOCK_TANK.get(),
                (be, side) -> be.isFormed() ? be.getFluidTank() : null
        );

        // Directional fluid handlers
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.POWERED_CENTRIFUGE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputTank() : be.getFuelTank()
        );

        // Alembic Heart multibloc: delegation IOConfig quand forme, fallback honey quand standalone
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.ALEMBIC_HEART.get(),
                (be, side) -> be.isFormed()
                    ? be.getFluidHandlerForBlock(be.getBlockPos(), side)
                    : be.getHoneyTank()
        );

        // Infuser Heart multibloc: pas de capability directe quand forme (pipes passent par les reservoirs)
        // event.registerBlockEntity(
        //         Capabilities.FluidHandler.BLOCK,
        //         ApicaBlockEntities.INFUSER_HEART.get(),
        //         (be, side) -> be.isFormed() ? null : be.getHoneyTank()
        // );

        // Centrifuge Heart multibloc: pas de capability directe (pipes passent par les reservoirs)
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.CENTRIFUGE_HEART.get(),
                (be, side) -> null
        );

        // Apica Furnaces: fuel tank accessible de tous les cotes
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.HONEY_FURNACE.get(),
                (be, side) -> be.getFuelTank()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.ROYAL_FURNACE.get(),
                (be, side) -> be.getFuelTank()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.NECTAR_FURNACE.get(),
                (be, side) -> be.getFuelTank()
        );

        // Liquid Trash Can: accepte tout fluide, void immediat
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ApicaBlockEntities.LIQUID_TRASH_CAN.get(),
                (be, side) -> be.getFluidHandler()
        );
    }

    private void registerItemCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.ITEM_PIPE.get(),
                (be, side) -> be.getFilteredBuffer()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.ITEM_PIPE_MK2.get(),
                (be, side) -> be.getFilteredBuffer()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.ITEM_PIPE_MK3.get(),
                (be, side) -> be.getFilteredBuffer()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.ITEM_PIPE_MK4.get(),
                (be, side) -> be.getFilteredBuffer()
        );

        // Crystallizer: extraction seulement du crystal produit (toutes directions)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.CRYSTALLIZER.get(),
                (be, side) -> be.getOutputSlot()
        );

        // Honey Reservoir: delegation au controleur multibloc si formé
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.HONEY_RESERVOIR.get(),
                (be, side) -> {
                    if (be.getControllerPos() != null) {
                        var provider = be.findCapabilityProvider();
                        if (provider != null) {
                            return provider.getItemHandlerForBlock(be.getBlockPos(), side);
                        }
                        return null;
                    }
                    return null;
                }
        );

        // Centrifuge Heart: pas de capability directe (pipes passent par les reservoirs)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.CENTRIFUGE_HEART.get(),
                (be, side) -> null
        );

        // Apica Furnaces: input accessible de tous les cotes sauf bas, output par le bas
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.HONEY_FURNACE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputSlots() : be.getInputSlots()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.ROYAL_FURNACE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputSlots() : be.getInputSlots()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.NECTAR_FURNACE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputSlots() : be.getInputSlots()
        );

        // Storage Barrel: expose automation handler
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.STORAGE_BARREL.get(),
                (be, side) -> be.getAutomationHandler()
        );

        // Trash Can: accepte tout, void immediat
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ApicaBlockEntities.TRASH_CAN.get(),
                (be, side) -> be.getAutomationHandler()
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
        CodexManager.load(server);
        CodexBookManager.load(server);
        QuestManager.load(server);
        com.chapeau.apica.core.config.InjectionConfigManager.load(server);
        com.chapeau.apica.core.config.ResonatorConfigManager.load(server);
    }

    // =========================================================================
    // PLAYER SYNC
    // =========================================================================

    private void syncCodexDataToPlayer(ServerPlayer player) {
        CodexPlayerData data = player.getData(ApicaAttachments.CODEX_DATA);

        int unlockedCount = data.getUnlockedNodes().size();
        int discoveredCount = data.getDiscoveredNodes().size();
        if (unlockedCount == 0 && discoveredCount == 0 && data.getFirstOpenDay() != -1) {
            LOGGER.warn("Codex data for player {} appears empty (0 unlocked, 0 discovered) despite having a firstOpenDay ({}). Possible data loss.",
                player.getName().getString(), data.getFirstOpenDay());
        }

        PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
        LOGGER.info("Synced codex data to player {}: {} unlocked, {} discovered nodes",
            player.getName().getString(), unlockedCount, discoveredCount);
    }

    private void syncQuestDataToPlayer(ServerPlayer player) {
        QuestPlayerData data = player.getData(ApicaAttachments.QUEST_DATA);
        PacketDistributor.sendToPlayer(player, new QuestSyncPacket(data));
        LOGGER.info("Synced quest data to player {}: {} completed quests",
            player.getName().getString(), data.getCompletedQuests().size());
    }
}
