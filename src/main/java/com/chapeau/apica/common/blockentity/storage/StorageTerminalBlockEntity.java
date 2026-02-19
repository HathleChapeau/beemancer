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
 * | ApicaBlockEntities          | Type du BlockEntity    | Constructeur          |
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
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.ControllerStats;
import com.chapeau.apica.common.block.storage.InterfaceRequest;
import com.chapeau.apica.common.menu.storage.StorageTerminalMenu;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageTerminalBlockEntity.class);
    public static final int PAGE_SIZE = 9;
    public static final int PAGES = 3;
    public static final int DEPOSIT_SLOTS = PAGE_SIZE * PAGES;
    public static final int PICKUP_SLOTS = PAGE_SIZE * PAGES;
    public static final int TOTAL_SLOTS = DEPOSIT_SLOTS + PICKUP_SLOTS;
    private static final int MAX_PENDING_REQUESTS = 16;

    // Position du controller lié
    @Nullable
    private BlockPos controllerPos = null;

    // Flag pour éviter de re-scanner pendant l'extraction par une bee
    private boolean isExtracting = false;
    private boolean isSaving = false;
    private boolean isLoading = false;
    private boolean isCompacting = false;
    // Flags lus par le controller dans processTerminals()
    boolean needsDepositScan = false;
    boolean needsProcessPending = false;

    // File d'attente des requêtes en attente
    private final Queue<PendingRequest> pendingRequests = new LinkedList<>();

    // Slots internes — les items restent dans les slots jusqu'a extraction par une bee
    private final ItemStackHandler depositSlots = new ItemStackHandler(DEPOSIT_SLOTS) {
        @Override
        public void setSize(int size) {
            if (size < DEPOSIT_SLOTS) return;
            super.setSize(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (isLoading) return;
            setChanged();
            if (!isExtracting) {
                needsDepositScan = true;
                notifyControllerDepositsChanged();
            }
        }
    };

    private final ItemStackHandler pickupSlots = new ItemStackHandler(PICKUP_SLOTS) {
        @Override
        public void setSize(int size) {
            if (size < PICKUP_SLOTS) return;
            super.setSize(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (isLoading || isCompacting) return;
            setChanged();
            if (getStackInSlot(slot).isEmpty()) {
                compactPickupSlots();
                needsProcessPending = true;
            }
        }
    };

    public StorageTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(ApicaBlockEntities.STORAGE_TERMINAL.get(), pos, blockState);
    }

    /**
     * Notifie le controller que les deposit slots ont change (push model).
     * Permet a l'aggregateur de marquer ses donnees comme perimees
     * pour un refresh plus rapide au prochain tick sync.
     */
    private void notifyControllerDepositsChanged() {
        if (controllerPos == null || level == null || level.isClientSide()) return;
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            controller.getItemAggregator().markDirty();
        }
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

        // Controller n'existe plus: casser le lien
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

        InterfaceRequest request = new InterfaceRequest(
            worldPosition, worldPosition, InterfaceRequest.RequestType.IMPORT,
            template, count, InterfaceRequest.TaskOrigin.REQUEST
        );
        controller.getRequestManager().publishRequest(request);

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
            StorageControllerBlockEntity controller = getController();
            if (controller != null) {
                controller.notifyViewers(Component.translatable("gui.apica.network.queue_full"));
            }
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
     * Traite les requetes en attente quand de la place se libere.
     * Publie des InterfaceRequests via le RequestManager.
     * Appele par le controller dans processTerminals().
     */
    void processPendingRequests() {
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
     * Scanne les deposit slots et cree des requetes EXPORT non-preloaded.
     * Les items restent dans les slots — les bees viennent les extraire.
     * Evite les doublons: ne cree pas de requete si une est deja active pour le meme type.
     * Appele par le controller dans processTerminals().
     */
    void scanDepositSlots() {
        if (level == null || level.isClientSide()) return;
        StorageControllerBlockEntity controller = getController();
        if (controller == null) return;

        RequestManager requestManager = controller.getRequestManager();

        // Collecter les types distincts et leur total dans les deposit slots
        List<ItemStack> templates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            ItemStack stack = depositSlots.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            boolean found = false;
            for (int j = 0; j < templates.size(); j++) {
                if (ItemStack.isSameItemSameComponents(templates.get(j), stack)) {
                    counts.set(j, counts.get(j) + stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                templates.add(stack.copyWithCount(1));
                counts.add(stack.getCount());
            }
        }

        // Pour chaque type, creer une requete si aucune n'est active (O(1) via index)
        for (int j = 0; j < templates.size(); j++) {
            ItemStack template = templates.get(j);
            int totalCount = counts.get(j);

            if (!requestManager.hasRequestFor(worldPosition, InterfaceRequest.RequestType.EXPORT, template)) {
                InterfaceRequest request = new InterfaceRequest(
                    worldPosition, worldPosition, InterfaceRequest.RequestType.EXPORT,
                    template, totalCount, InterfaceRequest.TaskOrigin.REQUEST, false
                );
                requestManager.publishRequest(request);
            }
        }
    }

    /**
     * Extrait des items des deposit slots uniquement.
     * Appele par le systeme de livraison quand une bee arrive au terminal.
     *
     * @return les items extraits (count peut etre inferieur si pas assez)
     */
    public ItemStack extractFromDeposit(ItemStack template, int count) {
        if (template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        isExtracting = true;
        try {
            ItemStack result = template.copy();
            result.setCount(0);
            int needed = count;

            for (int i = 0; i < DEPOSIT_SLOTS && needed > 0; i++) {
                ItemStack stack = depositSlots.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(stack, template)) {
                    int toTake = Math.min(needed, stack.getCount());
                    stack.shrink(toTake);
                    result.grow(toTake);
                    needed -= toTake;
                    if (stack.isEmpty()) {
                        depositSlots.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }

            if (result.getCount() > 0) {
                setChanged();
            }
            return result;
        } finally {
            isExtracting = false;
        }
    }

    /**
     * Compte les items d'un type dans les deposit slots uniquement.
     */
    public int countInDeposit(ItemStack template) {
        if (template.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < DEPOSIT_SLOTS; i++) {
            ItemStack stack = depositSlots.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // === Slots Handlers ===

    public ItemStackHandler getDepositSlots() {
        return depositSlots;
    }

    public ItemStackHandler getPickupSlots() {
        return pickupSlots;
    }

    /**
     * Compacte les pickup slots en decalant les items pour remplir les trous.
     */
    /**
     * Compacte les pickup slots en decalant les items pour remplir les trous.
     */
    private void compactPickupSlots() {
        isCompacting = true;
        try {
            int writeIndex = 0;
            for (int readIndex = 0; readIndex < PICKUP_SLOTS; readIndex++) {
                ItemStack stack = pickupSlots.getStackInSlot(readIndex);
                if (!stack.isEmpty()) {
                    if (writeIndex != readIndex) {
                        pickupSlots.setStackInSlot(writeIndex, stack);
                        pickupSlots.setStackInSlot(readIndex, ItemStack.EMPTY);
                    }
                    writeIndex++;
                }
            }
        } finally {
            isCompacting = false;
        }
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
        return Component.translatable("container.apica.storage_terminal");
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
            essenceSlots = new net.neoforged.neoforge.items.ItemStackHandler(HiveManager.MAX_ESSENCE_SLOTS);
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

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        LOGGER.debug("[Terminal] setRemoved at {}", worldPosition);
        if (level != null && !level.isClientSide() && controllerPos != null) {
            // [FIX] Cleanup silencieux: NE PAS appeler cancelRequestsFromSource()
            // car elle cascade vers cancelRequest() → parent.setChanged() + cancelTask()
            // → DeliveryTaskCanceller.cancelTask() → parent.setChanged()
            // Tous ces setChanged() re-dirtient le chunk du controller pendant le world unload
            // → boucle infinie dans saveAllChunks → "saving world" hang
            //
            // [FIX] Pendant le shutdown, NE PAS appeler level.getBlockEntity(controllerPos).
            // Si le controller a deja ete remove du chunk, getBlockEntity() le RE-CREE
            // (EntityCreationType.IMMEDIATE), ce qui re-dirtie le chunk → boucle infinie.
            // Le registre sera nettoye au reload via validation.
            if (!com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) {
                BlockEntity be = level.getBlockEntity(controllerPos);
                if (be instanceof StorageControllerBlockEntity controller) {
                    controller.getNetworkRegistry().unregisterBlock(worldPosition);
                    controller.getItemAggregator().removeViewersForTerminal(worldPosition);
                }
            }
            controllerPos = null;
            pendingRequests.clear();
        }
        super.setRemoved();
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (com.chapeau.apica.common.block.storage.StorageEvents.isShuttingDown()) {
            LOGGER.info("[Apica] Saving Terminal at {} — controller:{}, pendingRequests:{}",
                worldPosition, controllerPos, pendingRequests.size());
        }
        LOGGER.debug("[Terminal] saveAdditional START at {}", worldPosition);
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
            LOGGER.debug("[Terminal] saveAdditional END at {}", worldPosition);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        isLoading = true;
        try {
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
        } finally {
            isLoading = false;
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
