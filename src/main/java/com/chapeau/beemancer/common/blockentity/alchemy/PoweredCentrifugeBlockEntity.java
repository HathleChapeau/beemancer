/**
 * ============================================================
 * [PoweredCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse automatique
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Consomme du miel comme carburant
 * - Accepte les combs definis par recettes JSON
 * - Process automatique avec outputs probabilistes
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.PoweredCentrifugeBlock;
import com.chapeau.beemancer.common.menu.alchemy.PoweredCentrifugeMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingOutput;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.CentrifugeRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class PoweredCentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    private static final int HONEY_CONSUMPTION = 10; // mB per tick while working
    private static final int DEFAULT_PROCESS_TIME = 100;

    private final ItemStackHandler inputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null;
        }
    };

    private final ItemStackHandler outputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final FluidTank fuelTank = new FluidTank(8000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank outputTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    private int currentProcessingSlot = -1;
    @Nullable
    private RecipeHolder<CentrifugeRecipe> currentRecipe = null;

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
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 4; }
    };

    public PoweredCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.POWERED_CENTRIFUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PoweredCentrifugeBlockEntity be) {
        boolean wasWorking = state.getValue(PoweredCentrifugeBlock.WORKING);
        boolean isWorking = false;

        // Find a slot with a valid recipe if we don't have one
        if (be.currentRecipe == null || be.currentProcessingSlot == -1) {
            be.findValidRecipe(level);
        }

        if (be.currentRecipe != null && be.currentProcessingSlot >= 0) {
            if (be.fuelTank.getFluidAmount() >= HONEY_CONSUMPTION) {
                be.fuelTank.drain(HONEY_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);
                be.progress++;
                isWorking = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processItem(level.getRandom());
                    be.progress = 0;
                    be.currentRecipe = null;
                    be.currentProcessingSlot = -1;
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

    private void findValidRecipe(Level level) {
        for (int i = 0; i < inputSlots.getSlots(); i++) {
            ItemStack stack = inputSlots.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ProcessingRecipeInput input = ProcessingRecipeInput.ofItem(stack);
            Optional<RecipeHolder<CentrifugeRecipe>> recipe = level.getRecipeManager().getRecipeFor(
                BeemancerRecipeTypes.CENTRIFUGING.get(),
                input,
                level
            );

            if (recipe.isPresent()) {
                currentRecipe = recipe.get();
                currentProcessTime = recipe.get().value().processingTime();
                currentProcessingSlot = i;
                return;
            }
        }
        currentRecipe = null;
        currentProcessingSlot = -1;
    }

    private void processItem(RandomSource random) {
        if (currentRecipe == null || currentProcessingSlot < 0) return;

        CentrifugeRecipe recipe = currentRecipe.value();

        // Consume input
        inputSlots.extractItem(currentProcessingSlot, 1, false);

        // Produce fluid output
        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            outputTank.fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }

        // Produce item outputs with probabilities
        for (ProcessingOutput output : recipe.results()) {
            output.roll(random).ifPresent(this::addToOutput);
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
    @Nullable
    public RecipeHolder<CentrifugeRecipe> getCurrentRecipe() { return currentRecipe; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.powered_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new PoweredCentrifugeMenu(containerId, playerInv, this, dataAccess);
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
