/**
 * ============================================================
 * [ManualCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse manuelle
 * ============================================================
 * 
 * FONCTIONNEMENT:
 * - Accepte les combs du mod (Common, Noble, Diligent, Royal)
 * - Le joueur doit maintenir clic droit pour faire tourner
 * - Chaque type de comb produit différents outputs
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.ManualCentrifugeBlock;
import com.chapeau.beemancer.common.menu.alchemy.ManualCentrifugeMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

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

    public boolean isValidComb(ItemStack stack) {
        Item item = stack.getItem();
        return item == BeemancerItems.COMMON_COMB.get()
            || item == BeemancerItems.NOBLE_COMB.get()
            || item == BeemancerItems.DILIGENT_COMB.get()
            || item == BeemancerItems.ROYAL_COMB.get();
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
        for (int i = 0; i < combStorage.size(); i++) {
            ItemStack comb = combStorage.get(i);
            if (comb.isEmpty()) continue;

            processComb(comb);
            combStorage.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void processComb(ItemStack comb) {
        Item item = comb.getItem();

        if (item == BeemancerItems.ROYAL_COMB.get()) {
            // Royal Comb -> Royal Jelly + Propolis (no honey!)
            FluidStack royalJelly = new FluidStack(BeemancerFluids.ROYAL_JELLY_SOURCE.get(), 250);
            fluidTank.fill(royalJelly, IFluidHandler.FluidAction.EXECUTE);
            addOutput(new ItemStack(BeemancerItems.PROPOLIS.get()));
            
        } else if (item == BeemancerItems.COMMON_COMB.get()) {
            // Common Comb -> 250mB Honey + Pollen
            FluidStack honey = new FluidStack(BeemancerFluids.HONEY_SOURCE.get(), 250);
            fluidTank.fill(honey, IFluidHandler.FluidAction.EXECUTE);
            addOutput(new ItemStack(BeemancerItems.POLLEN.get()));
            
        } else if (item == BeemancerItems.NOBLE_COMB.get()) {
            // Noble Comb -> 300mB Honey + Pollen + Beeswax
            FluidStack honey = new FluidStack(BeemancerFluids.HONEY_SOURCE.get(), 300);
            fluidTank.fill(honey, IFluidHandler.FluidAction.EXECUTE);
            addOutput(new ItemStack(BeemancerItems.POLLEN.get()));
            addOutput(new ItemStack(BeemancerItems.BEESWAX.get()));
            
        } else if (item == BeemancerItems.DILIGENT_COMB.get()) {
            // Diligent Comb -> 350mB Honey + Pollen + Propolis
            FluidStack honey = new FluidStack(BeemancerFluids.HONEY_SOURCE.get(), 350);
            fluidTank.fill(honey, IFluidHandler.FluidAction.EXECUTE);
            addOutput(new ItemStack(BeemancerItems.POLLEN.get()));
            addOutput(new ItemStack(BeemancerItems.PROPOLIS.get()));
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
