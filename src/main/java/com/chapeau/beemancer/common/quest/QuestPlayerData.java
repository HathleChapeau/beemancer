/**
 * ============================================================
 * [QuestPlayerData.java]
 * Description: Données des quêtes complétées par un joueur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Codec               | Sérialisation        | Sauvegarde NeoForge            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerAttachments (stockage)
 * - QuestManager (vérification)
 * - CodexScreen (affichage états)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * Stocke les quêtes complétées par un joueur.
 * Attaché au joueur via NeoForge Attachments.
 */
public class QuestPlayerData {

    private final Set<String> completedQuests;

    public static final Codec<QuestPlayerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf()
                            .xmap(HashSet::new, list -> list.stream().toList())
                            .fieldOf("completed_quests")
                            .forGetter(data -> new HashSet<>(data.completedQuests))
            ).apply(instance, QuestPlayerData::new)
    );

    public QuestPlayerData() {
        this.completedQuests = new HashSet<>();
    }

    public QuestPlayerData(Set<String> completedQuests) {
        this.completedQuests = new HashSet<>(completedQuests);
    }

    // ============================================================
    // GESTION DES QUETES
    // ============================================================

    /**
     * Vérifie si une quête est complétée.
     */
    public boolean isCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    /**
     * Marque une quête comme complétée.
     * @return true si la quête n'était pas déjà complétée
     */
    public boolean complete(String questId) {
        return completedQuests.add(questId);
    }

    /**
     * Retourne l'ensemble des quêtes complétées.
     */
    public Set<String> getCompletedQuests() {
        return new HashSet<>(completedQuests);
    }

    // ============================================================
    // SERIALISATION NBT
    // ============================================================

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag questList = new ListTag();
        for (String questId : completedQuests) {
            questList.add(StringTag.valueOf(questId));
        }
        tag.put("completed_quests", questList);
        return tag;
    }

    public static QuestPlayerData fromNbt(CompoundTag tag) {
        Set<String> quests = new HashSet<>();
        if (tag.contains("completed_quests", Tag.TAG_LIST)) {
            ListTag list = tag.getList("completed_quests", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                quests.add(list.getString(i));
            }
        }
        return new QuestPlayerData(quests);
    }

    // ============================================================
    // COPIE
    // ============================================================

    public QuestPlayerData copy() {
        return new QuestPlayerData(new HashSet<>(completedQuests));
    }
}
