/**
 * ============================================================
 * [ResonatorConfigManager.java]
 * Description: Gestionnaire de configuration des waveforms par stat et niveau pour le resonateur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | Chemin des ressources          |
 * | Gson                | Parsing JSON         | Lecture du fichier config       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Apica.java (chargement au demarrage serveur)
 * - ResonatorScreen.java (lecture des waveforms cibles par trait/niveau)
 *
 * ============================================================
 */
package com.chapeau.apica.core.config;

import com.chapeau.apica.Apica;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Charge les parametres d'onde (freq, amp, phase) par stat ET par niveau.
 * Structure JSON: stat_waveforms -> statName -> level -> {frequency, amplitude, phase}
 */
public class ResonatorConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResonatorConfigManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "config/resonator_waveforms.json");

    /** Cle: "statName:level" (ex: "drop:2"), valeur: waveform correspondante. */
    private static final Map<String, StatWaveform> waveforms = new HashMap<>();
    private static boolean loaded = false;

    public static void load(MinecraftServer server) {
        if (server == null) {
            LOGGER.warn("Server is null, using default resonator waveforms");
            setupDefaults();
            return;
        }
        load(server.getResourceManager());
    }

    public static void load(ResourceManager resourceManager) {
        waveforms.clear();
        try {
            Optional<Resource> resource = resourceManager.getResource(CONFIG_PATH);
            if (resource.isEmpty()) {
                LOGGER.warn("resonator_waveforms.json not found, using defaults");
                setupDefaults();
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("stat_waveforms")) {
                    JsonObject stats = root.getAsJsonObject("stat_waveforms");
                    for (Map.Entry<String, JsonElement> statEntry : stats.entrySet()) {
                        String statName = statEntry.getKey();
                        JsonObject levels = statEntry.getValue().getAsJsonObject();
                        for (Map.Entry<String, JsonElement> lvlEntry : levels.entrySet()) {
                            String level = lvlEntry.getKey();
                            JsonObject wf = lvlEntry.getValue().getAsJsonObject();
                            int freq = wf.has("frequency") ? wf.get("frequency").getAsInt() : 20;
                            int amp = wf.has("amplitude") ? wf.get("amplitude").getAsInt() : 50;
                            int phase = wf.has("phase") ? wf.get("phase").getAsInt() : 0;
                            waveforms.put(statName + ":" + level, new StatWaveform(freq, amp, phase));
                        }
                    }
                }
            }
            loaded = true;
            LOGGER.info("Loaded {} resonator stat waveform entries", waveforms.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load resonator_waveforms.json", e);
            setupDefaults();
        }
    }

    private static void setupDefaults() {
        waveforms.put("drop:1", new StatWaveform(8, 60, 20));
        waveforms.put("drop:2", new StatWaveform(19, 45, 145));
        waveforms.put("drop:3", new StatWaveform(33, 80, 265));
        waveforms.put("drop:4", new StatWaveform(51, 55, 70));
        waveforms.put("speed:1", new StatWaveform(14, 50, 310));
        waveforms.put("speed:2", new StatWaveform(27, 75, 85));
        waveforms.put("speed:3", new StatWaveform(44, 40, 200));
        waveforms.put("speed:4", new StatWaveform(67, 90, 340));
        waveforms.put("foraging:1", new StatWaveform(5, 70, 155));
        waveforms.put("foraging:2", new StatWaveform(21, 35, 290));
        waveforms.put("foraging:3", new StatWaveform(38, 85, 50));
        waveforms.put("foraging:4", new StatWaveform(56, 60, 225));
        waveforms.put("tolerance:1", new StatWaveform(11, 40, 240));
        waveforms.put("tolerance:2", new StatWaveform(24, 65, 30));
        waveforms.put("tolerance:3", new StatWaveform(42, 50, 175));
        waveforms.put("tolerance:4", new StatWaveform(63, 85, 320));
        waveforms.put("activity:1", new StatWaveform(18, 55, 100));
        waveforms.put("activity:2", new StatWaveform(35, 80, 260));
        waveforms.put("activity:3", new StatWaveform(54, 45, 15));
        loaded = true;
    }

    // ========== API PUBLIQUE ==========

    /**
     * Retourne les parametres d'onde pour une stat a un niveau donne.
     * @param statName nom de la stat (drop, speed, foraging, tolerance, activity)
     * @param level niveau du trait (1, 2, 3, 4...)
     * @return la waveform ou null si inconnue
     */
    public static StatWaveform getStatWaveform(String statName, int level) {
        if (!loaded) setupDefaults();
        return waveforms.get(statName + ":" + level);
    }

    public static boolean isLoaded() { return loaded; }

    /**
     * Charge cote client si pas encore fait (lazy loading).
     */
    public static void ensureClientLoaded() {
        if (!loaded) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                load(mc.getResourceManager());
            }
        }
    }

    // ========== DATA CLASS ==========

    public static class StatWaveform {
        public final int frequency;
        public final int amplitude;
        public final int phase;

        public StatWaveform(int frequency, int amplitude, int phase) {
            this.frequency = frequency;
            this.amplitude = amplitude;
            this.phase = phase;
        }
    }
}
