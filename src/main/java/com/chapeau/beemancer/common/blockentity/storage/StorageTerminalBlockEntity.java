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

import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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

import java.util.Collections;
import java.util.List;

/**
 * Terminal d'accès au réseau de stockage.
 *
 * Fonctionnalités:
 * - Liaison à un Storage Controller
 * - 9 slots de dépôt (items → réseau)
 * - 9 slots de pickup (requêtes → joueur)
 * - Accès aux items agrégés via le controller
 */
public class StorageTerminalBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int DEPOSIT_SLOTS = 9;
    public static final int PICKUP_SLOTS = 9;
    public static final int TOTAL_SLOTS = DEPOSIT_SLOTS + PICKUP_SLOTS;

    // Position du controller lié
    @Nullable
    private BlockPos controllerPos = null;

    // Flag pour éviter la récursion lors du dépôt
    private boolean isDepositing = false;

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

        // Informer le controller
        if (level != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageControllerBlockEntity controller) {
                controller.linkTerminal(worldPosition);
            }
        }

        setChanged();
        syncToClient();
    }

    /**
     * Retire la liaison au controller.
     */
    public void unlinkController() {
        if (controllerPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof StorageControllerBlockEntity controller) {
                controller.unlinkTerminal(worldPosition);
            }
        }
        controllerPos = null;
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
     * Demande des items au réseau.
     *
     * @return les items extraits, placés dans les slots pickup
     */
    public ItemStack requestItem(ItemStack template, int count) {
        StorageControllerBlockEntity controller = getController();
        if (controller == null) return ItemStack.EMPTY;

        ItemStack extracted = controller.extractItem(template, count);
        if (extracted.isEmpty()) return ItemStack.EMPTY;

        // Placer dans les slots pickup
        ItemStack remaining = extracted.copy();
        for (int i = 0; i < PICKUP_SLOTS && !remaining.isEmpty(); i++) {
            remaining = pickupSlots.insertItem(i, remaining, false);
        }

        // Si reste des items, les remettre dans le réseau
        if (!remaining.isEmpty()) {
            controller.depositItem(remaining);
        }

        return extracted;
    }

    /**
     * Tente de déposer un item du slot deposit vers le réseau.
     */
    private void tryDepositToNetwork(int slot) {
        if (level == null || level.isClientSide()) return;

        StorageControllerBlockEntity controller = getController();
        if (controller == null) return;

        ItemStack stack = depositSlots.getStackInSlot(slot);
        if (stack.isEmpty()) return;

        // Flag pour éviter la récursion
        isDepositing = true;
        try {
            ItemStack remaining = controller.depositItem(stack);
            depositSlots.setStackInSlot(slot, remaining);
        } finally {
            isDepositing = false;
        }
    }

    // === Slots Handlers ===

    public ItemStackHandler getDepositSlots() {
        return depositSlots;
    }

    public ItemStackHandler getPickupSlots() {
        return pickupSlots;
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
        return new StorageTerminalMenu(containerId, playerInventory, this, worldPosition);
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
        super.saveAdditional(tag, registries);

        if (controllerPos != null) {
            tag.put("ControllerPos", NbtUtils.writeBlockPos(controllerPos));
        }

        tag.put("DepositSlots", depositSlots.serializeNBT(registries));
        tag.put("PickupSlots", pickupSlots.serializeNBT(registries));
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
    }

    // === Sync Client ===

    private void syncToClient() {
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
