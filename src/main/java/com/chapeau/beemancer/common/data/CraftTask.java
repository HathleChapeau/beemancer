/**
 * ============================================================
 * [CraftTask.java]
 * Description: Tache de craft unitaire avec etats, ressources et timeout
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CraftingPaperData   | Donnees recette      | Ingredients et resultat        |
 * | ItemStack           | Items                | Templates et comptage          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CraftManager.java (creation, orchestration, tick)
 * - StorageControllerBlockEntity.java (assignation bees)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represente une tache de craft unitaire dans le systeme de crafting automatique.
 *
 * Cycle de vie:
 * - PENDING_RESOURCES: en attente de livraison des ingredients par les bees
 * - CRAFTING: tous les ingredients sont dans le craft buffer, craft en cours
 * - RETURNING_ITEMS: craft termine, bee de retour ramene le resultat au reseau
 * - COMPLETED: le resultat a ete ramene au reseau par la bee de retour
 * - CANCELLED: annule (timeout, erreur, ou annulation manuelle)
 *
 * Chaque CraftTask correspond a un seul CraftingPaper. Les crafts imbriques
 * sont representes par des sous-taches (subTaskIds) qui doivent se completer
 * avant que la tache parente puisse demander ses propres ressources.
 */
public class CraftTask {

    public static final long TIMEOUT_TICKS = 2400; // 2 minutes

    public enum TaskState {
        PENDING_RESOURCES,
        CRAFTING,
        RETURNING_ITEMS,
        COMPLETED,
        CANCELLED
    }

    private final UUID taskId;
    @Nullable private final UUID parentTaskId;
    private final CraftingPaperData recipeData;
    private TaskState state;
    private long createdTick;
    private long lastActivityTick;

    /** Ressources primaires requises (apres resolution des sous-crafts). */
    private final Map<ResourceKey, Integer> requiredResources;

    /** Ressources deja livrees par les bees au craft buffer. */
    private final Map<ResourceKey, Integer> deliveredResources;

    /** IDs des sous-taches (crafts imbriques) qui doivent se completer d'abord. */
    private final List<UUID> subTaskIds;

    /** IDs des DeliveryTasks actuellement en vol pour cette tache. */
    private final List<UUID> activeDeliveryIds;

    /**
     * Constructeur pour une nouvelle tache.
     */
    public CraftTask(CraftingPaperData recipeData, long gameTick,
                     @Nullable UUID parentTaskId) {
        this.taskId = UUID.randomUUID();
        this.parentTaskId = parentTaskId;
        this.recipeData = recipeData;
        this.state = TaskState.PENDING_RESOURCES;
        this.createdTick = gameTick;
        this.lastActivityTick = gameTick;
        this.requiredResources = new HashMap<>();
        this.deliveredResources = new HashMap<>();
        this.subTaskIds = new ArrayList<>();
        this.activeDeliveryIds = new ArrayList<>();

        computeRequiredResources();
    }

    /**
     * Constructeur NBT interne.
     */
    private CraftTask(UUID taskId, @Nullable UUID parentTaskId,
                      CraftingPaperData recipeData, TaskState state,
                      long createdTick, long lastActivityTick,
                      Map<ResourceKey, Integer> requiredResources,
                      Map<ResourceKey, Integer> deliveredResources,
                      List<UUID> subTaskIds, List<UUID> activeDeliveryIds) {
        this.taskId = taskId;
        this.parentTaskId = parentTaskId;
        this.recipeData = recipeData;
        this.state = state;
        this.createdTick = createdTick;
        this.lastActivityTick = lastActivityTick;
        this.requiredResources = new HashMap<>(requiredResources);
        this.deliveredResources = new HashMap<>(deliveredResources);
        this.subTaskIds = new ArrayList<>(subTaskIds);
        this.activeDeliveryIds = new ArrayList<>(activeDeliveryIds);
    }

    // === Getters ===

    public UUID getTaskId() { return taskId; }
    @Nullable public UUID getParentTaskId() { return parentTaskId; }
    public CraftingPaperData getRecipeData() { return recipeData; }
    public TaskState getState() { return state; }
    public long getCreatedTick() { return createdTick; }
    public long getLastActivityTick() { return lastActivityTick; }
    public Map<ResourceKey, Integer> getRequiredResources() { return Collections.unmodifiableMap(requiredResources); }
    public Map<ResourceKey, Integer> getDeliveredResources() { return Collections.unmodifiableMap(deliveredResources); }
    public List<UUID> getSubTaskIds() { return Collections.unmodifiableList(subTaskIds); }
    public List<UUID> getActiveDeliveryIds() { return Collections.unmodifiableList(activeDeliveryIds); }
    public ItemStack getResult() { return recipeData.result().copy(); }

    public boolean isRoot() { return parentTaskId == null; }
    public boolean hasSubTasks() { return !subTaskIds.isEmpty(); }

    // === State Management ===

    public void setState(TaskState state) { this.state = state; }

    public void touchActivity(long gameTick) { this.lastActivityTick = gameTick; }

    /**
     * Verifie si cette tache a depasse le timeout (aucune activite pendant TIMEOUT_TICKS).
     */
    public boolean isTimedOut(long currentTick) {
        return currentTick - lastActivityTick > TIMEOUT_TICKS;
    }

    // === Sub-tasks ===

    public void addSubTask(UUID subTaskId) {
        subTaskIds.add(subTaskId);
    }

    /**
     * Verifie si toutes les sous-taches sont completees.
     */
    public boolean areSubTasksComplete(Map<UUID, CraftTask> allTasks) {
        for (UUID subId : subTaskIds) {
            CraftTask sub = allTasks.get(subId);
            if (sub == null || sub.getState() != TaskState.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    // === Delivery tracking ===

    public void addActiveDelivery(UUID deliveryId) {
        activeDeliveryIds.add(deliveryId);
    }

    public void removeActiveDelivery(UUID deliveryId) {
        activeDeliveryIds.remove(deliveryId);
    }

    public boolean hasActiveDeliveries() {
        return !activeDeliveryIds.isEmpty();
    }

    // === Resource tracking ===

    /**
     * Calcule les ressources requises a partir de la recette.
     * Agrege les ingredients identiques (ex: 4 planks = 1 entree de count 4).
     */
    private void computeRequiredResources() {
        for (ItemStack ingredient : recipeData.ingredients()) {
            if (ingredient.isEmpty()) continue;
            ResourceKey key = ResourceKey.of(ingredient);
            requiredResources.merge(key, 1, Integer::sum);
        }
    }

    /**
     * Enregistre qu'une ressource a ete livree au craft buffer.
     */
    public void recordDelivery(ItemStack delivered, int count) {
        if (delivered.isEmpty() || count <= 0) return;
        ResourceKey key = ResourceKey.of(delivered);
        deliveredResources.merge(key, count, Integer::sum);
    }

    /**
     * Calcule les ressources encore manquantes.
     */
    public Map<ResourceKey, Integer> getMissingResources() {
        Map<ResourceKey, Integer> missing = new HashMap<>();
        for (Map.Entry<ResourceKey, Integer> entry : requiredResources.entrySet()) {
            int delivered = deliveredResources.getOrDefault(entry.getKey(), 0);
            int needed = entry.getValue() - delivered;
            if (needed > 0) {
                missing.put(entry.getKey(), needed);
            }
        }
        return missing;
    }

    /**
     * Verifie si toutes les ressources requises ont ete livrees.
     */
    public boolean areAllResourcesDelivered() {
        return getMissingResources().isEmpty();
    }

    // === ResourceKey: identifies an item type for resource tracking ===

    /**
     * Cle immutable pour identifier un type d'item (ignore le count).
     * Utilise comme cle dans les maps de ressources.
     */
    public static final class ResourceKey {
        private final ItemStack template;

        private ResourceKey(ItemStack template) {
            this.template = template.copyWithCount(1);
        }

        public static ResourceKey of(ItemStack stack) {
            return new ResourceKey(stack);
        }

        public ItemStack getTemplate() { return template.copy(); }

        public boolean matches(ItemStack other) {
            return ItemStack.isSameItemSameComponents(template, other);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ResourceKey other)) return false;
            return ItemStack.isSameItemSameComponents(template, other.template);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(template);
        }

        public CompoundTag save(HolderLookup.Provider registries) {
            return (CompoundTag) template.save(registries);
        }

        public static ResourceKey load(CompoundTag tag, HolderLookup.Provider registries) {
            return new ResourceKey(ItemStack.parseOptional(registries, tag));
        }
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        if (parentTaskId != null) {
            tag.putUUID("ParentTaskId", parentTaskId);
        }
        tag.put("RecipeData", CraftingPaperData.saveToTag(recipeData, registries));
        tag.putString("State", state.name());
        tag.putLong("CreatedTick", createdTick);
        tag.putLong("LastActivityTick", lastActivityTick);

        // Required resources
        ListTag reqList = new ListTag();
        for (Map.Entry<ResourceKey, Integer> entry : requiredResources.entrySet()) {
            CompoundTag resTag = new CompoundTag();
            resTag.put("Item", entry.getKey().save(registries));
            resTag.putInt("Count", entry.getValue());
            reqList.add(resTag);
        }
        tag.put("RequiredResources", reqList);

        // Delivered resources
        ListTag delList = new ListTag();
        for (Map.Entry<ResourceKey, Integer> entry : deliveredResources.entrySet()) {
            CompoundTag resTag = new CompoundTag();
            resTag.put("Item", entry.getKey().save(registries));
            resTag.putInt("Count", entry.getValue());
            delList.add(resTag);
        }
        tag.put("DeliveredResources", delList);

        // Sub-task IDs
        if (!subTaskIds.isEmpty()) {
            ListTag subList = new ListTag();
            for (UUID id : subTaskIds) {
                CompoundTag idTag = new CompoundTag();
                idTag.putUUID("Id", id);
                subList.add(idTag);
            }
            tag.put("SubTaskIds", subList);
        }

        // Active delivery IDs (saved as PENDING_RESOURCES on reload since bees are ephemeral)

        return tag;
    }

    public static CraftTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID taskId = tag.getUUID("TaskId");
        UUID parentTaskId = tag.contains("ParentTaskId") ? tag.getUUID("ParentTaskId") : null;

        CraftingPaperData recipeData = CraftingPaperData.loadFromTag(
                tag.getCompound("RecipeData"), registries);

        TaskState state;
        try {
            state = TaskState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            state = TaskState.CANCELLED;
        }

        long createdTick = tag.getLong("CreatedTick");
        long lastActivityTick = tag.getLong("LastActivityTick");

        // Required resources
        Map<ResourceKey, Integer> requiredResources = new HashMap<>();
        if (tag.contains("RequiredResources", Tag.TAG_LIST)) {
            ListTag reqList = tag.getList("RequiredResources", Tag.TAG_COMPOUND);
            for (int i = 0; i < reqList.size(); i++) {
                CompoundTag resTag = reqList.getCompound(i);
                ResourceKey key = ResourceKey.load(resTag.getCompound("Item"), registries);
                requiredResources.put(key, resTag.getInt("Count"));
            }
        }

        // Delivered resources
        Map<ResourceKey, Integer> deliveredResources = new HashMap<>();
        if (tag.contains("DeliveredResources", Tag.TAG_LIST)) {
            ListTag delList = tag.getList("DeliveredResources", Tag.TAG_COMPOUND);
            for (int i = 0; i < delList.size(); i++) {
                CompoundTag resTag = delList.getCompound(i);
                ResourceKey key = ResourceKey.load(resTag.getCompound("Item"), registries);
                deliveredResources.put(key, resTag.getInt("Count"));
            }
        }

        // Sub-task IDs
        List<UUID> subTaskIds = new ArrayList<>();
        if (tag.contains("SubTaskIds", Tag.TAG_LIST)) {
            ListTag subList = tag.getList("SubTaskIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < subList.size(); i++) {
                subTaskIds.add(subList.getCompound(i).getUUID("Id"));
            }
        }

        // Bees are ephemeral: no active deliveries loaded
        List<UUID> activeDeliveryIds = new ArrayList<>();

        // Reset CRAFTING/RETURNING_ITEMS state on load (bees are ephemeral)
        if (state == TaskState.CRAFTING || state == TaskState.RETURNING_ITEMS) {
            state = TaskState.PENDING_RESOURCES;
        }

        return new CraftTask(taskId, parentTaskId, recipeData, state,
                createdTick, lastActivityTick,
                requiredResources, deliveredResources,
                subTaskIds, activeDeliveryIds);
    }
}
