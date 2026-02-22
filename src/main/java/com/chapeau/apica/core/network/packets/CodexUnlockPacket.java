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
 * | ApicaAttachments| Accès aux données    | Récupération attachment        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (envoi lors du clic sur un node)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.core.registry.ApicaAttachments;
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
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "codex_unlock"));

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
                    Apica.LOGGER.warn("Player {} tried to unlock unknown node: {}",
                        player.getName().getString(), packet.nodeFullId);
                    return;
                }

                CodexPlayerData data = player.getData(ApicaAttachments.CODEX_DATA);
                if (data.unlock(node)) {
                    long currentDay = player.serverLevel().getDayTime() / 24000L;
                    data.recordUnlockDay(packet.nodeFullId, currentDay);
                    player.setData(ApicaAttachments.CODEX_DATA, data);

                    Apica.LOGGER.debug("Player {} unlocked codex node: {} on day {}",
                        player.getName().getString(), packet.nodeFullId, currentDay);
                } else if (data.isUnlocked(packet.nodeFullId)) {
                    Apica.LOGGER.debug("Player {} requested unlock for already-unlocked node: {} — resyncing",
                        player.getName().getString(), packet.nodeFullId);
                }

                // Always sync back — corrects any client-server desync
                PacketDistributor.sendToPlayer(player, new CodexSyncPacket(data));
            }
        });
    }
}
