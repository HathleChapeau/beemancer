/**
 * ============================================================
 * [Beemancer.java]
 * Description: Point d'entrée principal du mod Beemancer
 * ============================================================
 */
package com.chapeau.beemancer;

import com.chapeau.beemancer.client.ClientSetup;
import com.chapeau.beemancer.common.block.storage.StorageEvents;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.content.gene.GeneInit;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.network.BeemancerNetwork;
import com.chapeau.beemancer.core.network.packets.CodexSyncPacket;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeSerializers;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.registry.*;
import net.minecraft.resources.ResourceLocation;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

@Mod(Beemancer.MOD_ID)
public class Beemancer {
    public static final String MOD_ID = "beemancer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public Beemancer(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Beemancer...");

        BeemancerBlocks.register(modEventBus);
        BeemancerItems.register(modEventBus);
        BeemancerBlockEntities.register(modEventBus);
        BeemancerMenus.register(modEventBus);
        BeemancerCreativeTabs.register(modEventBus);
        BeemancerEntities.register(modEventBus);
        BeemancerAttachments.register(modEventBus);
        BeemancerSounds.register(modEventBus);
        BeemancerFluids.register(modEventBus);
        BeemancerRecipeTypes.register(modEventBus);
        BeemancerRecipeSerializers.register(modEventBus);

        BeemancerNetwork.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.register(StorageEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register(modEventBus);
        }

        LOGGER.info("Beemancer initialized!");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GeneInit.registerAllGenes();
            LOGGER.info("Gene system initialized with {} genes",
                    com.chapeau.beemancer.core.gene.GeneRegistry.getAllGenes().size());
        });
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(BeemancerEntities.MAGIC_BEE.get(), MagicBeeEntity.createAttributes().build());
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.MANUAL_CENTRIFUGE.get(), (be, side) -> be.getFluidTank());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.HONEY_TANK.get(), (be, side) -> be.getFluidTank());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.CREATIVE_TANK.get(), (be, side) -> be.getFluidTank());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.HONEY_PIPE.get(), (be, side) -> be.getBuffer());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.POWERED_CENTRIFUGE.get(),
                (be, side) -> side == Direction.DOWN ? be.getOutputTank() : be.getFuelTank());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.CRYSTALLIZER.get(),
                (be, side) -> be.getInputTank());

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.ALEMBIC.get(),
                (be, side) -> {
                    if (side == Direction.DOWN) return be.getNectarTank();
                    if (side == Direction.UP) return be.getRoyalJellyTank();
                    return be.getHoneyTank();
                });

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                BeemancerBlockEntities.INFUSER.get(),
                (be, side) -> be.getHoneyTank());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                BeemancerBlockEntities.ITEM_PIPE.get(),
                (be, side) -> be.getBuffer());
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Loading Beemancer data configurations...");
        BreedingManager.loadCombinations(event.getServer());
        BeeBehaviorManager.load(event.getServer());
        CodexManager.load(event.getServer());
        LOGGER.info("Beemancer data configurations loaded!");
    }

    private void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CodexPlayerData data = player.getData(BeemancerAttachments.CODEX_DATA);
            PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
            LOGGER.debug("Synced codex data to player {}", player.getName().getString());
        }
    }
}
