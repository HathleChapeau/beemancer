/**
 * ============================================================
 * [StorageTerminalBlockEntity.java]
 * Description: BlockEntity pour le Storage Terminal - interface réseau
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | BeemancerBlockEntities          | Type du BlockEntity    | Constructeur          |
 * | StorageControllerBlockEntity    | Controller lié         | Accès items agrégés   |
 * | StorageTerminalMenu             | Menu associé           | Interface joueur      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalBlock.java (création et interaction)
 * - StorageTerminalMenu.java (accès aux slots)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.block.storage.InterfaceRequest;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Terminal d'accès au réseau de stockage.
 *
 * Fonctionnalités:
 * - Liaison à un Storage Controller
 * - 9 slots de dépôt (items → réseau)
 * - 9 slots de pickup (requêtes → joueur)
 * - File d'attente pour les requêtes quand pas assez de place
 * - Accès aux items agrégés via le controller
 */
public class StorageTerminalBlockEntity extends BlockEntity implements MenuProvider, Container, IDeliveryEndpoint {

    public static final int DEPOSIT_SLOTS = 9;
    public static final int PICKUP_SLOTS = 9;
    public static final int TOTAL_SLOTS = DEPOSIT_SLOTS + PICKUP_SLOTS;
    private static final int MAX_PENDING_REQUESTS = 16;

    // Position du controller lié
    @Nullable
    private BlockPos controllerPos = null;

    // Flag pour éviter la récursion lors du dépôt
    private boolean isDepositing = false;
    private boolean isSaving = false;

    // File d'attente des requêtes en attente
    private final Queue<PendingRequest> pendingRequests = new LinkedList<>();

    // Slots internes
    private final ItemStackHandler depositSlots = new ItemStackHandler(DEPOSIT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Transférer automatiquement vers le réseau (éviter récursion)
            if (!isDepositing) {
                tryDepositToNetwork(slot);
            }
        }
    };

    private final ItemStackHandler pickupSlots = new ItemStackHandler(PICKUP_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Quand un slot se libère, traiter la file d'attente
            if (getStackInSlot(slot).isEmpty()) {
                processPendingRequests();
            }
        }
    };

    public StorageTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_TERMINAL.get(), pos, blockState);
    }

    // === Liaison Controller ===

    /**
     * Lie ce terminal à un controller.
     */
    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        syncToClient();
    }

    /**
     * Retire la liaison au controller.
     */
    public void unlinkController() {
        if (controllerPos != null && level != null) {
            // Retirer du registre central
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof StorageControllerBlockEntity controller) {
                controller.getNetworkRegistry().unregisterBlock(worldPosition);
                controller.getItemAggregator().removeViewersForTerminal(worldPosition);
                controller.setChanged();
            }
        }
        controllerPos = null;
        cancelAllPendingRequests();
        setChanged();
        syncToClient();
    }

    /**
     * Vérifie si le terminal est lié à un controller valide.
     */
    public boolean isLinked() {
        return controllerPos != null && getController() != null;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * Récupère le controller lié.
     */
    @Nullable
    public StorageControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            return controller;
        }

        // Controller n'existe plus
        controllerPos = null;
        setChanged();
        return null;
    }

    // === Accès aux Items ===

    /**
     * Retourne les items agrégés du réseau.
     */
    public List<ItemStack> getAggregatedItems() {
        StorageControllerBlockEntity controller = getController();
        if (controller == null) return Collections.emptyList();
        return controller.getAggregatedItems();
    }

    /**
     * Demande des items au réseau via le RequestManager.
     * Si la demande depasse la quantite max, le surplus est mis en file d'attente.
     *
     * @return toujours EMPTY (les items arrivent via la bee)
     */
    public ItemStack requestItem(ItemStack template, int count) {
        StorageControllerBlockEntity controller = getController();
        if (controller == null) return ItemStack.EMPTY;

        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());
        int toRequest = Math.min(count, maxQuantity);

        InterfaceRequest request = new InterfaceRequest(
            worldPosition, worldPosition, InterfaceRequest.RequestType.IMPORT,
            template, toRequest, InterfaceRequest.TaskOrigin.REQUEST
        );
        controller.getRequestManager().publishRequest(request);

        // Ajouter le reste a la file d'attente si demande > max quantite
        int pendingCount = count - toRequest;
        if (pendingCount > 0) {
            addToPendingQueue(template.copy(), pendingCount);
        }

        setChanged();
        return ItemStack.EMPTY;
    }

    /**
     * Calcule l'espace disponible dans les slots pickup pour un type d'item.
     */
    private int calculateAvailableSpace(ItemStack template) {
        int space = 0;
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            ItemStack existing = pickupSlots.getStackInSlot(i);
            if (existing.isEmpty()) {
                space += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, template)) {
                space += existing.getMaxStackSize() - existing.getCount();
            }
        }
        return space;
    }

    /**
     * Ajoute une requête à la file d'attente.
     */
    private void addToPendingQueue(ItemStack template, int count) {
        if (count <= 0) return;

        // Limiter le nombre de requêtes en attente
        if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
            return;
        }

        // Fusionner avec une requête existante pour le même item
        for (PendingRequest request : pendingRequests) {
            if (ItemStack.isSameItemSameComponents(request.item, template)) {
                request.count += count;
                setChanged();
                return;
            }
        }

        // Nouvelle requête
        pendingRequests.add(new PendingRequest(template.copyWithCount(1), count));
        setChanged();
    }

    /**
     * Traite les requêtes en attente quand de la place se libère.
     * Publie des InterfaceRequests via le RequestManager.
     */
    private void processPendingRequests() {
        if (level == null || level.isClientSide()) return;
        if (pendingRequests.isEmpty()) return;

        StorageControllerBlockEntity controller = getController();
        if (controller == null) return;

        int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());
        RequestManager requestManager = controller.getRequestManager();

        List<PendingRequest> toRemove = new ArrayList<>();

        for (PendingRequest request : pendingRequests) {
            int toRequest = Math.min(request.count, maxQuantity);

            InterfaceRequest interfaceRequest = new InterfaceRequest(
                worldPosition, worldPosition, InterfaceRequest.RequestType.IMPORT,
                request.item, toRequest, InterfaceRequest.TaskOrigin.REQUEST
            );
            requestManager.publishRequest(interfaceRequest);

            request.count -= toRequest;
            if (request.count <= 0) {
                toRemove.add(request);
            }
        }

        pendingRequests.removeAll(toRemove);

        if (!toRemove.isEmpty()) {
            setChanged();
        }
    }

    /**
     * Annule toutes les requêtes en attente.
     * Les items réservés sont remis dans le réseau.
     */
    private void cancelAllPendingRequests() {
        pendingRequests.clear();
        setChanged();
    }

    /**
     * Retourne la liste des requêtes en attente (lecture seule).
     */
    public List<PendingRequest> getPendingRequests() {
        return Collections.unmodifiableList(new ArrayList<>(pendingRequests));
    }

    /**
     * Retourne le nombre total d'items en attente.
     */
    public int getTotalPendingCount() {
        int total = 0;
        for (PendingRequest request : pendingRequests) {
            total += request.count;
        }
        return total;
    }

    /**
     * Tente de deposer un item du slot deposit vers le reseau via RequestManager.
     */
    private void tryDepositToNetwork(int slot) {
        if (level == null || level.isClientSide()) return;

        StorageControllerBlockEntity controller = getController();
        if (controller == null) return;

        isDepositing = true;
        try {
            int maxQuantity = ControllerStats.getQuantity(controller.getEssenceSlots());

            while (true) {
                ItemStack stack = depositSlots.getStackInSlot(slot);
                if (stack.isEmpty()) break;

                int toDeposit = Math.min(stack.getCount(), maxQuantity);

                ItemStack toSend = stack.copy();
                toSend.setCount(toDeposit);
                stack.shrink(toDeposit);
                depositSlots.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

                InterfaceRequest request = new InterfaceRequest(
                    worldPosition, worldPosition, InterfaceRequest.RequestType.EXPORT,
                    toSend, toDeposit, InterfaceRequest.TaskOrigin.REQUEST, true
                );
                controller.getRequestManager().publishRequest(request);
            }
        } finally {
            isDepositing = false;
        }
    }

    // === Tick ===

    public static void serverTick(StorageTerminalBlockEntity be) {
        // Traiter périodiquement les requêtes en attente (chaque seconde)
        if (be.level != null && be.level.getGameTime() % 20 == 0) {
            be.processPendingRequests();
        }
    }

    // === Slots Handlers ===

    public ItemStackHandler getDepositSlots() {
        return depositSlots;
    }

    public ItemStackHandler getPickupSlots() {
        return pickupSlots;
    }

    @Override
    public ItemStack receiveDeliveredItems(ItemStack items) {
        return insertIntoPickupSlots(items);
    }

    /**
     * Insère des items dans les slots pickup (appelé par DeliveryBeeEntity).
     * @return le reste non inséré
     */
    public ItemStack insertIntoPickupSlots(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();
        for (int i = 0; i < PICKUP_SLOTS && !remaining.isEmpty(); i++) {
            remaining = pickupSlots.insertItem(i, remaining, false);
        }
        if (!remaining.equals(stack)) {
            setChanged();
        }
        return remaining;
    }

    // === Container Implementation ===

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            if (!depositSlots.getStackInSlot(i).isEmpty()) return false;
        }
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            if (!pickupSlots.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < DEPOSIT_SLOTS) {
            return depositSlots.getStackInSlot(slot);
        } else {
            return pickupSlots.getStackInSlot(slot - DEPOSIT_SLOTS);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result;
        if (slot < DEPOSIT_SLOTS) {
            result = depositSlots.extractItem(slot, amount, false);
        } else {
            result = pickupSlots.extractItem(slot - DEPOSIT_SLOTS, amount, false);
        }
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < DEPOSIT_SLOTS) {
            ItemStack stack = depositSlots.getStackInSlot(slot);
            depositSlots.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        } else {
            ItemStack stack = pickupSlots.getStackInSlot(slot - DEPOSIT_SLOTS);
            pickupSlots.setStackInSlot(slot - DEPOSIT_SLOTS, ItemStack.EMPTY);
            return stack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < DEPOSIT_SLOTS) {
            depositSlots.setStackInSlot(slot, stack);
        } else {
            pickupSlots.setStackInSlot(slot - DEPOSIT_SLOTS, stack);
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
            worldPosition.getX() + 0.5,
            worldPosition.getY() + 0.5,
            worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            depositSlots.setStackInSlot(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            pickupSlots.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.storage_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Ajouter le joueur aux viewers du controller
        StorageControllerBlockEntity controller = getController();
        if (controller != null) {
            controller.addViewer(player.getUUID(), worldPosition);
        }
        // Récupérer les essence slots du controller (ou un handler vide si pas lié)
        net.neoforged.neoforge.items.ItemStackHandler essenceSlots;
        if (controller != null) {
            essenceSlots = controller.getEssenceSlots();
        } else {
            essenceSlots = new net.neoforged.neoforge.items.ItemStackHandler(4);
        }
        return new StorageTerminalMenu(containerId, playerInventory, this, worldPosition, essenceSlots);
    }

    /**
     * Appelé quand le menu est fermé.
     */
    public void onMenuClosed(Player player) {
        StorageControllerBlockEntity controller = getController();
        if (controller != null) {
            controller.removeViewer(player.getUUID());
        }
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isSaving = true;
        try {
            super.saveAdditional(tag, registries);

            if (controllerPos != null) {
                tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
            }

            tag.put("DepositSlots", depositSlots.serializeNBT(registries));
            tag.put("PickupSlots", pickupSlots.serializeNBT(registries));

            // Sauvegarder les requêtes en attente
            ListTag pendingTag = new ListTag();
            for (PendingRequest request : pendingRequests) {
                CompoundTag requestTag = new CompoundTag();
                requestTag.put("Item", request.item.saveOptional(registries));
                requestTag.putInt("Count", request.count);
                pendingTag.add(requestTag);
            }
            tag.put("PendingRequests", pendingTag);
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

        if (tag.contains("DepositSlots")) {
            depositSlots.deserializeNBT(registries, tag.getCompound("DepositSlots"));
        }
        if (tag.contains("PickupSlots")) {
            pickupSlots.deserializeNBT(registries, tag.getCompound("PickupSlots"));
        }

        // Charger les requêtes en attente
        pendingRequests.clear();
        if (tag.contains("PendingRequests")) {
            ListTag pendingTag = tag.getList("PendingRequests", Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingTag.size(); i++) {
                CompoundTag requestTag = pendingTag.getCompound(i);
                ItemStack item = ItemStack.parseOptional(registries, requestTag.getCompound("Item"));
                int count = requestTag.getInt("Count");
                if (!item.isEmpty() && count > 0) {
                    pendingRequests.add(new PendingRequest(item, count));
                }
            }
        }
    }

    // === Sync Client ===

    private void syncToClient() {
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

    // === Classes internes ===

    /**
     * Représente une requête en attente.
     */
    public static class PendingRequest {
        public final ItemStack item;
        public int count;

        public PendingRequest(ItemStack item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
