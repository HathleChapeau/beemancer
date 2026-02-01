/**
 * ============================================================
 * [PoweredCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse automatique
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - 1 slot d'entree + 4 slots de sortie
 * - Consomme du miel comme carburant
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class PoweredCentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    // --- TIER CONFIG ---
    public static final int TIER1_HONEY_CONSUMPTION = 10;
    public static final int TIER1_PROCESS_TIME = 100;
    public static final int TIER1_TANK_CAPACITY = 8000;

    public static final int TIER2_HONEY_CONSUMPTION = 15;
    public static final int TIER2_PROCESS_TIME = 60;
    public static final int TIER2_TANK_CAPACITY = 16000;

    public static final int TIER3_HONEY_CONSUMPTION = 25;
    public static final int TIER3_PROCESS_TIME = 30;
    public static final int TIER3_TANK_CAPACITY = 32000;

    private final int honeyConsumption;
    private final int baseFuelCapacity;
    private final int baseOutputCapacity;
    private final float processTimeMultiplier;

    // Suivi du type d'item en entree pour reset de progression
    private ItemStack previousInputType = ItemStack.EMPTY;

    // 1 slot d'entree
    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            currentRecipe = null;
            checkInputChanged();
        }
    };

    // 4 slots de sortie
    private final ItemStackHandler outputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final FluidTank fuelTank;
    private final FluidTank outputTank;

    private int progress = 0;
    private int currentProcessTime = TIER1_PROCESS_TIME;
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
        this(BeemancerBlockEntities.POWERED_CENTRIFUGE.get(), pos, state,
            TIER1_HONEY_CONSUMPTION, TIER1_TANK_CAPACITY, 1.0f);
    }

    public PoweredCentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        int honeyConsumption, int tankCapacity, float processTimeMultiplier) {
        super(type, pos, state);
        this.honeyConsumption = honeyConsumption;
        this.baseFuelCapacity = tankCapacity;
        this.baseOutputCapacity = tankCapacity;
        this.processTimeMultiplier = processTimeMultiplier;

        this.fuelTank = new FluidTank(tankCapacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get();
            }
            @Override
            protected void onContentsChanged() { setChanged(); }
        };

        this.outputTank = new FluidTank(tankCapacity) {
            @Override
            protected void onContentsChanged() { setChanged(); }
        };
    }

    // Factory methods for tiered versions
    public static PoweredCentrifugeBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new PoweredCentrifugeBlockEntity(
            BeemancerBlockEntities.POWERED_CENTRIFUGE_TIER2.get(), pos, state,
            TIER2_HONEY_CONSUMPTION, TIER2_TANK_CAPACITY, 0.6f);
    }

    public static PoweredCentrifugeBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new PoweredCentrifugeBlockEntity(
            BeemancerBlockEntities.POWERED_CENTRIFUGE_TIER3.get(), pos, state,
            TIER3_HONEY_CONSUMPTION, TIER3_TANK_CAPACITY, 0.3f);
    }

    /**
     * Verifie si l'item en entree a change de type ou est devenu vide.
     * Reset la progression si c'est le cas.
     */
    private void checkInputChanged() {
        ItemStack current = inputSlot.getStackInSlot(0);
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, previousInputType)) {
            progress = 0;
        }
        previousInputType = current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PoweredCentrifugeBlockEntity be) {
        boolean wasWorking = state.getValue(PoweredCentrifugeBlock.WORKING);
        boolean isWorking = false;

        // Find a valid recipe if we don't have one
        if (be.currentRecipe == null) {
            be.findValidRecipe(level);
        }

        if (be.currentRecipe != null) {
            // Verifier qu'on a de l'espace pour les outputs AVANT de consommer du fuel
            if (be.fuelTank.getFluidAmount() >= be.honeyConsumption && be.hasOutputSpace()) {
                be.fuelTank.drain(be.honeyConsumption, IFluidHandler.FluidAction.EXECUTE);
                be.progress++;
                isWorking = true;

                if (be.progress >= be.currentProcessTime) {
                    be.processItem(level.getRandom());
                    be.progress = 0;
                    be.currentRecipe = null;
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
        ItemStack stack = inputSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            currentRecipe = null;
            return;
        }

        ProcessingRecipeInput input = ProcessingRecipeInput.ofItem(stack);
        Optional<RecipeHolder<CentrifugeRecipe>> recipe = level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CENTRIFUGING.get(),
            input,
            level
        );

        if (recipe.isPresent()) {
            currentRecipe = recipe.get();
            currentProcessTime = Math.max(1, (int)(recipe.get().value().processingTime() * processTimeMultiplier));
        } else {
            currentRecipe = null;
        }
    }

    private void processItem(RandomSource random) {
        if (currentRecipe == null) return;

        CentrifugeRecipe recipe = currentRecipe.value();

        // Consume input
        inputSlot.extractItem(0, 1, false);

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
                outputSlots.setStackInSlot(i, stack.copy());
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack) &&
                       existing.getCount() < existing.getMaxStackSize()) {
                int toAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(toAdd);
                return;
            }
        }
    }

    /**
     * Verifie s'il y a au moins un slot de sortie disponible pour eviter de perdre des items
     */
    private boolean hasOutputSpace() {
        // Verifier l'espace pour les fluides
        if (currentRecipe != null) {
            FluidStack fluidOutput = currentRecipe.value().getFluidOutput();
            if (!fluidOutput.isEmpty()) {
                int spaceInTank = outputTank.getCapacity() - outputTank.getFluidAmount();
                if (spaceInTank < fluidOutput.getAmount()) {
                    return false;
                }
            }
        }

        // Verifier l'espace pour les items (au moins un slot vide ou non-plein)
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack existing = outputSlots.getStackInSlot(i);
            if (existing.isEmpty() || existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public FluidTank getFuelTank() { return fuelTank; }
    public FluidTank getOutputTank() { return outputTank; }
    public ItemStackHandler getInputSlot() { return inputSlot; }
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
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        progress = tag.getInt("Progress");
    }
}
