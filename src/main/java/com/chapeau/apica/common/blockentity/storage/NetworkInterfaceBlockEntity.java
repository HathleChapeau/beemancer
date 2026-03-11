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
 * | InterfaceTaskManager          | Gestion tasks        | Delegation tasks               |
 * | InterfaceFilterManager        | Gestion filtres      | Delegation filtres             |
 * | StorageControllerBlockEntity  | Controller lie       | Acces reseau de stockage       |
 * | InterfaceFilter               | Filtre individuel    | Facade vers FilterManager      |
 * | InterfaceTask                 | Tache unitaire       | Facade vers TaskManager        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ImportInterfaceBlockEntity.java (extends)
 * - ExportInterfaceBlockEntity.java (extends)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.InterfaceTask;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.menu.storage.NetworkInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Parent abstrait pour les Import et Export Interface.
 *
 * Orchestre:
 * - InterfaceFilterManager (filtres et globalSelectedSlots)
 * - InterfaceTaskManager (tasks NEEDED/LOCKED/DELIVERED)
 * - Liaison a un controller (via edit mode shift+clic)
 * - Scan periodique de l'inventaire adjacent
 */
public abstract class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    protected static final int SCAN_INTERVAL = 40;

    @Nullable
    protected BlockPos controllerPos = null;

    protected final InterfaceFilterManager filterManager = new InterfaceFilterManager(this);
    protected final InterfaceTaskManager taskManager = new InterfaceTaskManager(this);
    protected boolean active = false;
    protected boolean hasAdjacentGui = false;
    protected boolean needsScan = false;
    private int guiCheckTimer = 0;
    private boolean isSaving = false;

    public NetworkInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        DebugWandItem.addDisplay(this, taskManager::getDebugText, new Vec3(0, 1, 0), 0xFFFFAA00);
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

    public void markNeedsScan() {
        this.needsScan = true;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    @Nullable
    public StorageControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (!level.hasChunkAt(controllerPos)) return null;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            if (!controller.isFormed()) return null;
            return controller;
        }
        controllerPos = null;
        // [FIX] Ne pas re-dirtier le chunk pendant le shutdown (saveAllChunks boucle infinie)
        if (!com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) {
            setChanged();
        }
        return null;
    }

    // === Filter Facade ===

    public List<InterfaceFilter> getFilters() { return filterManager.getFilters(); }
    public int getFilterCount() { return filterManager.getFilterCount(); }
    public InterfaceFilter getFilter(int index) { return filterManager.getFilter(index); }

    public void addFilter() { filterManager.addFilter(); }
    public void removeFilter(int index) { filterManager.removeFilter(index); }

    public void setFilterItem(int filterIdx, int slot, ItemStack stack) {
        filterManager.setFilterItem(filterIdx, slot, stack);
    }
    public void clearFilterItem(int filterIdx, int slot) {
        filterManager.clearFilterItem(filterIdx, slot);
    }
    public void setFilterMode(int filterIdx, InterfaceFilter.FilterMode mode) {
        filterManager.setFilterMode(filterIdx, mode);
    }
    public void setFilterText(int filterIdx, String text) {
        filterManager.setFilterText(filterIdx, text);
    }
    public void setFilterQuantity(int filterIdx, int quantity) {
        filterManager.setFilterQuantity(filterIdx, quantity);
    }
    public void setFilterSelectedSlots(int filterIdx, Set<Integer> slots) {
        filterManager.setFilterSelectedSlots(filterIdx, slots);
    }
    public void setFilterInverted(int filterIdx, boolean inverted) {
        filterManager.setFilterInverted(filterIdx, inverted);
    }

    public Set<Integer> getGlobalSelectedSlots() { return filterManager.getGlobalSelectedSlots(); }
    public void setGlobalSelectedSlots(Set<Integer> slots) {
        filterManager.setGlobalSelectedSlots(slots);
    }

    public boolean matchesAnyFilter(ItemStack stack, boolean checkEmpty) {
        return filterManager.matchesAnyFilter(stack, checkEmpty);
    }

    // === Task Facade ===

    @Nullable
    public InterfaceTask getTask(UUID taskId) { return taskManager.getTask(taskId); }
    public Collection<InterfaceTask> getTasks() { return taskManager.getTasks(); }
    public int getLockedCount(ItemStack template) { return taskManager.getLockedCount(template); }
    public int getDeliveredCount(ItemStack template) { return taskManager.getDeliveredCount(template); }
    public void markTaskDelivered(UUID taskId) { taskManager.markTaskDelivered(taskId); }
    public void unlockTask(UUID taskId) { taskManager.unlockTask(taskId); }

    // === Active Toggle ===

    public boolean hasAdjacentGui() {
        return hasAdjacentGui;
    }

    public boolean isImport() {
        return this instanceof ImportInterfaceBlockEntity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        boolean wasActive = this.active;
        this.active = active;
        needsScan = true;
        setChanged();
        syncToClient();

        if (wasActive && !active) {
            taskManager.cancelAllTasks();
        }
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        if (isImport()) {
            return Component.translatable("container.apica.import_interface");
        }
        return Component.translatable("container.apica.export_interface");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new NetworkInterfaceMenu(containerId, playerInv, this);
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
    public IItemHandler getAdjacentItemHandler() {
        if (level == null) return null;
        BlockPos adjPos = getAdjacentPos();
        if (!level.hasChunkAt(adjPos)) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, adjPos, getFacing().getOpposite());
    }

    /**
     * Retourne les slots operables pour un filtre donne.
     * Si le filtre a des selectedSlots, les utilise; sinon tous.
     *
     * [FIX] Si la selection filtree est vide (slots selectionnes hors limites du handler),
     * retourne tous les slots du handler. Cela arrive quand l'utilisateur selectionne
     * des slots dans le GUI d'un bloc (ex: four, slot 1=fuel) mais que le handler
     * accessible via capability n'expose qu'un sous-ensemble des slots (ex: 1 slot seulement
     * selon la face d'acces). Dans ce cas, on insere dans tous les slots disponibles.
     */
    protected int[] getOperableSlots(IItemHandler handler, Set<Integer> selectedSlots) {
        if (selectedSlots.isEmpty()) {
            int[] all = new int[handler.getSlots()];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        int[] filtered = selectedSlots.stream()
            .filter(s -> s >= 0 && s < handler.getSlots())
            .mapToInt(Integer::intValue)
            .toArray();

        // [FIX] Si tous les slots selectionnes sont hors limites, utiliser tous les slots
        // du handler comme fallback. Cela evite que l'insertion echoue completement.
        if (filtered.length == 0) {
            int[] all = new int[handler.getSlots()];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }

        return filtered;
    }

    /**
     * Retourne les slots operables globaux (quand 0 filtres).
     */
    protected int[] getGlobalOperableSlots(IItemHandler handler) {
        return getOperableSlots(handler, filterManager.getGlobalSelectedSlots());
    }

    // === Utilities ===

    protected String itemKey(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().toString()
            + ":" + stack.getComponentsPatch().hashCode();
    }

    // === Server Tick ===

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        // [FIX] Pendant le shutdown, ne plus modifier le monde (setChanged dans getController, etc.)
        if (com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) return;

        long gameTick = level.getGameTime();

        guiCheckTimer++;
        if (guiCheckTimer >= 100) {
            guiCheckTimer = 0;
            boolean hadGui = hasAdjacentGui;
            BlockPos adjPos = getAdjacentPos();
            if (!level.hasChunkAt(adjPos)) return;
            BlockEntity adjacentBe = level.getBlockEntity(adjPos);
            hasAdjacentGui = adjacentBe instanceof net.minecraft.world.MenuProvider;
            if (hadGui != hasAdjacentGui) {
                syncToClient();
            }
        }

        // Unlock zombie LOCKED tasks (bee disappeared)
        if (gameTick % 20 == 0) {
            if (taskManager.checkLockedTimeouts(gameTick)) {
                needsScan = true;
            }
        }

        boolean regularScan = ((gameTick + worldPosition.hashCode()) % SCAN_INTERVAL) == 0;
        if (regularScan || needsScan) {
            needsScan = false;

            if (!active) return;

            StorageControllerBlockEntity controller = getController();
            if (controller == null) return;

            IItemHandler adjacent = getAdjacentItemHandler();
            if (adjacent == null) return;

            doScan(adjacent, controller);
        }
    }

    protected abstract void doScan(IItemHandler adjacent, StorageControllerBlockEntity controller);

    // === NBT ===

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NetworkInterfaceBlockEntity.class);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) {
            LOGGER.info("[Apica] Saving Interface at {} — controller:{}, tasks:{}, active:{}",
                worldPosition, controllerPos, taskManager.size(), active);
        }
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);

            if (controllerPos != null) {
                tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
            }

            filterManager.save(tag, registries);

            tag.putBoolean("Active", active);
            tag.putBoolean("HasAdjacentGui", hasAdjacentGui);

            taskManager.save(tag, registries);

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

        filterManager.load(tag, registries);

        active = tag.getBoolean("Active");
        hasAdjacentGui = tag.getBoolean("HasAdjacentGui");

        taskManager.load(tag, registries);

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
        if (level != null && !level.isClientSide()) {
            // [FIX] Nettoyage silencieux: clear les tasks sans cancelTask() qui appelle
            // controller.setChanged() / beeSpawner.redirectBee() / recallBee etc.
            // Pendant le world unload, ces appels re-dirtient des chunks → boucle infinie saveAllChunks.
            // Les bees seront cleanup par killAllDeliveryBees() du controller.
            // Les tasks orphelines seront nettoyees au prochain chargement.
            taskManager.clearTasksSilent();
        }
        super.setRemoved();
    }
}
