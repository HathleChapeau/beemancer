/**
 * ============================================================
 * [IncubatorBlockEntity.java]
 * Description: BlockEntity incubateur avec timer d'incubation
 * ============================================================
 */
package com.chapeau.beemancer.common.block.incubator;

import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.IncubatorMenu;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class IncubatorBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.Container {
    public static final int SLOT_COUNT = 1;
    public static final int INCUBATION_TIME = 600; // 30 seconds
    
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int incubationProgress = 0;
    
    // ContainerData for GUI sync
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> incubationProgress;
                case 1 -> INCUBATION_TIME;
                default -> 0;
            };
        }
        
        @Override
        public void set(int index, int value) {
            if (index == 0) incubationProgress = value;
        }
        
        @Override
        public int getCount() {
            return 2;
        }
    };
    
    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INCUBATOR.get(), pos, state);
    }

    // --- Container Implementation ---

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            incubationProgress = 0;
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        // Reset progress when item changes
        incubationProgress = 0;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this 
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void clearContent() {
        items.clear();
        incubationProgress = 0;
    }

    // --- Tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity incubator) {
        ItemStack stack = incubator.items.get(0);
        
        // Only process larva items
        if (stack.isEmpty() || !stack.is(BeemancerItems.BEE_LARVA.get())) {
            if (incubator.incubationProgress > 0) {
                incubator.incubationProgress = 0;
                incubator.setChanged();
            }
            return;
        }
        
        // Increment progress
        incubator.incubationProgress++;
        
        // Check if complete
        if (incubator.incubationProgress >= INCUBATION_TIME) {
            // Transform larva to bee
            BeeGeneData geneData = BeeLarvaItem.getGeneData(stack);
            ItemStack beeItem = MagicBeeItem.createWithGenes(geneData);
            
            incubator.items.set(0, beeItem);
            incubator.incubationProgress = 0;
        }
        
        incubator.setChanged();
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", incubationProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        incubationProgress = tag.getInt("Progress");
    }

    // --- Menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.incubator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IncubatorMenu(containerId, playerInventory, this, containerData);
    }

    // --- Drop Contents ---

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, worldPosition, this);
        }
    }
}
