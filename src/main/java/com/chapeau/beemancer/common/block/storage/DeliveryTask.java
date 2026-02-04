/**
 * ============================================================
 * [DeliveryTask.java]
 * Description: Tache de livraison avec origine, priorite, dependances et splitting
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | BlockPos       | Positions source/dest| Source et destination           |
 * | ItemStack      | Template item        | Item a transporter             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageDeliveryManager.java (queue de livraison, spawn bees)
 * - RequestManager.java (creation taches depuis demandes)
 * - DeliveryBeeEntity.java (execution de la tache)
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
 * Represente une tache de livraison dans le reseau de stockage.
 * Flux unifie: abeille vole a sourcePos (extraction), puis a destPos (depot).
 *
 * Supporte:
 * - Origine (REQUEST = joueur, AUTOMATION = interface)
 * - Priorite (lower = higher priority, default 0)
 * - Dependances (task IDs qui doivent etre completees avant execution)
 * - Splitting (division en sous-taches quand la quantite depasse la capacite d'une abeille)
 * - Preloaded (items pre-charges sur l'abeille, saute la phase source)
 */
public class DeliveryTask {

    public enum DeliveryState {
        QUEUED,
        FLYING,
        COMPLETED,
        FAILED
    }

    public enum TaskOrigin {
        REQUEST,
        AUTOMATION
    }

    private final UUID taskId;
    private final ItemStack template;
    private int count;
    private final BlockPos sourcePos;
    private final BlockPos destPos;
    private final TaskOrigin origin;
    private final boolean preloaded;
    private DeliveryState state;
    private int priority;
    private final List<UUID> dependencies;

    /**
     * Constructeur standard (priorite 0, pas de dependances, pas de preload).
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin) {
        this(template, count, sourcePos, destPos, 0, Collections.emptyList(), origin, false);
    }

    /**
     * Constructeur avec preloaded.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin, boolean preloaded) {
        this(template, count, sourcePos, destPos, 0, Collections.emptyList(), origin, preloaded);
    }

    /**
     * Constructeur complet.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, int priority, List<UUID> dependencies,
                        TaskOrigin origin, boolean preloaded) {
        this.taskId = UUID.randomUUID();
        this.template = template.copy();
        this.count = count;
        this.sourcePos = sourcePos;
        this.destPos = destPos;
        this.origin = origin;
        this.preloaded = preloaded;
        this.state = DeliveryState.QUEUED;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
    }

    /**
     * Constructeur interne (pour load NBT).
     */
    private DeliveryTask(UUID taskId, ItemStack template, int count, BlockPos sourcePos,
                         BlockPos destPos, DeliveryState state,
                         int priority, List<UUID> dependencies,
                         TaskOrigin origin, boolean preloaded) {
        this.taskId = taskId;
        this.template = template;
        this.count = count;
        this.sourcePos = sourcePos;
        this.destPos = destPos;
        this.origin = origin;
        this.preloaded = preloaded;
        this.state = state;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
    }

    // === Getters ===

    public UUID getTaskId() { return taskId; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public BlockPos getSourcePos() { return sourcePos; }
    public BlockPos getDestPos() { return destPos; }
    public TaskOrigin getOrigin() { return origin; }
    public boolean isPreloaded() { return preloaded; }
    public DeliveryState getState() { return state; }
    public int getPriority() { return priority; }
    public List<UUID> getDependencies() { return Collections.unmodifiableList(dependencies); }

    // === Setters ===

    public void setState(DeliveryState state) { this.state = state; }
    public void setCount(int count) { this.count = count; }
    public void setPriority(int priority) { this.priority = priority; }

    // === Logic ===

    public boolean isActive() {
        return state == DeliveryState.FLYING;
    }

    /**
     * Verifie si toutes les dependances de cette tache sont completees.
     */
    public boolean isReady(Set<UUID> completedTaskIds) {
        if (dependencies.isEmpty()) return true;
        return completedTaskIds.containsAll(dependencies);
    }

    /**
     * Divise cette tache: reduit le count a beeCapacity et retourne
     * une nouvelle tache avec le reste des items.
     */
    public DeliveryTask splitRemaining(int beeCapacity) {
        if (count <= beeCapacity) return null;

        int remaining = count - beeCapacity;
        this.count = beeCapacity;

        DeliveryTask splitTask = new DeliveryTask(
            template.copy(), remaining, sourcePos, destPos,
            priority, dependencies, origin, preloaded
        );
        return splitTask;
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putLong("DestPos", destPos.asLong());
        tag.putString("Origin", origin.name());
        tag.putBoolean("Preloaded", preloaded);
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

        return tag;
    }

    public static DeliveryTask load(CompoundTag tag, HolderLookup.Provider registries) {
        UUID id = tag.getUUID("TaskId");
        ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("Template"));
        int count = tag.getInt("Count");

        // Backward compat: accepter TargetPos ou SourcePos
        BlockPos source;
        if (tag.contains("SourcePos")) {
            source = BlockPos.of(tag.getLong("SourcePos"));
        } else if (tag.contains("TargetPos")) {
            source = BlockPos.of(tag.getLong("TargetPos"));
        } else {
            source = BlockPos.of(tag.getLong("TargetChest"));
        }

        // Backward compat: accepter DestPos ou TerminalPos
        BlockPos dest;
        if (tag.contains("DestPos")) {
            dest = BlockPos.of(tag.getLong("DestPos"));
        } else {
            dest = BlockPos.of(tag.getLong("TerminalPos"));
        }

        DeliveryState state = DeliveryState.valueOf(tag.getString("State"));
        int priority = tag.getInt("Priority");

        TaskOrigin origin = tag.contains("Origin")
            ? TaskOrigin.valueOf(tag.getString("Origin"))
            : TaskOrigin.REQUEST;

        boolean preloaded = tag.getBoolean("Preloaded");

        List<UUID> dependencies = new ArrayList<>();
        if (tag.contains("Dependencies")) {
            ListTag depTag = tag.getList("Dependencies", Tag.TAG_COMPOUND);
            for (int i = 0; i < depTag.size(); i++) {
                dependencies.add(depTag.getCompound(i).getUUID("Id"));
            }
        }

        return new DeliveryTask(id, template, count, source, dest, state,
            priority, dependencies, origin, preloaded);
    }
}
