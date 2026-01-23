/**
 * ============================================================
 * [PoweredCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse automatique
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.PoweredCentrifugeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class PoweredCentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    private static final int HONEY_CONSUMPTION = 10; // mB per tick
    private static final int PROCESS_TIME = 100; // 5 seconds
    private static final int HONEY_OUTPUT = 250;
    private static final int ROYAL_JELLY_OUTPUT = 100;

    private final ItemStackHandler inputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.HONEYCOMB) || stack.is(BeemancerItems.ROYAL_COMB.get());
        }
    };

    private final ItemStackHandler outputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank fuelTank = new FluidTank(8000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank outputTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress = 0;
    private int currentProcessTime = 0;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> fuelTank.getFluidAmount();
                case 3 -> outputTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> currentProcessTime = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PoweredCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.POWERED_CENTRIFUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PoweredCentrifugeBlockEntity be) {
        boolean wasWorking = state.getValue(PoweredCentrifugeBlock.WORKING);
        boolean isWorking = false;

        if (be.canProcess()) {
            if (be.fuelTank.getFluidAmount() >= HONEY_CONSUMPTION) {
                be.fuelTank.drain(HONEY_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);
                be.progress++;
                isWorking = true;

                if (be.progress >= PROCESS_TIME) {
                    be.processItem();
                    be.progress = 0;
                }
            }
        } else {
            be.progress = 0;
        }

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(PoweredCentrifugeBlock.WORKING, isWorking), 3);
        }

        be.setChanged();
    }

    private boolean canProcess() {
        for (int i = 0; i < inputSlots.getSlots(); i++) {
            ItemStack stack = inputSlots.getStackInSlot(i);
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void processItem() {
        for (int i = 0; i < inputSlots.getSlots(); i++) {
            ItemStack stack = inputSlots.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            boolean isRoyal = stack.is(BeemancerItems.ROYAL_COMB.get());

            // Output fluid
            if (isRoyal) {
                outputTank.fill(new FluidStack(BeemancerFluids.ROYAL_JELLY_SOURCE.get(), ROYAL_JELLY_OUTPUT),
                    IFluidHandler.FluidAction.EXECUTE);
                addToOutput(new ItemStack(BeemancerItems.PROPOLIS.get()));
            } else {
                outputTank.fill(new FluidStack(BeemancerFluids.HONEY_SOURCE.get(), HONEY_OUTPUT),
                    IFluidHandler.FluidAction.EXECUTE);
                addToOutput(new ItemStack(BeemancerItems.BEESWAX.get()));
            }

            stack.shrink(1);
            break;
        }
    }

    private void addToOutput(ItemStack stack) {
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack existing = outputSlots.getStackInSlot(i);
            if (existing.isEmpty()) {
                outputSlots.setStackInSlot(i, stack);
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && 
                       existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                return;
            }
        }
    }

    public FluidTank getFuelTank() { return fuelTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public ItemStackHandler getInputSlots() { return inputSlots; }
    public ItemStackHandler getOutputSlots() { return outputSlots; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.powered_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        // Menu implementation would go here
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputSlots.serializeNBT(registries));
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlots.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        progress = tag.getInt("Progress");
    }
}
