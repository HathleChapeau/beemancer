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
package com.chapeau.beemancer.common.codex.book;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import net.minecraft.server.MinecraftServer;

import java.io.InputStreamReader;
import java.util.*;

public class CodexBookManager {

    private static final Gson GSON = new Gson();
    private static final Map<String, CodexBookContent> CONTENTS = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Charge depuis le serveur (accès aux data packs).
     * Doit être appelé APRÈS CodexManager.load().
     */
    public static void load(MinecraftServer server) {
        load(server.getResourceManager());
    }

    /**
     * Charge le contenu des pages du livre depuis les fichiers JSON.
     * Itère sur tous les nodeIds connus et tente de charger
     * data/beemancer/codex/book/{nodeId}.json pour chacun.
     * @param resourceManager Le gestionnaire de ressources
     */
    public static void load(ResourceManager resourceManager) {
        CONTENTS.clear();

        // Collecter tous les nodeIds uniques depuis le CodexManager
        Set<String> nodeIds = new HashSet<>();
        for (CodexPage page : CodexPage.values()) {
            List<CodexNode> nodes = CodexManager.getNodesForPage(page);
            if (nodes != null) {
                for (CodexNode node : nodes) {
                    nodeIds.add(node.getId());
                }
            }
        }

        // Charger le JSON de chaque node par chemin exact
        for (String nodeId : nodeIds) {
            ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                    Beemancer.MOD_ID, "codex/book/" + nodeId + ".json");

            Optional<Resource> resourceOpt = resourceManager.getResource(path);
            if (resourceOpt.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resourceOpt.get().open())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    CodexBookContent content = CodexBookContent.fromJson(nodeId, json);
                    CONTENTS.put(nodeId, content);
                    Beemancer.LOGGER.info("Loaded book content for node: {} ({} sections)",
                            nodeId, content.getSections().size());
                } catch (Exception e) {
                    Beemancer.LOGGER.error("Failed to load book content for {}: {}", nodeId, e.getMessage());
                }
            }
        }

        loaded = true;
        Beemancer.LOGGER.info("CodexBook loaded: {} custom pages out of {} nodes",
                CONTENTS.size(), nodeIds.size());
    }

    /**
     * Retourne le contenu d'une page pour un node donné.
     * Si aucun contenu custom n'existe, génère un contenu par défaut (en-tête seul).
     * @param nodeId L'identifiant du node (pas le fullId)
     * @return Le contenu de la page
     */
    public static CodexBookContent getContent(String nodeId) {
        CodexBookContent content = CONTENTS.get(nodeId);
        if (content != null) {
            return content;
        }
        return CodexBookContent.createDefault(nodeId);
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
