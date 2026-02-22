/**
 * ============================================================
 * [CheckObtainQuestsPacket.java]
 * Description: Packet client→server pour vérifier les quêtes OBTAIN côté serveur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | QuestManager        | Vérification OBTAIN  | Check inventaire serveur       |
 * | QuestSyncPacket     | Sync résultat        | Renvoi des quêtes complétées   |
 * | CodexSyncPacket     | Sync codex           | Renvoi des nodes découverts    |
 * | ApicaAttachments    | Accès aux données    | Récupération attachment        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (envoi à l'ouverture du codex)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.quest.QuestManager;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.registry.ApicaAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

public record CheckObtainQuestsPacket() implements CustomPacketPayload {

    public static final Type<CheckObtainQuestsPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "check_obtain_quests"));

    public static final StreamCodec<FriendlyByteBuf, CheckObtainQuestsPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CheckObtainQuestsPacket decode(FriendlyByteBuf buf) {
            return new CheckObtainQuestsPacket();
        }

        @Override
        public void encode(FriendlyByteBuf buf, CheckObtainQuestsPacket packet) {
            // Pas de données à envoyer
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CheckObtainQuestsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Set<String> newCompletions = QuestManager.checkObtainQuests(player);

                if (!newCompletions.isEmpty()) {
                    Apica.LOGGER.debug("Server-side OBTAIN check for {}: {} quests completed",
                        player.getName().getString(), newCompletions.size());

                    // Sync quest data back to client (quests only — codex state is computed
                    // transiently from quests + unlocked nodes, no need to re-sync codex here)
                    QuestPlayerData questData = player.getData(ApicaAttachments.QUEST_DATA);
                    PacketDistributor.sendToPlayer(player, new QuestSyncPacket(questData));
                }
            }
        });
    }
}
