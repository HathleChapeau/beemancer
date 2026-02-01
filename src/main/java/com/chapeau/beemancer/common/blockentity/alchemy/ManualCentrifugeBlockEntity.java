/**
 * ============================================================
 * [ManualCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse manuelle
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - 1 slot d'entree + 4 slots de sortie
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

public class ManualCentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    private static final int PROCESS_TIME = 60; // Ticks to process (3 seconds of holding)

    // Suivi du type d'item en entree pour reset de progression
    private ItemStack previousInputType = ItemStack.EMPTY;

    // 1 slot d'entree
    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            checkInputChanged();
        }
    };

    // 4 slots de sortie
    private final ItemStackHandler outputSlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManualCentrifugeBlockEntity be) {
        int currentTick = (int) level.getGameTime();

        // Advance progress each tick while cranking (within 10 ticks of last interaction)
        if (be.isSpinning && be.hasInputToProcess() && (currentTick - be.lastInteractionTick) <= 10) {
            // Only advance if a valid recipe exists for the current input
            if (be.findRecipe() != null) {
                be.progress++;
                if (be.progress >= PROCESS_TIME) {
                    be.processInput();
                    be.progress = 0;
                }
            }
        } else if (be.isSpinning && (currentTick - be.lastInteractionTick) > 10) {
            be.isSpinning = false;
        }

        // Update block state
        boolean blockSpinning = state.getValue(ManualCentrifugeBlock.SPINNING);
        if (blockSpinning != be.isSpinning) {
            level.setBlock(pos, state.setValue(ManualCentrifugeBlock.SPINNING, be.isSpinning), 3);
        }

        be.setChanged();
    }

    /**
     * Called by CrankBlock when player holds right-click on the crank.
     * Registers the interaction timestamp; serverTick advances progress.
     */
    public boolean onPlayerSpin(Level level) {
        if (!hasInputToProcess()) return false;

        lastInteractionTick = (int) level.getGameTime();
        isSpinning = true;
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
        return !stack.isEmpty() && level != null && isValidComb(stack, level);
    }

    public boolean hasInputToProcess() {
        return !inputSlot.getStackInSlot(0).isEmpty();
    }

    /**
     * Alias pour compatibilite avec ManualCentrifugeBlock
     */
    public boolean hasCombsToProcess() {
        return hasInputToProcess();
    }

    /**
     * Verifie si on peut inserer un comb dans le slot d'entree
     */
    public boolean canInsertComb() {
        ItemStack current = inputSlot.getStackInSlot(0);
        return current.isEmpty() || current.getCount() < current.getMaxStackSize();
    }

    /**
     * Insere un comb dans le slot d'entree
     */
    public void insertComb(ItemStack comb) {
        ItemStack current = inputSlot.getStackInSlot(0);
        if (current.isEmpty()) {
            inputSlot.setStackInSlot(0, comb.copy());
        } else if (ItemStack.isSameItemSameComponents(current, comb)) {
            current.grow(comb.getCount());
        }
        setChanged();
    }

    /**
     * Extrait un item de sortie
     */
    public ItemStack extractOutput() {
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack stack = outputSlots.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack extracted = outputSlots.extractItem(i, stack.getCount(), false);
                setChanged();
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Recherche une recette de centrifugation pour l'item en entree.
     */
    private CentrifugeRecipe findRecipe() {
        if (level == null) return null;

        ItemStack input = inputSlot.getStackInSlot(0);
        if (input.isEmpty()) return null;

        ProcessingRecipeInput recipeInput = ProcessingRecipeInput.ofItem(input);
        return level.getRecipeManager().getRecipeFor(
            BeemancerRecipeTypes.CENTRIFUGING.get(),
            recipeInput,
            level
        ).map(RecipeHolder::value).orElse(null);
    }

    /**
     * Verifie qu'il y a de la place pour les outputs (au moins 1 slot libre ou non-plein).
     */
    private boolean hasOutputSpace() {
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack existing = outputSlots.getStackInSlot(i);
            if (existing.isEmpty() || existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void processInput() {
        CentrifugeRecipe recipe = findRecipe();
        if (recipe == null) return;

        // Check output space before consuming input
        if (!hasOutputSpace() && !recipe.results().isEmpty()) return;

        // Consume 1 input item
        inputSlot.extractItem(0, 1, false);

        // Produce fluid output
        FluidStack fluidOutput = recipe.getFluidOutput();
        if (!fluidOutput.isEmpty()) {
            fluidTank.fill(fluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }

        // Produce item outputs with probabilities
        for (ProcessingOutput output : recipe.results()) {
            output.roll(level.getRandom()).ifPresent(this::addOutput);
        }

        setChanged();
    }

    private void addOutput(ItemStack stack) {
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

    public ItemStackHandler getInputSlot() { return inputSlot; }
    public ItemStackHandler getOutputSlots() { return outputSlots; }
    public FluidTank getFluidTank() { return fluidTank; }
    public int getProgress() { return progress; }
    public int getProcessTime() { return PROCESS_TIME; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        ItemStack input = inputSlot.getStackInSlot(0);
        if (!input.isEmpty()) drops.add(input);
        for (int i = 0; i < outputSlots.getSlots(); i++) {
            ItemStack output = outputSlots.getStackInSlot(i);
            if (!output.isEmpty()) drops.add(output);
        }
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
        tag.put("Input", inputSlot.serializeNBT(registries));
        tag.put("Output", outputSlots.serializeNBT(registries));
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Input")) {
            inputSlot.deserializeNBT(registries, tag.getCompound("Input"));
        }
        if (tag.contains("Output")) {
            outputSlots.deserializeNBT(registries, tag.getCompound("Output"));
        }
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
        progress = tag.getInt("Progress");
    }
}
