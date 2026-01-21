/**
 * ============================================================
 * [StorageCrateBlockEntity.java]
 * Description: BlockEntity avec inventaire 54 slots (double coffre)
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation           |
 * |--------------------------|------------------------|-----------------------|
 * | BeemancerBlockEntities   | Type du BlockEntity    | Constructeur          |
 * | BeemancerMenus           | Type du menu           | Création du container |
 * | StorageCrateMenu         | Menu associé           | Interface joueur      |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - StorageCrateBlock.java (création et interaction)
 * - BeemancerBlockEntities.java (enregistrement)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.menu.StorageCrateMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class StorageCrateBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CONTAINER_SIZE = 54; // Double chest size (6 rows * 9 columns)
    
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    public StorageCrateBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_CRATE.get(), pos, blockState);
    }

    // --- Container Implementation ---

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    // --- MenuProvider Implementation ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.storage_crate");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageCrateMenu(containerId, playerInventory, this, this.worldPosition);
    }

    // --- Utility ---

    public void dropContents() {
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, this.worldPosition, this);
        }
    }
}
