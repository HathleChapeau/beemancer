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
            Codec.STRING.listOf().fieldOf("unlocked_nodes").forGetter(data -> List.copyOf(data.unlockedNodes))
        ).apply(instance, list -> {
            CodexPlayerData data = new CodexPlayerData();
            data.unlockedNodes.addAll(list);
            data.ensureDefaultNodesUnlocked();
            return data;
        })
    );

    // IDs des nodes headers (bleus) qui sont débloqués par défaut
    private static final Set<String> DEFAULT_UNLOCKED_NODES = Set.of(
        "apica:the_beginning",      // APICA header
        "alchemy:manual_centrifuge", // ALCHEMY header
        "artifacts:altar",           // ARTIFACTS header
        "logistics:crystallyzer"     // LOGISTICS header
    );

    private final Set<String> unlockedNodes = new HashSet<>();

    public CodexPlayerData() {
        ensureDefaultNodesUnlocked();
    }

    /**
     * S'assure que les nodes par défaut (headers bleus) sont débloqués.
     */
    public void ensureDefaultNodesUnlocked() {
        unlockedNodes.addAll(DEFAULT_UNLOCKED_NODES);
    }

    public Set<String> getUnlockedNodes() {
        return unlockedNodes;
    }

    public boolean isUnlocked(String fullNodeId) {
        return unlockedNodes.contains(fullNodeId);
    }

    public boolean isUnlocked(CodexNode node) {
        return unlockedNodes.contains(node.getFullId());
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
        ListTag list = new ListTag();
        for (String nodeId : unlockedNodes) {
            list.add(StringTag.valueOf(nodeId));
        }
        tag.put("unlocked", list);
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
        // S'assurer que les nodes par défaut sont toujours débloqués
        data.ensureDefaultNodesUnlocked();
        return data;
    }

    public CodexPlayerData copy() {
        CodexPlayerData copy = new CodexPlayerData();
        copy.unlockedNodes.addAll(this.unlockedNodes);
        copy.ensureDefaultNodesUnlocked();
        return copy;
    }

    /**
     * Vérifie si un node est un header (débloqué par défaut).
     */
    public static boolean isDefaultNode(String fullNodeId) {
        return DEFAULT_UNLOCKED_NODES.contains(fullNodeId);
    }
}
