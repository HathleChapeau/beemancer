/**
 * ============================================================
 * [RidingInputPacket.java]
 * Description: Packet pour synchroniser l'input du joueur vers le serveur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | Beemancer               | MOD_ID               | Type packet                    |
 * | RideableBeeEntity       | Entité cible         | Réception de l'input           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - RidingInputHandler.java: Envoi du packet côté client
 * - BeemancerNetwork.java: Enregistrement
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet envoyé du client au serveur pour transmettre l'input de déplacement.
 * Envoyé chaque tick quand le joueur monte une RideableBeeEntity.
 */
public record RidingInputPacket(
    int entityId,
    float forward,
    float strafe,
    boolean jump,
    boolean sprint,
    float cameraYaw
) implements CustomPacketPayload {

    public static final Type<RidingInputPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "riding_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RidingInputPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, RidingInputPacket::entityId,
            ByteBufCodecs.FLOAT, RidingInputPacket::forward,
            ByteBufCodecs.FLOAT, RidingInputPacket::strafe,
            ByteBufCodecs.BOOL, RidingInputPacket::jump,
            ByteBufCodecs.BOOL, RidingInputPacket::sprint,
            ByteBufCodecs.FLOAT, RidingInputPacket::cameraYaw,
            RidingInputPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RidingInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Vérifier que le joueur monte bien l'entité
            Entity vehicle = player.getVehicle();
            if (vehicle == null || vehicle.getId() != packet.entityId) return;
            if (!(vehicle instanceof RideableBeeEntity bee)) return;

            // Transmettre l'input à l'entité
            bee.receiveInput(
                packet.forward,
                packet.strafe,
                packet.jump,
                packet.sprint,
                packet.cameraYaw
            );
        });
    }
}
