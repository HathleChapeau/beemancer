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
 * | DeliveryTask                  | Taches livraison     | Creation taches                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ImportInterfaceBlockEntity.java (extends)
 * - ExportInterfaceBlockEntity.java (extends)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Parent abstrait pour les Import et Export Interface.
 *
 * Fonctionnalites communes:
 * - Liaison a un controller (via edit mode shift+clic)
 * - 9 filter slots (ghost items, mode ITEM)
 * - 9 text filters (mode TEXT)
 * - Mode global ITEM ou TEXT
 * - Selected slots (overlay sur le GUI adjacent)
 * - Suivi des taches en cours (pendingTasks)
 * - Scan periodique de l'inventaire adjacent
 */
public abstract class NetworkInterfaceBlockEntity extends BlockEntity implements MenuProvider {

    public enum FilterMode {
        ITEM,
        TEXT
    }

    protected static final int FILTER_SLOTS = 9;
    protected static final int SCAN_INTERVAL = 40;

    @Nullable
    protected BlockPos controllerPos = null;

    protected final ItemStackHandler filterSlots = new ItemStackHandler(FILTER_SLOTS);
    protected final String[] textFilters = new String[FILTER_SLOTS];
    protected FilterMode filterMode = FilterMode.ITEM;
    protected final Set<Integer> selectedSlots = new HashSet<>();
    protected final Map<String, UUID> pendingTasks = new HashMap<>();
    protected boolean hasAdjacentGui = false;
    protected int scanTimer = 0;
    private int guiCheckTimer = 0;

    public NetworkInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            textFilters[i] = "";
        }
    }

    // === Controller Link ===

    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        syncToClient();
    }

    public void unlinkController() {
        this.controllerPos = null;
        pendingTasks.clear();
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
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            return controller;
        }
        controllerPos = null;
        setChanged();
        return null;
    }

    // === Filter Management ===

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(FilterMode mode) {
        this.filterMode = mode;
        setChanged();
        syncToClient();
    }

    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        filterSlots.setStackInSlot(slot, stack.copyWithCount(1));
        setChanged();
        syncToClient();
    }

    public void clearFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        filterSlots.setStackInSlot(slot, ItemStack.EMPTY);
        setChanged();
        syncToClient();
    }

    public ItemStack getFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return ItemStack.EMPTY;
        return filterSlots.getStackInSlot(slot);
    }

    public ItemStackHandler getFilterSlots() {
        return filterSlots;
    }

    public void setTextFilter(int slot, String text) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        textFilters[slot] = text != null ? text : "";
        setChanged();
        syncToClient();
    }

    public String getTextFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return "";
        return textFilters[slot];
    }

    public String[] getTextFilters() {
        return textFilters;
    }

    // === Slot Selection ===

    public Set<Integer> getSelectedSlots() {
        return selectedSlots;
    }

    public void setSelectedSlots(Set<Integer> slots) {
        selectedSlots.clear();
        selectedSlots.addAll(slots);
        setChanged();
        syncToClient();
    }

    public boolean hasAdjacentGui() {
        return hasAdjacentGui;
    }

    public boolean isImport() {
        return this instanceof ImportInterfaceBlockEntity;
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
     * Verifie si un item correspond aux filtres actifs.
     * En mode ITEM: compare avec les ghost slots non-vides.
     * En mode TEXT: parse #tag/@mod/texte.
     *
     * @param checkEmpty si true, retourne true quand il n'y a aucun filtre actif
     */
    public boolean matchesFilter(ItemStack stack, boolean checkEmpty) {
        if (filterMode == FilterMode.ITEM) {
            return matchesItemFilter(stack, checkEmpty);
        } else {
            return matchesTextFilter(stack, checkEmpty);
        }
    }

    private boolean matchesItemFilter(ItemStack stack, boolean checkEmpty) {
        boolean hasAnyFilter = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack filter = filterSlots.getStackInSlot(i);
            if (!filter.isEmpty()) {
                hasAnyFilter = true;
                if (ItemStack.isSameItemSameComponents(filter, stack)) {
                    return true;
                }
            }
        }
        return !hasAnyFilter && checkEmpty;
    }

    private boolean matchesTextFilter(ItemStack stack, boolean checkEmpty) {
        boolean hasAnyFilter = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            String text = textFilters[i];
            if (text == null || text.isEmpty()) continue;
            hasAnyFilter = true;

            if (text.startsWith("#")) {
                String tagName = text.substring(1);
                if (matchesTag(stack, tagName)) return true;
            } else if (text.startsWith("@")) {
                String namespace = text.substring(1);
                String itemNs = stack.getItem().builtInRegistryHolder()
                    .key().location().getNamespace();
                if (itemNs.equals(namespace)) return true;
            } else {
                String displayName = stack.getHoverName().getString().toLowerCase();
                if (displayName.contains(text.toLowerCase())) return true;
            }
        }
        return !hasAnyFilter && checkEmpty;
    }

    private boolean matchesTag(ItemStack stack, String tagName) {
        var tags = stack.getTags().toList();
        for (var tag : tags) {
            String fullPath = tag.location().toString();
            String path = tag.location().getPath();
            if (path.equals(tagName) || fullPath.equals(tagName)
                || path.contains(tagName)) {
                return true;
            }
        }
        return false;
    }

    // === Adjacent Inventory ===

    /**
     * Retourne la direction vers laquelle l'interface pointe (face du bloc).
     */
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Retourne la position du bloc adjacent (inventaire cible).
     * L'inventaire est derriere l'interface (opposite du facing, car le facing pointe vers le joueur).
     */
    public BlockPos getAdjacentPos() {
        return worldPosition.relative(getFacing().getOpposite());
    }

    /**
     * Retourne l'inventaire adjacent comme Container, ou null.
     */
    @Nullable
    protected Container getAdjacentInventory() {
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(getAdjacentPos());
        if (be instanceof Container container) {
            return container;
        }
        return null;
    }

    /**
     * Retourne les slots operables (selectedSlots si non-vide, sinon tous).
     */
    protected int[] getOperableSlots(Container container) {
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

    // === Pending Tasks ===

    /**
     * Genere une cle unique pour un item (pour le suivi des taches en cours).
     */
    protected String itemKey(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().toString()
            + ":" + stack.getComponentsPatch().hashCode();
    }

    /**
     * Nettoie les taches qui ne sont plus en cours dans le delivery manager.
     */
    protected void cleanupPendingTasks() {
        StorageControllerBlockEntity controller = getController();
        if (controller == null) {
            pendingTasks.clear();
            return;
        }
        pendingTasks.entrySet().removeIf(entry ->
            !controller.getDeliveryManager().isTaskPending(entry.getValue()));
    }

    // === Server Tick ===

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        scanTimer++;
        guiCheckTimer++;

        // Update hasAdjacentGui toutes les 100 ticks (~5 sec)
        if (guiCheckTimer >= 100) {
            guiCheckTimer = 0;
            boolean hadGui = hasAdjacentGui;
            BlockEntity adjacentBe = level.getBlockEntity(getAdjacentPos());
            hasAdjacentGui = adjacentBe instanceof net.minecraft.world.MenuProvider;
            if (hadGui != hasAdjacentGui) {
                syncToClient();
            }
        }

        if (scanTimer >= SCAN_INTERVAL) {
            scanTimer = 0;
            cleanupPendingTasks();

            StorageControllerBlockEntity controller = getController();
            if (controller == null) return;

            Container adjacent = getAdjacentInventory();
            if (adjacent == null) return;

            doScan(adjacent, controller);
        }
    }

    /**
     * Methode abstraite: logique de scan specifique a l'import ou l'export.
     */
    protected abstract void doScan(Container adjacent, StorageControllerBlockEntity controller);

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        tag.put("FilterSlots", filterSlots.serializeNBT(registries));
        tag.putString("FilterMode", filterMode.name());

        ListTag textTag = new ListTag();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Text", textFilters[i]);
            textTag.add(entry);
        }
        tag.put("TextFilters", textTag);

        if (!selectedSlots.isEmpty()) {
            tag.putIntArray("SelectedSlots", selectedSlots.stream().mapToInt(Integer::intValue).toArray());
        }

        if (!pendingTasks.isEmpty()) {
            ListTag tasksTag = new ListTag();
            for (Map.Entry<String, UUID> entry : pendingTasks.entrySet()) {
                CompoundTag taskEntry = new CompoundTag();
                taskEntry.putString("Key", entry.getKey());
                taskEntry.putUUID("TaskId", entry.getValue());
                tasksTag.add(taskEntry);
            }
            tag.put("PendingTasks", tasksTag);
        }

        saveExtra(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("ControllerPos")) {
            NbtUtils.readBlockPos(tag, "ControllerPos").ifPresent(pos -> controllerPos = pos);
        } else {
            controllerPos = null;
        }

        if (tag.contains("FilterSlots")) {
            filterSlots.deserializeNBT(registries, tag.getCompound("FilterSlots"));
        }

        if (tag.contains("FilterMode")) {
            try {
                filterMode = FilterMode.valueOf(tag.getString("FilterMode"));
            } catch (IllegalArgumentException e) {
                filterMode = FilterMode.ITEM;
            }
        }

        if (tag.contains("TextFilters")) {
            ListTag textTag = tag.getList("TextFilters", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(textTag.size(), FILTER_SLOTS); i++) {
                textFilters[i] = textTag.getCompound(i).getString("Text");
            }
        }

        selectedSlots.clear();
        if (tag.contains("SelectedSlots")) {
            for (int s : tag.getIntArray("SelectedSlots")) {
                selectedSlots.add(s);
            }
        }

        pendingTasks.clear();
        if (tag.contains("PendingTasks")) {
            ListTag tasksTag = tag.getList("PendingTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tasksTag.size(); i++) {
                CompoundTag taskEntry = tasksTag.getCompound(i);
                pendingTasks.put(taskEntry.getString("Key"), taskEntry.getUUID("TaskId"));
            }
        }

        loadExtra(tag, registries);
    }

    /**
     * Hook pour les sous-classes: sauvegarder des donnees additionnelles.
     */
    protected abstract void saveExtra(CompoundTag tag, HolderLookup.Provider registries);

    /**
     * Hook pour les sous-classes: charger des donnees additionnelles.
     */
    protected abstract void loadExtra(CompoundTag tag, HolderLookup.Provider registries);

    // === Sync Client ===

    public void syncToClient() {
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
}
