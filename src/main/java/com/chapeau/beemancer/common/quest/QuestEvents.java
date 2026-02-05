/**
 * ============================================================
 * [QuestEvents.java]
 * Description: Gestionnaire d'événements pour la détection des quêtes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | QuestManager        | Gestion des quêtes   | Recherche et completion        |
 * | Quest               | Définition quête     | Vérification des conditions    |
 * | BeemancerAttachments| Données joueur       | Accès aux quêtes complétées    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Machines (extraction d'items)
 * - IncubatorBlockEntity (extraction d'abeilles)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.network.packets.QuestSyncPacket;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Classe utilitaire pour déclencher la completion des quêtes.
 * Appelée par les machines et l'incubateur quand un joueur extrait un item.
 */
public class QuestEvents {

    /**
     * Appelé quand un joueur extrait un item d'une machine.
     * Vérifie si une quête MACHINE_OUTPUT correspond.
     *
     * @param player Le joueur qui extrait
     * @param machineType Le type de machine (ex: "crystallizer", "centrifuge")
     * @param extractedItem L'item extrait
     */
    public static void onMachineExtract(Player player, String machineType, ItemStack extractedItem) {
        if (player.level().isClientSide() || extractedItem.isEmpty()) {
            return;
        }

        ResourceLocation itemId = extractedItem.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (itemId == null) {
            return;
        }

        Quest quest = QuestManager.findMachineOutputQuest(machineType, itemId);
        if (quest != null && !QuestManager.isQuestCompleted(player, quest.getId())) {
            if (QuestManager.completeQuest(player, quest.getId())) {
                syncQuestData(player);
                Beemancer.LOGGER.info("Player {} completed MACHINE_OUTPUT quest: {}",
                        player.getName().getString(), quest.getId());
            }
        }
    }

    /**
     * Appelé quand un joueur extrait une abeille magique de l'incubateur.
     * Vérifie si une quête BEE_INCUBATOR correspond à l'espèce.
     *
     * @param player Le joueur qui extrait
     * @param species L'espèce de l'abeille extraite
     */
    public static void onBeeIncubatorExtract(Player player, String species) {
        if (player.level().isClientSide() || species == null || species.isEmpty()) {
            return;
        }

        Quest quest = QuestManager.findBeeIncubatorQuest(species);
        if (quest != null && !QuestManager.isQuestCompleted(player, quest.getId())) {
            if (QuestManager.completeQuest(player, quest.getId())) {
                syncQuestData(player);
                Beemancer.LOGGER.info("Player {} completed BEE_INCUBATOR quest: {} (species: {})",
                        player.getName().getString(), quest.getId(), species);
            }
        }
    }

    /**
     * Synchronise les données de quêtes au client après une completion.
     */
    private static void syncQuestData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            QuestPlayerData data = serverPlayer.getData(BeemancerAttachments.QUEST_DATA);
            PacketDistributor.sendToPlayer(serverPlayer, new QuestSyncPacket(data));
        }
    }
}
