/**
 * ============================================================
 * [AlembicBlockEntity.java]
 * Description: Mélange Honey et Royal Jelly pour produire du Nectar
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Input 1: Honey (500mB)
 * - Input 2: Royal Jelly (250mB)
 * - Output: Nectar (500mB) (ou selon recette JSON)
 * - Process time: configurable via recette
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.AlembicBlock;
import com.chapeau.beemancer.common.menu.alchemy.AlembicMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.DistillingRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AlembicBlockEntity extends BlockEntity implements MenuProvider {
    private static final int DEFAULT_PROCESS_TIME = 80;

    // Flag pour eviter l'invalidation de la recette quand la machine drain elle-meme
    private boolean isProcessingDrain = false;

    private final FluidTank honeyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (!isProcessingDrain) {
                currentRecipe = null;
            }
        }
    };

    private final FluidTank royalJellyTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (!isProcessingDrain) {
                currentRecipe = null;
            }
        }
    };

    private final FluidTank nectarTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int progress = 0;
    private int currentProcessTime = DEFAULT_PROCESS_TIME;
    @Nullable
    private RecipeHolder<DistillingRecipe> currentRecipe = null;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> currentProcessTime;
                case 2 -> honeyTank.getFluidAmount();
                case 3 -> royalJellyTank.getFluidAmount();
                case 4 -> nectarTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) currentProcessTime = value;
        }
        @Override
        public int getCount() { return 5; }
    };

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALEMBIC.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity be) {
        boolean wasDistilling = state.getValue(AlembicBlock.DISTILLING);
        boolean isDistilling = false;

        Optional<RecipeHolder<DistillingRecipe>> recipe = be.findRecipe(level);
        if (recipe.isPresent()) {
            be.currentRecipe = recipe.get();
            be.currentProcessTime = recipe.get().value().processingTime();

            if (be.canProcess(recipe.get().value())) {
                be.progress++;
                isDistilling = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processFluids(recipe.get().value());
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

        // Auto-output nectar to block below (pattern atomique)
        if (be.nectarTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null && !be.nectarTank.isEmpty()) {
                int toTransferAmount = Math.min(100, be.nectarTank.getFluidAmount());
                FluidStack toTransfer = new FluidStack(be.nectarTank.getFluid().getFluid(), toTransferAmount);
                int canFill = cap.fill(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                if (canFill > 0) {
                    FluidStack actualTransfer = new FluidStack(be.nectarTank.getFluid().getFluid(), canFill);
                    int filled = cap.fill(actualTransfer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) be.nectarTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        if (wasDistilling != isDistilling) {
            level.setBlock(pos, state.setValue(AlembicBlock.DISTILLING, isDistilling), 3);
        }

        be.setChanged();
    }

    private Optional<RecipeHolder<DistillingRecipe>> findRecipe(Level level) {
        // Eviter les lookups inutiles si les tanks sont vides
        if (honeyTank.isEmpty() && royalJellyTank.isEmpty()) {
            return Optional.empty();
        }
        ProcessingRecipeInput input = createRecipeInput();
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.DISTILLING.get(),
            input,
            level
        );
    }

    private ProcessingRecipeInput createRecipeInput() {
        return ProcessingRecipeInput.of(
            List.of(),
            List.of(honeyTank.getFluid(), royalJellyTank.getFluid())
        );
    }

    private boolean canProcess(DistillingRecipe recipe) {
        FluidStack output = recipe.getFluidOutput();
        // Check if we have space for output
        return nectarTank.getFluidAmount() + output.getAmount() <= nectarTank.getCapacity();
    }

    private void processFluids(DistillingRecipe recipe) {
        // Consume inputs based on recipe (drain interne - ne pas invalider la recette)
        isProcessingDrain = true;
        if (recipe.fluidIngredients().size() >= 1) {
            honeyTank.drain(recipe.fluidIngredients().get(0).amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (recipe.fluidIngredients().size() >= 2) {
            royalJellyTank.drain(recipe.fluidIngredients().get(1).amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        isProcessingDrain = false;

        // Produce output
        nectarTank.fill(recipe.getFluidOutput(), IFluidHandler.FluidAction.EXECUTE);
    }

    public FluidTank getHoneyTank() { return honeyTank; }
    public FluidTank getRoyalJellyTank() { return royalJellyTank; }
    public FluidTank getNectarTank() { return nectarTank; }
    @Nullable
    public RecipeHolder<DistillingRecipe> getCurrentRecipe() { return currentRecipe; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.alembic");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new AlembicMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("HoneyTank", honeyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("RoyalJellyTank", royalJellyTank.writeToNBT(registries, new CompoundTag()));
        tag.put("NectarTank", nectarTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        honeyTank.readFromNBT(registries, tag.getCompound("HoneyTank"));
        royalJellyTank.readFromNBT(registries, tag.getCompound("RoyalJellyTank"));
        nectarTank.readFromNBT(registries, tag.getCompound("NectarTank"));
        progress = tag.getInt("Progress");
    }
}
