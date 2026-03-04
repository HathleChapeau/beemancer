/**
 * ============================================================
 * [DubstepRadioSyncPacket.java]
 * Description: Packet S2C pour synchroniser les donnees completes du sequenceur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | SequenceData             | Donnees completes    | Serialisation/deserialisation  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DubstepRadioBlockEntity (envoi quand joueur ouvre le menu)
 * - DubstepRadioScreen (reception et mise a jour locale)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.data.SequenceData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DubstepRadioSyncPacket(BlockPos pos, CompoundTag sequenceTag)
        implements CustomPacketPayload {

    public static final Type<DubstepRadioSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "dubstep_radio_sync"));

    public static final StreamCodec<FriendlyByteBuf, DubstepRadioSyncPacket> STREAM_CODEC =
            StreamCodec.of(DubstepRadioSyncPacket::write, DubstepRadioSyncPacket::read);

    private static void write(FriendlyByteBuf buf, DubstepRadioSyncPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeNbt(packet.sequenceTag());
    }

    private static DubstepRadioSyncPacket read(FriendlyByteBuf buf) {
        return new DubstepRadioSyncPacket(buf.readBlockPos(), buf.readNbt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DubstepRadioSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof DubstepRadioSyncReceiver receiver) {
                SequenceData data = new SequenceData();
                if (packet.sequenceTag() != null) {
                    data.load(packet.sequenceTag());
                }
                receiver.onSequenceSync(packet.pos(), data);
            }
        });
    }

    /**
     * Interface implementee par DubstepRadioScreen pour recevoir le sync.
     */
    public interface DubstepRadioSyncReceiver {
        void onSequenceSync(BlockPos pos, SequenceData data);
    }
}
