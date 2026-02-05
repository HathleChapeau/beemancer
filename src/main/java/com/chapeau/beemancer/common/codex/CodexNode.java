/**
 * ============================================================
 * [CodexNode.java]
 * Description: Représentation d'un node dans le Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPage           | Page parente         | Organisation par onglet        |
 * | CodexNodeCategory   | Style du node        | Rendu visuel                   |
 * | ResourceLocation    | Icône du node        | Affichage dans le GUI          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexManager (stockage des nodes)
 * - CodexNodeWidget (rendu)
 * - CodexPlayerData (vérification déblocage)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.codex;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.quest.NodeVisibility;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CodexNode {
    private final String id;
    private final CodexPage page;
    private final int x;
    private final int y;
    private final ResourceLocation icon;
    private final CodexNodeCategory category;
    @Nullable
    private final String parentId;
    private final boolean hiddenUntilParentUnlocked;
    private final List<String> childrenIds;
    private final JsonObject unlockCondition;
    private final JsonObject rewards;
    @Nullable
    private final String breedingParent1;
    @Nullable
    private final String breedingParent2;
    private final NodeVisibility visibility;
    @Nullable
    private final String questId;

    public CodexNode(String id, CodexPage page, int x, int y,
                     ResourceLocation icon, CodexNodeCategory category,
                     @Nullable String parentId, boolean hiddenUntilParentUnlocked,
                     JsonObject unlockCondition, JsonObject rewards,
                     @Nullable String breedingParent1, @Nullable String breedingParent2,
                     NodeVisibility visibility, @Nullable String questId) {
        this.id = id;
        this.page = page;
        this.x = x;
        this.y = y;
        this.icon = icon;
        this.category = category;
        this.parentId = parentId;
        this.hiddenUntilParentUnlocked = hiddenUntilParentUnlocked;
        this.childrenIds = new ArrayList<>();
        this.unlockCondition = unlockCondition;
        this.rewards = rewards;
        this.breedingParent1 = breedingParent1;
        this.breedingParent2 = breedingParent2;
        this.visibility = visibility;
        this.questId = questId;
    }

    public String getId() {
        return id;
    }

    public String getFullId() {
        return page.getId() + ":" + id;
    }

    public CodexPage getPage() {
        return page;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public CodexNodeCategory getCategory() {
        return category;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    public boolean isHiddenUntilParentUnlocked() {
        return hiddenUntilParentUnlocked;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public void addChild(String childId) {
        if (!childrenIds.contains(childId)) {
            childrenIds.add(childId);
        }
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public Component getTitle() {
        return Component.translatable("codex." + Beemancer.MOD_ID + "." + page.getId() + "." + id + ".title");
    }

    public Component getDescription() {
        return Component.translatable("codex." + Beemancer.MOD_ID + "." + page.getId() + "." + id + ".desc");
    }

    public JsonObject getUnlockCondition() {
        return unlockCondition;
    }

    public JsonObject getRewards() {
        return rewards;
    }

    @Nullable
    public String getBreedingParent1() {
        return breedingParent1;
    }

    @Nullable
    public String getBreedingParent2() {
        return breedingParent2;
    }

    public boolean hasBreedingParents() {
        return breedingParent1 != null && breedingParent2 != null;
    }

    public NodeVisibility getVisibility() {
        return visibility;
    }

    @Nullable
    public String getQuestId() {
        return questId;
    }

    public boolean hasQuest() {
        return questId != null && !questId.isEmpty();
    }

    public static CodexNode fromJson(JsonObject json, CodexPage page) {
        String id = json.get("id").getAsString();

        JsonObject pos = json.getAsJsonObject("position");
        int x = pos.get("x").getAsInt();
        int y = pos.get("y").getAsInt();

        String iconPath = json.get("icon").getAsString();
        ResourceLocation icon = ResourceLocation.parse(iconPath);

        String categoryStr = json.has("category") ? json.get("category").getAsString() : "NORMAL";
        CodexNodeCategory category = CodexNodeCategory.fromId(categoryStr);

        String parentId = json.has("parent") && !json.get("parent").isJsonNull()
            ? json.get("parent").getAsString()
            : null;

        boolean hidden = json.has("hidden_until_parent_unlocked")
            && json.get("hidden_until_parent_unlocked").getAsBoolean();

        JsonObject unlockCondition = json.has("unlock_condition")
            ? json.getAsJsonObject("unlock_condition")
            : new JsonObject();

        JsonObject rewards = json.has("rewards")
            ? json.getAsJsonObject("rewards")
            : new JsonObject();

        String breedingParent1 = json.has("breeding_parent1") && !json.get("breeding_parent1").isJsonNull()
            ? json.get("breeding_parent1").getAsString()
            : null;

        String breedingParent2 = json.has("breeding_parent2") && !json.get("breeding_parent2").isJsonNull()
            ? json.get("breeding_parent2").getAsString()
            : null;

        String visibilityStr = json.has("visibility") ? json.get("visibility").getAsString() : "VISIBLE";
        NodeVisibility visibility = NodeVisibility.fromString(visibilityStr);

        String questId = json.has("quest_id") && !json.get("quest_id").isJsonNull()
            ? json.get("quest_id").getAsString()
            : null;

        return new CodexNode(id, page, x, y, icon, category, parentId, hidden, unlockCondition, rewards,
                breedingParent1, breedingParent2, visibility, questId);
    }
}
