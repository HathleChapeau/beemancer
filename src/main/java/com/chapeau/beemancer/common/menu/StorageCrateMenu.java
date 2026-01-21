/**
 * ============================================================
 * [StorageCrateMenu.java]
 * Description: Menu/Container pour le StorageCrate (54 slots)
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation           |
 * |--------------------------|------------------------|-----------------------|
 * | BeemancerMenus           | Type du menu           | Constructeur          |
 * | StorageCrateBlockEntity  | Source des données     | Accès inventaire      |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - StorageCrateBlockEntity.java (création du menu)
 * - StorageCrateScreen.java (affichage)
 * - BeemancerMenus.java (enregistrement)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import com.chapeau.beemancer.common.blockentity.storage.StorageCrateBlockEntity;
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

public class StorageCrateMenu extends AbstractContainerMenu {
    private static final int CONTAINER_ROWS = 6;
    private static final int CONTAINER_COLUMNS = 9;
    private static final int CONTAINER_SIZE = CONTAINER_ROWS * CONTAINER_COLUMNS; // 54
    
    private final Container container;
    private final BlockPos blockPos;

    // Client constructor (from network)
    public StorageCrateMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE), extraData.readBlockPos());
    }

    // Server constructor
    public StorageCrateMenu(int containerId, Inventory playerInventory, Container container, BlockPos pos) {
        super(BeemancerMenus.STORAGE_CRATE.get(), containerId);
        this.container = container;
        this.blockPos = pos;
        
        checkContainerSize(container, CONTAINER_SIZE);
        container.startOpen(playerInventory.player);

        // Container slots (6 rows of 9)
        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < CONTAINER_COLUMNS; col++) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory slots (3 rows of 9)
        int playerInventoryY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        // Player hotbar (1 row of 9)
        int hotbarY = playerInventoryY + 58;
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
            
            if (slotIndex < CONTAINER_SIZE) {
                // Move from container to player inventory
                if (!this.moveItemStackTo(slotStack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to container
                if (!this.moveItemStackTo(slotStack, 0, CONTAINER_SIZE, false)) {
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
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }
}
