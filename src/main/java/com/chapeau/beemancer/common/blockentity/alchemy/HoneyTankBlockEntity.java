/**
 * ============================================================
 * [HoneyTankBlockEntity.java]
 * Description: BlockEntity pour le tank de stockage de fluides
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.menu.alchemy.HoneyTankMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class HoneyTankBlockEntity extends BlockEntity implements MenuProvider {
    // --- TIER CONFIG ---
    public static final int TIER1_CAPACITY = 16000;
    public static final int TIER2_CAPACITY = 64000;
    public static final int TIER3_CAPACITY = 256000;

    private final int capacity;
    protected final FluidTank fluidTank;

    // Slot pour bucket
    protected final ItemStackHandler bucketSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Accept buckets with fluid
            return isBucketWithFluid(stack);
        }
    };

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? fluidTank.getFluidAmount() : 0;
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 1; }
    };

    public HoneyTankBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.HONEY_TANK.get(), pos, state, TIER1_CAPACITY);
    }

    public HoneyTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.capacity = capacity;
        this.fluidTank = new FluidTank(capacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    // Factory methods for tiered versions
    public static HoneyTankBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new HoneyTankBlockEntity(BeemancerBlockEntities.HONEY_TANK_TIER2.get(), pos, state, TIER2_CAPACITY);
    }

    public static HoneyTankBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new HoneyTankBlockEntity(BeemancerBlockEntities.HONEY_TANK_TIER3.get(), pos, state, TIER3_CAPACITY);
    }

    public int getCapacity() { return capacity; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyTankBlockEntity be) {
        // Process bucket slot
        be.processBucketSlot();

        // Transfer to block below
        if (be.fluidTank.getFluidAmount() > 0) {
            BlockEntity below = level.getBlockEntity(pos.below());
            if (below != null) {
                var cap = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    pos.below(), Direction.UP);
                if (cap != null) {
                    FluidStack toTransfer = be.fluidTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                    if (!toTransfer.isEmpty()) {
                        int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                        if (filled > 0) {
                            be.fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }
    }

    protected void processBucketSlot() {
        ItemStack bucket = bucketSlot.getStackInSlot(0);
        if (bucket.isEmpty()) return;

        // Check if bucket has fluid we can accept
        FluidStack contained = getFluidFromBucket(bucket);
        if (contained.isEmpty() || !fluidTank.isFluidValid(contained)) return;

        // Try to fill the tank
        int canFill = fluidTank.fill(contained, IFluidHandler.FluidAction.SIMULATE);
        if (canFill >= FluidType.BUCKET_VOLUME) {
            fluidTank.fill(new FluidStack(contained.getFluid(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
            bucketSlot.setStackInSlot(0, new ItemStack(Items.BUCKET));
        }
    }

    protected boolean isBucketWithFluid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FluidStack fluid = getFluidFromBucket(stack);
        return !fluid.isEmpty() && fluidTank.isFluidValid(fluid);
    }

    protected FluidStack getFluidFromBucket(ItemStack bucket) {
        if (bucket.isEmpty()) return FluidStack.EMPTY;

        // Check using capability
        var cap = bucket.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if (cap != null) {
            FluidStack drained = cap.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty()) return drained;
        }

        return FluidStack.EMPTY;
    }

    public FluidTank getFluidTank() { return fluidTank; }
    public int getFluidAmount() { return fluidTank.getFluidAmount(); }
    public FluidStack getFluid() { return fluidTank.getFluid(); }
    public ItemStackHandler getBucketSlot() { return bucketSlot; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.honey_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new HoneyTankMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Bucket", bucketSlot.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
        if (tag.contains("Bucket")) {
            bucketSlot.deserializeNBT(registries, tag.getCompound("Bucket"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }
}
