/**
 * ============================================================
 * [CodexPlayerData.java]
 * Description: Données de progression du Codex pour un joueur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Nodes débloqués      | Stockage des IDs               |
 * | CodexManager        | Vérification nodes   | Validation des déblocages      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerAttachments (enregistrement de l'attachment)
 * - CodexScreen (affichage de la progression)
 * - CodexUnlockPacket (mise à jour serveur)
 * - CodexBookScreen (calcul Day X)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodexPlayerData {
    public static final Codec<CodexPlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.listOf().fieldOf("unlocked_nodes").forGetter(data -> List.copyOf(data.unlockedNodes)),
            Codec.STRING.listOf().fieldOf("discovered_nodes").forGetter(data -> List.copyOf(data.discoveredNodes)),
            Codec.LONG.fieldOf("first_open_day").forGetter(data -> data.firstOpenDay),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("unlock_days").forGetter(data -> data.unlockDays)
        ).apply(instance, (unlockedList, discoveredList, openDay, days) -> {
            CodexPlayerData data = new CodexPlayerData();
            data.unlockedNodes.addAll(unlockedList);
            data.discoveredNodes.addAll(discoveredList);
            data.firstOpenDay = openDay;
            data.unlockDays.putAll(days);
            return data;
        })
    );

    private final Set<String> unlockedNodes = new HashSet<>();
    private final Set<String> discoveredNodes = new HashSet<>();
    private long firstOpenDay = -1;
    private final Map<String, Long> unlockDays = new HashMap<>();

    public CodexPlayerData() {
    }

    // ============================================================
    // NODES (existant)
    // ============================================================

    public Set<String> getUnlockedNodes() {
        return unlockedNodes;
    }

    public Set<String> getDiscoveredNodes() {
        return discoveredNodes;
    }

    public boolean isUnlocked(String fullNodeId) {
        return unlockedNodes.contains(fullNodeId);
    }

    public boolean isUnlocked(CodexNode node) {
        return unlockedNodes.contains(node.getFullId());
    }

    public boolean isDiscovered(String fullNodeId) {
        return discoveredNodes.contains(fullNodeId);
    }

    public boolean isDiscovered(CodexNode node) {
        return discoveredNodes.contains(node.getFullId());
    }

    /**
     * Marque un node comme découvert (quête complétée).
     * @return true si le node vient d'être découvert
     */
    public boolean discover(String fullNodeId) {
        return discoveredNodes.add(fullNodeId);
    }

    public boolean discover(CodexNode node) {
        return discoveredNodes.add(node.getFullId());
    }

    public boolean unlock(CodexNode node) {
        if (!CodexManager.canUnlock(node, unlockedNodes)) {
            return false;
        }
        return unlockedNodes.add(node.getFullId());
    }

    public boolean unlock(String fullNodeId) {
        CodexNode node = CodexManager.getNode(fullNodeId);
        if (node == null) {
            return false;
        }
        return unlock(node);
    }

    public void forceUnlock(String fullNodeId) {
        unlockedNodes.add(fullNodeId);
    }

    public int getUnlockedCount() {
        return unlockedNodes.size();
    }

    public int getUnlockedCountForPage(CodexPage page) {
        int count = 0;
        String prefix = page.getId() + ":";
        for (String nodeId : unlockedNodes) {
            if (nodeId.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    // ============================================================
    // DAY TRACKING
    // ============================================================

    /**
     * Enregistre le jour MC de la première ouverture du Codex.
     * Ne fait rien si déjà enregistré.
     * @param currentDay Le jour MC actuel (level.getDayTime() / 24000L)
     * @return true si c'est la première ouverture
     */
    public boolean recordFirstOpen(long currentDay) {
        if (firstOpenDay == -1) {
            firstOpenDay = currentDay;
            return true;
        }
        return false;
    }

    public long getFirstOpenDay() {
        return firstOpenDay;
    }

    public void setFirstOpenDay(long day) {
        this.firstOpenDay = day;
    }

    /**
     * Enregistre le jour MC où un node a été débloqué.
     * @param fullNodeId L'ID complet du node (page:id)
     * @param currentDay Le jour MC actuel
     */
    public void recordUnlockDay(String fullNodeId, long currentDay) {
        unlockDays.putIfAbsent(fullNodeId, currentDay);
    }

    /**
     * Retourne le jour MC où un node a été débloqué.
     * @param fullNodeId L'ID complet du node
     * @return Le jour MC, ou -1 si pas encore débloqué
     */
    public long getUnlockDay(String fullNodeId) {
        return unlockDays.getOrDefault(fullNodeId, -1L);
    }

    public Map<String, Long> getUnlockDays() {
        return unlockDays;
    }

    /**
     * Calcule le jour relatif d'un node (par rapport à la première ouverture).
     * @param fullNodeId L'ID complet du node
     * @return Le jour relatif (1-based), ou -1 si données indisponibles
     */
    public long getRelativeDay(String fullNodeId) {
        if (firstOpenDay == -1) {
            return -1;
        }
        long unlockDay = getUnlockDay(fullNodeId);
        if (unlockDay == -1) {
            return -1;
        }
        return unlockDay - firstOpenDay + 1;
    }

    // ============================================================
    // SÉRIALISATION
    // ============================================================

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag unlockedList = new ListTag();
        for (String nodeId : unlockedNodes) {
            unlockedList.add(StringTag.valueOf(nodeId));
        }
        tag.put("unlocked", unlockedList);

        ListTag discoveredList = new ListTag();
        for (String nodeId : discoveredNodes) {
            discoveredList.add(StringTag.valueOf(nodeId));
        }
        tag.put("discovered", discoveredList);

        tag.putLong("first_open_day", firstOpenDay);

        CompoundTag daysTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : unlockDays.entrySet()) {
            daysTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("unlock_days", daysTag);

        return tag;
    }

    public static CodexPlayerData fromNbt(CompoundTag tag) {
        CodexPlayerData data = new CodexPlayerData();
        if (tag.contains("unlocked", Tag.TAG_LIST)) {
            ListTag list = tag.getList("unlocked", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                data.unlockedNodes.add(list.getString(i));
            }
        }
        if (tag.contains("discovered", Tag.TAG_LIST)) {
            ListTag list = tag.getList("discovered", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                data.discoveredNodes.add(list.getString(i));
            }
        }
        if (tag.contains("first_open_day", Tag.TAG_LONG)) {
            data.firstOpenDay = tag.getLong("first_open_day");
        }
        if (tag.contains("unlock_days", Tag.TAG_COMPOUND)) {
            CompoundTag daysTag = tag.getCompound("unlock_days");
            for (String key : daysTag.getAllKeys()) {
                data.unlockDays.put(key, daysTag.getLong(key));
            }
        }
        return data;
    }

    public CodexPlayerData copy() {
        CodexPlayerData copy = new CodexPlayerData();
        copy.unlockedNodes.addAll(this.unlockedNodes);
        copy.discoveredNodes.addAll(this.discoveredNodes);
        copy.firstOpenDay = this.firstOpenDay;
        copy.unlockDays.putAll(this.unlockDays);
        return copy;
    }
}
