/**
 * ============================================================
 * [ResonatorConfigManager.java]
 * Description: Gestionnaire de configuration des waveforms par stat pour le resonateur
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
 * - ResonatorScreen.java (lecture des waveforms cibles)
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
 * Charge les parametres d'onde (freq, amp, phase) associes a chaque stat d'abeille.
 * Utilise par le resonateur pour determiner les valeurs cibles en fonction de l'abeille posee.
 */
public class ResonatorConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResonatorConfigManager.class);
    private static final ResourceLocation CONFIG_PATH = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "config/resonator_waveforms.json");

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
                    for (Map.Entry<String, JsonElement> entry : stats.entrySet()) {
                        JsonObject wf = entry.getValue().getAsJsonObject();
                        int freq = wf.has("frequency") ? wf.get("frequency").getAsInt() : 20;
                        int amp = wf.has("amplitude") ? wf.get("amplitude").getAsInt() : 50;
                        int phase = wf.has("phase") ? wf.get("phase").getAsInt() : 0;
                        waveforms.put(entry.getKey(), new StatWaveform(freq, amp, phase));
                    }
                }
            }
            loaded = true;
            LOGGER.info("Loaded {} resonator stat waveforms", waveforms.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load resonator_waveforms.json", e);
            setupDefaults();
        }
    }

    private static void setupDefaults() {
        waveforms.put("drop", new StatWaveform(12, 80, 0));
        waveforms.put("speed", new StatWaveform(35, 65, 90));
        waveforms.put("foraging", new StatWaveform(22, 75, 180));
        waveforms.put("tolerance", new StatWaveform(50, 55, 270));
        waveforms.put("activity", new StatWaveform(68, 90, 45));
        loaded = true;
    }

    // ========== API PUBLIQUE ==========

    /**
     * Retourne les parametres d'onde pour une stat donnee.
     * @param statName nom de la stat (drop, speed, foraging, tolerance, activity)
     * @return la waveform ou null si inconnue
     */
    public static StatWaveform getStatWaveform(String statName) {
        if (!loaded) setupDefaults();
        return waveforms.get(statName);
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
