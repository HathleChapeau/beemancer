/**
 * ============================================================
 * [DubstepRadioEditPacket.java]
 * Description: Packet C2S pour activer/desactiver un pitch dans la grille du sequenceur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | DubstepRadioBlockEntity  | BE cible             | Application du changement      |
 * | TrackData                | Donnees track        | setPitchActive                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioScreen (envoi des edits)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.radio.DubstepRadioBlockEntity;
import com.chapeau.apica.common.data.TrackData;
import com.chapeau.apica.common.menu.DubstepRadioMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DubstepRadioEditPacket(BlockPos pos, int trackIndex, int stepIndex,
                                     int pitch, boolean activate)
        implements CustomPacketPayload {

    public static final Type<DubstepRadioEditPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "dubstep_radio_edit"));

    public static final StreamCodec<FriendlyByteBuf, DubstepRadioEditPacket> STREAM_CODEC =
            StreamCodec.of(DubstepRadioEditPacket::write, DubstepRadioEditPacket::read);

    private static void write(FriendlyByteBuf buf, DubstepRadioEditPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeByte(packet.trackIndex());
        buf.writeByte(packet.stepIndex());
        buf.writeByte(packet.pitch());
        buf.writeBoolean(packet.activate());
    }

    private static DubstepRadioEditPacket read(FriendlyByteBuf buf) {
        return new DubstepRadioEditPacket(
                buf.readBlockPos(), buf.readByte(), buf.readByte(),
                buf.readByte(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DubstepRadioEditPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof DubstepRadioMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof DubstepRadioBlockEntity be) {
                TrackData track = be.getSequenceData().getTrack(packet.trackIndex());
                if (track != null) {
                    track.setPitchActive(packet.stepIndex(), packet.pitch(), packet.activate());
                    be.setChanged();
                }
            }
        });
    }
}
