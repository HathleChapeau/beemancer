/**
 * ============================================================
 * [CodexBookManager.java]
 * Description: Gestionnaire central du contenu des pages du Codex Book
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexBookContent    | Données de contenu   | Stockage et accès              |
 * | CodexManager        | Liste des nodes      | Itération sur les nodeIds      |
 * | Gson                | Parsing JSON         | Chargement des fichiers        |
 * | ResourceManager     | Accès ressources     | Lecture des données            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookScreen (récupération du contenu à afficher)
 *
 * ============================================================
 */
package com.chapeau.apica.common.codex.book;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import net.minecraft.server.MinecraftServer;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CodexBookManager {

    private static final Gson GSON = new Gson();
    private static final Map<String, CodexBookContent> CONTENTS = new HashMap<>();
    private static final Set<String> ATTEMPTED = new HashSet<>();
    private static ResourceManager cachedResourceManager;
    private static boolean loaded = false;

    /**
     * Charge depuis le serveur (accès aux data packs).
     * Doit être appelé APRÈS CodexManager.load().
     * Ne charge PAS les fichiers individuels — ils sont chargés à la demande (lazy loading).
     */
    public static void load(MinecraftServer server) {
        load(server.getResourceManager());
    }

    /**
     * Initialise le manager pour le lazy loading.
     * Les fichiers JSON individuels ne sont PAS lus ici — ils sont chargés
     * à la demande dans getContent() quand un joueur ouvre un node.
     * Cela élimine ~140 lectures de fichiers au démarrage du serveur.
     * @param resourceManager Le gestionnaire de ressources (stocké pour accès lazy)
     */
    public static void load(ResourceManager resourceManager) {
        CONTENTS.clear();
        ATTEMPTED.clear();
        cachedResourceManager = resourceManager;
        loaded = true;

        int nodeCount = 0;
        for (CodexPage page : CodexPage.values()) {
            List<CodexNode> nodes = CodexManager.getNodesForPage(page);
            if (nodes != null) {
                nodeCount += nodes.size();
            }
        }

        Apica.LOGGER.info("CodexBook initialized for lazy loading ({} nodes available)", nodeCount);
    }

    /**
     * Retourne le contenu d'une page pour un node donné.
     * Charge le fichier JSON à la demande si pas encore en cache.
     * Si aucun contenu custom n'existe, génère un contenu par défaut (en-tête seul).
     * @param nodeId L'identifiant du node (pas le fullId)
     * @return Le contenu de la page
     */
    public static CodexBookContent getContent(String nodeId) {
        CodexBookContent content = CONTENTS.get(nodeId);
        if (content != null) {
            return content;
        }

        if (!ATTEMPTED.contains(nodeId)) {
            content = loadSingleNode(nodeId);
            if (content != null) {
                return content;
            }
        }

        return CodexBookContent.createDefault(nodeId);
    }

    /**
     * Charge le contenu d'un seul node depuis son fichier JSON.
     * @param nodeId L'identifiant du node
     * @return Le contenu chargé, ou null si pas de fichier trouvé
     */
    private static CodexBookContent loadSingleNode(String nodeId) {
        ATTEMPTED.add(nodeId);

        ResourceManager rm = getResourceManager();
        if (rm == null) {
            return null;
        }

        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                Apica.MOD_ID, "codex/book/" + nodeId + ".json");

        Optional<Resource> resourceOpt = rm.getResource(path);
        if (resourceOpt.isPresent()) {
            try (InputStreamReader reader = new InputStreamReader(resourceOpt.get().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                CodexBookContent content = CodexBookContent.fromJson(nodeId, json);
                CONTENTS.put(nodeId, content);
                Apica.LOGGER.debug("Lazy-loaded book content for node: {} ({} sections)",
                        nodeId, content.getSections().size());
                return content;
            } catch (Exception e) {
                Apica.LOGGER.error("Failed to load book content for {}: {}", nodeId, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Retourne le ResourceManager disponible (serveur ou client).
     */
    private static ResourceManager getResourceManager() {
        if (cachedResourceManager != null) {
            return cachedResourceManager;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null) {
                return mc.getResourceManager();
            }
        } catch (Exception ignored) {
            // Server-side, Minecraft class not available
        }
        return null;
    }

    /**
     * S'assure que les données sont chargées côté client.
     */
    public static void ensureClientLoaded() {
        if (!loaded) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                CodexManager.ensureClientLoaded();
                load(mc.getResourceManager());
            }
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
