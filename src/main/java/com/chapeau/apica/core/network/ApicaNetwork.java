/**
 * ============================================================
 * [ApicaNetwork.java]
 * Description: Enregistrement des packets réseau (pattern Create)
 * ============================================================
 */
package com.chapeau.apica.core.network;

import com.chapeau.apica.Apica;
import com.chapeau.apica.core.network.packets.CheckObtainQuestsPacket;
import com.chapeau.apica.core.network.packets.CodexFirstOpenPacket;
import com.chapeau.apica.core.network.packets.CodexSyncPacket;
import com.chapeau.apica.core.network.packets.CodexUnlockPacket;
import com.chapeau.apica.core.network.packets.QuestSyncPacket;
import com.chapeau.apica.core.network.packets.StorageItemsSyncPacket;
import com.chapeau.apica.core.network.packets.StorageRequestPacket;
import com.chapeau.apica.core.network.packets.InterfaceActionPacket;
import com.chapeau.apica.core.network.packets.HoverbikeVariantPacket;
import com.chapeau.apica.core.network.packets.HoverbikePartSwapPacket;
import com.chapeau.apica.core.network.packets.HoverbikePartRemovePacket;
import com.chapeau.apica.core.network.packets.StorageTaskCancelPacket;
import com.chapeau.apica.core.network.packets.ItemFilterActionPacket;
import com.chapeau.apica.core.network.packets.LaunchVelocityPacket;
import com.chapeau.apica.core.network.packets.ResonatorFinishPacket;
import com.chapeau.apica.core.network.packets.ResonatorTraitMatchPacket;
import com.chapeau.apica.core.network.packets.ResonatorUpdatePacket;
import com.chapeau.apica.core.network.packets.StorageTasksSyncPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ApicaNetwork {

    /**
     * Called from Apica constructor via modEventBus.addListener()
     */
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Apica.MOD_ID);

        // Client to Server packets
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
                CheckObtainQuestsPacket.TYPE,
                CheckObtainQuestsPacket.STREAM_CODEC,
                CheckObtainQuestsPacket::handle
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

        registrar.playToServer(
                HoverbikeVariantPacket.TYPE,
                HoverbikeVariantPacket.STREAM_CODEC,
                HoverbikeVariantPacket::handle
        );

        registrar.playToServer(
                HoverbikePartSwapPacket.TYPE,
                HoverbikePartSwapPacket.STREAM_CODEC,
                HoverbikePartSwapPacket::handle
        );

        registrar.playToServer(
                HoverbikePartRemovePacket.TYPE,
                HoverbikePartRemovePacket.STREAM_CODEC,
                HoverbikePartRemovePacket::handle
        );

        registrar.playToServer(
                ResonatorUpdatePacket.TYPE,
                ResonatorUpdatePacket.STREAM_CODEC,
                ResonatorUpdatePacket::handle
        );

        registrar.playToServer(
                ResonatorFinishPacket.TYPE,
                ResonatorFinishPacket.STREAM_CODEC,
                ResonatorFinishPacket::handle
        );

        registrar.playToServer(
                ResonatorTraitMatchPacket.TYPE,
                ResonatorTraitMatchPacket.STREAM_CODEC,
                ResonatorTraitMatchPacket::handle
        );

        registrar.playToServer(
                ItemFilterActionPacket.TYPE,
                ItemFilterActionPacket.STREAM_CODEC,
                ItemFilterActionPacket::handle
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

        registrar.playToClient(
                LaunchVelocityPacket.TYPE,
                LaunchVelocityPacket.STREAM_CODEC,
                LaunchVelocityPacket::handle
        );

    }

    /**
     * Register network to mod event bus (called from main mod class)
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ApicaNetwork::registerPayloads);
    }
}
