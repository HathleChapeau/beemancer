/**
 * ============================================================
 * [NetworkInterfaceBlockEntity.java]
 * Description: Parent abstrait des Import/Export Interface BlockEntities
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageControllerBlockEntity  | Controller lie       | Acces reseau de stockage       |
 * | INetworkNode                  | Interface reseau     | Recherche controller           |
 * | InterfaceFilter               | Filtre individuel    | Systeme de filtres par ligne   |
 * | InterfaceTask                 | Tache unitaire       | Gestion tasks NEEDED/LOCKED/DEL  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ImportInterfaceBlockEntity.java (extends)
 * - ExportInterfaceBlockEntity.java (extends)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.InterfaceTask;
import com.chapeau.beemancer.common.item.debug.DebugWandItem;
import com.chapeau.beemancer.common.menu.storage.NetworkInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parent abstrait pour les Import et Export Interface.
 *
 * Fonctionnalites communes:
 * - Liaison a un controller (via edit mode shift+clic)
 * - Jusqu'a 3 filtres individuels (InterfaceFilter)
 * - globalSelectedSlots utilise quand 0 filtres
 * - Scan periodique de l'inventaire adjacent
 */
public abstract class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    protected static final int SCAN_INTERVAL = 40;

    @Nullable
    protected BlockPos controllerPos = null;

    protected final List<InterfaceFilter> filters = new ArrayList<>();
    protected final List<InterfaceTask> tasks = new ArrayList<>();
    protected final Set<Integer> globalSelectedSlots = new HashSet<>();
    protected boolean active = false;
    protected boolean hasAdjacentGui = false;
    protected int scanTimer = 0;
    private int guiCheckTimer = 0;
    private boolean isSaving = false;

    public NetworkInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        DebugWandItem.addDisplay(this, this::getTasksDebugText, new Vec3(0, 1, 0), 0xFFFFAA00);
    }

    /**
     * Texte debug affiche au-dessus de l'interface: liste des InterfaceTasks.
     */
    private String getTasksDebugText() {
        if (tasks.isEmpty()) return "Tasks: 0";

        int needed = 0, locked = 0, delivered = 0;
        for (InterfaceTask task : tasks) {
            switch (task.getState()) {
                case NEEDED -> needed++;
                case LOCKED -> locked++;
                case DELIVERED -> delivered++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tasks: ").append(tasks.size());
        sb.append(" (N:").append(needed).append(" L:").append(locked).append(" D:").append(delivered).append(")\n");

        for (InterfaceTask task : tasks) {
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

    // === Controller Link ===

    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        syncToClient();
    }

    public void unlinkController() {
        this.controllerPos = null;
        setChanged();
        syncToClient();
    }

    public boolean isLinked() {
        return controllerPos != null && getController() != null;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Nullable
    public StorageControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (!level.isLoaded(controllerPos)) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            return controller;
        }
        controllerPos = null;
        setChanged();
        return null;
    }

    // === Filter Management ===

    public List<InterfaceFilter> getFilters() {
        return filters;
    }

    public int getFilterCount() {
        return filters.size();
    }

    public InterfaceFilter getFilter(int index) {
        if (index < 0 || index >= filters.size()) return null;
        return filters.get(index);
    }

    public void addFilter() {
        if (filters.size() >= InterfaceFilter.MAX_FILTERS) return;
        filters.add(new InterfaceFilter());
        setChanged();
        syncToClient();
    }

    public void removeFilter(int index) {
        if (index < 0 || index >= filters.size()) return;
        filters.remove(index);
        setChanged();
        syncToClient();
    }

    public void setFilterItem(int filterIdx, int slot, ItemStack stack) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setItem(slot, stack);
        setChanged();
        syncToClient();
    }

    public void clearFilterItem(int filterIdx, int slot) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.clearItem(slot);
        setChanged();
        syncToClient();
    }

    public void setFilterMode(int filterIdx, InterfaceFilter.FilterMode mode) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setMode(mode);
        setChanged();
        syncToClient();
    }

    public void setFilterText(int filterIdx, String text) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setTextFilter(text);
        setChanged();
        syncToClient();
    }

    public void setFilterQuantity(int filterIdx, int quantity) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setQuantity(quantity);
        setChanged();
        syncToClient();
    }

    public void setFilterSelectedSlots(int filterIdx, Set<Integer> slots) {
        InterfaceFilter filter = getFilter(filterIdx);
        if (filter == null) return;
        filter.setSelectedSlots(slots);
        setChanged();
        syncToClient();
    }

    // === Global Selected Slots (used when 0 filters) ===

    public Set<Integer> getGlobalSelectedSlots() {
        return globalSelectedSlots;
    }

    public void setGlobalSelectedSlots(Set<Integer> slots) {
        globalSelectedSlots.clear();
        globalSelectedSlots.addAll(slots);
        setChanged();
        syncToClient();
    }

    public boolean hasAdjacentGui() {
        return hasAdjacentGui;
    }

    public boolean isImport() {
        return this instanceof ImportInterfaceBlockEntity;
    }

    // === Active Toggle ===

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        boolean wasActive = this.active;
        this.active = active;
        setChanged();
        syncToClient();

        // Si desactive, annuler toutes les tasks en cours (rappelle les abeilles en vol)
        if (wasActive && !active) {
            cancelAllTasks();
        }
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        if (isImport()) {
            return Component.translatable("container.beemancer.import_interface");
        }
        return Component.translatable("container.beemancer.export_interface");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new NetworkInterfaceMenu(containerId, playerInv, this);
    }

    // === Filter Matching ===

    /**
     * Verifie si un item correspond a au moins un filtre actif.
     * Si aucun filtre, retourne checkEmpty.
     */
    public boolean matchesAnyFilter(ItemStack stack, boolean checkEmpty) {
        if (filters.isEmpty()) return checkEmpty;
        for (InterfaceFilter filter : filters) {
            if (filter.matches(stack, false)) return true;
        }
        return false;
    }

    // === InterfaceTask Management ===

    /**
     * Cherche une InterfaceTask par son UUID.
     */
    @Nullable
    public InterfaceTask getTask(java.util.UUID taskId) {
        for (InterfaceTask task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    /**
     * Retourne la liste des InterfaceTasks (lecture seule pour GUI/debug).
     */
    public List<InterfaceTask> getTasks() {
        return java.util.Collections.unmodifiableList(tasks);
    }

    /**
     * Somme des lockedCount de toutes les tasks LOCKED pour un item donne.
     * Represente le nombre d'items "en cours de livraison" par les bees.
     */
    public int getLockedCount(ItemStack template) {
        int total = 0;
        for (InterfaceTask task : tasks) {
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.matchesItem(template)) {
                total += task.getLockedCount();
            }
        }
        return total;
    }

    /**
     * Somme des count de toutes les tasks DELIVERED pour un item donne.
     * Represente les items recemment livres pas encore nettoyes.
     */
    public int getDeliveredCount(ItemStack template) {
        int total = 0;
        for (InterfaceTask task : tasks) {
            if (task.getState() == InterfaceTask.TaskState.DELIVERED
                    && task.matchesItem(template)) {
                total += task.getCount();
            }
        }
        return total;
    }

    /**
     * Marque une task comme livree par la bee.
     */
    public void markTaskDelivered(java.util.UUID taskId) {
        InterfaceTask task = getTask(taskId);
        if (task != null && task.getState() == InterfaceTask.TaskState.LOCKED) {
            task.markDelivered();
            setChanged();
        }
    }

    /**
     * Deverrouille une task (bee echouee/timeout), remet en NEEDED.
     */
    public void unlockTask(java.util.UUID taskId) {
        InterfaceTask task = getTask(taskId);
        if (task != null && task.getState() == InterfaceTask.TaskState.LOCKED) {
            task.unlockTask();
            setChanged();
        }
    }

    /**
     * Retire les tasks DELIVERED. Appele au debut de doScan() pour reconcilier.
     */
    protected void cleanupDeliveredTasks() {
        boolean changed = tasks.removeIf(
            task -> task.getState() == InterfaceTask.TaskState.DELIVERED);
        if (changed) setChanged();
    }

    /**
     * Reconcilie les tasks existantes pour un item donne avec la demande actuelle.
     *
     * Met a jour les counts des tasks LOCKED (la bee lira le count actuel via
     * readCurrentInterfaceTaskCount), ajuste/supprime les tasks NEEDED, et cree
     * de nouvelles tasks si la demande augmente.
     *
     * @param template l'item template
     * @param totalDesired le nombre total d'items desires (apres deduction du stock actuel)
     * @param beeCapacity la capacite max par bee
     * @param type IMPORT ou EXPORT
     */
    protected void reconcileTasksForItem(ItemStack template, int totalDesired,
                                          int beeCapacity, InterfaceTask.TaskType type) {
        int remaining = Math.max(0, totalDesired);

        // Phase 1: Ajuster les tasks LOCKED (bees en vol)
        // Repartir la demande sur les tasks LOCKED existantes
        for (InterfaceTask task : tasks) {
            if (task.getState() != InterfaceTask.TaskState.LOCKED || !task.matchesItem(template)) continue;

            if (remaining <= 0) {
                // Plus besoin de cette task: mettre le count a 0, la bee adaptera
                task.setCount(0);
            } else {
                int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
                int taskShare = Math.min(remaining, itemBeeCapacity);
                task.setCount(taskShare);
                remaining -= taskShare;
            }
        }

        // Phase 2: Annuler les tasks LOCKED dont le count est 0 (rappeler les bees)
        StorageControllerBlockEntity controller = getController();
        java.util.Iterator<InterfaceTask> it = tasks.iterator();
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
        // Reduire ou supprimer les NEEDED excedentaires, ou en creer de nouvelles
        it = tasks.iterator();
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

        // Phase 4: Creer de nouvelles tasks NEEDED pour le reste
        int itemBeeCapacity = Math.min(beeCapacity, template.getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(remaining, itemBeeCapacity);
            tasks.add(new InterfaceTask(type, template, chunk));
            remaining -= chunk;
        }

        setChanged();
    }

    /**
     * Supprime les tasks (NEEDED et LOCKED) pour des items qui ne sont plus demandes.
     * Rappelle les bees LOCKED pour ces items.
     *
     * @param activeItemKeys set des itemKeys encore actifs apres le scan
     */
    protected void cleanupOrphanedTasks(Set<String> activeItemKeys) {
        StorageControllerBlockEntity controller = getController();
        boolean changed = false;

        java.util.Iterator<InterfaceTask> it = tasks.iterator();
        while (it.hasNext()) {
            InterfaceTask task = it.next();
            if (task.getState() == InterfaceTask.TaskState.DELIVERED) continue;

            String key = itemKey(task.getTemplate());
            if (activeItemKeys.contains(key)) continue;

            // Item plus demande: annuler
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.getAssignedBeeTaskId() != null && controller != null) {
                controller.getDeliveryManager().cancelTask(task.getAssignedBeeTaskId());
            }
            it.remove();
            changed = true;
        }

        if (changed) setChanged();
    }

    /**
     * Annule toutes les tasks: rappelle les bees LOCKED, supprime les NEEDED.
     * Appele quand l'interface est desactivee ou detruite.
     */
    protected void cancelAllTasks() {
        StorageControllerBlockEntity controller = getController();

        for (InterfaceTask task : tasks) {
            if (task.getState() == InterfaceTask.TaskState.LOCKED
                    && task.getAssignedBeeTaskId() != null && controller != null) {
                controller.getDeliveryManager().cancelTask(task.getAssignedBeeTaskId());
            }
        }
        tasks.clear();
        setChanged();
    }

    /**
     * Publie les tasks NEEDED aupres du controller pour assignation de bees.
     * Le controller trouve le coffre source/dest et cree un DeliveryTask.
     */
    protected void publishTodoTasks(StorageControllerBlockEntity controller) {
        for (InterfaceTask task : tasks) {
            if (task.getState() != InterfaceTask.TaskState.NEEDED) continue;
            controller.assignBeeToInterfaceTask(this, task);
        }
        setChanged();
    }

    // === Adjacent Inventory ===

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public BlockPos getAdjacentPos() {
        return worldPosition.relative(getFacing());
    }

    @Nullable
    public Container getAdjacentInventory() {
        if (level == null) return null;
        BlockPos adjPos = getAdjacentPos();
        if (!level.isLoaded(adjPos)) return null;
        BlockEntity be = level.getBlockEntity(adjPos);
        if (be instanceof Container container) {
            return container;
        }
        return null;
    }

    /**
     * Retourne les slots operables pour un filtre donne.
     * Si le filtre a des selectedSlots, les utilise; sinon tous.
     */
    protected int[] getOperableSlots(Container container, Set<Integer> selectedSlots) {
        if (selectedSlots.isEmpty()) {
            int[] all = new int[container.getContainerSize()];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        return selectedSlots.stream()
            .filter(s -> s >= 0 && s < container.getContainerSize())
            .mapToInt(Integer::intValue)
            .toArray();
    }

    /**
     * Retourne les slots operables globaux (quand 0 filtres).
     */
    protected int[] getGlobalOperableSlots(Container container) {
        return getOperableSlots(container, globalSelectedSlots);
    }

    // === Utilities ===

    protected String itemKey(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().toString()
            + ":" + stack.getComponentsPatch().hashCode();
    }

    // === Server Tick ===

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        scanTimer++;
        guiCheckTimer++;

        if (guiCheckTimer >= 100) {
            guiCheckTimer = 0;
            boolean hadGui = hasAdjacentGui;
            BlockPos adjPos = getAdjacentPos();
            if (!level.isLoaded(adjPos)) return;
            BlockEntity adjacentBe = level.getBlockEntity(adjPos);
            hasAdjacentGui = adjacentBe instanceof net.minecraft.world.MenuProvider;
            if (hadGui != hasAdjacentGui) {
                syncToClient();
            }
        }

        if (scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;

            if (!active) return;

            StorageControllerBlockEntity controller = getController();
            if (controller == null) return;

            Container adjacent = getAdjacentInventory();
            if (adjacent == null) return;

            doScan(adjacent, controller);
        }
    }

    protected abstract void doScan(Container adjacent, StorageControllerBlockEntity controller);

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);

            if (controllerPos != null) {
                tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
            }

            // Filters
            ListTag filtersTag = new ListTag();
            for (InterfaceFilter filter : filters) {
                filtersTag.add(filter.save(registries));
            }
            tag.put("Filters", filtersTag);

            // Global selected slots
            if (!globalSelectedSlots.isEmpty()) {
                tag.putIntArray("GlobalSelectedSlots",
                    globalSelectedSlots.stream().mapToInt(Integer::intValue).toArray());
            }

            tag.putBoolean("Active", active);
            tag.putBoolean("HasAdjacentGui", hasAdjacentGui);

            // InterfaceTasks
            ListTag tasksTag = new ListTag();
            for (InterfaceTask task : tasks) {
                tasksTag.add(task.save(registries));
            }
            tag.put("InterfaceTasks", tasksTag);

            saveExtra(tag, registries);
        } finally {
            isSaving = false;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("ControllerPos")) {
            NbtUtils.readBlockPos(tag, "ControllerPos").ifPresent(pos -> controllerPos = pos);
        } else {
            controllerPos = null;
        }

        // Filters
        filters.clear();
        if (tag.contains("Filters")) {
            ListTag filtersTag = tag.getList("Filters", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(filtersTag.size(), InterfaceFilter.MAX_FILTERS); i++) {
                InterfaceFilter filter = new InterfaceFilter();
                filter.load(filtersTag.getCompound(i), registries);
                filters.add(filter);
            }
        }

        // Global selected slots
        globalSelectedSlots.clear();
        if (tag.contains("GlobalSelectedSlots")) {
            for (int s : tag.getIntArray("GlobalSelectedSlots")) {
                globalSelectedSlots.add(s);
            }
        }

        active = tag.getBoolean("Active");
        hasAdjacentGui = tag.getBoolean("HasAdjacentGui");

        // InterfaceTasks
        tasks.clear();
        if (tag.contains("InterfaceTasks")) {
            ListTag tasksTag = tag.getList("InterfaceTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tasksTag.size(); i++) {
                tasks.add(InterfaceTask.load(tasksTag.getCompound(i), registries));
            }
        }

        loadExtra(tag, registries);
    }

    protected abstract void saveExtra(CompoundTag tag, HolderLookup.Provider registries);
    protected abstract void loadExtra(CompoundTag tag, HolderLookup.Provider registries);

    // === Sync Client ===

    public void syncToClient() {
        if (isSaving) return;
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Destruction ===

    @Override
    public void setRemoved() {
        // Annuler toutes les tasks et rappeler les abeilles en vol avant suppression
        if (level != null && !level.isClientSide()) {
            cancelAllTasks();
        }
        super.setRemoved();
    }
}
