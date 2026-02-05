/**
 * ============================================================
 * [QuestSyncPacket.java]
 * Description: Packet server→client pour synchroniser les données des quêtes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | QuestPlayerData     | Données à sync       | Transfert des quêtes complétées|
 * | BeemancerAttachments| Accès aux données    | Mise à jour attachment client  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - QuestEvents (après completion de quête)
 * - PlayerLoggedInEvent (sync initiale)
 * - BeemancerNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.quest.QuestPlayerData;
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

public record QuestSyncPacket(Set<String> completedQuests) implements CustomPacketPayload {

    public static final Type<QuestSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "quest_sync"));

    public static final StreamCodec<FriendlyByteBuf, QuestSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public QuestSyncPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Set<String> quests = new HashSet<>(size);
            for (int i = 0; i < size; i++) {
                quests.add(buf.readUtf());
            }
            return new QuestSyncPacket(quests);
        }

        @Override
        public void encode(FriendlyByteBuf buf, QuestSyncPacket packet) {
            buf.writeVarInt(packet.completedQuests.size());
            for (String questId : packet.completedQuests) {
                buf.writeUtf(questId);
            }
        }
    };

    public QuestSyncPacket(QuestPlayerData data) {
        this(new HashSet<>(data.getCompletedQuests()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                QuestPlayerData data = player.getData(BeemancerAttachments.QUEST_DATA);
                for (String questId : packet.completedQuests) {
                    data.complete(questId);
                }
                Beemancer.LOGGER.debug("Synced {} completed quests from server", packet.completedQuests.size());
            }
        });
    }
}
