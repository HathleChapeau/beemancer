/**
 * ============================================================
 * [LaunchVelocityPacket.java]
 * Description: Packet S2C pour envoyer une velocite non-clampee au client
 * ============================================================
 *
 * Le packet vanilla ClientboundSetEntityMotionPacket clamp la velocite
 * a ±3.9 blocs/tick par axe. Ce packet custom bypass cette limitation
 * en envoyant les doubles directement, puis en appliquant setDeltaMovement
 * cote client.
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | Apica                   | MOD_ID               | ResourceLocation du packet     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LaunchpadBlockEntity (envoi lors du lancement d'un joueur)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LaunchVelocityPacket(int entityId, double vx, double vy, double vz) implements CustomPacketPayload {

    public static final Type<LaunchVelocityPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "launch_velocity"));

    public static final StreamCodec<FriendlyByteBuf, LaunchVelocityPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LaunchVelocityPacket decode(FriendlyByteBuf buf) {
            return new LaunchVelocityPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(FriendlyByteBuf buf, LaunchVelocityPacket packet) {
            buf.writeVarInt(packet.entityId);
            buf.writeDouble(packet.vx);
            buf.writeDouble(packet.vy);
            buf.writeDouble(packet.vz);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LaunchVelocityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId);
            if (entity != null) {
                entity.setDeltaMovement(packet.vx, packet.vy, packet.vz);
                entity.hurtMarked = true;
            }
        });
    }
}
