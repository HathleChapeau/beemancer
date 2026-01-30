/**
 * ============================================================
 * [DeliveryTask.java]
 * Description: Tâche de livraison avec priorité, dépendances et splitting
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | BlockPos       | Positions cibles     | Coffre et terminal             |
 * | ItemStack      | Template item        | Item à transporter             |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (queue de livraison)
 * - DeliveryBeeEntity.java (exécution de la tâche)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Représente une tâche de livraison dans le réseau de stockage.
 * Chaque tâche décrit un transfert d'items entre un coffre et un terminal,
 * exécuté par une DeliveryBee.
 *
 * Supporte:
 * - Priorité (lower = higher priority, default 0)
 * - Dépendances (task IDs qui doivent être complétées avant exécution)
 * - Splitting (division en sous-tâches quand la quantité dépasse la capacité d'une abeille)
 * - Assignation à une abeille spécifique
 */
public class DeliveryTask {

    public enum DeliveryType {
        EXTRACT,
        DEPOSIT
    }

    public enum DeliveryState {
        QUEUED,
        FLYING,
        WAITING,
        RETURNING,
        COMPLETED,
        FAILED
    }

    private final UUID taskId;
    private final ItemStack template;
    private int count;
    private final BlockPos targetChest;
    private final BlockPos terminalPos;
    private final DeliveryType type;
    private DeliveryState state;
    private int priority;
    private final List<UUID> dependencies;
    private UUID assignedBeeId;

    /**
     * Constructeur standard (priorité 0, pas de dépendances).
     */
    public DeliveryTask(ItemStack template, int count, BlockPos targetChest,
                        BlockPos terminalPos, DeliveryType type) {
        this(template, count, targetChest, terminalPos, type, 0, Collections.emptyList());
    }

    /**
     * Constructeur avec priorité et dépendances.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos targetChest,
                        BlockPos terminalPos, DeliveryType type,
                        int priority, List<UUID> dependencies) {
        this.taskId = UUID.randomUUID();
        this.template = template.copy();
        this.count = count;
        this.targetChest = targetChest;
        this.terminalPos = terminalPos;
        this.type = type;
        this.state = DeliveryState.QUEUED;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
        this.assignedBeeId = null;
    }

    /**
     * Constructeur complet (pour load NBT).
     */
    private DeliveryTask(UUID taskId, ItemStack template, int count, BlockPos targetChest,
                         BlockPos terminalPos, DeliveryType type, DeliveryState state,
                         int priority, List<UUID> dependencies, UUID assignedBeeId) {
        this.taskId = taskId;
        this.template = template;
        this.count = count;
        this.targetChest = targetChest;
        this.terminalPos = terminalPos;
        this.type = type;
        this.state = state;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
        this.assignedBeeId = assignedBeeId;
    }

    // === Getters ===

    public UUID getTaskId() { return taskId; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public BlockPos getTargetChest() { return targetChest; }
    public BlockPos getTerminalPos() { return terminalPos; }
    public DeliveryType getType() { return type; }
    public DeliveryState getState() { return state; }
    public int getPriority() { return priority; }
    public List<UUID> getDependencies() { return Collections.unmodifiableList(dependencies); }
    public UUID getAssignedBeeId() { return assignedBeeId; }

    // === Setters ===

    public void setState(DeliveryState state) { this.state = state; }
    public void setCount(int count) { this.count = count; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setAssignedBeeId(UUID beeId) { this.assignedBeeId = beeId; }

    // === Logic ===

    public boolean isActive() {
        return state == DeliveryState.FLYING || state == DeliveryState.WAITING
            || state == DeliveryState.RETURNING;
    }

    /**
     * Vérifie si toutes les dépendances de cette tâche sont complétées.
     *
     * @param completedTaskIds ensemble des IDs de tâches complétées
     * @return true si prête à être exécutée
     */
    public boolean isReady(Set<UUID> completedTaskIds) {
        if (dependencies.isEmpty()) return true;
        return completedTaskIds.containsAll(dependencies);
    }

    /**
     * Divise cette tâche: réduit le count à beeCapacity et retourne
     * une nouvelle tâche avec le reste des items.
     *
     * @param beeCapacity quantité max que l'abeille peut transporter
     * @return nouvelle tâche avec les items restants, ou null si pas de split nécessaire
     */
    public DeliveryTask splitRemaining(int beeCapacity) {
        if (count <= beeCapacity) return null;

        int remaining = count - beeCapacity;
        this.count = beeCapacity;

        DeliveryTask splitTask = new DeliveryTask(
            template.copy(), remaining, targetChest, terminalPos, type,
            priority, dependencies
        );
        return splitTask;
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putLong("TargetChest", targetChest.asLong());
        tag.putLong("TerminalPos", terminalPos.asLong());
        tag.putString("Type", type.name());
        tag.putString("State", state.name());
        tag.putInt("Priority", priority);

        if (!dependencies.isEmpty()) {
            ListTag depTag = new ListTag();
            for (UUID dep : dependencies) {
                CompoundTag depEntry = new CompoundTag();
                depEntry.putUUID("Id", dep);
                depTag.add(depEntry);
            }
            tag.put("Dependencies", depTag);
        }

        if (assignedBeeId != null) {
            tag.putUUID("AssignedBeeId", assignedBeeId);
        }

        return tag;
    }

    public static DeliveryTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("TaskId");
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");
        BlockPos chest = BlockPos.of(tag.getLong("TargetChest"));
        BlockPos terminal = BlockPos.of(tag.getLong("TerminalPos"));
        DeliveryType type = DeliveryType.valueOf(tag.getString("Type"));
        DeliveryState state = DeliveryState.valueOf(tag.getString("State"));
        int priority = tag.getInt("Priority");

        List<UUID> dependencies = new ArrayList<>();
        if (tag.contains("Dependencies")) {
            ListTag depTag = tag.getList("Dependencies", Tag.TAG_COMPOUND);
            for (int i = 0; i < depTag.size(); i++) {
                dependencies.add(depTag.getCompound(i).getUUID("Id"));
            }
        }

        UUID assignedBeeId = tag.contains("AssignedBeeId") ? tag.getUUID("AssignedBeeId") : null;

        return new DeliveryTask(id, template, count, chest, terminal, type, state,
            priority, dependencies, assignedBeeId);
    }
}
