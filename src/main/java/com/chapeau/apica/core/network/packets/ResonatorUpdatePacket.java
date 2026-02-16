/**
 * ============================================================
 * [ResonatorUpdatePacket.java]
 * Description: Packet C2S pour envoyer les parametres du resonateur au serveur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorBlockEntity    | BE cible             | Application des changements    |
 * | ResonatorMenu           | Menu actif           | Verification container         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorScreen (envoi des parametres)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.resonator.ResonatorBlockEntity;
import com.chapeau.apica.common.menu.ResonatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResonatorUpdatePacket(BlockPos pos, int paramIndex, int value)
        implements CustomPacketPayload {

    public static final Type<ResonatorUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "resonator_update"));

    public static final StreamCodec<FriendlyByteBuf, ResonatorUpdatePacket> STREAM_CODEC =
            StreamCodec.of(ResonatorUpdatePacket::write, ResonatorUpdatePacket::read);

    private static void write(FriendlyByteBuf buf, ResonatorUpdatePacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeInt(packet.paramIndex());
        buf.writeInt(packet.value());
    }

    private static ResonatorUpdatePacket read(FriendlyByteBuf buf) {
        return new ResonatorUpdatePacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResonatorUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ResonatorMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof ResonatorBlockEntity be) {
                switch (packet.paramIndex()) {
                    case 0 -> be.setFrequency(packet.value());
                    case 1 -> be.setAmplitude(packet.value());
                    case 2 -> be.setPhase(packet.value());
                    case 3 -> be.setHarmonics(packet.value());
                }
            }
        });
    }
}
