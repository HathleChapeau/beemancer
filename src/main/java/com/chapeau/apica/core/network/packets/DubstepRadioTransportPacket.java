/**
 * ============================================================
 * [DubstepRadioTransportPacket.java]
 * Description: Packet bidirectionnel pour play/stop et changement BPM/swing
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | DubstepRadioBlockEntity  | BE cible             | Transport control              |
 * | SequencePlaybackEngine   | Moteur client        | Demarrage/arret playback       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen / TransportBarWidget (envoi C2S)
 * - DubstepRadioBlockEntity (broadcast S2C)
 * - ApicaNetwork (enregistrement bidirectionnel)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.radio.DubstepRadioBlockEntity;
import com.chapeau.apica.common.menu.DubstepRadioMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DubstepRadioTransportPacket(BlockPos pos, int action, int bpm, int swing)
        implements CustomPacketPayload {

    public static final int PLAY = 0;
    public static final int STOP = 1;
    public static final int SET_BPM = 2;
    public static final int SET_SWING = 3;

    public static final Type<DubstepRadioTransportPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "dubstep_radio_transport"));

    public static final StreamCodec<FriendlyByteBuf, DubstepRadioTransportPacket> STREAM_CODEC =
            StreamCodec.of(DubstepRadioTransportPacket::write, DubstepRadioTransportPacket::read);

    private static void write(FriendlyByteBuf buf, DubstepRadioTransportPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeByte(packet.action());
        buf.writeShort(packet.bpm());
        buf.writeByte(packet.swing());
    }

    private static DubstepRadioTransportPacket read(FriendlyByteBuf buf) {
        return new DubstepRadioTransportPacket(buf.readBlockPos(), buf.readByte(), buf.readUnsignedShort(), buf.readByte());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DubstepRadioTransportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof DubstepRadioMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof DubstepRadioBlockEntity be) {
                switch (packet.action()) {
                    case PLAY -> be.play();
                    case STOP -> be.stop();
                    case SET_BPM -> {
                        be.getSequenceData().setBpm(packet.bpm());
                        be.setChanged();
                    }
                    case SET_SWING -> {
                        be.getSequenceData().setSwing(packet.swing() / 100.0f);
                        be.setChanged();
                    }
                }
            }
        });
    }
}
