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

import javax.annotation.Nullable;
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
    @Nullable private final UUID parentTaskId;
    @Nullable private final UUID interfaceTaskId;
    @Nullable private final BlockPos interfacePos;
    private final ItemStack template;
    private int count;
    private final BlockPos sourcePos;
    private final BlockPos destPos;
    @Nullable private final BlockPos requesterPos;
    private final TaskOrigin origin;
    private final boolean preloaded;
    private DeliveryState state;
    private int priority;
    private final List<UUID> dependencies;

    /**
     * Constructeur standard (priorite 0, pas de dependances, pas de preload).
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin,
                        @Nullable BlockPos requesterPos) {
        this(template, count, sourcePos, destPos, 0, Collections.emptyList(),
            origin, false, requesterPos, null, null, null);
    }

    /**
     * Constructeur avec preloaded.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin, boolean preloaded,
                        @Nullable BlockPos requesterPos) {
        this(template, count, sourcePos, destPos, 0, Collections.emptyList(),
            origin, preloaded, requesterPos, null, null, null);
    }

    /**
     * Constructeur complet (sans interface).
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, int priority, List<UUID> dependencies,
                        TaskOrigin origin, boolean preloaded,
                        @Nullable BlockPos requesterPos,
                        @Nullable UUID parentTaskId) {
        this(template, count, sourcePos, destPos, priority, dependencies,
            origin, preloaded, requesterPos, parentTaskId, null, null);
    }

    /**
     * Constructeur complet avec interface task.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin, boolean preloaded,
                        @Nullable BlockPos requesterPos,
                        @Nullable UUID parentTaskId,
                        @Nullable UUID interfaceTaskId,
                        @Nullable BlockPos interfacePos) {
        this(template, count, sourcePos, destPos, 0, Collections.emptyList(),
            origin, preloaded, requesterPos, parentTaskId, interfaceTaskId, interfacePos);
    }

    /**
     * Constructeur maitre.
     */
    public DeliveryTask(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, int priority, List<UUID> dependencies,
                        TaskOrigin origin, boolean preloaded,
                        @Nullable BlockPos requesterPos,
                        @Nullable UUID parentTaskId,
                        @Nullable UUID interfaceTaskId,
                        @Nullable BlockPos interfacePos) {
        this.taskId = UUID.randomUUID();
        this.parentTaskId = parentTaskId;
        this.interfaceTaskId = interfaceTaskId;
        this.interfacePos = interfacePos;
        this.template = template.copy();
        this.count = count;
        this.sourcePos = sourcePos;
        this.destPos = destPos;
        this.requesterPos = requesterPos;
        this.origin = origin;
        this.preloaded = preloaded;
        this.state = DeliveryState.QUEUED;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
    }

    /**
     * Constructeur interne (pour load NBT).
     */
    private DeliveryTask(UUID taskId, @Nullable UUID parentTaskId, ItemStack template,
                         int count, BlockPos sourcePos, BlockPos destPos,
                         @Nullable BlockPos requesterPos, DeliveryState state,
                         int priority, List<UUID> dependencies,
                         TaskOrigin origin, boolean preloaded,
                         @Nullable UUID interfaceTaskId,
                         @Nullable BlockPos interfacePos) {
        this.taskId = taskId;
        this.parentTaskId = parentTaskId;
        this.interfaceTaskId = interfaceTaskId;
        this.interfacePos = interfacePos;
        this.template = template;
        this.count = count;
        this.sourcePos = sourcePos;
        this.destPos = destPos;
        this.requesterPos = requesterPos;
        this.origin = origin;
        this.preloaded = preloaded;
        this.state = state;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
    }

    // === Getters ===

    public UUID getTaskId() { return taskId; }
    @Nullable public UUID getParentTaskId() { return parentTaskId; }
    @Nullable public UUID getInterfaceTaskId() { return interfaceTaskId; }
    @Nullable public BlockPos getInterfacePos() { return interfacePos; }
    /**
     * Retourne l'ID racine du groupe de taches: parentTaskId si c'est une subtask, sinon taskId.
     * Utilise pour retrouver la request associee meme pour les subtasks split.
     */
    public UUID getRootTaskId() { return parentTaskId != null ? parentTaskId : taskId; }
    public ItemStack getTemplate() { return template.copy(); }
    public int getCount() { return count; }
    public BlockPos getSourcePos() { return sourcePos; }
    public BlockPos getDestPos() { return destPos; }
    public TaskOrigin getOrigin() { return origin; }
    public boolean isPreloaded() { return preloaded; }
    @Nullable public BlockPos getRequesterPos() { return requesterPos; }
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
     * La subtask herite du rootTaskId pour garder le lien avec la request parente.
     */
    public DeliveryTask splitRemaining(int beeCapacity) {
        if (interfaceTaskId != null) return null;
        if (count <= beeCapacity) return null;

        int remaining = count - beeCapacity;
        this.count = beeCapacity;

        UUID rootId = getRootTaskId();
        DeliveryTask splitTask = new DeliveryTask(
            template.copy(), remaining, sourcePos, destPos,
            priority, dependencies, origin, preloaded, requesterPos, rootId
        );
        return splitTask;
    }

    // === NBT ===

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("TaskId", taskId);
        if (parentTaskId != null) {
            tag.putUUID("ParentTaskId", parentTaskId);
        }
        if (interfaceTaskId != null) {
            tag.putUUID("InterfaceTaskId", interfaceTaskId);
        }
        if (interfacePos != null) {
            tag.putLong("InterfacePos", interfacePos.asLong());
        }
        tag.put("Template", template.saveOptional(registries));
        tag.putInt("Count", count);
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putLong("DestPos", destPos.asLong());
        tag.putString("Origin", origin.name());
        tag.putBoolean("Preloaded", preloaded);
        if (requesterPos != null) {
            tag.putLong("RequesterPos", requesterPos.asLong());
        }
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
        UUID parentId = tag.contains("ParentTaskId") ? tag.getUUID("ParentTaskId") : null;
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
        BlockPos requesterPos = tag.contains("RequesterPos")
            ? BlockPos.of(tag.getLong("RequesterPos")) : null;

        UUID interfaceTaskId = tag.contains("InterfaceTaskId")
            ? tag.getUUID("InterfaceTaskId") : null;
        BlockPos interfacePos = tag.contains("InterfacePos")
            ? BlockPos.of(tag.getLong("InterfacePos")) : null;

        List<UUID> dependencies = new ArrayList<>();
        if (tag.contains("Dependencies")) {
            ListTag depTag = tag.getList("Dependencies", Tag.TAG_COMPOUND);
            for (int i = 0; i < depTag.size(); i++) {
                dependencies.add(depTag.getCompound(i).getUUID("Id"));
            }
        }

        return new DeliveryTask(id, parentId, template, count, source, dest, requesterPos,
            state, priority, dependencies, origin, preloaded, interfaceTaskId, interfacePos);
    }
}
