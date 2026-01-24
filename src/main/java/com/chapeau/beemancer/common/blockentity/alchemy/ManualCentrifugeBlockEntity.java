/**
 * ============================================================
 * [ManualCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse manuelle
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Accepte les combs definis par recettes JSON
 * - Le joueur doit maintenir clic droit pour faire tourner
 * - Outputs avec probabilites definies par recette
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.ManualCentrifugeBlock;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
import com.chapeau.beemancer.core.recipe.BeemancerRecipeTypes;
import com.chapeau.beemancer.core.recipe.ProcessingOutput;
import com.chapeau.beemancer.core.recipe.ProcessingRecipeInput;
import com.chapeau.beemancer.core.recipe.type.CentrifugeRecipe;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.Optional;

public class ManualCentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_COMBS = 4;
    private static final int PROCESS_TIME = 60; // Ticks to process (3 seconds of holding)

    private final NonNullList<ItemStack> combStorage = NonNullList.withSize(MAX_COMBS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> outputStorage = NonNullList.withSize(4, ItemStack.EMPTY);
    
    private final FluidTank fluidTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int progress = 0;
    private int lastInteractionTick = 0;
    private boolean isSpinning = false;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> fluidTank.getFluidAmount();
                case 2 -> PROCESS_TIME;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }
        @Override
        public int getCount() { return 3; }
    };

    public ManualCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MANUAL_CENTRIFUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManualCentrifugeBlockEntity be) {
        boolean wasSpinning = state.getValue(ManualCentrifugeBlock.SPINNING);
        
        // Check if player stopped holding (no interaction for 5 ticks)
        int currentTick = (int) level.getGameTime();
        if (be.isSpinning && (currentTick - be.lastInteractionTick) > 5) {
            be.isSpinning = false;
            be.progress = Math.max(0, be.progress - 2); // Slowly lose progress if not holding
        }

        // Update block state
        if (wasSpinning != be.isSpinning) {
            level.setBlock(pos, state.setValue(ManualCentrifugeBlock.SPINNING, be.isSpinning), 3);
        }

        be.setChanged();
    }

    /**
     * Called when player holds right-click on the centrifuge
     * @return true if progress was made
     */
    public boolean onPlayerSpin(Level level) {
        if (!hasCombsToProcess()) return false;

        lastInteractionTick = (int) level.getGameTime();
        isSpinning = true;
        progress++;

        if (progress >= PROCESS_TIME) {
            processAllCombs();
            progress = 0;
            isSpinning = false;
            return true;
        }

        return true;
    }

    public boolean isValidComb(ItemStack stack, Level level) {
        if (stack.isEmpty() || level == null) return false;
        ProcessingRecipeInput input = ProcessingRecipeInput.ofItem(stack);
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CENTRIFUGING.get(),
            input,
            level
        ).isPresent();
    }

    public boolean isValidComb(ItemStack stack) {
        // Fallback for when level is not available
        return !stack.isEmpty() && level != null && isValidComb(stack, level);
    }

    public boolean canInsertComb() {
        for (ItemStack stack : combStorage) {
            if (stack.isEmpty()) return true;
        }
        return false;
    }

    public void insertComb(ItemStack comb) {
        for (int i = 0; i < combStorage.size(); i++) {
            if (combStorage.get(i).isEmpty()) {
                combStorage.set(i, comb);
                setChanged();
                return;
            }
        }
    }

    public boolean hasCombsToProcess() {
        for (ItemStack stack : combStorage) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    private void processAllCombs() {
        if (level == null) return;

        for (int i = 0; i < combStorage.size(); i++) {
            ItemStack comb = combStorage.get(i);
            if (comb.isEmpty()) continue;

            processComb(comb);
            combStorage.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void processComb(ItemStack comb) {
        if (level == null) return;

        ProcessingRecipeInput input = ProcessingRecipeInput.ofItem(comb);
        Optional<RecipeHolder<CentrifugeRecipe>> recipeHolder = level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CENTRIFUGING.get(),
            input,
            level
        );

        if (recipeHolder.isEmpty()) return;

        CentrifugeRecipe recipe = recipeHolder.get().value();

        // Produce fluid output
        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            fluidTank.fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }

        // Produce item outputs with probabilities
        for (ProcessingOutput output : recipe.results()) {
            output.roll(level.getRandom()).ifPresent(this::addOutput);
        }
    }

    private void addOutput(ItemStack stack) {
        for (int i = 0; i < outputStorage.size(); i++) {
            ItemStack existing = outputStorage.get(i);
            if (existing.isEmpty()) {
                outputStorage.set(i, stack);
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                return;
            }
        }
    }

    public ItemStack extractOutput() {
        for (int i = 0; i < outputStorage.size(); i++) {
            ItemStack stack = outputStorage.get(i);
            if (!stack.isEmpty()) {
                outputStorage.set(i, ItemStack.EMPTY);
                setChanged();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public FluidTank getFluidTank() { return fluidTank; }
    public int getProgress() { return progress; }
    public int getProcessTime() { return PROCESS_TIME; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        drops.addAll(combStorage);
        drops.addAll(outputStorage);
        return drops;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.manual_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ManualCentrifugeMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        CompoundTag combsTag = new CompoundTag();
        ContainerHelper.saveAllItems(combsTag, combStorage, registries);
        tag.put("Combs", combsTag);

        CompoundTag outputTag = new CompoundTag();
        ContainerHelper.saveAllItems(outputTag, outputStorage, registries);
        tag.put("Output", outputTag);

        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("Combs")) {
            ContainerHelper.loadAllItems(tag.getCompound("Combs"), combStorage, registries);
        }
        if (tag.contains("Output")) {
            ContainerHelper.loadAllItems(tag.getCompound("Output"), outputStorage, registries);
        }
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
        progress = tag.getInt("Progress");
    }
}
