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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodexPlayerData {
    public static final Codec<CodexPlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.listOf().fieldOf("unlocked_nodes").forGetter(data -> List.copyOf(data.unlockedNodes)),
            Codec.STRING.listOf().fieldOf("discovered_nodes").forGetter(data -> List.copyOf(data.discoveredNodes))
        ).apply(instance, (unlockedList, discoveredList) -> {
            CodexPlayerData data = new CodexPlayerData();
            data.unlockedNodes.addAll(unlockedList);
            data.discoveredNodes.addAll(discoveredList);
            return data;
        })
    );

    private final Set<String> unlockedNodes = new HashSet<>();
    private final Set<String> discoveredNodes = new HashSet<>();

    public CodexPlayerData() {
    }

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
        return data;
    }

    public CodexPlayerData copy() {
        CodexPlayerData copy = new CodexPlayerData();
        copy.unlockedNodes.addAll(this.unlockedNodes);
        copy.discoveredNodes.addAll(this.discoveredNodes);
        return copy;
    }
}
