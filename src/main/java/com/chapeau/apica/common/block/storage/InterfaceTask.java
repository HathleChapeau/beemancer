/**
 * ============================================================
 * [InterfaceTask.java]
 * Description: Tache geree par une interface import/export avec etats NEEDED/LOCKED/DELIVERED
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | ItemStack      | Template item        | Item a transporter             |
 * | BlockPos       | Positions            | Via NBT (long)                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceBlockEntity.java (liste de tasks geree par l'interface)
 * - ImportInterfaceBlockEntity.java (creation de tasks import)
 * - ExportInterfaceBlockEntity.java (creation de tasks export)
 * - StorageControllerBlockEntity.java (assignation de bees aux tasks)
 * - DeliveryBeeEntity.java (lecture du count actuel)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

/**
 * Represente une tache unitaire geree par une interface import/export.
 *
 * Cycle de vie:
 * - NEEDED: en attente d'assignation a une bee
 * - LOCKED: une bee a ete assignee (en transit). Le count peut evoluer via scan.
 * - DELIVERED: la bee a complete la livraison
 *
 * Le count est mutable: l'interface peut le mettre a jour entre les scans.
 * Quand une bee est LOCKED, lockedCount conserve le snapshot au moment du lock.
 * La bee lit count (actuel) au moment de l'extraction et de la livraison pour s'adapter.
 */
public class InterfaceTask {

    public enum TaskState {
        NEEDED,
        LOCKED,
        DELIVERED
    }

    public enum TaskType {
        IMPORT,
        EXPORT
    }

    private final UUID taskId;
    private final TaskType type;
    private final ItemStack template;
    private int count;
    private int lockedCount;
    private TaskState state;
    @Nullable private UUID assignedBeeTaskId;
    private long lockedTick;
    // [FIX] Pour les taches EXPORT, trackez les slots sources pour extraire du bon slot
    private int[] sourceSlots;

    /**
     * Constructeur standard pour une nouvelle task.
     */
    public InterfaceTask(TaskType type, ItemStack template, int count) {
        this(type, template, count, new int[0]);
    }

    /**
     * Constructeur avec slots sources (pour EXPORT: slots d'ou extraire).
     */
    public InterfaceTask(TaskType type, ItemStack template, int count, int[] sourceSlots) {
        this.taskId = UUID.randomUUID();
        this.type = type;
        this.template = template.copyWithCount(1);
        this.count = count;
        this.lockedCount = 0;
        this.state = TaskState.NEEDED;
        this.assignedBeeTaskId = null;
        this.lockedTick = 0;
        this.sourceSlots = sourceSlots != null ? Arrays.copyOf(sourceSlots, sourceSlots.length) : new int[0];
    }

    /**
     * Constructeur NBT interne.
     */
    private InterfaceTask(UUID taskId, TaskType type, ItemStack template, int count,
                          int lockedCount, TaskState state,
                          @Nullable UUID assignedBeeTaskId, long lockedTick, int[] sourceSlots) {
        this.taskId = taskId;
        this.type = type;
        this.template = template;
        this.count = count;
        this.lockedCount = lockedCount;
        this.state = state;
        this.assignedBeeTaskId = assignedBeeTaskId;
        this.lockedTick = lockedTick;
        this.sourceSlots = sourceSlots != null ? sourceSlots : new int[0];
    }

    // === Getters ===

    public UUID getTaskId() { return taskId; }
    public TaskType getType() { return type; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public int getLockedCount() { return lockedCount; }
    public TaskState getState() { return state; }
    @Nullable public UUID getAssignedBeeTaskId() { return assignedBeeTaskId; }
    public long getLockedTick() { return lockedTick; }
    /** Slots sources pour EXPORT (slots d'ou extraire dans le coffre adjacent). */
    public int[] getSourceSlots() { return sourceSlots != null ? sourceSlots : new int[0]; }

    // === Setters ===

    public void setCount(int count) { this.count = count; }

    // === State Transitions ===

    /**
     * Verrouille la task: une bee est assignee.
     * Snapshot le count actuel dans lockedCount.
     */
    public void lockTask(UUID beeTaskId, long gameTick) {
        this.state = TaskState.LOCKED;
        this.lockedCount = this.count;
        this.assignedBeeTaskId = beeTaskId;
        this.lockedTick = gameTick;
    }

    /**
     * Marque la task comme livree par la bee.
     */
    public void markDelivered() {
        this.state = TaskState.DELIVERED;
    }

    /**
     * Deverrouille la task: bee echouee/timeout, remet en NEEDED.
     */
    public void unlockTask() {
        this.state = TaskState.NEEDED;
        this.lockedCount = 0;
        this.assignedBeeTaskId = null;
        this.lockedTick = 0;
    }

    /**
     * Verifie si le template correspond a l'item donne.
     */
    public boolean matchesItem(ItemStack other) {
        return ItemStack.isSameItemSameComponents(template, other);
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        tag.putString("Type", type.name());
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putString("State", state.name());
        if (sourceSlots != null && sourceSlots.length > 0) {
            tag.put("SourceSlots", new IntArrayTag(sourceSlots));
        }
        return tag;
    }

    public static InterfaceTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID taskId = tag.getUUID("TaskId");
        TaskType type = TaskType.valueOf(tag.getString("Type"));
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");
        TaskState state = TaskState.valueOf(tag.getString("State"));
        int[] sourceSlots = tag.contains("SourceSlots") ? tag.getIntArray("SourceSlots") : new int[0];

        return new InterfaceTask(taskId, type, template, count, 0,
            state, null, 0, sourceSlots);
    }
}
