/**
 * ============================================================
 * [ManualCentrifugeBlockEntity.java]
 * Description: BlockEntity pour la centrifugeuse manuelle
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
    private static final int SPINS_REQUIRED = 5;
    private static final int HONEY_PER_COMB = 250;
    private static final int ROYAL_JELLY_PER_COMB = 100;

    private final NonNullList<ItemStack> combStorage = NonNullList.withSize(MAX_COMBS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> outputStorage = NonNullList.withSize(4, ItemStack.EMPTY);
    
    private final FluidTank fluidTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private int spinCount = 0;
    private int spinCooldown = 0;
    private boolean isProcessing = false;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> spinCount;
                case 1 -> fluidTank.getFluidAmount();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            if (index == 0) spinCount = value;
        }
        @Override
        public int getCount() { return 2; }
    };

    public ManualCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MANUAL_CENTRIFUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManualCentrifugeBlockEntity be) {
        boolean wasSpinning = state.getValue(ManualCentrifugeBlock.SPINNING);

        if (be.spinCooldown > 0) be.spinCooldown--;

        if (wasSpinning && be.spinCooldown <= 0 && !be.isProcessing) {
            level.setBlock(pos, state.setValue(ManualCentrifugeBlock.SPINNING, false), 3);
        }

        if (be.spinCount >= SPINS_REQUIRED && be.hasCombsToProcess()) {
            be.processCombs();
            be.spinCount = 0;
            be.isProcessing = false;
        }

        be.setChanged();
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

    public boolean canSpin() {
        return spinCooldown <= 0 && hasCombsToProcess();
    }

    public void spin() {
        if (canSpin()) {
            spinCount++;
            spinCooldown = 10;
            isProcessing = true;
            setChanged();
        }
    }

    private void processCombs() {
        for (int i = 0; i < combStorage.size(); i++) {
            ItemStack comb = combStorage.get(i);
            if (comb.isEmpty()) continue;

            boolean isRoyalComb = comb.is(BeemancerItems.ROYAL_COMB.get());

            if (isRoyalComb) {
                FluidStack royalJelly = new FluidStack(BeemancerFluids.ROYAL_JELLY_SOURCE.get(), ROYAL_JELLY_PER_COMB);
                fluidTank.fill(royalJelly, IFluidHandler.FluidAction.EXECUTE);
                addOutput(new ItemStack(BeemancerItems.PROPOLIS.get()));
            } else {
                FluidStack honey = new FluidStack(BeemancerFluids.HONEY_SOURCE.get(), HONEY_PER_COMB);
                fluidTank.fill(honey, IFluidHandler.FluidAction.EXECUTE);
                addOutput(new ItemStack(BeemancerItems.BEESWAX.get()));
            }

            combStorage.set(i, ItemStack.EMPTY);
        }
        setChanged();
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
        tag.putInt("SpinCount", spinCount);
        tag.putInt("SpinCooldown", spinCooldown);
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
        spinCount = tag.getInt("SpinCount");
        spinCooldown = tag.getInt("SpinCooldown");
    }
}
