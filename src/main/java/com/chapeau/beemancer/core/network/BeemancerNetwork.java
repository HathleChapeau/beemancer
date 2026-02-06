/**
 * ============================================================
 * [BeemancerNetwork.java]
 * Description: Enregistrement des packets réseau (pattern Create)
 * ============================================================
 */
package com.chapeau.beemancer.core.network;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.network.packets.BeeCreatorActionPacket;
import com.chapeau.beemancer.core.network.packets.CodexFirstOpenPacket;
import com.chapeau.beemancer.core.network.packets.CodexSyncPacket;
import com.chapeau.beemancer.core.network.packets.CodexUnlockPacket;
import com.chapeau.beemancer.core.network.packets.QuestSyncPacket;
import com.chapeau.beemancer.core.network.packets.StorageItemsSyncPacket;
import com.chapeau.beemancer.core.network.packets.StorageRequestPacket;
import com.chapeau.beemancer.core.network.packets.InterfaceActionPacket;
import com.chapeau.beemancer.core.network.packets.StorageTaskCancelPacket;
import com.chapeau.beemancer.core.network.packets.StorageTasksSyncPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class BeemancerNetwork {

    /**
     * Called from Beemancer constructor via modEventBus.addListener()
     */
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Beemancer.MOD_ID);

        // Client to Server packets
        registrar.playToServer(
                BeeCreatorActionPacket.TYPE,
                BeeCreatorActionPacket.STREAM_CODEC,
                BeeCreatorActionPacket::handle
        );

        registrar.playToServer(
                CodexUnlockPacket.TYPE,
                CodexUnlockPacket.STREAM_CODEC,
                CodexUnlockPacket::handle
        );

        registrar.playToServer(
                CodexFirstOpenPacket.TYPE,
                CodexFirstOpenPacket.STREAM_CODEC,
                CodexFirstOpenPacket::handle
        );

        registrar.playToServer(
                StorageRequestPacket.TYPE,
                StorageRequestPacket.STREAM_CODEC,
                StorageRequestPacket::handle
        );

        registrar.playToServer(
                StorageTaskCancelPacket.TYPE,
                StorageTaskCancelPacket.STREAM_CODEC,
                StorageTaskCancelPacket::handle
        );

        registrar.playToServer(
                InterfaceActionPacket.TYPE,
                InterfaceActionPacket.STREAM_CODEC,
                InterfaceActionPacket::handle
        );

        // Server to Client packets
        registrar.playToClient(
                CodexSyncPacket.TYPE,
                CodexSyncPacket.STREAM_CODEC,
                CodexSyncPacket::handle
        );

        registrar.playToClient(
                StorageItemsSyncPacket.TYPE,
                StorageItemsSyncPacket.STREAM_CODEC,
                StorageItemsSyncPacket::handle
        );

        registrar.playToClient(
                StorageTasksSyncPacket.TYPE,
                StorageTasksSyncPacket.STREAM_CODEC,
                StorageTasksSyncPacket::handle
        );

        registrar.playToClient(
                QuestSyncPacket.TYPE,
                QuestSyncPacket.STREAM_CODEC,
                QuestSyncPacket::handle
        );
    }

    /**
     * Register network to mod event bus (called from main mod class)
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BeemancerNetwork::registerPayloads);
    }
}
