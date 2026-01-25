/**
 * ============================================================
 * [StorageTerminalMenu.java]
 * Description: Menu pour le Storage Terminal avec dépôt et pickup
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | BeemancerMenus                  | Type du menu           | Constructeur          |
 * | StorageTerminalBlockEntity      | Source des données     | Accès inventaire      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalBlockEntity.java (création du menu)
 * - StorageTerminalScreen.java (affichage)
 * - BeemancerMenus.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu du Storage Terminal.
 *
 * Layout:
 * - 9 slots de dépôt (items → réseau)
 * - 9 slots de pickup (requêtes → joueur)
 * - Inventaire joueur standard
 *
 * Les items agrégés sont affichés séparément dans la GUI (pas de vrais slots).
 */
public class StorageTerminalMenu extends AbstractContainerMenu {

    public static final int DEPOSIT_SLOTS = 9;
    public static final int PICKUP_SLOTS = 9;
    public static final int TOTAL_TERMINAL_SLOTS = DEPOSIT_SLOTS + PICKUP_SLOTS;
    public static final int PLAYER_INVENTORY_SIZE = 36;

    private final Container container;
    private final BlockPos blockPos;
    @Nullable
    private final StorageTerminalBlockEntity terminal;

    // Cache des items agrégés (pour affichage dans la GUI)
    private List<ItemStack> aggregatedItems = new ArrayList<>();

    // Client constructor (from network)
    public StorageTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
             new SimpleContainer(TOTAL_TERMINAL_SLOTS),
             extraData.readBlockPos());
    }

    // Server constructor
    public StorageTerminalMenu(int containerId, Inventory playerInventory, Container container, BlockPos pos) {
        super(BeemancerMenus.STORAGE_TERMINAL.get(), containerId);
        this.container = container;
        this.blockPos = pos;

        if (container instanceof StorageTerminalBlockEntity be) {
            this.terminal = be;
        } else {
            this.terminal = null;
        }

        checkContainerSize(container, TOTAL_TERMINAL_SLOTS);
        container.startOpen(playerInventory.player);

        // Deposit slots (3x3 grid, left side)
        // Position Y à ajuster selon le design final
        int depositX = 8;
        int depositY = 126;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new Slot(container, index,
                    depositX + col * 18, depositY + row * 18));
            }
        }

        // Pickup slots (3x3 grid, right side)
        int pickupX = 98;
        int pickupY = 126;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = DEPOSIT_SLOTS + col + row * 3;
                this.addSlot(new Slot(container, index,
                    pickupX + col * 18, pickupY + row * 18));
            }
        }

        // Player inventory (3 rows)
        int playerY = 184;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, playerY + row * 18));
            }
        }

        // Player hotbar
        int hotbarY = 242;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (slotIndex < TOTAL_TERMINAL_SLOTS) {
                // Du terminal vers l'inventaire joueur
                if (!this.moveItemStackTo(slotStack, TOTAL_TERMINAL_SLOTS,
                    TOTAL_TERMINAL_SLOTS + PLAYER_INVENTORY_SIZE, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // De l'inventaire joueur vers les slots de dépôt
                if (!this.moveItemStackTo(slotStack, 0, DEPOSIT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);

        // Notifier le terminal de la fermeture
        if (terminal != null) {
            terminal.onMenuClosed(player);
        }
    }

    // === Accès aux Items Agrégés ===

    /**
     * Récupère les items agrégés du réseau.
     * Appelé par le Screen pour l'affichage.
     */
    public List<ItemStack> getAggregatedItems() {
        if (terminal != null) {
            return terminal.getAggregatedItems();
        }
        return aggregatedItems;
    }

    /**
     * Met à jour le cache des items agrégés (côté client).
     */
    public void setAggregatedItems(List<ItemStack> items) {
        this.aggregatedItems = items;
    }

    /**
     * Demande des items au réseau.
     *
     * @return true si la requête a été traitée
     */
    public boolean requestItem(ItemStack template, int count) {
        if (terminal == null) return false;
        ItemStack extracted = terminal.requestItem(template, count);
        return !extracted.isEmpty();
    }

    // === Getters ===

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Nullable
    public StorageTerminalBlockEntity getTerminal() {
        return terminal;
    }

    public boolean isLinked() {
        return terminal != null && terminal.isLinked();
    }
}
