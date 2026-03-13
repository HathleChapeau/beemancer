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
import com.chapeau.apica.core.network.packets.DubstepRadioEditPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioTrackPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioTransportPacket;
import com.chapeau.apica.core.network.packets.DubstepRadioSyncPacket;
import com.chapeau.apica.core.network.packets.MagazineEquipPacket;
import com.chapeau.apica.core.network.packets.MagazineReloadPacket;
import com.chapeau.apica.core.network.packets.MagazineReloadStartPacket;
import com.chapeau.apica.core.network.packets.MagazineReloadCompletePacket;
import com.chapeau.apica.core.network.packets.BackpackOpenPacket;
import com.chapeau.apica.core.network.packets.AccessoryEquipPacket;
import com.chapeau.apica.core.network.packets.AccessorySyncPacket;
import com.chapeau.apica.core.network.packets.BeeCreatorUpdatePacket;
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

        // Dubstep Radio C2S
        registrar.playToServer(
                DubstepRadioEditPacket.TYPE,
                DubstepRadioEditPacket.STREAM_CODEC,
                DubstepRadioEditPacket::handle
        );

        registrar.playToServer(
                DubstepRadioTrackPacket.TYPE,
                DubstepRadioTrackPacket.STREAM_CODEC,
                DubstepRadioTrackPacket::handle
        );

        registrar.playToServer(
                DubstepRadioTransportPacket.TYPE,
                DubstepRadioTransportPacket.STREAM_CODEC,
                DubstepRadioTransportPacket::handle
        );

        // Magazine C2S
        registrar.playToServer(
                MagazineEquipPacket.TYPE,
                MagazineEquipPacket.STREAM_CODEC,
                MagazineEquipPacket::handle
        );

        registrar.playToServer(
                MagazineReloadPacket.TYPE,
                MagazineReloadPacket.STREAM_CODEC,
                MagazineReloadPacket::handle
        );

        registrar.playToClient(
                MagazineReloadStartPacket.TYPE,
                MagazineReloadStartPacket.STREAM_CODEC,
                MagazineReloadStartPacket::handle
        );

        registrar.playToServer(
                MagazineReloadCompletePacket.TYPE,
                MagazineReloadCompletePacket.STREAM_CODEC,
                MagazineReloadCompletePacket::handle
        );

        // Bee Creator C2S
        registrar.playToServer(
                BeeCreatorUpdatePacket.TYPE,
                BeeCreatorUpdatePacket.STREAM_CODEC,
                BeeCreatorUpdatePacket::handle
        );

        // Backpack C2S
        registrar.playToServer(
                BackpackOpenPacket.TYPE,
                BackpackOpenPacket.STREAM_CODEC,
                BackpackOpenPacket::handle
        );

        // Accessory C2S
        registrar.playToServer(
                AccessoryEquipPacket.TYPE,
                AccessoryEquipPacket.STREAM_CODEC,
                AccessoryEquipPacket::handle
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

        // Accessory S2C
        registrar.playToClient(
                AccessorySyncPacket.TYPE,
                AccessorySyncPacket.STREAM_CODEC,
                AccessorySyncPacket::handle
        );

        // Dubstep Radio S2C
        registrar.playToClient(
                DubstepRadioSyncPacket.TYPE,
                DubstepRadioSyncPacket.STREAM_CODEC,
                DubstepRadioSyncPacket::handle
        );

    }

    /**
     * Register network to mod event bus (called from main mod class)
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ApicaNetwork::registerPayloads);
    }
}
