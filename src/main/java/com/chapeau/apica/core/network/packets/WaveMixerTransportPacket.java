/**
 * ============================================================
 * [WaveMixerTransportPacket.java]
 * Description: Packet C2S pour play/stop, changement BPM, et gestion des pages
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | WaveMixerBlockEntity  | BE cible             | Transport control              |
 * | SequencePlaybackEngine   | Moteur client        | Demarrage/arret playback       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - WaveMixerScreen / TransportBarWidget (envoi C2S)
 * - WaveMixerBlockEntity (broadcast S2C)
 * - ApicaNetwork (enregistrement bidirectionnel)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.radio.WaveMixerBlockEntity;
import com.chapeau.apica.common.menu.WaveMixerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WaveMixerTransportPacket(BlockPos pos, int action, int bpm, int swing)
        implements CustomPacketPayload {

    public static final int PLAY = 0;
    public static final int STOP = 1;
    public static final int SET_BPM = 2;
    public static final int ADD_PAGE = 4;
    public static final int REMOVE_PAGE = 5;

    public static final Type<WaveMixerTransportPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "wave_mixer_transport"));

    public static final StreamCodec<FriendlyByteBuf, WaveMixerTransportPacket> STREAM_CODEC =
            StreamCodec.of(WaveMixerTransportPacket::write, WaveMixerTransportPacket::read);

    private static void write(FriendlyByteBuf buf, WaveMixerTransportPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeByte(packet.action());
        buf.writeShort(packet.bpm());
        buf.writeByte(packet.swing());
    }

    private static WaveMixerTransportPacket read(FriendlyByteBuf buf) {
        return new WaveMixerTransportPacket(buf.readBlockPos(), buf.readByte(), buf.readUnsignedShort(), buf.readByte());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaveMixerTransportPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof WaveMixerMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof WaveMixerBlockEntity be) {
                switch (packet.action()) {
                    case PLAY -> be.play();
                    case STOP -> be.stop();
                    case SET_BPM -> {
                        be.getSequenceData().setBpm(packet.bpm());
                        be.setChanged();
                    }
                    case ADD_PAGE -> {
                        be.getSequenceData().addPage();
                        be.setChanged();
                    }
                    case REMOVE_PAGE -> {
                        be.getSequenceData().removePage(packet.bpm());
                        be.setChanged();
                    }
                }
            }
        });
    }
}
