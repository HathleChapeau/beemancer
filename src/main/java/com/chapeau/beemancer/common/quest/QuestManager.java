/**
 * ============================================================
 * [QuestManager.java]
 * Description: Gestionnaire central des quêtes - chargement et vérification
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Quest               | Définition quête     | Stockage et accès              |
 * | QuestPlayerData     | Données joueur       | Vérification completion        |
 * | Gson                | Parsing JSON         | Chargement du fichier          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - QuestEvents (détection completion)
 * - CodexScreen (vérification OBTAIN)
 * - CodexManager (calcul états nodes)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.*;

public class QuestManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, Quest> QUESTS_BY_ID = new HashMap<>();
    private static final Map<String, Quest> QUESTS_BY_NODE = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Charge depuis le serveur (appelé au démarrage serveur).
     */
    public static void load(MinecraftServer server) {
        load(server.getResourceManager());
    }

    /**
     * Charge depuis un ResourceManager (client ou serveur).
     */
    public static void load(ResourceManager resourceManager) {
        QUESTS_BY_ID.clear();
        QUESTS_BY_NODE.clear();

        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "quests/quests.json"
        );

        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(path);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    JsonArray questsArray = root.getAsJsonArray("quests");

                    for (JsonElement element : questsArray) {
                        JsonObject questJson = element.getAsJsonObject();
                        Quest quest = Quest.fromJson(questJson);
                        QUESTS_BY_ID.put(quest.getId(), quest);
                        QUESTS_BY_NODE.put(quest.getNodeId(), quest);
                    }
                }
                Beemancer.LOGGER.info("Quests loaded: {} quests", QUESTS_BY_ID.size());
            } else {
                Beemancer.LOGGER.warn("Quests file not found: {}", path);
            }
        } catch (Exception e) {
            Beemancer.LOGGER.error("Failed to load quests: {}", e.getMessage());
        }

        loaded = true;
    }

    // ============================================================
    // ACCES AUX QUETES
    // ============================================================

    @Nullable
    public static Quest getQuest(String questId) {
        return QUESTS_BY_ID.get(questId);
    }

    @Nullable
    public static Quest getQuestForNode(String nodeId) {
        return QUESTS_BY_NODE.get(nodeId);
    }

    public static Collection<Quest> getAllQuests() {
        return QUESTS_BY_ID.values();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * S'assure que les données sont chargées côté client.
     */
    public static void ensureClientLoaded() {
        if (!loaded) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                load(mc.getResourceManager());
            }
        }
    }

    // ============================================================
    // VERIFICATION ET COMPLETION
    // ============================================================

    /**
     * Vérifie si une quête est complétée pour un joueur.
     */
    public static boolean isQuestCompleted(Player player, String questId) {
        QuestPlayerData data = player.getData(BeemancerAttachments.QUEST_DATA);
        return data.isCompleted(questId);
    }

    /**
     * Marque une quête comme complétée.
     * @return true si la quête vient d'être complétée (pas déjà faite)
     */
    public static boolean completeQuest(Player player, String questId) {
        QuestPlayerData data = player.getData(BeemancerAttachments.QUEST_DATA);
        boolean newCompletion = data.complete(questId);
        if (newCompletion) {
            player.setData(BeemancerAttachments.QUEST_DATA, data);
            Beemancer.LOGGER.debug("Quest completed: {} for player {}", questId, player.getName().getString());
        }
        return newCompletion;
    }

    /**
     * Vérifie toutes les quêtes OBTAIN pour un joueur.
     * Appelé à l'ouverture du Codex.
     * @return Set des quêtes nouvellement complétées
     */
    public static Set<String> checkObtainQuests(Player player) {
        Set<String> newCompletions = new HashSet<>();
        QuestPlayerData data = player.getData(BeemancerAttachments.QUEST_DATA);

        for (Quest quest : QUESTS_BY_ID.values()) {
            if (quest.getType() == QuestType.OBTAIN && !data.isCompleted(quest.getId())) {
                if (quest.checkObtainQuest(player)) {
                    data.complete(quest.getId());
                    newCompletions.add(quest.getId());
                    Beemancer.LOGGER.debug("OBTAIN quest auto-completed: {}", quest.getId());
                }
            }
        }

        if (!newCompletions.isEmpty()) {
            player.setData(BeemancerAttachments.QUEST_DATA, data);
        }

        return newCompletions;
    }

    /**
     * Cherche une quête MACHINE_OUTPUT correspondant à un item extrait d'une machine.
     */
    @Nullable
    public static Quest findMachineOutputQuest(String machineType, ResourceLocation itemId) {
        for (Quest quest : QUESTS_BY_ID.values()) {
            if (quest.getType() == QuestType.MACHINE_OUTPUT
                    && quest.matchesMachine(machineType)
                    && quest.getTargetItem() != null
                    && quest.getTargetItem().equals(itemId)) {
                return quest;
            }
        }
        return null;
    }

    /**
     * Cherche une quête BEE_INCUBATOR correspondant à une espèce d'abeille.
     */
    @Nullable
    public static Quest findBeeIncubatorQuest(String species) {
        for (Quest quest : QUESTS_BY_ID.values()) {
            if (quest.getType() == QuestType.BEE_INCUBATOR && quest.matchesSpecies(species)) {
                return quest;
            }
        }
        return null;
    }
}
