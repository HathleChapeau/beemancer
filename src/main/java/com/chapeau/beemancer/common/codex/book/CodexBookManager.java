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
 * | Gson                | Parsing JSON         | Chargement des fichiers        |
 * | ResourceManager     | Accès ressources     | Lecture des données            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexBookScreen (récupération du contenu à afficher)
 * - Beemancer (initialisation au démarrage)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex.book;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CodexBookManager {

    private static final Gson GSON = new Gson();
    private static final Map<String, CodexBookContent> CONTENTS = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Charge le contenu des pages du livre depuis les fichiers JSON.
     * Fichiers attendus dans: data/beemancer/codex/book/{nodeId}.json
     * @param resourceManager Le gestionnaire de ressources
     */
    public static void load(ResourceManager resourceManager) {
        CONTENTS.clear();

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                "codex/book", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            String path = location.getPath();
            String nodeId = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);

            try (InputStreamReader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                CodexBookContent content = CodexBookContent.fromJson(nodeId, json);
                CONTENTS.put(nodeId, content);
                Beemancer.LOGGER.debug("Loaded book content for node: {}", nodeId);
            } catch (Exception e) {
                Beemancer.LOGGER.error("Failed to load book content for {}: {}", nodeId, e.getMessage());
            }
        }

        loaded = true;
        Beemancer.LOGGER.info("CodexBook loaded: {} custom pages, rest use defaults", CONTENTS.size());
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
                load(mc.getResourceManager());
            }
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
