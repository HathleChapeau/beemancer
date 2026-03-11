/**
 * ============================================================
 * [InterfaceTaskManager.java]
 * Description: Gere les InterfaceTasks (NEEDED/LOCKED/DELIVERED) pour une interface
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | InterfaceTask                 | Tache unitaire       | Map UUID->Task                 |
 * | StorageControllerBlockEntity  | Controller lie       | Cancel bees, publish tasks     |
 * | NetworkInterfaceBlockEntity   | Parent               | getController, setChanged      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceBlockEntity.java (delegation)
 * - ImportInterfaceBlockEntity.java (reconcile, cleanup)
 * - ExportInterfaceBlockEntity.java (reconcile, cleanup)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.InterfaceTask;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsule la Map des InterfaceTasks et toute la logique de reconciliation,
 * cleanup, publication et timeout.
 */
public class InterfaceTaskManager {

    private static final int LOCKED_TIMEOUT_TICKS = 1200;

    private final Map<UUID, InterfaceTask> tasks = new LinkedHashMap<>();
    private final NetworkInterfaceBlockEntity parent;

    public InterfaceTaskManager(NetworkInterfaceBlockEntity parent) {
        this.parent = parent;
    }

    // === Accessors ===

    @Nullable
    public InterfaceTask getTask(UUID taskId) {
        return tasks.get(taskId);
    }

    public Collection<InterfaceTask> getTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public int size() {
        return tasks.size();
    }

    // === Counting ===

    /**
     * Somme des lockedCount de toutes les tasks LOCKED pour un item donne.
     */
    public int getLockedCount(ItemStack template) {
        int total = 0;
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.matchesItem(template)) {
                total += task.getLockedCount();
            }
        }
        return total;
    }

    /**
     * Somme des count de toutes les tasks DELIVERED pour un item donne.
     */
    public int getDeliveredCount(ItemStack template) {
        int total = 0;
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() == InterfaceTask.TaskState.DELIVERED
                    && task.matchesItem(template)) {
                total += task.getCount();
            }
        }
        return total;
    }

    // === State Transitions ===

    public void markTaskDelivered(UUID taskId) {
        InterfaceTask task = tasks.get(taskId);
        if (task != null && task.getState() == InterfaceTask.TaskState.LOCKED) {
            task.markDelivered();
            parent.markNeedsScan();
            parent.setChanged();
        }
    }

    public void unlockTask(UUID taskId) {
        InterfaceTask task = tasks.get(taskId);
        if (task != null && task.getState() == InterfaceTask.TaskState.LOCKED) {
            task.unlockTask();
            parent.setChanged();
        }
    }

    // === Cleanup ===

    /**
     * Retire les tasks DELIVERED. Appele au debut de doScan().
     */
    public void cleanupDeliveredTasks() {
        boolean changed = tasks.values().removeIf(
            task -> task.getState() == InterfaceTask.TaskState.DELIVERED);
        if (changed) parent.setChanged();
    }

    /**
     * Supprime les tasks pour des items qui ne sont plus demandes.
     * Rappelle les bees LOCKED.
     */
    public void cleanupOrphanedTasks(Set<String> activeItemKeys) {
        StorageControllerBlockEntity controller = parent.getController();
        boolean changed = false;

        Iterator<InterfaceTask> it = tasks.values().iterator();
        while (it.hasNext()) {
            InterfaceTask task = it.next();
            if (task.getState() == InterfaceTask.TaskState.DELIVERED) continue;

            String key = itemKey(task.getTemplate());
            if (activeItemKeys.contains(key)) continue;

            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.getAssignedBeeTaskId() != null && controller != null) {
                controller.getDeliveryManager().cancelTask(task.getAssignedBeeTaskId());
            }
            it.remove();
            changed = true;
        }

        if (changed) parent.setChanged();
    }

    /**
     * Annule toutes les tasks et rappelle les bees LOCKED.
     */
    public void cancelAllTasks() {
        StorageControllerBlockEntity controller = parent.getController();

        for (InterfaceTask task : tasks.values()) {
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.getAssignedBeeTaskId() != null && controller != null) {
                controller.getDeliveryManager().cancelTask(task.getAssignedBeeTaskId());
            }
        }
        tasks.clear();
        parent.setChanged();
    }

    /**
     * Vide les tasks sans annuler les bees ni appeler setChanged().
     * Utilise UNIQUEMENT dans setRemoved() pour eviter de re-dirtier des chunks
     * pendant le world unload (cause boucle infinie saveAllChunks → save hang).
     */
    public void clearTasksSilent() {
        tasks.clear();
    }

    // === Reconciliation ===

    /**
     * Reconcilie les tasks existantes pour un item donne avec la demande actuelle.
     * 4 phases: ajuster LOCKED, annuler LOCKED a 0, ajuster NEEDED, creer NEEDED.
     */
    public void reconcileTasksForItem(ItemStack template, int totalDesired,
                                       int beeCapacity, InterfaceTask.TaskType type) {
        reconcileTasksForItem(template, totalDesired, beeCapacity, type, null);
    }

    /**
     * Reconcilie les tasks existantes pour un item donne avec la demande actuelle.
     * Pour les taches EXPORT, sourceSlots indique les slots d'ou extraire.
     * 4 phases: ajuster LOCKED, annuler LOCKED a 0, ajuster NEEDED, creer NEEDED.
     */
    public void reconcileTasksForItem(ItemStack template, int totalDesired,
                                       int beeCapacity, InterfaceTask.TaskType type,
                                       int[] sourceSlots) {
        int remaining = Math.max(0, totalDesired);

        // Phase 1: Ajuster les tasks LOCKED
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() != InterfaceTask.TaskState.LOCKED || !task.matchesItem(template)) continue;
            if (remaining <= 0) {
                task.setCount(0);
            } else {
                int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
                int taskShare = Math.min(remaining, itemBeeCapacity);
                task.setCount(taskShare);
                remaining -= taskShare;
            }
        }

        // Phase 2: Annuler les tasks LOCKED a count 0
        StorageControllerBlockEntity controller = parent.getController();
        Iterator<InterfaceTask> it = tasks.values().iterator();
        while (it.hasNext()) {
            InterfaceTask task = it.next();
            if (task.getState() != InterfaceTask.TaskState.LOCKED || !task.matchesItem(template)) continue;
            if (task.getCount() <= 0) {
                if (task.getAssignedBeeTaskId() != null && controller != null) {
                    controller.getDeliveryManager().cancelTask(task.getAssignedBeeTaskId());
                }
                it.remove();
            }
        }

        // Phase 3: Ajuster les tasks NEEDED
        it = tasks.values().iterator();
        while (it.hasNext()) {
            InterfaceTask task = it.next();
            if (task.getState() != InterfaceTask.TaskState.NEEDED || !task.matchesItem(template)) continue;
            if (remaining <= 0) {
                it.remove();
            } else {
                int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
                int taskShare = Math.min(remaining, itemBeeCapacity);
                task.setCount(taskShare);
                remaining -= taskShare;
            }
        }

        // Phase 4: Creer de nouvelles tasks NEEDED
        int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(remaining, itemBeeCapacity);
            // [FIX] Pour EXPORT, transmettre les sourceSlots pour extraire du bon slot
            InterfaceTask newTask = (sourceSlots != null && sourceSlots.length > 0)
                ? new InterfaceTask(type, template, chunk, sourceSlots)
                : new InterfaceTask(type, template, chunk);
            tasks.put(newTask.getTaskId(), newTask);
            remaining -= chunk;
        }

        parent.setChanged();
    }

    // === Publish ===

    /**
     * Publie les tasks NEEDED aupres du controller pour assignation de bees.
     */
    public void publishTodoTasks(StorageControllerBlockEntity controller) {
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() != InterfaceTask.TaskState.NEEDED) continue;
            controller.assignBeeToInterfaceTask(parent, task);
        }
        parent.setChanged();
    }

    // === Timeout ===

    /**
     * Deverrouille les tasks LOCKED dont le timeout a expire.
     * Retourne true si au moins une task a ete deverrouillee.
     */
    public boolean checkLockedTimeouts(long gameTick) {
        boolean unlocked = false;
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                && task.getLockedTick() > 0
                && (gameTick - task.getLockedTick()) > LOCKED_TIMEOUT_TICKS) {
                task.unlockTask();
                unlocked = true;
            }
        }
        return unlocked;
    }

    // === NBT ===

    public void save(CompoundTag parentTag, HolderLookup.Provider registries) {
        ListTag tasksTag = new ListTag();
        for (InterfaceTask task : tasks.values()) {
            if (task.getState() != InterfaceTask.TaskState.LOCKED) {
                tasksTag.add(task.save(registries));
            }
        }
        parentTag.put("InterfaceTasks", tasksTag);
    }

    public void load(CompoundTag parentTag, HolderLookup.Provider registries) {
        tasks.clear();
        if (parentTag.contains("InterfaceTasks")) {
            ListTag tasksTag = parentTag.getList("InterfaceTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tasksTag.size(); i++) {
                try {
                    InterfaceTask task = InterfaceTask.load(tasksTag.getCompound(i), registries);
                    tasks.put(task.getTaskId(), task);
                } catch (Exception e) {
                    com.chapeau.apica.Apica.LOGGER.warn("Skipping corrupted interface task at index {}", i, e);
                }
            }
        }
    }

    // === Debug ===

    public String getDebugText() {
        if (tasks.isEmpty()) return "Tasks: 0";

        int needed = 0, locked = 0, delivered = 0;
        for (InterfaceTask task : tasks.values()) {
            switch (task.getState()) {
                case NEEDED -> needed++;
                case LOCKED -> locked++;
                case DELIVERED -> delivered++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tasks: ").append(tasks.size());
        sb.append(" (N:").append(needed).append(" L:").append(locked).append(" D:").append(delivered).append(")\n");

        for (InterfaceTask task : tasks.values()) {
            String stateLetter = switch (task.getState()) {
                case NEEDED -> "N";
                case LOCKED -> "L";
                case DELIVERED -> "D";
            };
            String itemName = task.getTemplate().getItem().builtInRegistryHolder()
                .key().location().getPath();
            sb.append("[").append(stateLetter).append("] ")
              .append(itemName).append(" x").append(task.getCount()).append("\n");
        }

        return sb.toString().trim();
    }

    // === Utilities ===

    private String itemKey(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().toString()
            + ":" + stack.getComponentsPatch().hashCode();
    }
}
