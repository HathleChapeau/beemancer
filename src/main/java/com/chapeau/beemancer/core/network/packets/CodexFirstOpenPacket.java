/**
 * ============================================================
 * [CodexFirstOpenPacket.java]
 * Description: Packet client→server pour enregistrer la première ouverture du Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPlayerData     | Données joueur       | Enregistrement firstOpenDay    |
 * | BeemancerAttachments| Accès aux données    | Récupération attachment        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (envoi à la première ouverture)
 * - BeemancerNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CodexFirstOpenPacket() implements CustomPacketPayload {

    public static final Type<CodexFirstOpenPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "codex_first_open"));

    public static final StreamCodec<FriendlyByteBuf, CodexFirstOpenPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CodexFirstOpenPacket decode(FriendlyByteBuf buf) {
            return new CodexFirstOpenPacket();
        }

        @Override
        public void encode(FriendlyByteBuf buf, CodexFirstOpenPacket packet) {
            // Pas de données à envoyer
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CodexFirstOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CodexPlayerData data = player.getData(BeemancerAttachments.CODEX_DATA);
                long currentDay = player.serverLevel().getDayTime() / 24000L;

                if (data.recordFirstOpen(currentDay)) {
                    Beemancer.LOGGER.debug("Player {} first codex open on MC day {}",
                        player.getName().getString(), currentDay);

                    PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
                }
            }
        });
    }
}
