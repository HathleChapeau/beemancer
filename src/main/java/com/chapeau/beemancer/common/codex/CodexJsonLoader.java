/**
 * ============================================================
 * [CodexJsonLoader.java]
 * Description: Charge les nodes du Codex depuis codex.json avec positions de grille
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
 * Les positions JSON sont converties en positions de grille
 * relatives au node bleu (header) de chaque tab.
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

    // Couleur du node header (bleu)
    private static final String HEADER_COLOR = "#3498db";

    // Facteur de conversion: pixels JSON -> unités de grille
    // ~100 pixels JSON = 1 unité de grille (comme BEES)
    private static final float GRID_SCALE = 100.0f;

    /**
     * Données d'un node chargé depuis JSON.
     */
    public static class JsonNodeData {
        public final String name;
        public final CodexPage page;
        public final int gridX;
        public final int gridY;
        public final boolean isHeader;
        @Nullable
        public final String parentName;

        public JsonNodeData(String name, CodexPage page, int gridX, int gridY, boolean isHeader, @Nullable String parentName) {
            this.name = name;
            this.page = page;
            this.gridX = gridX;
            this.gridY = gridY;
            this.isHeader = isHeader;
            this.parentName = parentName;
        }
    }

    /**
     * Résultat du chargement pour un tab.
     */
    public static class TabData {
        public final CodexPage page;
        public final List<JsonNodeData> nodes;
        public final Map<String, List<String>> links;

        public TabData(CodexPage page, List<JsonNodeData> nodes, Map<String, List<String>> links) {
            this.page = page;
            this.nodes = nodes;
            this.links = links;
        }
    }

    private static Map<CodexPage, TabData> loadedTabs = null;

    /**
     * Charge les données depuis codex.json.
     */
    public static void load() {
        loadedTabs = new EnumMap<>(CodexPage.class);

        // Chercher codex.json dans plusieurs emplacements
        Path codexPath = findCodexJson();
        if (codexPath == null) {
            Beemancer.LOGGER.warn("codex.json not found, using fallback data");
            loadFallbackData();
            return;
        }

        try (FileReader reader = new FileReader(codexPath.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonArray tabs = root.getAsJsonArray("tabs");

            for (JsonElement tabElement : tabs) {
                JsonObject tabJson = tabElement.getAsJsonObject();
                String tabName = tabJson.get("name").getAsString();
                CodexPage page = TAB_MAPPING.get(tabName);

                if (page == null || page == CodexPage.BEES) {
                    // BEES utilise son propre système
                    continue;
                }

                loadTab(page, tabJson);
            }

            Beemancer.LOGGER.info("Loaded codex.json: {} tabs", loadedTabs.size());

        } catch (Exception e) {
            Beemancer.LOGGER.error("Failed to load codex.json: {}", e.getMessage());
            loadFallbackData();
        }
    }

    private static Path findCodexJson() {
        // Essayer plusieurs chemins
        String[] paths = {
            "codex.json",
            "../codex.json",
            "run/codex.json",
            "../run/codex.json"
        };

        for (String pathStr : paths) {
            Path path = Path.of(pathStr);
            if (Files.exists(path)) {
                Beemancer.LOGGER.info("Found codex.json at: {}", path.toAbsolutePath());
                return path;
            }
        }
        return null;
    }

    private static void loadTab(CodexPage page, JsonObject tabJson) {
        JsonArray items = tabJson.getAsJsonArray("items");

        // Première passe: trouver le node header (bleu) et collecter tous les nodes
        float headerX = 0, headerY = 0;
        boolean foundHeader = false;
        List<RawNode> rawNodes = new ArrayList<>();

        for (JsonElement itemElement : items) {
            JsonObject itemJson = itemElement.getAsJsonObject();
            String text = itemJson.get("text").getAsString();
            String nodeName = extractNodeName(text);

            if (IGNORED_NODES.contains(nodeName)) {
                continue;
            }

            float x = itemJson.get("x").getAsFloat();
            float y = itemJson.get("y").getAsFloat();
            String borderColor = itemJson.has("border_color") ? itemJson.get("border_color").getAsString() : "";
            boolean isHeader = HEADER_COLOR.equalsIgnoreCase(borderColor);

            if (isHeader && !foundHeader) {
                headerX = x;
                headerY = y;
                foundHeader = true;
            }

            rawNodes.add(new RawNode(nodeName, x, y, isHeader));
        }

        if (rawNodes.isEmpty()) {
            return;
        }

        // Si pas de header trouvé, utiliser le premier node comme référence
        if (!foundHeader && !rawNodes.isEmpty()) {
            headerX = rawNodes.get(0).x;
            headerY = rawNodes.get(0).y;
        }

        // Deuxième passe: convertir en positions de grille relatives au header
        List<JsonNodeData> nodes = new ArrayList<>();
        for (RawNode raw : rawNodes) {
            int gridX = Math.round((raw.x - headerX) / GRID_SCALE);
            int gridY = Math.round((raw.y - headerY) / GRID_SCALE);
            nodes.add(new JsonNodeData(raw.name, page, gridX, gridY, raw.isHeader, null));
        }

        // Créer les liens
        Map<String, List<String>> links = createLinks(page, nodes);

        loadedTabs.put(page, new TabData(page, nodes, links));
    }

    private static class RawNode {
        final String name;
        final float x, y;
        final boolean isHeader;

        RawNode(String name, float x, float y, boolean isHeader) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.isHeader = isHeader;
        }
    }

    /**
     * Données de fallback si codex.json n'est pas trouvé.
     */
    private static void loadFallbackData() {
        // APICA - centré sur "The Beginning"
        List<JsonNodeData> apicaNodes = List.of(
            new JsonNodeData("The Beginning", CodexPage.APICA, 0, 0, true, null),
            new JsonNodeData("1st bee", CodexPage.APICA, 0, -2, false, null),
            new JsonNodeData("Hive", CodexPage.APICA, 2, -1, false, null),
            new JsonNodeData("Manual Centrifuge", CodexPage.APICA, 2, 1, false, null),
            new JsonNodeData("Crystallyzer", CodexPage.APICA, 0, 2, false, null),
            new JsonNodeData("Altar", CodexPage.APICA, -2, 1, false, null),
            new JsonNodeData("Hive Multibloc", CodexPage.APICA, -2, -1, false, null)
        );
        loadedTabs.put(CodexPage.APICA, new TabData(CodexPage.APICA, apicaNodes, createLinks(CodexPage.APICA, apicaNodes)));

        // ALCHEMY - centré sur "Manual Centrifuge"
        List<JsonNodeData> alchemyNodes = List.of(
            new JsonNodeData("Manual Centrifuge", CodexPage.ALCHEMY, 0, 0, true, null),
            new JsonNodeData("Honey Pipe", CodexPage.ALCHEMY, 2, 2, false, null),
            new JsonNodeData("Centrifuge T1", CodexPage.ALCHEMY, -2, 0, false, null),
            new JsonNodeData("Centrifuge T2", CodexPage.ALCHEMY, -3, 2, false, null),
            new JsonNodeData("Centrifuge T3", CodexPage.ALCHEMY, -3, 4, false, null),
            new JsonNodeData("Portable Tank", CodexPage.ALCHEMY, -2, -2, false, null),
            new JsonNodeData("Tank", CodexPage.ALCHEMY, -2, -3, false, null),
            new JsonNodeData("Crystallyzer", CodexPage.ALCHEMY, 2, 0, false, null),
            new JsonNodeData("Infuser T1", CodexPage.ALCHEMY, 2, -1, false, null),
            new JsonNodeData("Infuser T2", CodexPage.ALCHEMY, 4, -2, false, null),
            new JsonNodeData("Infuser T3", CodexPage.ALCHEMY, 6, -2, false, null),
            new JsonNodeData("Alembic", CodexPage.ALCHEMY, 0, 2, false, null),
            new JsonNodeData("Incubator", CodexPage.ALCHEMY, 0, -3, false, null)
        );
        loadedTabs.put(CodexPage.ALCHEMY, new TabData(CodexPage.ALCHEMY, alchemyNodes, createLinks(CodexPage.ALCHEMY, alchemyNodes)));

        // ARTIFACTS - centré sur "Altar"
        List<JsonNodeData> artifactsNodes = List.of(
            new JsonNodeData("Altar", CodexPage.ARTIFACTS, 0, 0, true, null),
            new JsonNodeData("Extractor", CodexPage.ARTIFACTS, 2, -2, false, null),
            new JsonNodeData("Anti Breeding Crystal", CodexPage.ARTIFACTS, -1, -3, false, null)
        );
        loadedTabs.put(CodexPage.ARTIFACTS, new TabData(CodexPage.ARTIFACTS, artifactsNodes, createLinks(CodexPage.ARTIFACTS, artifactsNodes)));

        // LOGISTICS - centré sur "Crystallyzer"
        List<JsonNodeData> logisticsNodes = List.of(
            new JsonNodeData("Crystallyzer", CodexPage.LOGISTICS, 0, 0, true, null),
            new JsonNodeData("Storage Controller Heart", CodexPage.LOGISTICS, 2, -1, false, null),
            new JsonNodeData("Relay", CodexPage.LOGISTICS, 2, -3, false, null),
            new JsonNodeData("Interface", CodexPage.LOGISTICS, 5, -4, false, null),
            new JsonNodeData("Import", CodexPage.LOGISTICS, 5, -6, false, null),
            new JsonNodeData("Export", CodexPage.LOGISTICS, 7, -5, false, null),
            new JsonNodeData("Craft Auto", CodexPage.LOGISTICS, 8, -4, false, null),
            new JsonNodeData("Pipe T2", CodexPage.LOGISTICS, 0, 3, false, null),
            new JsonNodeData("Pipe T3", CodexPage.LOGISTICS, 0, 4, false, null),
            new JsonNodeData("Pipe T4", CodexPage.LOGISTICS, 0, 5, false, null),
            new JsonNodeData("Honey Pipe", CodexPage.LOGISTICS, 1, 2, false, null),
            new JsonNodeData("Item Pipe", CodexPage.LOGISTICS, -1, 2, false, null)
        );
        loadedTabs.put(CodexPage.LOGISTICS, new TabData(CodexPage.LOGISTICS, logisticsNodes, createLinks(CodexPage.LOGISTICS, logisticsNodes)));
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
