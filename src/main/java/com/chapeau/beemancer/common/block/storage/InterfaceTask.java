/**
 * ============================================================
 * [InterfaceTask.java]
 * Description: Tache geree par une interface import/export avec etats TODO/LOCKED/DELIVERED
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
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represente une tache unitaire geree par une interface import/export.
 *
 * Cycle de vie:
 * - TODO: en attente d'assignation a une bee
 * - LOCKED: une bee a ete assignee (en transit). Le count peut evoluer via scan.
 * - DELIVERED: la bee a complete la livraison
 *
 * Le count est mutable: l'interface peut le mettre a jour entre les scans.
 * Quand une bee est LOCKED, lockedCount conserve le snapshot au moment du lock.
 * La bee lit count (actuel) au moment de l'extraction et de la livraison pour s'adapter.
 */
public class InterfaceTask {

    public enum TaskState {
        TODO,
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

    /**
     * Constructeur standard pour une nouvelle task.
     */
    public InterfaceTask(TaskType type, ItemStack template, int count) {
        this.taskId = UUID.randomUUID();
        this.type = type;
        this.template = template.copyWithCount(1);
        this.count = count;
        this.lockedCount = 0;
        this.state = TaskState.TODO;
        this.assignedBeeTaskId = null;
        this.lockedTick = 0;
    }

    /**
     * Constructeur NBT interne.
     */
    private InterfaceTask(UUID taskId, TaskType type, ItemStack template, int count,
                          int lockedCount, TaskState state,
                          @Nullable UUID assignedBeeTaskId, long lockedTick) {
        this.taskId = taskId;
        this.type = type;
        this.template = template;
        this.count = count;
        this.lockedCount = lockedCount;
        this.state = state;
        this.assignedBeeTaskId = assignedBeeTaskId;
        this.lockedTick = lockedTick;
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
     * Deverrouille la task: bee echouee/timeout, remet en TODO.
     */
    public void unlockTask() {
        this.state = TaskState.TODO;
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
        tag.putInt("LockedCount", lockedCount);
        // LOCKED sauvegarde comme TODO (bees ephemeres au reload)
        if (state == TaskState.LOCKED) {
            tag.putString("State", TaskState.TODO.name());
        } else {
            tag.putString("State", state.name());
        }
        if (assignedBeeTaskId != null && state != TaskState.LOCKED) {
            tag.putUUID("AssignedBeeTaskId", assignedBeeTaskId);
        }
        tag.putLong("LockedTick", lockedTick);
        return tag;
    }

    public static InterfaceTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID taskId = tag.getUUID("TaskId");
        TaskType type = TaskType.valueOf(tag.getString("Type"));
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");
        int lockedCount = tag.getInt("LockedCount");
        TaskState state = TaskState.valueOf(tag.getString("State"));
        UUID assignedBeeTaskId = tag.contains("AssignedBeeTaskId")
            ? tag.getUUID("AssignedBeeTaskId") : null;
        long lockedTick = tag.getLong("LockedTick");

        // Reset locked state on load (bees ephemeres)
        if (state == TaskState.LOCKED) {
            state = TaskState.TODO;
            lockedCount = 0;
            assignedBeeTaskId = null;
            lockedTick = 0;
        }

        return new InterfaceTask(taskId, type, template, count, lockedCount,
            state, assignedBeeTaskId, lockedTick);
    }
}
