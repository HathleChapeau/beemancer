/**
 * ============================================================
 * [BeemancerNetwork.java]
 * Description: Enregistrement des packets réseau
 * ============================================================
 */
package com.chapeau.beemancer.core.network;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.network.packets.BeeCreatorActionPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Beemancer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class BeemancerNetwork {
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Beemancer.MOD_ID);
        
        registrar.playToServer(
                BeeCreatorActionPacket.TYPE,
                BeeCreatorActionPacket.STREAM_CODEC,
                BeeCreatorActionPacket::handle
        );
    }
}
