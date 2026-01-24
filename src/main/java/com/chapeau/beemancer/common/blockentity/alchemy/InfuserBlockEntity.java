/**
 * ============================================================
 * [InfuserBlockEntity.java]
 * Description: Infuse du bois avec du miel pour créer du bois emmiélé
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Input: Any wood (tag #logs) + 250mB Honey
 * - Output: HONEYED_WOOD (ou selon recette JSON)
 * - Process time: configurable via recette
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.InfuserBlock;
import com.chapeau.beemancer.common.menu.alchemy.InfuserMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.InfusingRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class InfuserBlockEntity extends BlockEntity implements MenuProvider {
    private static final int DEFAULT_PROCESS_TIME = 200;

    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null; // Invalidate cached recipe
        }
    };

    private final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final FluidTank honeyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            currentRecipe = null; // Invalidate cached recipe
        }
    };

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<InfusingRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> honeyTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 3; }
    };

    public InfuserBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.INFUSER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfuserBlockEntity be) {
        boolean wasWorking = state.getValue(InfuserBlock.WORKING);
        boolean isWorking = false;

        Optional<RecipeHolder<InfusingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = recipe.get().value().processingTime();

            if (be.canProcess(recipe.get().value())) {
                be.progress++;
                isWorking = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processItem(recipe.get().value());
                    be.progress = 0;
                    be.currentRecipe = null;
                }
            } else {
                be.progress = 0;
            }
        } else {
            be.progress = 0;
            be.currentRecipe = null;
        }

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(InfuserBlock.WORKING, isWorking), 3);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<InfusingRecipe>> findRecipe(Level level) {
        ProcessingRecipeInput input = createRecipeInput();
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.INFUSING.get(),
            input,
            level
        );
    }

    private ProcessingRecipeInput createRecipeInput() {
        return ProcessingRecipeInput.of(
            inputSlot.getStackInSlot(0),
            honeyTank.getFluid()
        );
    }

    private boolean canProcess(InfusingRecipe recipe) {
        // Check if we have space in output
        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) {
            return true;
        }

        ItemStack expectedOutput = recipe.result();
        return ItemStack.isSameItemSameComponents(output, expectedOutput)
            && output.getCount() < output.getMaxStackSize();
    }

    private void processItem(InfusingRecipe recipe) {
        // Consume input
        inputSlot.extractItem(0, 1, false);
        honeyTank.drain(recipe.fluidIngredient().amount(), IFluidHandler.FluidAction.EXECUTE);

        // Add result to output
        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty()) {
            outputSlot.setStackInSlot(0, recipe.result().copy());
        } else {
            output.grow(1);
        }
    }

    public FluidTank getHoneyTank() { return honeyTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }
    @Nullable
    public RecipeHolder<InfusingRecipe> getCurrentRecipe() { return currentRecipe; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new InfuserMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlot.serializeNBT(registries));
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlot.deserializeNBT(registries, tag.getCompound("Output"));
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        progress = tag.getInt("Progress");
    }
}
