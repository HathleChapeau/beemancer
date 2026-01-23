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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    public static void load(MinecraftServer server) {
        NODES_BY_PAGE.clear();
        NODES_BY_ID.clear();

        ResourceManager resourceManager = server.getResourceManager();

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
}
