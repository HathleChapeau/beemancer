/**
 * ============================================================
 * [CodexManager.java]
 * Description: Gestionnaire central du Codex - chargement et accès aux nodes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Données des nodes    | Stockage et accès              |
 * | CodexPage           | Pages du codex       | Organisation par onglet        |
 * | Gson                | Parsing JSON         | Chargement des fichiers        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (récupération des nodes à afficher)
 * - CodexPlayerData (vérification des dépendances)
 * - Beemancer (initialisation au démarrage)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.quest.NodeState;
import com.chapeau.beemancer.common.quest.NodeVisibility;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.*;

public class CodexManager {
    private static final Gson GSON = new Gson();
    private static final Map<CodexPage, List<CodexNode>> NODES_BY_PAGE = new EnumMap<>(CodexPage.class);
    private static final Map<String, CodexNode> NODES_BY_ID = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Charge depuis le serveur (appelé au démarrage serveur).
     */
    public static void load(MinecraftServer server) {
        load(server.getResourceManager());
    }

    /**
     * Charge depuis un ResourceManager (client ou serveur).
     * Appelé automatiquement côté client si nécessaire.
     */
    public static void load(ResourceManager resourceManager) {
        NODES_BY_PAGE.clear();
        NODES_BY_ID.clear();

        for (CodexPage page : CodexPage.values()) {
            List<CodexNode> nodes = loadPage(resourceManager, page);
            NODES_BY_PAGE.put(page, nodes);

            for (CodexNode node : nodes) {
                NODES_BY_ID.put(node.getFullId(), node);
            }
        }

        linkParentsAndChildren();
        loaded = true;
        Beemancer.LOGGER.info("Codex loaded: {} nodes total", NODES_BY_ID.size());
    }

    private static List<CodexNode> loadPage(ResourceManager resourceManager, CodexPage page) {
        List<CodexNode> nodes = new ArrayList<>();
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "codex/" + page.getId() + ".json"
        );

        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(path);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonObject root = GSON.fromJson(reader, JsonObject.class);
                    JsonArray nodesArray = root.getAsJsonArray("nodes");

                    for (JsonElement element : nodesArray) {
                        JsonObject nodeJson = element.getAsJsonObject();
                        CodexNode node = CodexNode.fromJson(nodeJson, page);
                        nodes.add(node);
                    }
                }
                Beemancer.LOGGER.debug("Loaded {} nodes for page {}", nodes.size(), page.getId());
            } else {
                Beemancer.LOGGER.warn("Codex file not found: {}", path);
            }
        } catch (Exception e) {
            Beemancer.LOGGER.error("Failed to load codex page {}: {}", page.getId(), e.getMessage());
        }

        return nodes;
    }

    private static void linkParentsAndChildren() {
        for (CodexNode node : NODES_BY_ID.values()) {
            String parentId = node.getParentId();
            if (parentId != null) {
                String fullParentId = node.getPage().getId() + ":" + parentId;
                CodexNode parent = NODES_BY_ID.get(fullParentId);
                if (parent != null) {
                    parent.addChild(node.getId());
                }
            }
        }
    }

    public static List<CodexNode> getNodesForPage(CodexPage page) {
        return NODES_BY_PAGE.getOrDefault(page, Collections.emptyList());
    }

    @Nullable
    public static CodexNode getNode(String fullId) {
        return NODES_BY_ID.get(fullId);
    }

    @Nullable
    public static CodexNode getNode(CodexPage page, String nodeId) {
        return NODES_BY_ID.get(page.getId() + ":" + nodeId);
    }

    public static Collection<CodexNode> getAllNodes() {
        return NODES_BY_ID.values();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * S'assure que les données sont chargées côté client.
     * Appelé par CodexScreen à l'ouverture.
     */
    public static void ensureClientLoaded() {
        if (!loaded) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                load(mc.getResourceManager());
            }
        }
    }

    public static List<CodexNode> getRootNodes(CodexPage page) {
        List<CodexNode> roots = new ArrayList<>();
        for (CodexNode node : getNodesForPage(page)) {
            if (node.isRoot()) {
                roots.add(node);
            }
        }
        return roots;
    }

    public static boolean canUnlock(CodexNode node, Set<String> unlockedNodes) {
        if (node.isRoot()) {
            return true;
        }
        String parentId = node.getParentId();
        if (parentId == null) {
            return true;
        }
        String fullParentId = node.getPage().getId() + ":" + parentId;
        return unlockedNodes.contains(fullParentId);
    }

    public static boolean isVisible(CodexNode node, Set<String> unlockedNodes) {
        if (!node.isHiddenUntilParentUnlocked()) {
            return true;
        }
        String parentId = node.getParentId();
        if (parentId == null) {
            return true;
        }
        String fullParentId = node.getPage().getId() + ":" + parentId;
        return unlockedNodes.contains(fullParentId);
    }

    // ============================================================
    // CALCUL DE L'ETAT DES NODES
    // ============================================================

    /**
     * Calcule l'état d'un node pour un joueur.
     * @param node Le node à évaluer
     * @param playerData Les données du joueur
     * @param completedQuests Set des quêtes complétées
     * @return L'état du node (LOCKED, DISCOVERED, UNLOCKED)
     */
    public static NodeState getNodeState(CodexNode node, CodexPlayerData playerData, Set<String> completedQuests) {
        String fullId = node.getFullId();

        // Déjà débloqué
        if (playerData.isUnlocked(fullId)) {
            return NodeState.UNLOCKED;
        }

        // Vérifier si le parent est débloqué (sauf root)
        if (!node.isRoot()) {
            String parentId = node.getParentId();
            if (parentId != null) {
                String fullParentId = node.getPage().getId() + ":" + parentId;
                if (!playerData.isUnlocked(fullParentId)) {
                    return NodeState.LOCKED;
                }
            }
        }

        // Vérifier si la quête est complétée
        String questId = node.getQuestId();
        if (questId != null && !questId.isEmpty()) {
            if (completedQuests.contains(questId)) {
                return NodeState.DISCOVERED;
            }
            return NodeState.LOCKED;
        }

        // Pas de quête = directement disponible si parent débloqué
        return NodeState.DISCOVERED;
    }

    /**
     * Détermine si un node doit être affiché.
     * @param node Le node à évaluer
     * @param state L'état calculé du node
     * @param playerData Les données du joueur
     * @return true si le node doit être visible
     */
    public static boolean isNodeVisible(CodexNode node, NodeState state, CodexPlayerData playerData) {
        NodeVisibility visibility = node.getVisibility();

        // HIDDEN: invisible tant que pas discovered ou unlocked
        if (visibility == NodeVisibility.HIDDEN) {
            return state == NodeState.DISCOVERED || state == NodeState.UNLOCKED;
        }

        // VISIBLE et SECRET: visible si parent débloqué (ou root)
        if (node.isRoot()) {
            return true;
        }

        String parentId = node.getParentId();
        if (parentId == null) {
            return true;
        }

        String fullParentId = node.getPage().getId() + ":" + parentId;
        return playerData.isUnlocked(fullParentId);
    }

    /**
     * Retourne le texte à afficher pour le titre d'un node.
     * @param node Le node
     * @param state L'état du node
     * @return Le titre ou "???" si SECRET et LOCKED
     */
    public static Component getDisplayTitle(CodexNode node, NodeState state) {
        if (node.getVisibility() == NodeVisibility.SECRET && state == NodeState.LOCKED) {
            return Component.literal("???");
        }
        return node.getTitle();
    }

    /**
     * Retourne le texte à afficher pour la description d'un node.
     * @param node Le node
     * @param state L'état du node
     * @return La description ou "???" si SECRET et LOCKED
     */
    public static Component getDisplayDescription(CodexNode node, NodeState state) {
        if (node.getVisibility() == NodeVisibility.SECRET && state == NodeState.LOCKED) {
            return Component.literal("???");
        }
        return node.getDescription();
    }
}
