/**
 * ============================================================
 * [CodexJsonLoader.java]
 * Description: Charge les nodes du Codex depuis codex.json avec positions normalisées
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Gson                | Parsing JSON         | Lecture du fichier             |
 * | CodexPage           | Pages du codex       | Mapping des tabs               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexManager (chargement des nodes)
 * - CodexScreen (positionnement des nodes)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

import com.chapeau.beemancer.Beemancer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Charge les nodes depuis codex.json à la racine du projet.
 * Les positions JSON (basées sur un écran full HD) sont normalisées
 * pour être utilisables dans n'importe quelle taille de fenêtre.
 */
public class CodexJsonLoader {
    private static final Gson GSON = new Gson();

    // Mapping des noms de tabs JSON vers CodexPage
    private static final Map<String, CodexPage> TAB_MAPPING = new HashMap<>();
    static {
        TAB_MAPPING.put("Main", CodexPage.APICA);
        TAB_MAPPING.put("Bees", CodexPage.BEES);
        TAB_MAPPING.put("Alchimie_tab", CodexPage.ALCHEMY);
        TAB_MAPPING.put("Artifacts_Tab", CodexPage.ARTIFACTS);
        TAB_MAPPING.put("Logistic_Tab", CodexPage.LOGISTICS);
    }

    // Nodes à ignorer
    private static final Set<String> IGNORED_NODES = Set.of(
        "Apica",
        "Get craft receip Item by default",
        "Obtain Craft by Unlock"
    );

    /**
     * Données d'un node chargé depuis JSON.
     */
    public static class JsonNodeData {
        public final String name;
        public final CodexPage page;
        public final float rawX;
        public final float rawY;
        public final float normalizedX;
        public final float normalizedY;
        @Nullable
        public final String parentName;

        public JsonNodeData(String name, CodexPage page, float rawX, float rawY,
                          float normalizedX, float normalizedY, @Nullable String parentName) {
            this.name = name;
            this.page = page;
            this.rawX = rawX;
            this.rawY = rawY;
            this.normalizedX = normalizedX;
            this.normalizedY = normalizedY;
            this.parentName = parentName;
        }
    }

    /**
     * Bounds d'un tab pour le scroll.
     */
    public static class TabBounds {
        public final float minX;
        public final float maxX;
        public final float minY;
        public final float maxY;
        public final float width;
        public final float height;

        public TabBounds(float minX, float maxX, float minY, float maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.width = maxX - minX;
            this.height = maxY - minY;
        }
    }

    /**
     * Résultat du chargement pour un tab.
     */
    public static class TabData {
        public final CodexPage page;
        public final List<JsonNodeData> nodes;
        public final TabBounds bounds;
        public final Map<String, List<String>> links;

        public TabData(CodexPage page, List<JsonNodeData> nodes, TabBounds bounds, Map<String, List<String>> links) {
            this.page = page;
            this.nodes = nodes;
            this.bounds = bounds;
            this.links = links;
        }
    }

    private static Map<CodexPage, TabData> loadedTabs = null;

    /**
     * Charge les données depuis codex.json.
     */
    public static void load() {
        loadedTabs = new EnumMap<>(CodexPage.class);

        Path codexPath = Path.of("codex.json");
        if (!Files.exists(codexPath)) {
            Beemancer.LOGGER.warn("codex.json not found at project root");
            return;
        }

        try (FileReader reader = new FileReader(codexPath.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonArray tabs = root.getAsJsonArray("tabs");

            for (JsonElement tabElement : tabs) {
                JsonObject tabJson = tabElement.getAsJsonObject();
                String tabName = tabJson.get("name").getAsString();
                CodexPage page = TAB_MAPPING.get(tabName);

                if (page == null) {
                    Beemancer.LOGGER.warn("Unknown tab in codex.json: {}", tabName);
                    continue;
                }

                List<JsonNodeData> rawNodes = new ArrayList<>();
                JsonArray items = tabJson.getAsJsonArray("items");

                // Première passe: collecter les nodes bruts
                float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
                float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

                for (JsonElement itemElement : items) {
                    JsonObject itemJson = itemElement.getAsJsonObject();
                    String text = itemJson.get("text").getAsString();
                    String nodeName = extractNodeName(text);

                    if (IGNORED_NODES.contains(nodeName)) {
                        continue;
                    }

                    float x = itemJson.get("x").getAsFloat();
                    float y = itemJson.get("y").getAsFloat();

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);

                    rawNodes.add(new JsonNodeData(nodeName, page, x, y, 0, 0, null));
                }

                if (rawNodes.isEmpty()) {
                    continue;
                }

                // Ajouter des marges
                float margin = 50;
                minX -= margin;
                maxX += margin;
                minY -= margin;
                maxY += margin;

                TabBounds bounds = new TabBounds(minX, maxX, minY, maxY);

                // Deuxième passe: normaliser les positions (0 à 1)
                List<JsonNodeData> normalizedNodes = new ArrayList<>();
                for (JsonNodeData raw : rawNodes) {
                    float normalizedX = (raw.rawX - minX) / bounds.width;
                    float normalizedY = (raw.rawY - minY) / bounds.height;
                    normalizedNodes.add(new JsonNodeData(
                        raw.name, raw.page, raw.rawX, raw.rawY,
                        normalizedX, normalizedY, null
                    ));
                }

                // Créer les liens logiques
                Map<String, List<String>> links = createLinks(page, normalizedNodes);

                loadedTabs.put(page, new TabData(page, normalizedNodes, bounds, links));
            }

            Beemancer.LOGGER.info("Loaded codex.json: {} tabs", loadedTabs.size());

        } catch (Exception e) {
            Beemancer.LOGGER.error("Failed to load codex.json: {}", e.getMessage());
        }
    }

    /**
     * Extrait le nom du node depuis le texte (première ligne).
     */
    private static String extractNodeName(String text) {
        int newlineIndex = text.indexOf('\n');
        if (newlineIndex > 0) {
            return text.substring(0, newlineIndex).trim();
        }
        return text.trim();
    }

    /**
     * Crée les liens logiques entre nodes basé sur la page.
     */
    private static Map<String, List<String>> createLinks(CodexPage page, List<JsonNodeData> nodes) {
        Map<String, List<String>> links = new HashMap<>();

        switch (page) {
            case APICA -> {
                addLink(links, "The Beginning", "1st bee");
                addLink(links, "1st bee", "Hive");
                addLink(links, "Hive", "Manual Centrifuge");
                addLink(links, "Hive", "Hive Multibloc");
                addLink(links, "Manual Centrifuge", "Crystallyzer");
                addLink(links, "Crystallyzer", "Altar");
            }
            case ALCHEMY -> {
                addLink(links, "Manual Centrifuge", "Portable Tank");
                addLink(links, "Portable Tank", "Tank");
                addLink(links, "Manual Centrifuge", "Honey Pipe");
                addLink(links, "Manual Centrifuge", "Crystallyzer");
                addLink(links, "Crystallyzer", "Centrifuge T1");
                addLink(links, "Centrifuge T1", "Centrifuge T2");
                addLink(links, "Centrifuge T2", "Centrifuge T3");
                addLink(links, "Crystallyzer", "Infuser T1");
                addLink(links, "Infuser T1", "Infuser T2");
                addLink(links, "Infuser T2", "Infuser T3");
                addLink(links, "Manual Centrifuge", "Alembic");
                addLink(links, "Infuser T1", "Incubator");
            }
            case ARTIFACTS -> {
                addLink(links, "Altar", "Extractor");
                addLink(links, "Altar", "Anti Breeding Crystal");
            }
            case LOGISTICS -> {
                addLink(links, "Crystallyzer", "Honey Pipe");
                addLink(links, "Crystallyzer", "Item Pipe");
                addLink(links, "Crystallyzer", "Storage Controller Heart");
                addLink(links, "Storage Controller Heart", "Relay");
                addLink(links, "Relay", "Interface");
                addLink(links, "Interface", "Import");
                addLink(links, "Interface", "Export");
                addLink(links, "Interface", "Craft Auto");
                addLink(links, "Crystallyzer", "Pipe T2");
                addLink(links, "Pipe T2", "Pipe T3");
                addLink(links, "Pipe T3", "Pipe T4");
            }
            default -> {}
        }

        return links;
    }

    private static void addLink(Map<String, List<String>> links, String from, String to) {
        links.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    /**
     * Récupère les données d'un tab.
     */
    @Nullable
    public static TabData getTabData(CodexPage page) {
        if (loadedTabs == null) {
            load();
        }
        return loadedTabs != null ? loadedTabs.get(page) : null;
    }

    /**
     * Vérifie si les données sont chargées.
     */
    public static boolean isLoaded() {
        return loadedTabs != null && !loadedTabs.isEmpty();
    }

    /**
     * Recharge les données.
     */
    public static void reload() {
        loadedTabs = null;
        load();
    }
}
