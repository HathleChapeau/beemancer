/**
 * ============================================================
 * [CodexSyncPacket.java]
 * Description: Packet server→client pour synchroniser les données du Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPlayerData     | Données à sync       | Transfert des nodes débloqués  |
 * | BeemancerAttachments| Accès aux données    | Mise à jour attachment client  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexUnlockPacket (après déblocage réussi)
 * - PlayerLoggedInEvent (sync initiale)
 * - BeemancerNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record CodexSyncPacket(Set<String> unlockedNodes, Set<String> discoveredNodes) implements CustomPacketPayload {

    public static final Type<CodexSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "codex_sync"));

    public static final StreamCodec<FriendlyByteBuf, CodexSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CodexSyncPacket decode(FriendlyByteBuf buf) {
            int unlockedSize = buf.readVarInt();
            Set<String> unlocked = new HashSet<>(unlockedSize);
            for (int i = 0; i < unlockedSize; i++) {
                unlocked.add(buf.readUtf());
            }

            int discoveredSize = buf.readVarInt();
            Set<String> discovered = new HashSet<>(discoveredSize);
            for (int i = 0; i < discoveredSize; i++) {
                discovered.add(buf.readUtf());
            }

            return new CodexSyncPacket(unlocked, discovered);
        }

        @Override
        public void encode(FriendlyByteBuf buf, CodexSyncPacket packet) {
            buf.writeVarInt(packet.unlockedNodes.size());
            for (String nodeId : packet.unlockedNodes) {
                buf.writeUtf(nodeId);
            }

            buf.writeVarInt(packet.discoveredNodes.size());
            for (String nodeId : packet.discoveredNodes) {
                buf.writeUtf(nodeId);
            }
        }
    };

    public CodexSyncPacket(CodexPlayerData data) {
        this(new HashSet<>(data.getUnlockedNodes()), new HashSet<>(data.getDiscoveredNodes()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CodexSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CodexPlayerData data = player.getData(BeemancerAttachments.CODEX_DATA);
                data.getUnlockedNodes().clear();
                data.getUnlockedNodes().addAll(packet.unlockedNodes);
                data.getDiscoveredNodes().clear();
                data.getDiscoveredNodes().addAll(packet.discoveredNodes);
                Beemancer.LOGGER.debug("Synced {} unlocked, {} discovered nodes from server",
                    packet.unlockedNodes.size(), packet.discoveredNodes.size());
            }
        });
    }
}
