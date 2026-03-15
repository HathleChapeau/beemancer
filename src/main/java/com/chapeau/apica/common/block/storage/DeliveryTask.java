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
package com.chapeau.apica.common.block.storage;

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

    // [AW] Timeout FLYING: tick auquel la tache est passee en FLYING
    private long flyingStartTick = -1;
    // [AV] Retry limit: nombre de retries + prochain tick de retry autorise
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;
    private long nextRetryTick = 0;

    /**
     * Constructeur maitre (prive). Utiliser builder() pour creer des instances.
     */
    private DeliveryTask(UUID taskId, ItemStack template, int count,
                         BlockPos sourcePos, BlockPos destPos,
                         TaskOrigin origin, boolean preloaded,
                         int priority, List<UUID> dependencies,
                         @Nullable BlockPos requesterPos,
                         @Nullable UUID parentTaskId,
                         @Nullable UUID interfaceTaskId,
                         @Nullable BlockPos interfacePos,
                         DeliveryState state) {
        this.taskId = taskId;
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
        this.state = state;
        this.priority = priority;
        this.dependencies = new ArrayList<>(dependencies);
    }

    /**
     * [BS] Cree un builder pour construire une DeliveryTask.
     * @param template Item a transporter
     * @param count    Quantite
     * @param sourcePos Position source (coffre extraction)
     * @param destPos   Position destination (coffre depot)
     * @param origin    Origine (REQUEST = joueur, AUTOMATION = interface)
     */
    public static Builder builder(ItemStack template, int count,
                                   BlockPos sourcePos, BlockPos destPos,
                                   TaskOrigin origin) {
        return new Builder(template, count, sourcePos, destPos, origin);
    }

    /**
     * [BS] Builder fluide pour construire des DeliveryTask.
     */
    public static class Builder {
        private final ItemStack template;
        private final int count;
        private final BlockPos sourcePos;
        private final BlockPos destPos;
        private final TaskOrigin origin;

        private int priority = 0;
        private List<UUID> dependencies = Collections.emptyList();
        private boolean preloaded = false;
        @Nullable private BlockPos requesterPos = null;
        @Nullable private UUID parentTaskId = null;
        @Nullable private UUID interfaceTaskId = null;
        @Nullable private BlockPos interfacePos = null;

        private Builder(ItemStack template, int count, BlockPos sourcePos,
                        BlockPos destPos, TaskOrigin origin) {
            this.template = template;
            this.count = count;
            this.sourcePos = sourcePos;
            this.destPos = destPos;
            this.origin = origin;
        }

        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder dependencies(List<UUID> deps) { this.dependencies = deps; return this; }
        public Builder preloaded(boolean preloaded) { this.preloaded = preloaded; return this; }
        public Builder requesterPos(@Nullable BlockPos pos) { this.requesterPos = pos; return this; }
        public Builder parentTaskId(@Nullable UUID id) { this.parentTaskId = id; return this; }
        public Builder interfaceTaskId(@Nullable UUID id) { this.interfaceTaskId = id; return this; }
        public Builder interfacePos(@Nullable BlockPos pos) { this.interfacePos = pos; return this; }

        public DeliveryTask build() {
            return new DeliveryTask(UUID.randomUUID(), template, count, sourcePos, destPos,
                origin, preloaded, priority, dependencies, requesterPos,
                parentTaskId, interfaceTaskId, interfacePos, DeliveryState.QUEUED);
        }
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

    // [AW] Flying timeout
    public long getFlyingStartTick() { return flyingStartTick; }
    public void setFlyingStartTick(long tick) { this.flyingStartTick = tick; }

    // [AV] Retry
    public int getRetryCount() { return retryCount; }
    public long getNextRetryTick() { return nextRetryTick; }

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
     * [AV] Verifie si la tache peut encore etre retentee (inférieur MAX_RETRIES).
     */
    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }

    /**
     * [AV] Incremente le compteur de retry et calcule le prochain tick autorise.
     * Backoff exponentiel: 20, 60, 180 ticks (1s, 3s, 9s).
     */
    public void incrementRetry(long currentTick) {
        retryCount++;
        nextRetryTick = currentTick + 20L * (long) Math.pow(3, retryCount - 1);
    }

    /**
     * [AV] Verifie si le prochain retry est autorise au tick actuel.
     */
    public boolean isRetryReady(long currentTick) {
        return currentTick >= nextRetryTick;
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

        return DeliveryTask.builder(template.copy(), remaining, sourcePos, destPos, origin)
            .priority(priority)
            .dependencies(dependencies)
            .preloaded(preloaded)
            .requesterPos(requesterPos)
            .parentTaskId(getRootTaskId())
            .build();
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

        // [AW] Timeout FLYING
        if (flyingStartTick >= 0) {
            tag.putLong("FlyingStartTick", flyingStartTick);
        }
        // [AV] Retry limit
        if (retryCount > 0) {
            tag.putInt("RetryCount", retryCount);
            tag.putLong("NextRetryTick", nextRetryTick);
        }

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
        BlockPos source = BlockPos.of(tag.getLong("SourcePos"));
        BlockPos dest = BlockPos.of(tag.getLong("DestPos"));
        DeliveryState state = DeliveryState.valueOf(tag.getString("State"));
        int priority = tag.getInt("Priority");
        TaskOrigin origin = TaskOrigin.valueOf(tag.getString("Origin"));
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

        DeliveryTask task = new DeliveryTask(id, template, count, source, dest,
            origin, preloaded, priority, dependencies, requesterPos,
            parentId, interfaceTaskId, interfacePos, state);

        // [AW] Timeout FLYING
        if (tag.contains("FlyingStartTick")) {
            task.flyingStartTick = tag.getLong("FlyingStartTick");
        }
        // [AV] Retry limit
        if (tag.contains("RetryCount")) {
            task.retryCount = tag.getInt("RetryCount");
            task.nextRetryTick = tag.getLong("NextRetryTick");
        }

        return task;
    }
}
