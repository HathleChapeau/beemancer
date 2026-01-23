/**
 * ============================================================
 * [AlembicBlockEntity.java]
 * Description: Distille le Nectar en Nectar purifié
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.AlembicBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class AlembicBlockEntity extends BlockEntity {
    private static final int DISTILL_RATE = 2; // mB per tick
    private static final int RESIDUE_CHANCE = 20; // 1 in 20 chance per 100mB

    private final FluidTank inputTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
        }
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final FluidTank outputTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() { setChanged(); }
    };

    private final ItemStackHandler residueSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int processedAmount = 0;

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.ALEMBIC.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity be) {
        boolean wasDistilling = state.getValue(AlembicBlock.DISTILLING);
        boolean isDistilling = false;

        if (be.canDistill()) {
            FluidStack drained = be.inputTank.drain(DISTILL_RATE, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                // Output is the same nectar (purified)
                be.outputTank.fill(new FluidStack(BeemancerFluids.NECTAR_SOURCE.get(), drained.getAmount()),
                    IFluidHandler.FluidAction.EXECUTE);
                be.processedAmount += drained.getAmount();
                isDistilling = true;

                // Chance to produce residue
                if (be.processedAmount >= 100) {
                    be.processedAmount -= 100;
                    if (level.random.nextInt(RESIDUE_CHANCE) == 0) {
                        ItemStack existing = be.residueSlot.getStackInSlot(0);
                        if (existing.isEmpty()) {
                            be.residueSlot.setStackInSlot(0, new ItemStack(BeemancerItems.PROPOLIS.get()));
                        } else if (existing.getCount() < existing.getMaxStackSize()) {
                            existing.grow(1);
                        }
                    }
                }
            }
        }

        // Auto-output
        if (be.outputTank.getFluidAmount() > 0) {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
            if (cap != null) {
                FluidStack toTransfer = be.outputTank.drain(100, IFluidHandler.FluidAction.SIMULATE);
                int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) be.outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        if (wasDistilling != isDistilling) {
            level.setBlock(pos, state.setValue(AlembicBlock.DISTILLING, isDistilling), 3);
        }
    }

    private boolean canDistill() {
        return inputTank.getFluidAmount() > 0 && outputTank.getFluidAmount() < outputTank.getCapacity();
    }

    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Output", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("Residue", residueSlot.serializeNBT(registries));
        tag.putInt("Processed", processedAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(registries, tag.getCompound("Input"));
        outputTank.readFromNBT(registries, tag.getCompound("Output"));
        residueSlot.deserializeNBT(registries, tag.getCompound("Residue"));
        processedAmount = tag.getInt("Processed");
    }
}
