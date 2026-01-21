/**
 * ============================================================
 * [BeemancerNetwork.java]
 * Description: Enregistrement des packets réseau (pattern Create)
 * ============================================================
 */
package com.chapeau.beemancer.core.network;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.network.packets.BeeCreatorActionPacket;
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
    }
    
    /**
     * Register network to mod event bus (called from main mod class)
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BeemancerNetwork::registerPayloads);
    }
}
