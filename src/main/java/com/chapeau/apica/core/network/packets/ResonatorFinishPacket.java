/**
 * ============================================================
 * [ResonatorFinishPacket.java]
 * Description: Packet C2S envoye quand le joueur clique "Finish" apres l'analyse
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorBlockEntity    | BE cible             | completeAnalysis()             |
 * | ResonatorMenu           | Menu actif           | Verification container         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorScreen (envoi au clic Finish)
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

public record ResonatorFinishPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ResonatorFinishPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "resonator_finish"));

    public static final StreamCodec<FriendlyByteBuf, ResonatorFinishPacket> STREAM_CODEC =
            StreamCodec.of(ResonatorFinishPacket::write, ResonatorFinishPacket::read);

    private static void write(FriendlyByteBuf buf, ResonatorFinishPacket packet) {
        buf.writeBlockPos(packet.pos());
    }

    private static ResonatorFinishPacket read(FriendlyByteBuf buf) {
        return new ResonatorFinishPacket(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResonatorFinishPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ResonatorMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof ResonatorBlockEntity resonator) {
                resonator.completeAnalysis(player);
            }
        });
    }
}
