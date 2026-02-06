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
 * - CodexFirstOpenPacket (après enregistrement premier open)
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record CodexSyncPacket(Set<String> unlockedNodes, Set<String> discoveredNodes,
                               long firstOpenDay, Map<String, Long> unlockDays) implements CustomPacketPayload {

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

            long openDay = buf.readLong();

            int daysSize = buf.readVarInt();
            Map<String, Long> days = new HashMap<>(daysSize);
            for (int i = 0; i < daysSize; i++) {
                String nodeId = buf.readUtf();
                long day = buf.readLong();
                days.put(nodeId, day);
            }

            return new CodexSyncPacket(unlocked, discovered, openDay, days);
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

            buf.writeLong(packet.firstOpenDay);

            buf.writeVarInt(packet.unlockDays.size());
            for (Map.Entry<String, Long> entry : packet.unlockDays.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeLong(entry.getValue());
            }
        }
    };

    public CodexSyncPacket(CodexPlayerData data) {
        this(new HashSet<>(data.getUnlockedNodes()), new HashSet<>(data.getDiscoveredNodes()),
             data.getFirstOpenDay(), new HashMap<>(data.getUnlockDays()));
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
                data.setFirstOpenDay(packet.firstOpenDay);
                data.getUnlockDays().clear();
                data.getUnlockDays().putAll(packet.unlockDays);
                Beemancer.LOGGER.debug("Synced {} unlocked, {} discovered nodes, day {} from server",
                    packet.unlockedNodes.size(), packet.discoveredNodes.size(), packet.firstOpenDay);
            }
        });
    }
}
