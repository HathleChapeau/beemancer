/**
 * ============================================================
 * [Quest.java]
 * Description: Définition d'une quête liée à un node du Codex
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | QuestType           | Type de quête        | Déterminer comment vérifier    |
 * | ResourceLocation    | Identifiants         | Items et machines              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - QuestManager (stockage et vérification)
 * - QuestEvents (détection completion)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.quest;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Représente une quête qui doit être complétée pour découvrir un node.
 */
public class Quest {

    private final String id;
    private final String nodeId;
    private final QuestType type;
    private final ResourceLocation targetItem;
    private final int targetCount;
    private final String targetMachine;
    private final String targetSpecies;

    public Quest(String id, String nodeId, QuestType type,
                 @Nullable ResourceLocation targetItem, int targetCount,
                 @Nullable String targetMachine, @Nullable String targetSpecies) {
        this.id = id;
        this.nodeId = nodeId;
        this.type = type;
        this.targetItem = targetItem;
        this.targetCount = Math.max(1, targetCount);
        this.targetMachine = targetMachine;
        this.targetSpecies = targetSpecies;
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public QuestType getType() {
        return type;
    }

    @Nullable
    public ResourceLocation getTargetItem() {
        return targetItem;
    }

    public int getTargetCount() {
        return targetCount;
    }

    @Nullable
    public String getTargetMachine() {
        return targetMachine;
    }

    @Nullable
    public String getTargetSpecies() {
        return targetSpecies;
    }

    // ============================================================
    // VERIFICATION
    // ============================================================

    /**
     * Vérifie si un item correspond à la cible de cette quête.
     */
    public boolean matchesItem(ItemStack stack) {
        if (targetItem == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = stack.getItemHolder().unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        return targetItem.equals(itemId) && stack.getCount() >= targetCount;
    }

    /**
     * Vérifie si une espèce d'abeille correspond à la cible.
     */
    public boolean matchesSpecies(String species) {
        return targetSpecies != null && targetSpecies.equals(species);
    }

    /**
     * Vérifie si un type de machine correspond.
     */
    public boolean matchesMachine(String machineType) {
        return targetMachine != null && targetMachine.equals(machineType);
    }

    /**
     * Vérifie si la quête OBTAIN est complétée (item dans inventaire).
     */
    public boolean checkObtainQuest(Player player) {
        if (type != QuestType.OBTAIN || targetItem == null) {
            return false;
        }

        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation itemId = stack.getItemHolder().unwrapKey()
                        .map(key -> key.location())
                        .orElse(null);
                if (targetItem.equals(itemId)) {
                    count += stack.getCount();
                    if (count >= targetCount) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ============================================================
    // PARSING JSON
    // ============================================================

    /**
     * Crée une quête depuis un objet JSON.
     */
    public static Quest fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String nodeId = json.get("node").getAsString();
        QuestType type = QuestType.fromString(json.get("type").getAsString());

        ResourceLocation targetItem = null;
        if (json.has("item")) {
            targetItem = ResourceLocation.parse(json.get("item").getAsString());
        }

        int targetCount = json.has("count") ? json.get("count").getAsInt() : 1;
        String targetMachine = json.has("machine") ? json.get("machine").getAsString() : null;
        String targetSpecies = json.has("species") ? json.get("species").getAsString() : null;

        return new Quest(id, nodeId, type, targetItem, targetCount, targetMachine, targetSpecies);
    }
}
