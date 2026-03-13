/**
 * ============================================================
 * [WaveMixerTrackPacket.java]
 * Description: Packet C2S pour modifier une track (ajout, suppression, mute, solo, volume, instrument)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | WaveMixerBlockEntity  | BE cible             | Modification des tracks        |
 * | DubstepInstrument        | Instruments          | Changement d'instrument        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - WaveMixerScreen / InstrumentColumnWidget (envoi des changements)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.radio.WaveMixerBlockEntity;
import com.chapeau.apica.common.data.DubstepInstrument;
import com.chapeau.apica.common.data.SequenceData;
import com.chapeau.apica.common.data.TrackData;
import com.chapeau.apica.common.menu.WaveMixerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WaveMixerTrackPacket(BlockPos pos, int trackIndex, int action, int value)
        implements CustomPacketPayload {

    /** Actions */
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int MUTE = 2;
    public static final int SOLO = 3;
    public static final int VOLUME = 4;
    public static final int INSTRUMENT = 5;

    public static final Type<WaveMixerTrackPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "wave_mixer_track"));

    public static final StreamCodec<FriendlyByteBuf, WaveMixerTrackPacket> STREAM_CODEC =
            StreamCodec.of(WaveMixerTrackPacket::write, WaveMixerTrackPacket::read);

    private static void write(FriendlyByteBuf buf, WaveMixerTrackPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeByte(packet.trackIndex());
        buf.writeByte(packet.action());
        buf.writeInt(packet.value());
    }

    private static WaveMixerTrackPacket read(FriendlyByteBuf buf) {
        return new WaveMixerTrackPacket(buf.readBlockPos(), buf.readByte(), buf.readByte(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaveMixerTrackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof WaveMixerMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof WaveMixerBlockEntity be) {
                SequenceData data = be.getSequenceData();
                switch (packet.action()) {
                    case ADD -> data.addTrack(DubstepInstrument.fromIndex(packet.value()));
                    case REMOVE -> data.removeTrack(packet.trackIndex());
                    case MUTE -> {
                        TrackData track = data.getTrack(packet.trackIndex());
                        if (track != null) track.setMuted(packet.value() == 1);
                    }
                    case SOLO -> {
                        TrackData track = data.getTrack(packet.trackIndex());
                        if (track != null) track.setSolo(packet.value() == 1);
                    }
                    case VOLUME -> {
                        TrackData track = data.getTrack(packet.trackIndex());
                        if (track != null) track.setVolume(packet.value() / 100.0f);
                    }
                    case INSTRUMENT -> {
                        TrackData track = data.getTrack(packet.trackIndex());
                        if (track != null) track.setInstrument(DubstepInstrument.fromIndex(packet.value()));
                    }
                }
                be.setChanged();
            }
        });
    }
}
