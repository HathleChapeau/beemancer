/**
 * ============================================================
 * [CodexUnlockPacket.java]
 * Description: Packet client→server pour débloquer un node du Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPlayerData     | Données joueur       | Déblocage du node              |
 * | CodexManager        | Validation node      | Vérification existence         |
 * | BeemancerAttachments| Accès aux données    | Récupération attachment        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (envoi lors du clic sur un node)
 * - BeemancerNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CodexUnlockPacket(String nodeFullId) implements CustomPacketPayload {

    public static final Type<CodexUnlockPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "codex_unlock"));

    public static final StreamCodec<FriendlyByteBuf, CodexUnlockPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, CodexUnlockPacket::nodeFullId,
        CodexUnlockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CodexUnlockPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CodexNode node = CodexManager.getNode(packet.nodeFullId);
                if (node == null) {
                    Beemancer.LOGGER.warn("Player {} tried to unlock unknown node: {}", 
                        player.getName().getString(), packet.nodeFullId);
                    return;
                }

                CodexPlayerData data = player.getData(BeemancerAttachments.CODEX_DATA);
                if (data.unlock(node)) {
                    Beemancer.LOGGER.debug("Player {} unlocked codex node: {}", 
                        player.getName().getString(), packet.nodeFullId);
                    
                    // Sync back to client
                    PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
                }
            }
        });
    }
}
